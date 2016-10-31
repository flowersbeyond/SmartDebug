package cn.edu.thu.tsmart.tool.da.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement.Result;

import cn.edu.thu.tsmart.tool.da.core.fl.BasicBlock;
import cn.edu.thu.tsmart.tool.da.core.fl.FaultLocalizer;
import cn.edu.thu.tsmart.tool.da.core.search.SearchEngine;
import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;
import cn.edu.thu.tsmart.tool.da.core.validator.CheckpointListener;
import cn.edu.thu.tsmart.tool.da.core.validator.FixValidator;
import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.Checkpoint;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointManager;
import cn.edu.thu.tsmart.tool.da.tracer.CFGCache;
import cn.edu.thu.tsmart.tool.da.tracer.DynamicTranslator;
import cn.edu.thu.tsmart.tool.da.tracer.ITraceEventAllDoneListener;
//import cn.edu.thu.tsmart.tool.da.tracer.ITraceEventAllDoneListener;
import cn.edu.thu.tsmart.tool.da.tracer.ITraceEventListener;
import cn.edu.thu.tsmart.tool.da.tracer.trace.InvokeTraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.util.FileUtils;

public class BugFixer extends Job{
	private static String NAME = "Resolve Bug Job";
	private BugFixSession session;
	
	private SearchEngine fixGenerator;
	private FixValidator fixValidator;
	
