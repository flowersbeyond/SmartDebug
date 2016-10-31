package cn.edu.thu.tsmart.tool.da.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import cn.edu.thu.tsmart.tool.da.core.search.strategy.ExpressionRepositoryManager;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.MyExpressionGenerator;
import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointOnNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.InvokeTraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.TraceNode;

import com.ibm.wala.ssa.SSACFG;

public class BugFixSession {

	/**
	 * 1-1
	 */
	private IJavaProject project;
	
	private static final String FL_RESULT_DUMP = "flresult.dump";
	private static final String TEST_RESULT_DUMP = "testresult.dump";
	private static final String BB_PROGRESS_DUMP = "bbprogress.dump";
	// TODO: test type might be a test suite. so we need to modify our code
	// accordingly.
	/**
	 * 1-1 测试用例的类
	 */
	private IType testsType;
	private ArrayList<IMethod> testMethods;

	private FixSiteManager fixSiteManager;
	private ExpressionRepositoryManager exprRepoManager;

	private FixGoalTable fixGoal;
	/**
	 * 每个tracenode上的CP
	 */
	private Map<TraceNode, CheckpointOnNode> traceNodeCheckpointMap = new HashMap<>();
	/**
	 * 每个TC对应的trace
	 */
	private Map<TestCase, ArrayList<InvokeTraceNode>> traceMap = new HashMap<TestCase, ArrayList<InvokeTraceNode>>();
	/**
	 * 存测试结果 2016-07-05
	 */
	private Map<TestCase, Boolean> testResultMap = new HashMap<TestCase, Boolean>();
	/**
	 * FL时用
	 */
	private Map<SSACFG.BasicBlock, ArrayList<TestCase>> coverageMap = new HashMap<SSACFG.BasicBlock, ArrayList<TestCase>>();
	private Map<BasicBlock, ArrayList<TestCase>> dumpCoverageMap = new HashMap<BasicBlock, ArrayList<TestCase>>();
	
	private Set<SSACFG.BasicBlock> failedCoveredBlocks = new HashSet<SSACFG.BasicBlock>();
//	private Map<ArrayList<BasicBlock>, Boolean> problemMap;
	private ArrayList<BasicBlock> suspectList;
	
	private int bbProgress = 0;
	private boolean NEED_TO_STOP = false;
	/**
	 * testcase 对应的可launch的
	 */
	private Map<TestCase, ILaunchConfiguration> launchConfigurationMap = new HashMap<TestCase, ILaunchConfiguration>();

