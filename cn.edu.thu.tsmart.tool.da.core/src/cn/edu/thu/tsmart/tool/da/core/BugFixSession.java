package cn.edu.thu.tsmart.tool.da.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
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
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement.Result;

import cn.edu.thu.tsmart.tool.da.core.fl.BasicBlock;
import cn.edu.thu.tsmart.tool.da.core.fl.FaultLocalizer;
import cn.edu.thu.tsmart.tool.da.core.search.ExpressionGenerator;
import cn.edu.thu.tsmart.tool.da.core.search.Filter;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.GnrFixGenerator;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.GnrFixSiteManager;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.npe.NPEFixGenerator;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.npe.fs.NPEFixSiteManager;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSiteManager;
import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.Checkpoint;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointManager;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointUtils;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.ConditionItem;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.StatusCode;
import cn.edu.thu.tsmart.tool.da.tracer.CFGCache;
import cn.edu.thu.tsmart.tool.da.tracer.DynamicTranslator;
import cn.edu.thu.tsmart.tool.da.tracer.ITraceEventListener;
import cn.edu.thu.tsmart.tool.da.tracer.trace.InvokeTraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.TraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.util.FileUtils;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.util.config.AnalysisScopeReader;

public class BugFixSession {

	
	private int FIX_SESSION_TYPE = 1;
	public static final int GENERAL = 0;
	public static final int NPE = 1;
	
	public int getFixSessionType(){
		return this.FIX_SESSION_TYPE;
	}
	
	private static final String FL_RESULT_DUMP = "flresult.dump";
	private static final String TEST_RESULT_DUMP = "testresult.dump";
	private static final String BB_PROGRESS_DUMP = "bbprogress.dump";
	// TODO: test type might be a test suite. so we need to modify our code
	// accordingly.
	
	
	/**
	 * basic info
	 */
	private IJavaProject project;	
	private IType testsType;
	private ArrayList<IMethod> testMethods;
	
	// wala info
	private AnalysisScope scope;
	private ClassHierarchy cha;

	//private Map<TestCase, ArrayList<InvokeTraceNode>> traceMap = new HashMap<TestCase, ArrayList<InvokeTraceNode>>();

	/**
	 * trace and test cases
	 */
	private Map<TestCase, Boolean> testResultMap = new HashMap<TestCase, Boolean>();
	private Map<TestCase, String> testExceptionTraceMap = new HashMap<TestCase, String>();
	private Map<SSACFG.BasicBlock, ArrayList<TestCase>> coverageMap = new HashMap<SSACFG.BasicBlock, ArrayList<TestCase>>();
	private Map<BasicBlock, ArrayList<TestCase>> dumpCoverageMap = new HashMap<BasicBlock, ArrayList<TestCase>>();
	private Set<SSACFG.BasicBlock> failedCoveredBlocks = new HashSet<SSACFG.BasicBlock>();
	private ArrayList<BasicBlock> suspectList;
	
	private int bbProgress = 0;
	
	public int getBBProgress(){
		return this.bbProgress;
	}
	
	public void increaseBBProgress(){
		this.bbProgress ++;
	}
	
	private Map<TestCase, ILaunchConfiguration> launchConfigurationMap = new HashMap<TestCase, ILaunchConfiguration>();

	
	/**
	 * This is the mutual lock to manipulate breakpoints. both FixGenerator and
	 * Validator needs to install or delete their own breakpoints in order to
	 * halt the tested program, therefore only one of them can manipulate the
	 * breakpoints at the same time.
	 */
	private Object breakpointMutualLock = new Object();

	/**
	 * global controllers
	 */
	private SearchEngine fixer;
	private CandidateQueue candidateQueue;
	private Filter filter;
	private Logger logger;
	private ExpressionGenerator exprGenerator;
	private AbstractFixSiteManager fixSiteManager;
	
