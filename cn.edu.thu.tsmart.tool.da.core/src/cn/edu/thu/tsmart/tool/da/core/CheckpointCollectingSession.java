package cn.edu.thu.tsmart.tool.da.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;

import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointOnNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.AbstractCommonTraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.InvokeTraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.TraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.TraceUtil;

/**
 * 2016-07-05 时, 被设计出来存储 TestCase - Trace - CheckpointOnTraceNode 的对应关系. 名字叫
 * CheckpointCollectingSession, 主要因为许多字段与
 * cn.edu.thu.tsmart.tool.da.core.FixSession 的相同 <br>
 * 2016-07-18 事实上的 TraceManager, CheckpointOnNode-Manager.
 * 
 * @author LI Tianchi
 *
 */
public class CheckpointCollectingSession {

	private static CheckpointCollectingSession singleton = null;

	/**
	 * Precondition: 已用 {@link #initSingleton(IJavaProject, IType, ArrayList)}
	 * 初始化过
	 * 
	 * @return
	 */
	public static CheckpointCollectingSession getSingleton() {
		return singleton;
	}

	public static boolean isInCollectingProgress() {
		return singleton != null;
	}

	/**
	 * 用 testsType 中所有的 @Test 方法来初始化 CheckpointCollectingSession
	 * 
	 * @param project
	 * @param testsType
	 */
	public static void initSingleton(IJavaProject project, IType testsType) {
		if (singleton == null) {
			ArrayList<IMethod> testMethods = TestCaseUtil.getTestMethods(testsType);
			singleton = new CheckpointCollectingSession(project, testsType, testMethods);
			singleton.initLaunchConfiguration();
		}
	}

	private void initLaunchConfiguration() {
		LaunchConfigurationCreatorForCheckpointCollection creator = new LaunchConfigurationCreatorForCheckpointCollection();
		for (IMethod method : testMethods) {
			ILaunchConfiguration config = creator.createLaunchConfiguration(method);
			TestCase tc = new TestCase(method.getDeclaringType().getFullyQualifiedName(), method.getElementName());
			launchConfigurationOfTestCases.put(tc, config);
		}
	}

