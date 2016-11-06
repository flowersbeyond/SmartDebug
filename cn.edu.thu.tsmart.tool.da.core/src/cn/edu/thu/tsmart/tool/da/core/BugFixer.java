package cn.edu.thu.tsmart.tool.da.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaBreakpointListener;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement.Result;

import cn.edu.thu.tsmart.tool.da.core.fl.BasicBlock;
import cn.edu.thu.tsmart.tool.da.core.fl.FaultLocalizer;
import cn.edu.thu.tsmart.tool.da.core.search.SearchEngine;
import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;
import cn.edu.thu.tsmart.tool.da.core.validator.FixValidator;
import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.Checkpoint;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointManager;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointUtils;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.ConditionItem;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.StatusCode;
import cn.edu.thu.tsmart.tool.da.tracer.CFGCache;
import cn.edu.thu.tsmart.tool.da.tracer.DynamicTranslator;
//import cn.edu.thu.tsmart.tool.da.tracer.ITraceEventAllDoneListener;
import cn.edu.thu.tsmart.tool.da.tracer.ITraceEventListener;
import cn.edu.thu.tsmart.tool.da.tracer.trace.InvokeTraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.util.FileUtils;

public class BugFixer extends Job{
	private static String NAME = "Resolve Bug Job";
	private BugFixSession session;
	
	private SearchEngine fixGenerator;
	private FixValidator fixValidator;
	
	public BugFixer(BugFixSession session){
		super(BugFixer.NAME);
		setUser(true);
		this.session = session;
	}
	
	
	private void initFixSession(SubProgressMonitor subPM) {
		
		//if(!CheckpointManager.getInstance().isInSync()){
		updateDebugProcess();
		//}
		
		ArrayList<TestCase> passTCs = session.getCorrectTCs();
		ArrayList<TestCase> failTCs = session.getFailTCs();
		for(TestCase tc: passTCs){
			session.addTestResult(tc, true);
		}
		for(TestCase tc: failTCs){
			session.addTestResult(tc, false);
		}
			
		DynamicTranslator.clearAnalysisScope();
		try{
			//collect traces for all test methods.
			
			ArrayList<IMethod> testMethods = session.getTestMethods();
			subPM.subTask("Initializing Fix Session...");
			subPM.beginTask("Initializing Fix Session...", testMethods.size() + 1);
			
			writeMethodNames(testMethods);
			
			//collect trace of failing methods:
			CFGCache.clearCache();
			FaultLocalizer localizer = new FaultLocalizer(session.getLogger());
			Checkpoint targetCP = session.getFixGoal();
			if(targetCP != null){
				TestCase targetTC = targetCP.getOwnerTestCase();
				ArrayList<Set<BasicBlock>> traceBlocks = collectTrace(targetTC, true);
				Set<BasicBlock> correctTrace = traceBlocks.get(0);
				Set<BasicBlock> failTrace = traceBlocks.get(1);
				session.mergeCoverageInfo(failTrace, targetTC, true);
				session.mergeCoverageInfo(correctTrace, targetTC, true);
				
				localizer.mergeNewTrace(correctTrace, true);
				localizer.mergeNewTrace(failTrace, false);
				
				for(TestCase tc: failTCs){
					if(!tc.equals(targetTC)){
						ArrayList<Set<BasicBlock>> blocks = collectTrace(tc, false);
						Set<BasicBlock> correctT = blocks.get(0);
						Set<BasicBlock> failT = blocks.get(1);
						session.mergeCoverageInfo(correctT, targetTC, false);
						session.mergeCoverageInfo(failT, targetTC, false);
						localizer.mergeNewTrace(correctTrace, true);
						localizer.mergeNewTrace(failTrace, false);
					}
				}
				//updateInstrumentMethods();
				for(TestCase tc: passTCs){
					ArrayList<Set<BasicBlock>> blocks = collectTrace(tc, false);
					session.mergeCoverageInfo(blocks.get(0), tc, false);
					localizer.mergeNewTrace(blocks.get(0), true);
					System.gc();
				}
			}
			
			else {
				for(TestCase tc: failTCs){
					ArrayList<Set<BasicBlock>> traceBlocks = collectTrace(tc, true);
					session.mergeCoverageInfo(traceBlocks.get(1), tc, true);
					localizer.mergeNewTrace(traceBlocks.get(1), false);
					System.gc();
				}
				//updateInstrumentMethods();
				for(TestCase tc: passTCs){
					ArrayList<Set<BasicBlock>> traceBlocks = collectTrace(tc, false);
					session.mergeCoverageInfo(traceBlocks.get(0), tc, false);
					localizer.mergeNewTrace(traceBlocks.get(0), true);
					System.gc();
				}
			}
			
			ArrayList<BasicBlock> suspects = localizer.localize();
			session.setSuspectList(suspects);
		} finally{
			subPM.done();
		}
	}