	public BugFixSession(IJavaProject project, IType testsType) {
		this.FIX_SESSION_TYPE = NPE;
		
		this.project = project;
		this.testsType = testsType;
		this.testMethods = EclipseUtils.getTestMethods(testsType);
		
		//init wala scope
		if(scope == null || cha == null){
			String projectClassPath;
			try {
				String projectPath = EclipseUtils.getProjectDir(project);
				File scopeFile = new File(projectPath + "bin-scope.txt");
				if(scopeFile.exists()){
					scope = AnalysisScopeReader.readJavaScope(projectPath + "bin-scope.txt", null, DynamicTranslator.class.getClassLoader());
				} else {
					projectClassPath = EclipseUtils.getProjectDir(project)
							+ "/bin/";
					scope = AnalysisScopeReader
							.makeJavaBinaryAnalysisScope(projectClassPath, null);
				}
				cha = ClassHierarchy.make(scope);
			} catch (JavaModelException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassHierarchyException e) {
				e.printStackTrace();
			}
		}
		
		LaunchConfigurationCreator creator = new LaunchConfigurationCreator();
		for (IMethod method : testMethods) {
			ILaunchConfiguration config = creator.createLaunchConfiguration(method);
			TestCase tc = new TestCase(method.getDeclaringType().getFullyQualifiedName(), method.getElementName());
			launchConfigurationMap.put(tc, config);
		}
		switch (FIX_SESSION_TYPE){
			case GENERAL:{
				this.fixSiteManager = new GnrFixSiteManager(this);
				this.fixer = new SearchEngine(this, new GnrFixGenerator(this));
				break;
			}
			case NPE: {
				this.fixSiteManager = new NPEFixSiteManager(this);
				this.fixer = new SearchEngine(this, new NPEFixGenerator(this));
				break;
			}
		}
		this.exprGenerator = new ExpressionGenerator(this);
		this.filter = new Filter(this);
		this.candidateQueue = new CandidateQueue();
		
		this.logger = new Logger(project.getProject().getName());
		this.logger.startLog();

	}
	
	public void clearUpCache(){
		this.fixSiteManager.clearCache();
		if(candidateQueue != null)
			candidateQueue.clearCache();
		CFGCache.clearCache();
		if(this.fixer != null)
			this.fixer.clearCache();
		
		this.bbProgress = 0;
		//traceMap = new HashMap<TestCase, ArrayList<InvokeTraceNode>>();
		testResultMap = new HashMap<TestCase, Boolean>();
		coverageMap = new HashMap<SSACFG.BasicBlock, ArrayList<TestCase>>();
		dumpCoverageMap = new HashMap<BasicBlock, ArrayList<TestCase>>();		
		failedCoveredBlocks = new HashSet<SSACFG.BasicBlock>();
	}

	
	/**
	 * LaunchConfigurations
	 * 
	 * @param tc
	 * @return
	 */
	public ILaunchConfiguration findLaunchConfiguration(TestCase tc) {
		for (TestCase testcase : launchConfigurationMap.keySet()) {
			if (testcase.equals(tc)) {
				return launchConfigurationMap.get(testcase);
			}
		}
		return null;
	}

	/**
	 * init all matrixes in this function, including: TestCase-> test result;
	 * BasicBlock -> TestCase; (the coverage information) trace blocks -> test
	 * result, and return it.
	 * 
	 * @return trace blocks -> test result
	 */

	private void mergeCoverageInfo(Set<BasicBlock> blockTrace, TestCase tc, boolean includeNew){
		for(BasicBlock bb: blockTrace) {
			if(!coverageMap.containsKey(bb.getSSABasicBlock()) && includeNew){
				ArrayList<TestCase> tcList = new ArrayList<TestCase>();
				tcList.add(tc);
				coverageMap.put(bb.getSSABasicBlock(), tcList);
				failedCoveredBlocks.add(bb.getSSABasicBlock());
			} else if(coverageMap.containsKey(bb.getSSABasicBlock())){
				ArrayList<TestCase> tcList = coverageMap.get(bb.getSSABasicBlock());
				tcList.add(tc);
			}
		}
	}
	
	private void toBlockTrace(InvokeTraceNode tr, Map<SSACFG.BasicBlock, BasicBlock> bbmap, Map<Integer, Set<BasicBlock>> blockBuckets) {
		ArrayList<TraceNode> traceNodes = tr.getCalleeTrace();
		for (TraceNode node : traceNodes) {
			ArrayList<SSACFG.BasicBlock> ssablocks = node.getBlockTrace();
			int timeStamp = node.getTimeStamp();
			if(!blockBuckets.containsKey(timeStamp)){
				blockBuckets.put(timeStamp, new HashSet<BasicBlock>());
			}
			if (ssablocks != null) {
				for (SSACFG.BasicBlock ssabb : ssablocks) {
					BasicBlock bb = bbmap.get(ssabb);
					if (bb == null) {
						bb = new BasicBlock(node.getMethodKey(), ssabb);
						bbmap.put(ssabb, bb);
					}
					blockBuckets.get(timeStamp).add(bb);
				}
			}
			ArrayList<InvokeTraceNode> calleeNodes = node.getCalleeList();
			if (calleeNodes != null) {
				for (InvokeTraceNode calleeNode : calleeNodes) {
					toBlockTrace(calleeNode, bbmap, blockBuckets);
				}
			}
		}		
	}
	