	/**
	 * 2016-07-01 16:56:29 抄 BugFixSession.java 里的 LaunchConfigurationCreator
	 * <br>
	 * super.createLaunchConfiguration 是 protected 的! 必须包一层...
	 */
	class LaunchConfigurationCreatorForCheckpointCollection extends JUnitLaunchShortcut {
		public ILaunchConfiguration createLaunchConfiguration(IMethod method) {
			try {
				return super.createLaunchConfiguration(method);
			} catch (CoreException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * 1 CheckpointCollectingSession-属于-1 project
	 */
	private IJavaProject project;
	/**
	 * 1 CheckpointCollectingSession-处理-1 测试用例类
	 */
	private IType testsType;
	/**
	 * 此次 checkpoint collecting session 为哪些测试用例方法收集 checkpoint. <br>
	 * testMethods 唯一决定了 List<TestCase> getTestCaseList() 的结果.
	 */
	private List<IMethod> testMethods = new ArrayList<>();

	/**
	 * TestCase 对应的可 launch 物.
	 */
	private Map<TestCase, ILaunchConfiguration> launchConfigurationOfTestCases = new HashMap<>();
	/**
	 * 每个 TestCase 对应的 trace 的 rootInvocationTrace (按 DynamicTranslator 的字段)
	 * <br>
	 * 虽然 存起来; 暂时 没有用
	 */
	private Map<TestCase, ArrayList<InvokeTraceNode>> rootInvokeTraceNodeListOfTestCases = new HashMap<>();
	/**
	 * 线性存储Trace链. <br>
	 * 哎要不是为了树形展示trace, 可以不要 Map<TestCase, ArrayList<InvokeTraceNode>> traceMap
	 * 吧?<br>
	 * 还是要的吧. Trace的形状的保证...
	 */
	private Map<TestCase, List<AbstractCommonTraceNode>> commonTraceNodeListOnTestCases = new HashMap<>();
	/**
	 * 某 TC 上某 id 的 TraceNode 的 Checkpoint
	 */
	private Map<TestCase, Map<TraceNode, CheckpointOnNode>> fromTestCaseAndTraceNodeToCheckpoint = new HashMap<>();

	/**
	 * 存测试结果 2016-07-05 TODO 看 BugFixSession 里的 testResultMap 使用,
	 * 略凌乱(涉及到launch一遍+用testresultListener什么的...
	 */
	private Map<TestCase, Boolean> testResultOfTestCases = new HashMap<>();

	private Map<TestCase, Map<Integer, BreakpointData>> fromTestCaseToFromNodeIdToBreakpointData = new HashMap<>();

	public BreakpointData getBreakpointDataForTraceNodeForTheOnlyTestCase(int nodeID) {
		return getBreakpointDataForTraceNode(getTheOnlyTestCase(), nodeID);
	}

	/**
	 * After calculation...
	 * 
	 * @param tc
	 * @param nodeID
	 * @return
	 */
	public BreakpointData getBreakpointDataForTraceNode(TestCase tc, int nodeID) {

		// // 法1
		// TraceNode node=(TraceNode)this.getCommonTraceNodeFrom(tc, nodeID);
		// List<InvokeTraceNode>
		// trace=this.getRootInvokeTraceNodesForTestCase(tc);
		// int hitCount=TraceUtil.getHitCount(node, (ArrayList<InvokeTraceNode>)
		// trace);
		// int lineNum=node.getStartLineNum();
		// return new BreakpointData(lineNum, hitCount);

		// 法2
		if (fromTestCaseToFromNodeIdToBreakpointData.get(tc) == null) {
			calculateBreakpointDataForTraceNode(tc); // 也算是 lazy eval 了...?
		}
		return fromTestCaseToFromNodeIdToBreakpointData.get(tc).get(nodeID);
	}

	/**
	 * 虽会被 {@link #getBreakpointDataForTraceNode(TestCase, int)} lazy调用 <br>
	 * 但可在trace收集完成后主动调用以提高 get 时的效率...
	 * 
	 * // 是重造轮子... // 但是可能是更正确的... 2016-07-22 21:35:49
	 * 
	 * @param tc
	 */
	public void calculateBreakpointDataForTraceNode(TestCase tc) {
		Map<Integer, BreakpointData> fromNodeIdToBpData = new HashMap<>();
		Map<Integer, Integer> fromLineNumberToKnownHitcount = new HashMap<>();
		List<AbstractCommonTraceNode> nodes = this.getCommonTraceForTestCase(tc);
		for (AbstractCommonTraceNode node : nodes) {
			if (node instanceof TraceNode) {
				Integer lineNum = ((TraceNode) node).getStartLineNum();
				Integer hitCount = 0;
				if (fromLineNumberToKnownHitcount.containsKey(lineNum)) {
					hitCount = fromLineNumberToKnownHitcount.get(lineNum);
				}
				fromLineNumberToKnownHitcount.put(lineNum, hitCount + 1);
				System.err.printf("ID\t%d\tline\t%d\tcnt\t%d\n", node.getId(), lineNum, hitCount+1);
				fromNodeIdToBpData.put(node.getId(), new BreakpointData(lineNum, hitCount + 1));
			} else {
				// pass
			}
		}
		fromTestCaseToFromNodeIdToBreakpointData.put(tc, fromNodeIdToBpData);
	}

	/**
	 * 真素大逆不道...
	 * 
	 * @author LI Tianchi
	 *
	 */
	public class BreakpointData {
		public int lineNum;
		public int hitCount;

		public BreakpointData(int lineNum, int hitCount) {
			this.lineNum = lineNum;
			this.hitCount = hitCount;
		}
	}

	/**
	 * Factory 化... 不许私自 new CheckpointCollectingSession ( ﾟ∀。)
	 * 
	 * @param project
	 * @param testsType
	 * @param testMethods
	 */
	private CheckpointCollectingSession(IJavaProject project, IType testsType, ArrayList<IMethod> testMethods) {
		super();
		this.project = project;
		this.testsType = testsType;
		this.testMethods = testMethods;
	}

	/***
	 * 增, 改<br>
	 * 依赖: a, b \in TraceNode -> ( a.id==b.id -> a.equals(b) &&
	 * a.hashCode()==b.hashCode() )
	 * 
	 * @param cp
	 * @param tc
	 * @param node
	 */
	public void assignCheckpointToTraceNode(CheckpointOnNode cp, TestCase tc, TraceNode node) {
		Map<TraceNode, CheckpointOnNode> fromTraceNodeToCheckpoint = fromTestCaseAndTraceNodeToCheckpoint.get(tc);
		if (fromTraceNodeToCheckpoint == null) {
			fromTraceNodeToCheckpoint = new HashMap<>(); // 感觉这个 lazy 很美...
			fromTestCaseAndTraceNodeToCheckpoint.put(tc, fromTraceNodeToCheckpoint);
		}
		fromTraceNodeToCheckpoint.put(node, cp);
	}

	/**
	 * @see #assignCheckpointToTraceNode(CheckpointOnNode, TestCase, TraceNode)
	 * @param cp
	 * @param node
	 */
	public void assignCheckpointToTraceNodeOfTheOnlyTestCase(CheckpointOnNode cp, TraceNode node) {
		assignCheckpointToTraceNode(cp, getTheOnlyTestCase(), node);
	}

	public void assignNewCheckpointToTraceNode(TestCase tc, TraceNode node) {
		assignCheckpointToTraceNode(createNewCheckpoint(node), tc, node);
	}

	/**
	 * 这一套手工柯里化...
	 * 
	 * @param tc
	 * @param node
	 */
	public void assignNewCheckpointToTraceNodeOfTheOnlyTestCase(TraceNode node) {
		assignCheckpointToTraceNode(createNewCheckpoint(node), getTheOnlyTestCase(), node);
	}

	/**
	 * 我是不是重造了 CheckpointManager 啊... // TODO checkpoint收集阶段和使用阶段 用不同的数据结构如何?
	 * 
	 * @param host
	 * @return
	 */
	private CheckpointOnNode createNewCheckpoint(TraceNode host) {
		String classnameX = host.getClassName().replaceAll("/", ".");
		IFile file = null;
		try {
			file = (IFile) (this.getProject().findType(classnameX).getCompilationUnit().getCorrespondingResource());
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new CheckpointOnNode(file);
	}

	/**
	 * 查<br>
	 * 依赖: a, b \in TraceNode -> ( a.id==b.id -> a.equals(b) &&
	 * a.hashCode()==b.hashCode() )
	 * 
	 * @param node
	 * @return The Checkpoint attached on the given TraceNode.<br>
	 *         Return null if the TraceNode has no Checkpoint.
	 */
	public CheckpointOnNode findCheckpointFromTraceNode(TestCase tc, TraceNode node) {
		Map<TraceNode, CheckpointOnNode> fromTraceNodeToCheckpoint = fromTestCaseAndTraceNodeToCheckpoint.get(tc);
		if (fromTraceNodeToCheckpoint != null) {
			return fromTraceNodeToCheckpoint.get(node);
		} else {
			return null;
		}
	}

	/**
	 * 
	 * @see #findCheckpointFromTraceNode(TestCase, TraceNode)
	 * @param node
	 * @return
	 */
	public CheckpointOnNode findCheckpointFromTraceNodeOfTheOnlyTestCase(TraceNode node) {
		return findCheckpointFromTraceNode(getTheOnlyTestCase(), node);
	}

	/**
	 * 删. 增删改查备矣!<br>
	 * 
	 * @param tc
	 * @param node
	 */
	public void removeCheckpointFromTraceNode(TestCase tc, TraceNode node) {
		Map<TraceNode, CheckpointOnNode> fromTraceNodeToCheckpoint = fromTestCaseAndTraceNodeToCheckpoint.get(tc);
		fromTraceNodeToCheckpoint.remove(node); // 嗯 是有返回值的.
	}

	/**
	 * @see CheckpointCollectingSession#removeCheckpointFromTraceNode(TestCase,
	 *      TraceNode)
	 * @param node
	 */
	public void removeCheckpointFromTraceNodeOfTheOnlyTestCase(TraceNode node) {
		removeCheckpointFromTraceNode(this.getTheOnlyTestCase(), node);
	}

	public IJavaProject getProject() {
		return project;
	}

	/**
	 * @param tc
	 * @param trace
	 */
	public void addTestTrace(TestCase tc, ArrayList<InvokeTraceNode> trace) {
		rootInvokeTraceNodeListOfTestCases.put(tc, trace);
	}

	public Map<TestCase, ArrayList<InvokeTraceNode>> getTraceMap() {
		return rootInvokeTraceNodeListOfTestCases;
	}

	/**
	 * @param tc
	 * @return
	 */
	public List<InvokeTraceNode> getRootInvokeTraceNodesForTestCase(TestCase tc) {
		return rootInvokeTraceNodeListOfTestCases.get(tc);
	}

	/**
	 * @param tc
	 * @return
	 */
	public List<AbstractCommonTraceNode> getCommonTraceForTestCase(TestCase tc) {
		return commonTraceNodeListOnTestCases.get(tc);
	}

	/**
	 * 
	 * @param tc
	 * @param id
	 * @return <b>null</b> if <b>tc</b> has no trace stored here, or no node on
	 *         the trace of <b>tc</b> has that <b>id</b>.
	 */
	public AbstractCommonTraceNode getCommonTraceNodeFrom(TestCase tc, Integer id) {
		List<AbstractCommonTraceNode> nodeList = this.commonTraceNodeListOnTestCases.get(tc);
		if (nodeList != null) {
			try {
				return nodeList.get(id);
			} catch (IndexOutOfBoundsException oob) {
				return null;
			}
		} else {
			return null;
		}
	}

	public void setCommonTraceNodeTo(TestCase tc, AbstractCommonTraceNode node) {
		List<AbstractCommonTraceNode> fromIdToNode = this.commonTraceNodeListOnTestCases.get(tc);
		if (fromIdToNode != null) {
			fromIdToNode.add(node);
		} else {
			fromIdToNode = new ArrayList<>();
			this.commonTraceNodeListOnTestCases.put(tc, fromIdToNode);
			fromIdToNode.add(node);
		}
	}

	/**
	 * workaround: 2016-07-15 假设只有一个(测出错误的)测试用例, 因此只trace它,
	 * 只收集它的trace上的checkpoint condition, 只以它为依据做FL.<br>
	 * 考虑到以后可能有多testcase, 本class中的数据结构按多testcase设计.
	 * 但提供一些method(比如本method)用于单testcase情形. Nearer My Spaghetti to Thee <br>
	 * 这么丑陋, 还是封装起来吧. 详见 call hierarchy <br>
	 * 还是public了, 尝试加断点时用...
	 */
	public TestCase getTheOnlyTestCase() {
		List<TestCase> testCases = this.getTestCaseList();
		if (testCases.isEmpty()) {
			return null;
		} else {
			return testCases.get(0);
		}
	}

	/**
	 * 传入一个参数 构造新函数 柯里化233
	 * 
	 * @see CheckpointCollectingSession#getCommonTraceNodeFrom(TestCase tc,
	 *      Integer id)
	 */
	public AbstractCommonTraceNode getCommonTraceNodeFromTheOnlyTestCase(Integer id) {
		return this.getCommonTraceNodeFrom(this.getTheOnlyTestCase(), id);
	}

	public void setCommonTraceNodeToTheOnlyTestCase(AbstractCommonTraceNode node) {
		setCommonTraceNodeTo(this.getTheOnlyTestCase(), node);
	}

	public Map<TestCase, ILaunchConfiguration> getLaunchConfigurationOfTestCases() {
		return launchConfigurationOfTestCases;
	}

	public List<IMethod> getTestMethods() {
		return testMethods;
	}

	/**
	 * Precondition: this.testMethods 非空<br>
	 * 动机: IMethod -> TestCase 有平凡的映射 new
	 * TestCase(method.getDeclaringType().getFullyQualifiedName(),
	 * method.getElementName()); <br>
	 * 数据量也不大, 所以不需要搞个 List<TestCase> 作为 "cache"<br>
	 * Java 8 Stream or FP 教程:
	 * http://winterbe.com/posts/2014/07/31/java8-stream-tutorial-examples/
	 */
	public List<TestCase> getTestCaseList() {
		return testMethods.stream()
				.map(method -> new TestCase(method.getDeclaringType().getFullyQualifiedName(), method.getElementName()))
				.collect(Collectors.toList());
	}

	/**
	 * @see {@link BugFixSession#findLaunchConfiguration(TestCase)} <br>
	 */
	public ILaunchConfiguration findLaunchConfiguration(TestCase tc) {
		return launchConfigurationOfTestCases.get(tc);
	}
}
