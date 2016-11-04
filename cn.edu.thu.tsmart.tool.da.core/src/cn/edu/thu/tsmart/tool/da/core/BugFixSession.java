package cn.edu.thu.tsmart.tool.da.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;

import cn.edu.thu.tsmart.tool.da.core.fl.BasicBlock;
import cn.edu.thu.tsmart.tool.da.core.search.fixSite.FixSiteManager;
import cn.edu.thu.tsmart.tool.da.core.search.flt.Filter;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.ExpressionGenerator;
import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.Checkpoint;
import cn.edu.thu.tsmart.tool.da.tracer.trace.InvokeTraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.TraceNode;

import com.ibm.wala.ssa.SSACFG;

public class BugFixSession {

	private IJavaProject project;
	
	private static final String FL_RESULT_DUMP = "flresult.dump";
	private static final String TEST_RESULT_DUMP = "testresult.dump";
	private static final String BB_PROGRESS_DUMP = "bbprogress.dump";
	// TODO: test type might be a test suite. so we need to modify our code
	// accordingly.
	
	private IType testsType;
	private ArrayList<IMethod> testMethods;

	private FixSiteManager fixSiteManager;
	
	private Map<TestCase, ArrayList<InvokeTraceNode>> traceMap = new HashMap<TestCase, ArrayList<InvokeTraceNode>>();

	private Map<TestCase, Boolean> testResultMap = new HashMap<TestCase, Boolean>();

	private Map<SSACFG.BasicBlock, ArrayList<TestCase>> coverageMap = new HashMap<SSACFG.BasicBlock, ArrayList<TestCase>>();
	private Map<BasicBlock, ArrayList<TestCase>> dumpCoverageMap = new HashMap<BasicBlock, ArrayList<TestCase>>();
	
	private Set<SSACFG.BasicBlock> failedCoveredBlocks = new HashSet<SSACFG.BasicBlock>();

	private ArrayList<BasicBlock> suspectList;
	
	private int bbProgress = 0;
	private boolean NEED_TO_STOP = false;
	
	private Map<TestCase, ILaunchConfiguration> launchConfigurationMap = new HashMap<TestCase, ILaunchConfiguration>();

	public ExpressionGenerator getExpressionGenerator() {
		return this.exprGenerator;
	}
	/**
	 * This is the mutual lock to manipulate breakpoints. both FixGenerator and
	 * Validator needs to install or delete their own breakpoints in order to
	 * halt the tested program, therefore only one of them can manipulate the
	 * breakpoints at the same time.
	 */
	private Object breakpointMutualLock = new Object();

	private BugFixer fixer;
	private CandidateQueue candidateQueue;
	private Filter filter;
	private Logger logger;
	private ExpressionGenerator exprGenerator;

	public BugFixSession(IJavaProject project, IType testsType) {
		this.project = project;
		this.testsType = testsType;
		this.testMethods = EclipseUtils.getTestMethods(testsType);
		this.fixSiteManager = new FixSiteManager(this);
		this.fixer = new BugFixer(this);
		this.exprGenerator = new ExpressionGenerator(this);
		this.candidateQueue = new CandidateQueue();
		this.logger = new Logger(project.getProject().getName());
		this.logger.startLog();

		LaunchConfigurationCreator creator = new LaunchConfigurationCreator();
		for (IMethod method : testMethods) {
			ILaunchConfiguration config = creator.createLaunchConfiguration(method);
			TestCase tc = new TestCase(method.getDeclaringType().getFullyQualifiedName(), method.getElementName());
			launchConfigurationMap.put(tc, config);
		}
		this.filter = new Filter(this);
	}
	

	public int getBBProgress(){
		return this.bbProgress;
	}
	
	public void increaseBBProgress(){
		this.bbProgress ++;
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

	public void mergeCoverageInfo(Set<BasicBlock> blockTrace, TestCase tc, boolean includeNew){
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
	
	public ArrayList<Set<BasicBlock>> toBlockTrace(ArrayList<InvokeTraceNode> trnodes){
		Map<SSACFG.BasicBlock, BasicBlock> bbmap = new HashMap<SSACFG.BasicBlock, BasicBlock>();
		Map<Integer, Set<BasicBlock>> blockBuckets = new HashMap<Integer, Set<BasicBlock>>();
		for(InvokeTraceNode tr: trnodes){
			toBlockTrace(tr, bbmap, blockBuckets);
		}
		
		int maxTimeStamp = -1;
		for(Integer stamp: blockBuckets.keySet()){
			if(stamp > maxTimeStamp)
				stamp = maxTimeStamp;
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
		traces.add(piece1);
		traces.add(piece2);
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

	public void addTestResult(TestCase testcase, boolean result) {
		testResultMap.put(testcase, result);
	}

	public Map<TestCase, Boolean> getTestResultMap() {
		return this.testResultMap;
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

	public IJavaProject getProject() {
		return this.project;
	}

	public IType getTestsType() {
		return testsType;
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

	public void setBugFixer(BugFixer fixer) {
		this.fixer = fixer;
	}

	public BugFixer getBugFixer() {
		return fixer;
	}

	public Logger getLogger(){
		return this.logger;
	}
	
	public Filter getFilter(){
		return this.filter;
	}
	public FixSiteManager getFixSiteManager() {
		return fixSiteManager;
	}

	public CandidateQueue getCandidateQueue() {
		return this.candidateQueue;
	}

	public Object getBreakpointMutualLock() {
		return this.breakpointMutualLock;
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
	
}

/**
 * super.createLaunchConfiguration 是 protected 的! 必须包一层...
 */
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