	private ArrayList<Set<BasicBlock>> toBlockTrace(ArrayList<InvokeTraceNode> trnodes, boolean testPassed){
		Map<SSACFG.BasicBlock, BasicBlock> bbmap = new HashMap<SSACFG.BasicBlock, BasicBlock>();
		Map<Integer, Set<BasicBlock>> blockBuckets = new HashMap<Integer, Set<BasicBlock>>();
		for(InvokeTraceNode tr: trnodes){
			toBlockTrace(tr, bbmap, blockBuckets);
		}
		
		int maxTimeStamp = -1;
		for(Integer stamp: blockBuckets.keySet()){
			if(stamp > maxTimeStamp)
				maxTimeStamp = stamp;
		}
		Set<BasicBlock> piece1 = new HashSet<BasicBlock>();
		Set<BasicBlock> piece2 = new HashSet<BasicBlock>();
		for(Integer stamp: blockBuckets.keySet()){
			Set<BasicBlock> blocks = blockBuckets.get(stamp);
			for(BasicBlock block: blocks){
				String className = block.getClassName().replaceAll("/", ".");
				if (!(className.equals(testsType.getFullyQualifiedName()))) {
					if(maxTimeStamp != 0 && stamp == maxTimeStamp ){
						piece2.add(block);
					} else
						piece1.add(block);
				}
			}
		}
		
		ArrayList<Set<BasicBlock>> traces = new ArrayList<Set<BasicBlock>>();
		if(!testPassed && piece2.size() == 0){
			traces.add(piece2);
			traces.add(piece1);
		} else {
			traces.add(piece1);
			traces.add(piece2);
		}
		return traces;
	}
	
	public void setSuspectList(ArrayList<BasicBlock> suspectList) {
		this.suspectList = suspectList;
	}

	public ArrayList<BasicBlock> getSuspectList() {
		return this.suspectList;
	}



	public ArrayList<IMethod> getTestMethods() {
		return this.testMethods;
	}
/*
	public void addTestTrace(TestCase tc, ArrayList<InvokeTraceNode> trace) {
		traceMap.put(tc, trace);
	}

	public Map<TestCase, ArrayList<InvokeTraceNode>> getTraceMap() {
		return traceMap;
	}

	public ArrayList<InvokeTraceNode> getTraceForTestCase(TestCase tc) {
		for (TestCase key : traceMap.keySet()) {
			if (key.getClassName().equals(tc.getClassName()) && key.getMethodName().equals(tc.getMethodName())) {
				return traceMap.get(key);
			}
		}
		return null;
	}
*/
	private void addTestResult(TestCase testcase, boolean result) {
		testResultMap.put(testcase, result);
	}

	public Map<TestCase, Boolean> getTestResultMap() {
		return this.testResultMap;
	}
	
	public String getExceptionInfo(){
		if(this.testExceptionTraceMap.size() >= 1){
			TestCase tc = testExceptionTraceMap.keySet().iterator().next();
			String exceptionInfo = testExceptionTraceMap.get(tc);
			return exceptionInfo;
		}
		
		return "";
	}
	
	public TestCase getExceptionTestCase(){
		if(this.testExceptionTraceMap.size() >= 1){
			TestCase tc = testExceptionTraceMap.keySet().iterator().next();
			return tc;
		}
		
		return null;
	}
	public boolean getTestResult(TestCase testCase) {
		for (TestCase key : testResultMap.keySet()) {
			if (key.getClassName().equals(testCase.getClassName())
					&& key.getMethodName().equals(testCase.getMethodName())) {
				return testResultMap.get(key);
			}
		}
		return false;
	}
	
	public TestCase getCoveringTestCase(BasicBlock block) {
		ArrayList<TestCase> tcList;
		if(this.coverageMap.size() != 0){
			tcList = this.coverageMap.get(block.getSSABasicBlock());
		} else {
			tcList = this.dumpCoverageMap.get(block);
		}
		
		for (TestCase tc : tcList) {
			Boolean result = this.testResultMap.get(tc);
			if (!result)
				return tc;
		}
		return null;
	}