	public MyExpressionGenerator getExpressionGenerator() {
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
	private MyExpressionGenerator exprGenerator;

	public BugFixSession(IJavaProject project, IType testsType) {
		this.project = project;
		this.testsType = testsType;
		this.testMethods = TestCaseUtil.getTestMethods(testsType);
		this.fixSiteManager = new FixSiteManager(this);
		this.exprRepoManager = new ExpressionRepositoryManager(this);
		this.fixer = new BugFixer(this);
		this.exprGenerator = new MyExpressionGenerator(this);
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
	
	public void dumpSession(){
		String testresultFileName = project.getProject().getLocation().toOSString() + "/data/" + TEST_RESULT_DUMP;
		System.out.println("Dump session file:" + testresultFileName);
		File testresultFile = new File(testresultFileName);
		if(!testresultFile.exists()){
			try {
				testresultFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(testresultFile));
			writer.write("");
			writer.close();
			writer = new BufferedWriter(new FileWriter(testresultFile, true));
			for(TestCase tc: testResultMap.keySet()){
				Boolean result = testResultMap.get(tc);
				String str = tc.getClassName() + " " + tc.getMethodName() + " ";
				if(result)
					str += "1";
				else
					str += "0";
				writer.append(str);
				writer.newLine();
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String dumpFileName = project.getProject().getLocation().toOSString() + "/data/" + FL_RESULT_DUMP;
		System.out.println("Dump session file:" + dumpFileName);
		File dumpFile = new File(dumpFileName);
		if(!dumpFile.exists()){
			try {
				dumpFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(dumpFile));
			writer.write("");
			writer.close();
			writer = new BufferedWriter(new FileWriter(dumpFile, true));
			for(BasicBlock bb: suspectList){
				List<TestCase> tcList = coverageMap.get(bb.getSSABasicBlock());
				StringBuffer buf = new StringBuffer(bb.toDumpString() + " ");
				for(TestCase tc: tcList){
					buf.append(tc.getClassName());
					buf.append(":");
					buf.append(tc.getMethodName());
					buf.append(" ");
				}
				writer.append(buf.toString());
				writer.newLine();
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		dumpBBProgress();
		
	}
	public void dumpBBProgress(){
		String bbprogressFileName = project.getProject().getLocation().toOSString() + "/data/" + BB_PROGRESS_DUMP;
		File bbprogressFile = new File(bbprogressFileName);
		if(!bbprogressFile.exists()){
			try {
				bbprogressFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(bbprogressFile));
			writer.write(this.bbProgress + "");
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean loadSession(){
		String testresultFileName = project.getProject().getLocation().toOSString() + "/data/" + TEST_RESULT_DUMP;
		System.out.println("load session from:"+testresultFileName);
		File testresultFile = new File(testresultFileName);
		if(!testresultFile.exists()){
			return false;
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(testresultFile));
			String s = reader.readLine();
			while(s != null){
				String[] frags = s.split(" ");
				TestCase tc = new TestCase(frags[0], frags[1]);
				if(frags[2].equals("0"))
					testResultMap.put(tc, false);
				else
					testResultMap.put(tc, true);
				s = reader.readLine();
			}
			
			reader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		String dumpfileName = project.getProject().getLocation().toOSString() + "/data/" + FL_RESULT_DUMP;
		System.out.println("load session from:"+dumpfileName);
		File dumpFile = new File(dumpfileName);
		if(!dumpFile.exists()){
			return false;
		}
		
		suspectList = new ArrayList<BasicBlock>();	
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dumpFile));
			String s = reader.readLine();
			while(s != null){
				String[] frags = s.split(" ");
				BasicBlock block = BasicBlock.parseString(frags[0]);
				ArrayList<TestCase> tcList = new ArrayList<TestCase>();
				for(int i = 1; i < frags.length; i ++){
					String[] tcStr = frags[i].split(":");
					TestCase tc = new TestCase(tcStr[0], tcStr[1]);
					tcList.add(tc);
				}
				dumpCoverageMap.put(block, tcList);
				suspectList.add(block);
				s = reader.readLine();
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return loadBBProgress();
	}

	public boolean loadBBProgress(){
		String bbprogressFileName = project.getProject().getLocation().toOSString() + "/data/" + BB_PROGRESS_DUMP;
		File bbprogressFile = new File(bbprogressFileName);
		if(!bbprogressFile.exists()){
			return false;
		}
			
		try {
			BufferedReader reader = new BufferedReader(new FileReader(bbprogressFile));
			String s = reader.readLine();
			this.bbProgress = Integer.parseInt(s);			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
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
//	public void initMatrixes() {
//
//		problemMap = new HashMap<ArrayList<BasicBlock>, Boolean>();
//		coverageMap = new HashMap<SSACFG.BasicBlock, ArrayList<TestCase>>();
//
//		for (TestCase tc : testResultMap.keySet()) {
//			ArrayList<InvokeTraceNode> fullTrace = getTraceForTestCase(tc);
//
//			ArrayList<BasicBlock> rawBlockTrace = new ArrayList<BasicBlock>();
//			HashMap<SSACFG.BasicBlock, BasicBlock> bbmap = new HashMap<SSACFG.BasicBlock, BasicBlock>();
//			for (InvokeTraceNode tr : fullTrace)
//				rawBlockTrace.addAll(toBlockTrace(tr, bbmap));
//
//			// now we remove the blocks in "test code" since they should be
//			// correct already.
//			ArrayList<BasicBlock> blockTrace = new ArrayList<BasicBlock>();
//			for (BasicBlock block : rawBlockTrace) {
//				String className = block.getClassName().replaceAll("/", ".");
//				if (!(className.equals(testsType.getFullyQualifiedName()))) {
//					blockTrace.add(block);
//				}
//			}
//			problemMap.put(blockTrace, testResultMap.get(tc));
//
//			for (BasicBlock bb : blockTrace) {
//				ArrayList<TestCase> tcList = coverageMap.get(bb.getSSABasicBlock());
//				if (tcList == null) {
//					tcList = new ArrayList<TestCase>();
//				}
//				if (!tcList.contains(tc)) {
//					tcList.add(tc);
//				}
//
//				coverageMap.put(bb.getSSABasicBlock(), tcList);
//			}
//
//		}
//
//	}


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
	
	private Set<BasicBlock> toBlockTrace(InvokeTraceNode tr, Map<SSACFG.BasicBlock, BasicBlock> bbmap) {
		Set<BasicBlock> rawBlockTrace = new HashSet<BasicBlock>();

		ArrayList<TraceNode> traceNodes = tr.getCalleeTrace();
		for (TraceNode node : traceNodes) {
			ArrayList<SSACFG.BasicBlock> ssablocks = node.getBlockTrace();
			if(node.getMethodKey().indexOf("flipIfWarranted") != -1){
				int i = 0;
			}
			if (ssablocks != null) {
				for (SSACFG.BasicBlock ssabb : ssablocks) {
					BasicBlock bb = bbmap.get(ssabb);
					if (bb == null) {
						bb = new BasicBlock(node.getMethodKey(), ssabb);
						bbmap.put(ssabb, bb);
					}
					rawBlockTrace.add(bb);
				}
			}
			ArrayList<InvokeTraceNode> calleeNodes = node.getCalleeList();
			if (calleeNodes != null) {
				for (InvokeTraceNode calleeNode : calleeNodes) {
					rawBlockTrace.addAll(toBlockTrace(calleeNode, bbmap));
				}
			}
		}
		Set<BasicBlock> blockTrace = new HashSet<BasicBlock>();
		for (BasicBlock block : rawBlockTrace) {
			String className = block.getClassName().replaceAll("/", ".");
			if (!(className.equals(testsType.getFullyQualifiedName()))) {
				blockTrace.add(block);
			}
		}
		return blockTrace;
	}
	
	public Set<BasicBlock> toBlockTrace(ArrayList<InvokeTraceNode> trnodes){
		Map<SSACFG.BasicBlock, BasicBlock> bbmap = new HashMap<SSACFG.BasicBlock, BasicBlock>();
		Set<BasicBlock> blockTrace = new HashSet<BasicBlock>();
		for(InvokeTraceNode tr: trnodes){
			blockTrace.addAll(toBlockTrace(tr, bbmap));
		}
		return blockTrace;
	}
	
	
	public void setFixGoalTable(FixGoalTable table) {
		this.fixGoal = table;
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

	public ExpressionRepositoryManager getExprRepoManager() {
		return exprRepoManager;
	}

	public FixGoalTable getFixGoal() {
		return this.fixGoal;
	}

	public CandidateQueue getCandidateQueue() {
		return this.candidateQueue;
	}

	public Object getBreakpointMutualLock() {
		return this.breakpointMutualLock;
	}

	/*public void generateStubs() {
		try {
			GenerateStubs generatestubs = new GenerateStubs(this.project);
			generatestubs.perform();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/

	public void assignCheckpointToTraceNode(CheckpointOnNode cp, TraceNode node) {
		traceNodeCheckpointMap.put(node, cp);
	}

	/**
	 * 
	 * @param node
	 * @return The Checkpoint attached on the given TraceNode.<br>
	 *         Return null if the TraceNode has no Checkpoint.
	 */
	public CheckpointOnNode findCheckpointFromTraceNode(TraceNode node) {
		return traceNodeCheckpointMap.get(node);
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