	private ArrayList<Set<BasicBlock>> collectTrace(TestCase testCase, boolean includeNewBlocks) {
		
		ILaunchConfiguration config = session.findLaunchConfiguration(testCase);
		ArrayList<Checkpoint> cps = CheckpointManager.getInstance().getConditionForTestCase(testCase);
		if(cps == null)
			cps = new ArrayList<Checkpoint>();
		
		try{
			IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
			IBreakpoint[] bps = manager.getBreakpoints();
			ArrayList<IBreakpoint> enabledBreakpoints = new ArrayList<IBreakpoint>();
			for(IBreakpoint bp: bps){
				if(bp.isEnabled()){
					enabledBreakpoints.add(bp);
					bp.setEnabled(false);
				}					
			}
			
			IBreakpoint[] cpsArray = new IBreakpoint[cps.size()];
			Map<IBreakpoint, Checkpoint> bpcpMap = new HashMap<IBreakpoint, Checkpoint>();
			for(int i = 0; i < cps.size(); i ++){
				IBreakpoint bp = CheckpointUtils.createBreakpoint(cps.get(i));
				bpcpMap.put(bp, cps.get(i));
				cpsArray[i] = bp;
			}
			manager.addBreakpoints(cpsArray);
			
			TraceAndTestResultListener listener = new TraceAndTestResultListener(config, bpcpMap, session.getProject(), includeNewBlocks);
			
			
			JDIDebugModel.addJavaBreakpointListener(listener);
			DebugPlugin.getDefault().getLaunchManager().addLaunchListener(listener);
			JUnitCore.addTestRunListener(listener);
			
			Object lock = listener.getLock();
			synchronized (lock) {
				config.launch("sdtrace", new NullProgressMonitor());
				//DebugUIPlugin.launchInBackground(tcLaunchConfig, ILaunchManager.DEBUG_MODE);
				lock.wait();				
			}
			
			
			
			boolean testResult;
			if(!listener.junitTestCaseFailed()){
				testResult = true;
				System.out.println(config.toString() + ": passed");
			} else {
				testResult = false;
				System.out.println(config.toString() + ": failed");
				
			}
			
			JUnitCore.removeTestRunListener(listener);
			JDIDebugModel.removeJavaBreakpointListener(listener);
			DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(listener);

			manager.removeBreakpoints(cpsArray, true);
			for(IBreakpoint bp: enabledBreakpoints){
				bp.setEnabled(true);
			}
			
			ArrayList<InvokeTraceNode> trace = listener.getTrace();
			ArrayList<Set<BasicBlock>> blockTraces = session.toBlockTrace(trace, testResult);
			return blockTraces;
		} catch(CoreException e){
			return null;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private void writeMethodNames(ArrayList<IMethod> testMethods) {
		if(testMethods.size() == 0)
			return;
		IMethod method = testMethods.get(0);
		String className = method.getDeclaringType().getFullyQualifiedName();
		String projdir;
		
		try {
			projdir = session.getProject().getUnderlyingResource().getLocation().toOSString();
			String configFileDir = projdir + "/sdtrace/testcase-config";
			File configFile = FileUtils.ensureFile(configFileDir);
			BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));
			writer.write(className + "\n");
			writer.write(testMethods.size() + "\n");
			for(IMethod m: testMethods){
				writer.write(m.getElementName() + "\n");
			}
			writer.close();
			
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	protected class SimpleTestResultListener extends TestRunListener{
		private Object lock;
		boolean testcaseFailed;
		
		public SimpleTestResultListener(Object lock){
			this.lock = lock;
		}
		
		@Override
		public void testCaseFinished(ITestCaseElement testCaseElement) {
			Result result = testCaseElement.getTestResult(false);
			if(result.equals(Result.ERROR) || result.equals(Result.FAILURE)){
				this.testcaseFailed = true;
			}
			synchronized(lock){
				lock.notifyAll();
			}
		}
		
		public boolean getTestPassed(){
			return !testcaseFailed;
		}
		
	}

	/**
	 * This listener is responsible for:
	 * 1. update the checkpoint passing conditions of this test
	 * 2. collect the fault part and correct part of the execution trace
	 * 3. listen to the test case result
	 * @author Evelyn
	 *
	 */
	protected class TraceAndTestResultListener extends TestRunListener implements IJavaBreakpointListener, ILaunchesListener2{

		private DynamicTranslator translator;
		private ILaunchConfiguration config;
		private Map<IBreakpoint, Checkpoint> bpcpMap;
		protected boolean testcaseFailed = false;
		
		protected boolean TEST_FINISHED_FLAG = false;
		protected boolean LAUNCH_TERMINATED_FLAG = false;
		protected boolean CHECKPOINT_VIOLATED_FLAG = false;
		
		protected Object lock = new Object();
		
		public Object getLock() {
			return lock;
		}
		public TraceAndTestResultListener(ILaunchConfiguration config,
				Map<IBreakpoint, Checkpoint> bpcpMap, IJavaProject project, boolean includeNewBlocks) {
			this.config = config;
			this.bpcpMap = bpcpMap;
			translator = new DynamicTranslator(project, includeNewBlocks);			
		}
		
		public ArrayList<InvokeTraceNode> getTrace(){
			return translator.getCurrentTrace();
		}
		
		public boolean junitTestCaseFailed(){
			return testcaseFailed;
		}
		
		@Override
		public synchronized int breakpointHit(IJavaThread thread, IJavaBreakpoint breakpoint) {
			Checkpoint cp = bpcpMap.get(breakpoint);
			if(cp != null){
				try{
					IJavaStackFrame stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
					
					String className = stackFrame.getDeclaringTypeName();
					String methodName = stackFrame.getMethodName();
					int lineNumber = stackFrame.getLineNumber();
					
					translator.handleNewActions();
					int cpCondition = checkCheckpoint(cp, thread);
					
					if(cpCondition == 0){
						CHECKPOINT_VIOLATED_FLAG = true;
						this.testcaseFailed = true;
						
						//SuspendAction added to describe "Suspension" during a debug session
						translator.increaseTimeStamp();
						translator.setFailCPFound();
						//TrActionFactory.produceSuspendAction(className, methodName, lineNumber, cp, false);
						translator.fireTraceEvent(ITraceEventListener.LAUNCH_TERMINATED);
						thread.getLaunch().terminate();
					} else if (cpCondition == 1){
						//SuspendAction added to describe "Suspension" during a debug session
						translator.increaseTimeStamp();
					}
					
					return DONT_SUSPEND;
				}catch(DebugException e){
					e.printStackTrace();
					return DONT_SUSPEND;
				}				
			}
			return DONT_SUSPEND;
		}
		
		private int checkCheckpoint(Checkpoint cp, IJavaThread thread) throws DebugException{
			ArrayList<ConditionItem> items = cp.getConditions();
			boolean findValidCondition = false;
			for(ConditionItem item: items){
				IJavaValue value = EclipseUtils.evaluateExpr(item.getHitCondition(), thread, (IJavaStackFrame)thread.getTopStackFrame(), session.getProject());
				if(value != null && value.toString().equals("true")){
					IJavaValue condValue = EclipseUtils.evaluateExpr(item.getExpectation(), thread, (IJavaStackFrame)thread.getTopStackFrame(), session.getProject());
					if(condValue != null && condValue.toString().equals("false")){
						findValidCondition = true;
						return 0;
					}
					if(condValue != null && condValue.toString().equals("true")){
						findValidCondition = true;
					}
				}
			}
			
			if(findValidCondition)
				return 1;
			
			return -1;
		}
		
		@Override
		public synchronized void launchesTerminated(ILaunch[] launches) {
			for(int i = 0; i < launches.length; i ++){
				ILaunch launch = launches[i];
				if(launch.getLaunchConfiguration().equals(config)){
					
					this.LAUNCH_TERMINATED_FLAG = true;
					
					translator.handleNewActions();
					translator.fireTraceEvent(ITraceEventListener.LAUNCH_TERMINATED);
					if(TEST_FINISHED_FLAG || this.CHECKPOINT_VIOLATED_FLAG){
						synchronized(lock){
							lock.notifyAll();
						}
					}
				}
			}
			
		}
		
		@Override
		public synchronized void testCaseFinished(ITestCaseElement testCaseElement) {
			Result result = testCaseElement.getTestResult(false);
			if(result.equals(Result.ERROR) || result.equals(Result.FAILURE)){
				this.testcaseFailed = true;
			}
			TEST_FINISHED_FLAG = true;
			if(LAUNCH_TERMINATED_FLAG){
				synchronized(lock){
					lock.notifyAll();
				}
			}
		}

		@Override
		public void launchesRemoved(ILaunch[] launches) {}

		@Override
		public void launchesAdded(ILaunch[] launches) {}

		@Override
		public void launchesChanged(ILaunch[] launches) {}

		@Override
		public void addingBreakpoint(IJavaDebugTarget target,
				IJavaBreakpoint breakpoint) {}

		@Override
		public int installingBreakpoint(IJavaDebugTarget target,
				IJavaBreakpoint breakpoint, IJavaType type) { return 0; }

		@Override
		public void breakpointInstalled(IJavaDebugTarget target,
				IJavaBreakpoint breakpoint) {}

		@Override
		public void breakpointRemoved(IJavaDebugTarget target,
				IJavaBreakpoint breakpoint) {}

		@Override
		public void breakpointHasRuntimeException(
				IJavaLineBreakpoint breakpoint, DebugException exception) {}

		@Override
		public void breakpointHasCompilationErrors(
				IJavaLineBreakpoint breakpoint, Message[] errors) {}		
	}

	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final int ticks = 100000;
		int allworkload = 100000;
		monitor.beginTask("SmartDebugger Resolving Bugs...", ticks);
		try {
			// we just entered into a new fix session, everything needs to
			// be initialized
			session.getLogger().log(Logger.DATA_MODE, Logger.INIT, "initialize started...");
			initFixSession(new SubProgressMonitor(monitor, 5000));
			session.getLogger().log(Logger.DATA_MODE, Logger.INIT_FINISHED, "initialization finished...");
			
			
			if (fixGenerator == null) {
				fixGenerator = new SearchEngine(session);
				fixValidator = new FixValidator(session);
				allworkload = allworkload - 5000;
			}
			
			int allSuspiciousBlockNum = session.getSuspectList().size();
			session.getLogger().log(Logger.DATA_MODE, Logger.FL_TOTAL, allSuspiciousBlockNum + "" );
			
			int suspiciousBlockLeft = allSuspiciousBlockNum - session.getBBProgress();
			double eachBBWorkload = (double)allworkload / (double)suspiciousBlockLeft;
			
			session.getLogger().log(Logger.DATA_MODE, Logger.BEGIN_SEARCH, session.getBBProgress() + "");
			boolean shouldstop = false;
			while(session.getBBProgress() < allSuspiciousBlockNum){
				if(shouldstop)
					break;
				int newBBProgress = 0;
				int bbProgressCount = session.getBBProgress();
				for(int i = bbProgressCount; i < allSuspiciousBlockNum; i ++){
					BasicBlock bb = session.getSuspectList().get(i);
					
					session.getLogger().log(Logger.DATA_MODE, Logger.SEARCH_FIX, i + "");
					ArrayList<Fix> fixes = fixGenerator.searchFixes(bb);
					if(fixes != null){
						session.getCandidateQueue().appendNewFixes(fixes);
						session.getLogger().log(Logger.DATA_MODE, Logger.END_SEARCH_FIX, i + "");
					} else {
						session.getLogger().log(Logger.DATA_MODE, Logger.IGNORE_BB, i + "");
					}
					session.increaseBBProgress();
					
					if(session.getCandidateQueue().getSize() > 30){
						newBBProgress = i + 1 - bbProgressCount;
						break;
					}
					monitor.worked((int)(eachBBWorkload / 5));
					if(monitor.isCanceled()){
						return Status.CANCEL_STATUS;
					}							
				}
				
				
				double fixValidationProgressShare = eachBBWorkload * newBBProgress / 5 * 4;
				int eachValidationProgress = (int)(fixValidationProgressShare / session.getCandidateQueue().getSize());
				
				session.getLogger().log(Logger.DATA_MODE, "BEGIN_VALIDATE", session.getCandidateQueue().getSize() + "");
				while(session.getCandidateQueue().hasNextFix()){
					Fix fix = session.getCandidateQueue().getNextFix();
					boolean findFix = fixValidator.validate(fix);
					if(findFix){
						session.getLogger().log(Logger.DATA_MODE, Logger.FIX_DONE, "success");
						try {
							session.getProject().getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
						} catch (CoreException e) {
							e.printStackTrace();
						}
						return Status.OK_STATUS;
					}
					monitor.worked(eachValidationProgress);
					if(monitor.isCanceled()){
						try {
							session.getProject().getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
						} catch (CoreException e) {
							e.printStackTrace();
						}
						return Status.CANCEL_STATUS;
					}
				}
				try {
					session.getProject().getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
				} catch (CoreException e) {
					e.printStackTrace();
				}
				session.getLogger().log(Logger.DATA_MODE, "END_VALIDATE", "");
				if(monitor.isCanceled()){
					return Status.CANCEL_STATUS;
				}
			}

		} finally {
			monitor.done();
		}

		return Status.OK_STATUS;
	}

	public void stopAnalysis(){
		this.cancel();
	}


	public void updateDebugProcess() {
		IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
		IBreakpoint existingbps[] = manager.getBreakpoints();
		try {
			manager.removeBreakpoints(existingbps, true);
		} catch (CoreException e2) {
			e2.printStackTrace();
		}
		
		ArrayList<TestCase> correctTCs = new ArrayList<TestCase>();
		ArrayList<TestCase> failTCs = new ArrayList<TestCase>();
		
		ArrayList<IMethod> testMethods = session.getTestMethods();
				
		for(IMethod testMethod: testMethods){
			String className = testMethod.getDeclaringType().getFullyQualifiedName();
			String methodName = testMethod.getElementName();
			TestCase testCase = new TestCase(className, methodName);
			ILaunchConfiguration config = session.findLaunchConfiguration(testCase);
			
			Object lock = new Object();
			ArrayList<Checkpoint> cps = CheckpointManager.getInstance().getConditionForTestCase(testMethod);
			if(cps == null)
				cps = new ArrayList<Checkpoint>();
			IBreakpoint bps[] = new IBreakpoint[cps.size()];
			Map<IBreakpoint, Checkpoint> bpcpMap = new HashMap<IBreakpoint, Checkpoint>();
			for(int i = 0; i < cps.size(); i ++){
				bps[i] = CheckpointUtils.createBreakpoint(cps.get(i));
				bpcpMap.put(bps[i], cps.get(i));
			}
			CheckpointConditionRefresher listener = new CheckpointConditionRefresher(config, cps, bpcpMap, lock);
			
			try {				
				manager.addBreakpoints(bps);
			} catch (CoreException e1) {
				e1.printStackTrace();
			}
			
			JUnitCore.addTestRunListener(listener);
			DebugPlugin.getDefault().getLaunchManager().addLaunchListener(listener);
			JDIDebugModel.addJavaBreakpointListener(listener);
			synchronized (lock) {
				try {
					config.launch("debug", new NullProgressMonitor());
					lock.wait();
				} catch (CoreException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
							
			}
			
			try {
				manager.removeBreakpoints(bps, true);
			} catch (CoreException e1) {
				e1.printStackTrace();
			}
			
			JUnitCore.removeTestRunListener(listener);
			DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(listener);
			JDIDebugModel.removeJavaBreakpointListener(listener);
			
			if(listener.getTestPassed()){
				testCase.setStatus(StatusCode.PASSED);
				correctTCs.add(testCase);
			} else {
				if(cps.size() == 0){
					testCase.setStatus(StatusCode.FAILED);
					failTCs.add(testCase);
				}
				else {
					ConditionItem item = listener.getFailedExpectation();
					if(item == null){
						// condition items are out of date
						testCase.setStatus(StatusCode.OUT_OF_DATE);
					} else {
						failTCs.add(testCase);
						testCase.setStatus(StatusCode.FAILED);
					}
				
				}
			}
			
			CheckpointManager.getInstance().registerTestCase(testCase);
			
		}
		try {
			manager.addBreakpoints(existingbps);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CheckpointManager.getInstance().setSync();
		session.setAllTestResults(correctTCs, failTCs);
	}


	public void clearCache() {
		if(this.fixGenerator != null)
			this.fixGenerator.clearCache();
		if(this.fixValidator != null)
			this.fixValidator.clearCache();
	}
}