	public ArrayList<TestCase> getPassCoveringTCs(BasicBlock block) {
		ArrayList<TestCase> tcList;
		if(this.coverageMap.size() != 0){
			tcList = this.coverageMap.get(block.getSSABasicBlock());
		} else {
			tcList = this.dumpCoverageMap.get(block);
		}
		ArrayList<TestCase> passTCs = new ArrayList<TestCase>();
		for (TestCase tc : tcList) {
			Boolean result = this.testResultMap.get(tc);
			if (result)
				passTCs.add(tc);
		}
		return passTCs;
	}
	
	public TestCase getFailCoveringTC(BasicBlock block){
		ArrayList<TestCase> tcList;
		if(this.coverageMap.size() != 0){
			tcList = this.coverageMap.get(block.getSSABasicBlock());
		} else {
			tcList = this.dumpCoverageMap.get(block);
		}
		for (TestCase tc : tcList) {
			Boolean result = this.testResultMap.get(tc);
			if (!result)
				return tc;
		}
		return null;
	}

	public IJavaProject getProject() {
		return this.project;
	}

	public IType getTestsType() {
		return testsType;
	}

	
	public ClassHierarchy getCHA(){
		return this.cha;
	}
	
	public AnalysisScope getAnalysisScope(){
		return this.scope;
	}
	
	public void setBugFixer(SearchEngine fixer) {
		this.fixer = fixer;
	}

	public SearchEngine getBugFixer() {
		return fixer;
	}

	public Logger getLogger(){
		return this.logger;
	}
	
	public Filter getFilter(){
		return this.filter;
	}
	public AbstractFixSiteManager getFixSiteManager() {
		return fixSiteManager;
	}

	public CandidateQueue getCandidateQueue() {
		return this.candidateQueue;
	}

	public ExpressionGenerator getExpressionGenerator() {
		return this.exprGenerator;
	}
	
	public Object getBreakpointMutualLock() {
		return this.breakpointMutualLock;
	}

	private boolean NEED_TO_STOP = false;
	public boolean needToStop() {
		
		return this.NEED_TO_STOP;
	}
	
	public void setNeedToStop(){
		this.NEED_TO_STOP = true;
	}

	private Checkpoint fixGoal;
	public Checkpoint getFixGoal() {
		return this.fixGoal;
	}
	
	public void setFixGoal(Checkpoint fixGoal){
		this.fixGoal = fixGoal;
	}

	private ArrayList<TestCase> correctTCs = null;
	private ArrayList<TestCase> failTCs = null;
	public void setAllTestResults(ArrayList<TestCase> correctTCs, ArrayList<TestCase> failTCs) {
		this.correctTCs = correctTCs;
		this.failTCs = failTCs;
	}
	
	public ArrayList<TestCase> getCorrectTCs(){
		return this.correctTCs;
	}
	
	public ArrayList<TestCase> getFailTCs(){
		return this.failTCs;
	}
	
	
	
	
	 
	public void initFixSession(SubProgressMonitor subPM) {
		updateDebugProcess();
		
		ArrayList<TestCase> passTCs = this.getCorrectTCs();
		ArrayList<TestCase> failTCs = this.getFailTCs();
		for(TestCase tc: passTCs){
			addTestResult(tc, true);
		}
		for(TestCase tc: failTCs){
			addTestResult(tc, false);
		}
			
		DynamicTranslator.initAnalysisScope(scope, cha);
		try{
			//collect traces for all test methods.
			
			ArrayList<IMethod> testMethods = this.testMethods;
			subPM.subTask("Initializing Fix Session...");
			subPM.beginTask("Initializing Fix Session...", testMethods.size() + 1);
			
			writeMethodNames(testMethods);
			
			//collect trace of failing methods:
			CFGCache.clearCache();
			FaultLocalizer localizer = new FaultLocalizer(this.logger);
			Checkpoint targetCP = this.fixGoal;
			if(targetCP != null){
				TestCase targetTC = targetCP.getOwnerTestCase();
				ArrayList<Set<BasicBlock>> traceBlocks = collectTrace(targetTC, true);
				Set<BasicBlock> correctTrace = traceBlocks.get(0);
				Set<BasicBlock> failTrace = traceBlocks.get(1);
				mergeCoverageInfo(failTrace, targetTC, true);
				mergeCoverageInfo(correctTrace, targetTC, true);
				
				localizer.mergeNewTrace(correctTrace, true);
				localizer.mergeNewTrace(failTrace, false);
				
				for(TestCase tc: failTCs){
					if(!tc.equals(targetTC)){
						ArrayList<Set<BasicBlock>> blocks = collectTrace(tc, false);
						Set<BasicBlock> correctT = blocks.get(0);
						Set<BasicBlock> failT = blocks.get(1);
						mergeCoverageInfo(correctT, targetTC, false);
						mergeCoverageInfo(failT, targetTC, false);
						localizer.mergeNewTrace(correctTrace, true);
						localizer.mergeNewTrace(failTrace, false);
					}
				}
				//updateInstrumentMethods();
				for(TestCase tc: passTCs){
					ArrayList<Set<BasicBlock>> blocks = collectTrace(tc, false);
					mergeCoverageInfo(blocks.get(0), tc, false);
					localizer.mergeNewTrace(blocks.get(0), true);
					System.gc();
				}
			}
			
			else {
				for(TestCase tc: failTCs){
					ArrayList<Set<BasicBlock>> traceBlocks = collectTrace(tc, true);
					mergeCoverageInfo(traceBlocks.get(1), tc, true);
					localizer.mergeNewTrace(traceBlocks.get(1), false);
					System.gc();
				}
				//updateInstrumentMethods();
				for(TestCase tc: passTCs){
					ArrayList<Set<BasicBlock>> traceBlocks = collectTrace(tc, false);
					mergeCoverageInfo(traceBlocks.get(0), tc, false);
					localizer.mergeNewTrace(traceBlocks.get(0), true);
					System.gc();
				}
			}
			
			ArrayList<BasicBlock> suspects = localizer.localize();
			setSuspectList(suspects);
		} finally{
			subPM.done();
		}
	}