	/**
	 * 模仿 ITraceEventListener 那一套
	 * */
	private static ITraceEventAllDoneListener registedTraceEventAllDoneListener;
	public static void registerTraceEventAllDoneListener(ITraceEventAllDoneListener l){
		registedTraceEventAllDoneListener=l;
	}
	
	
	public BugFixer(BugFixSession session){
		super(BugFixer.NAME);
		setUser(true);
		this.session = session;
	}
	
	
	private void initFixSession(boolean updateTraces, SubProgressMonitor subPM) {
		if(this.session.loadSession())
			return;
		
		FixGoalTable goalTable = new FixGoalTable();
		DynamicTranslator.clearAnalysisScope();
		try{
			//collect traces for all test methods.
			
			ArrayList<IMethod> testMethods = session.getTestMethods();
			subPM.subTask("Initializing Fix Session...");
			subPM.beginTask("Initializing Fix Session...", testMethods.size() + 1);
			
			writeMethodNames(testMethods);
			
			ArrayList<TestCase> passTCs = new ArrayList<TestCase>();
			ArrayList<TestCase> failTCs = new ArrayList<TestCase>();
			for(IMethod testMethod: testMethods){
				String className = testMethod.getDeclaringType().getFullyQualifiedName();
				String methodName = testMethod.getElementName();
				TestCase testCase = new TestCase(className, methodName);
				ILaunchConfiguration config = session.findLaunchConfiguration(testCase);
				
				Object lock = new Object();
				SimpleTestResultListener listener = new SimpleTestResultListener(lock);
				JUnitCore.addTestRunListener(listener);
				
				synchronized (lock) {
					try {
						config.launch("run", new NullProgressMonitor());
						lock.wait();
					} catch (CoreException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
								
				}
				
				boolean testResult = listener.getTestPassed();
				if(testResult){
					passTCs.add(testCase);					
				}
				else{
					failTCs.add(testCase);				
				}
				session.addTestResult(testCase, testResult);
				goalTable.registerFixGoal(testMethod, null, true);
			}
			
			//collect trace of failing methods:
			CFGCache.clearCache();
			FaultLocalizer localizer = new FaultLocalizer(session.getLogger());
			for(TestCase tc: failTCs){
				Set<BasicBlock> traceBlocks = collectTrace(tc, true);
				session.mergeCoverageInfo(traceBlocks, tc, true);
				localizer.mergeNewTrace(traceBlocks, false);
				System.gc();
			}
			//updateInstrumentMethods();
			for(TestCase tc: passTCs){
				Set<BasicBlock> traceBlocks = collectTrace(tc, false);
				session.mergeCoverageInfo(traceBlocks, tc, false);
				localizer.mergeNewTrace(traceBlocks, true);
				System.gc();
			}
			// when reading 2016-08-15:  
			session.setFixGoalTable(goalTable); 
			ArrayList<BasicBlock> suspects = localizer.localize();
			session.setSuspectList(suspects);
			session.dumpSession();
		} finally{
			subPM.done();
		}
	}


	private Set<BasicBlock> collectTrace(TestCase testCase, boolean includeNewBlocks) {
		
		ILaunchConfiguration config = session.findLaunchConfiguration(testCase);
		ArrayList<Checkpoint> cps = CheckpointManager.getInstance().getConditionForTestCase(testCase);
		if(cps == null)
			cps = new ArrayList<Checkpoint>();
		
		TraceAndTestResultListener listener = new TraceAndTestResultListener(config, cps, session.getProject(), includeNewBlocks);
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
			
			Checkpoint[] cpsArray = new Checkpoint[cps.size()];
			cpsArray = cps.toArray(cpsArray);
			manager.addBreakpoints(cpsArray);
			
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
			Checkpoint cp = listener.getFailedCheckpoint();
			if(cp == null && !listener.junitTestCaseFailed()){
				testResult = true;
				//goalTable.registerFixGoal(testMethod, null, true);
				System.out.println(config.toString() + ": passed");
			} else {
				//if(cp == null)
					//goalTable.registerFixGoal(testMethod, null, true);
				//else
					//goalTable.registerFixGoal(testMethod, cp, false);
				testResult = false;
				System.out.println(config.toString() + ": failed");
				
			}
			
		
			JUnitCore.removeTestRunListener(listener);
			JDIDebugModel.removeJavaBreakpointListener(listener);
			DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(listener);

			manager.removeBreakpoints(cpsArray, false);
			for(IBreakpoint bp: enabledBreakpoints){
				bp.setEnabled(true);
			}
			
			ArrayList<InvokeTraceNode> trace = listener.getTrace();
			Set<BasicBlock> blockTrace = session.toBlockTrace(trace);
			return blockTrace;
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

	protected class TraceAndTestResultListener extends CheckpointListener{

		private DynamicTranslator translator;
		public TraceAndTestResultListener(ILaunchConfiguration config,
				ArrayList<Checkpoint> cps, IJavaProject project, boolean includeNewBlocks) {
			super(config, cps);
			translator = new DynamicTranslator(project, includeNewBlocks);			
		}
		
		public ArrayList<InvokeTraceNode> getTrace(){
			return translator.getCurrentTrace();
		}
		
		@Override
		public synchronized int breakpointHit(IJavaThread thread, IJavaBreakpoint breakpoint) {
			if(breakpoint instanceof Checkpoint){
				Checkpoint cp = (Checkpoint)breakpoint;
								

				if(breakpoint instanceof Checkpoint && thread.getLaunch().getLaunchConfiguration().equals(config)){
					try{
						IJavaStackFrame stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
						
						String className = stackFrame.getDeclaringTypeName();
						String methodName = stackFrame.getMethodName();
						int lineNumber = stackFrame.getLineNumber();
						
						int result = checkCheckpoint(cp, thread);
						if(result == SUSPEND){
							failedCheckpoint = cp;
							CHECKPOINT_VIOLATED_FLAG = true;
							
							
							//SuspendAction added to describe "Suspension" during a debug session
							translator.addSuspendAction(className, methodName, "methodSignature", lineNumber);
							//TrActionFactory.produceSuspendAction(className, methodName, lineNumber, cp, false);
							
							translator.handleNewActions();
							translator.fireTraceEvent(ITraceEventListener.LAUNCH_TERMINATED);
							thread.getLaunch().terminate();
						} else{
							//SuspendAction added to describe "Suspension" during a debug session
							translator.addSuspendAction(className, methodName, "methodSignature", lineNumber);
							translator.handleNewActions();
							//TODO: check if this is correct? why do we need to fire a LAUNCH_TERMINATED_EVENT?
							translator.fireTraceEvent(ITraceEventListener.HIT_BREAKPOINT);
						}
						return result;
					}catch(DebugException e){
						e.printStackTrace();
						return DONT_CARE;
					}
					
					
				}				
				
			}
			return DONT_CARE;
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
	}

	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final int ticks = 100000;
		int allworkload = 100000;
		monitor.beginTask("SmartDebugger Resolving Bugs...", ticks);
		try {
			if (fixGenerator == null) {
				// we just entered into a new fix session, everything needs to
				// be initialized
				session.getLogger().log(Logger.DATA_MODE, Logger.INIT, "initialize started...");
				initFixSession(true, new SubProgressMonitor(monitor, 5000));
				session.getLogger().log(Logger.DATA_MODE, Logger.INIT_FINISHED, "initialization finished...");
				
				fixGenerator = new SearchEngine(session);
				fixValidator = new FixValidator(session);
				allworkload = allworkload - 5000;
			}
				// 2016-06-22 init 完成之后搞一个事件...
				if(registedTraceEventAllDoneListener != null){
					registedTraceEventAllDoneListener.handleEvent();
				}
				
				int allSuspiciousBlockNum = session.getSuspectList().size();
				session.getLogger().log(Logger.DATA_MODE, Logger.FL_TOTAL, allSuspiciousBlockNum + "" );
				
				int suspiciousBlockLeft = allSuspiciousBlockNum - session.getBBProgress();
				double eachBBWorkload = (double)allworkload / (double)suspiciousBlockLeft;
				
				session.getLogger().log(Logger.DATA_MODE, Logger.BEGIN_SEARCH, session.getBBProgress() + "");
				boolean shouldstop = false;
				while(session.getBBProgress() < allSuspiciousBlockNum){
//					IDebugEventSetListener listener = Synthesizer.getDebugEventListener();
//					if(listener != null){
//						DebugPlugin.getDefault().removeDebugEventListener(listener);
//					}
					if(shouldstop)
						break;
					int newBBProgress = 0;
					int bbProgressCount = session.getBBProgress();
					for(int i = bbProgressCount; i < allSuspiciousBlockNum; i ++){
						BasicBlock bb = session.getSuspectList().get(i);
//						if(!fixValidator.isProperBasicBlock(bb)){
//							session.increaseBBProgress();
//							continue;
//						}
						
							session.getLogger().log(Logger.DATA_MODE, Logger.SEARCH_FIX, i + "");
							ArrayList<Fix> fixes = fixGenerator.searchFixes(session.getSuspectList().get(i));
							if(fixes != null){
								session.getCandidateQueue().appendNewFixes(fixes);
								session.getLogger().log(Logger.DATA_MODE, Logger.END_SEARCH_FIX, i + "");
							} else {
								if(session.needToStop()){
									return Status.CANCEL_STATUS;
								}
								session.getLogger().log(Logger.DATA_MODE, Logger.IGNORE_BB, i + "");
							}
							session.increaseBBProgress();
							
							if(fixValidator.isProperBasicBlock(bb)){
								shouldstop = true;
								monitor.setCanceled(true);
								
							}
							if(fixValidator.isProperBasicBlock(bb) || session.getCandidateQueue().getSize() > 30){
								newBBProgress = i + 1 - bbProgressCount;
								break;
							}
							monitor.worked((int)(eachBBWorkload / 5));
							if(monitor.isCanceled()){
								return Status.CANCEL_STATUS;
							}
							
							
					}
					
					/*
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
						*/
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
}