	private ArrayList<Set<BasicBlock>> collectTrace(TestCase testCase, boolean includeNewBlocks) {
		
		ILaunchConfiguration config = findLaunchConfiguration(testCase);
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
			
			TraceAndTestResultListener listener = new TraceAndTestResultListener(config, bpcpMap, this.project, includeNewBlocks);
			
			
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
			
			if(listener.getExceptionTrace() != null){
				this.testExceptionTraceMap.put(testCase, listener.getExceptionTrace());
			}
			
			JUnitCore.removeTestRunListener(listener);
			JDIDebugModel.removeJavaBreakpointListener(listener);
			DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(listener);

			manager.removeBreakpoints(cpsArray, true);
			for(IBreakpoint bp: enabledBreakpoints){
				bp.setEnabled(true);
			}
			
			ArrayList<InvokeTraceNode> trace = listener.getTrace();
			ArrayList<Set<BasicBlock>> blockTraces = toBlockTrace(trace, testResult);
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
			projdir = this.project.getUnderlyingResource().getLocation().toOSString();
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
	 */
	 
	protected class TraceAndTestResultListener extends TestRunListener implements IJavaBreakpointListener, ILaunchesListener2{

		private DynamicTranslator translator;
		private ILaunchConfiguration config;
		private Map<IBreakpoint, Checkpoint> bpcpMap;
		protected boolean testcaseFailed = false;
		protected String exceptionTrace = null;
		
		private IJavaProject project;
		
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
			this.project = project;
			translator = new DynamicTranslator(project, includeNewBlocks);			
		}
		
		public ArrayList<InvokeTraceNode> getTrace(){
			return translator.getCurrentTrace();
		}
		
		public boolean junitTestCaseFailed(){
			return testcaseFailed;
		}
		
		public String getExceptionTrace(){
			return exceptionTrace;
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
				IJavaValue value = EclipseUtils.evaluateExpr(item.getHitCondition(), thread, (IJavaStackFrame)thread.getTopStackFrame(), project);
				if(value != null && value.toString().equals("true")){
					IJavaValue condValue = EclipseUtils.evaluateExpr(item.getExpectation(), thread, (IJavaStackFrame)thread.getTopStackFrame(), project);
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
				this.exceptionTrace = testCaseElement.getFailureTrace().getTrace();
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
		
		ArrayList<IMethod> testMethods = this.getTestMethods();
				
		for(IMethod testMethod: testMethods){
			String className = testMethod.getDeclaringType().getFullyQualifiedName();
			String methodName = testMethod.getElementName();
			TestCase testCase = new TestCase(className, methodName);
			ILaunchConfiguration config = this.findLaunchConfiguration(testCase);
			
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
		this.setAllTestResults(correctTCs, failTCs);
	}

}


class LaunchConfigurationCreator extends JUnitLaunchShortcut {
	public ILaunchConfiguration createLaunchConfiguration(IMethod method) {
		try {
			return super.createLaunchConfiguration(method);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}
}