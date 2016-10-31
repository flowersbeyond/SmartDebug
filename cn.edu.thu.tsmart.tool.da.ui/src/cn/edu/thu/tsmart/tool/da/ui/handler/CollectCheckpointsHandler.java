package cn.edu.thu.tsmart.tool.da.ui.handler;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElementContainer;

import cn.edu.thu.tsmart.tool.da.core.CheckpointCollectingSession;
import cn.edu.thu.tsmart.tool.da.core.CheckpointCollectingSession.BreakpointData;
import cn.edu.thu.tsmart.tool.da.core.ICallback;
import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointOnNode;
import cn.edu.thu.tsmart.tool.da.tracer.DynamicTranslator;
import cn.edu.thu.tsmart.tool.da.tracer.trace.InvokeTraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.TraceNode;

public class CollectCheckpointsHandler extends AbstractHandler {

	/**
	 * 鏉ユ簮鍒楄〃<br>
	 * cn.edu.thu.tsmart.tool.da.core.BugFixer.java 76 琛�
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("============= UI: CollectCheckpointsHandler executes");
		initCheckpointCollectingSession();
		DebugEventListener addedDebugEventListenerForInitialTrace = addDebugEventListenerAndRenewDynamicTranslator();
		// 鍟娾�︹�� 涓�浼氭敼鎴� java8 method reference濂戒簡鈥︹��
		launchCheckpointCollectingSession(new ICallback() {
			@Override
			public void run() {
				ArrayList<InvokeTraceNode> currentTrace = addedDebugEventListenerForInitialTrace.getTranslator()
						.getCurrentTrace();
				assignIDtoTrace(currentTrace, true); // 灏卞湪绗竴娆＄粨鏉熸椂鍋氱舰?!
				DebugPlugin.getDefault().removeDebugEventListener(addedDebugEventListenerForInitialTrace);
				List<IJavaLineBreakpoint> addedBreakpoints = addRecommendedBreakpoints();
				DebugEventListener addedDebugEventListenerForCheckpointCollection = addDebugEventListenerAndRenewDynamicTranslator();
				// try {
				// Thread.sleep(1000);
				// } catch (InterruptedException e1) {
				// // TODO Auto-generated catch block
				// e1.printStackTrace();
				// }
				launchCheckpointCollectingSession(new ICallback() {
					@Override
					public void run() {
						IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
						try {
							// cast exception...
							// manager.removeBreakpoints((IBreakpoint[])
							// addedBreakpoints.toArray(), true);
							for (IJavaLineBreakpoint bp : addedBreakpoints) {
								manager.removeBreakpoint(bp, true);
							}
						} catch (CoreException e) {
							e.printStackTrace();
						}
						DebugPlugin.getDefault()
								.removeDebugEventListener(addedDebugEventListenerForCheckpointCollection);
					}
				});
			}
		});
		System.out.println("============= UI: CollectCheckpointsHandler executes done");
		return null;
	}

	/**
	 * 浠� TestRunSession 鍙栧嚭寰呮墽琛岀殑 Testcase 鐨勪俊鎭�, 鍒濆鍖� CheckpointCollectingSession
	 * <br>
	 * 鐢变簬 CheckpointCollectingSession.initSingleton(project, type); 骞傜瓑,
	 * 鎵�浠ユ湰鏂规硶搴旇鏄箓绛夌殑.
	 */
	private void initCheckpointCollectingSession() {
		// getTestRunSessions(): "The list is sorted by age, youngest first."
		ArrayList<TestRunSession> testRunSessions = (ArrayList<TestRunSession>) (JUnitCorePlugin.getModel()
				.getTestRunSessions());
		// 姣忔墽琛屼竴娆unit, sessions.size()++
		System.out.println("getTestRunSessions size " + testRunSessions.size());
		// impl type: TestSuiteElement
		ITestElement element = testRunSessions.get(0).getChildren()[0];
		// full qualified name, e.g. com.lee.TestXY
		String testClassName = peelTestCaseElementContainer(element).getTestClassName();
		// impl type: JavaProject
		IJavaProject project = testRunSessions.get(0).getLaunchedProject();
		IType type = null;
		try {
			type = project.findType(testClassName);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		CheckpointCollectingSession.initSingleton(project, type);
	}

	private static int nowID = 0;

	/**
	 * 姣忓綋姝ゆ椂 灏遍潪甯告兂瑕佹暟鎹簱.
	 * 
	 * @param currentTrace
	 *            should not be empty
	 */
	public static void assignIDtoTrace(ArrayList<InvokeTraceNode> currentTrace, boolean isFirstTime) {
		System.err.println("assignIDtoTrace start");
		// TODO 寰�鍒楄〃閲屽瓨... 鎯宠鏁版嵁搴撴児. SQLite 鏉ヤ竴濂�?
		nowID = 0;
		for (InvokeTraceNode itn : currentTrace) {
			assignIdCascade(itn, isFirstTime);
		}
		System.err.println("assignIDtoTrace end");
	}

	/**
	 * 绾ц仈鍦� set id
	 * 
	 * @param itn
	 * @param nowID
	 * @return
	 */
	private static void assignIdCascade(InvokeTraceNode itn, boolean isFirstTime) {
		itn.setId(nowID++);
		CheckpointCollectingSession cpcSession = CheckpointCollectingSession.getSingleton();
		// if (cpcSession.getCommonTraceNodeFromTheOnlyTestCase(itn.getId()) ==
		// null) {
		// 璇存槑鏄涓�娆℃敹闆� // TODO 浠ュ悗 涓嶉渶瑕佸垽鏂簡澶ф 鍙湁绗竴娆′細杩欎箞鎼�
		if (isFirstTime) {
			cpcSession.setCommonTraceNodeToTheOnlyTestCase(itn);
		}
		// }
		ArrayList<TraceNode> tns = itn.getCalleeTrace();
		for (TraceNode tn : tns) {
			assignIdCascade(tn, isFirstTime);
		}
	}

	private static void assignIdCascade(TraceNode tn, boolean isFirstTime) {
		tn.setId(nowID++);
		CheckpointCollectingSession cpcSession = CheckpointCollectingSession.getSingleton();
		// if (cpcSession.getCommonTraceNodeFromTheOnlyTestCase(tn.getId()) ==
		// null) {
		// 璇存槑鏄涓�娆℃敹闆�
		if (isFirstTime) {
			cpcSession.setCommonTraceNodeToTheOnlyTestCase(tn);
		}
		// }
		ArrayList<InvokeTraceNode> itns = tn.getCalleeList();
		for (InvokeTraceNode itn : itns) {
			assignIdCascade(itn, isFirstTime);
		}
	}

	/**
	 * 2016-07-19 17:44:49 Just like
	 * cn.edu.thu.tsmart.tool.da.core.search.strategy.SymExpressionFixer line
	 * 516
	 * 
	 * @return The added breakpoints
	 */
	private List<IJavaLineBreakpoint> addRecommendedBreakpoints() {
		IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
		CheckpointCollectingSession cpcSession = CheckpointCollectingSession.getSingleton(); // 鍏ㄥ眬鍙橀噺鐢ㄨ捣鏉ョ湡鐖藉晩.
		List<IJavaLineBreakpoint> recommendBreakpoints = new ArrayList<>();
		try {
			addRecommend(93, recommendBreakpoints, cpcSession);
			// -> (44,2) -> 瀹為檯鍋� id=(92, 93[line 44])
			addRecommend(70, recommendBreakpoints, cpcSession);
			// -> (48,3) -> 瀹為檯鍋� id=(81, 82[line 48],85)
			// 涓嶅噯鐨勫師鍥�: '鍦╰race涓嚭鐜颁竴娆�' != 'hit count ++'

			// java.lang.ClassCastException: [Ljava.lang.Object; cannot be cast
			// to [Lorg.eclipse.debug.core.model.IBreakpoint;
			// TODO why?
			// manager.addBreakpoints((IBreakpoint[]) bps.toArray());
			for (IJavaLineBreakpoint bp : recommendBreakpoints) {
				manager.addBreakpoint(bp);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return recommendBreakpoints;
	}

	private void addRecommend(int recommendNodeID, List<IJavaLineBreakpoint> recommendBreakpoints,
			CheckpointCollectingSession cpcSession) throws CoreException {
		TraceNode recommendNode = (TraceNode) cpcSession.getCommonTraceNodeFromTheOnlyTestCase(recommendNodeID);
		// int line = recommendNode.getStartLineNum();
		// int hitcount = 2;// TODO 绠楄繖鐜╂剰...
		BreakpointData bpData = cpcSession.getBreakpointDataForTraceNodeForTheOnlyTestCase(recommendNodeID);
		System.out.println(
				"recommendID " + recommendNodeID + " lineNum " + bpData.lineNum + " hitCount " + bpData.hitCount);
		CheckpointOnNode recommendCheckpoint = cpcSession.findCheckpointFromTraceNodeOfTheOnlyTestCase(recommendNode);
		if (recommendCheckpoint == null) {
			cpcSession.assignNewCheckpointToTraceNodeOfTheOnlyTestCase(recommendNode);
			recommendCheckpoint = cpcSession.findCheckpointFromTraceNodeOfTheOnlyTestCase(recommendNode);
		}
		// 鍟� TODO 璺熷濮愮‘璁�: createLineBreakpoint 鐨勭涓�涓弬鏁版槸 breakpoint淇℃伅鐨勫瓨鍌ㄤ綅缃�
		IJavaLineBreakpoint recommendBreakpoint = JDIDebugModel.createLineBreakpoint(recommendCheckpoint.getFile(),
				recommendNode.getClassName().replaceAll("/", "."), bpData.lineNum, -1, -1, bpData.hitCount, true, null);
		recommendBreakpoint.setEnabled(true);
		recommendBreakpoints.add(recommendBreakpoint);
	}

	/**
	 * add debugEventListener,<br>
	 * renew DynamicTranslator. *
	 * 
	 * @return added DebugEventListener
	 */
	private DebugEventListener addDebugEventListenerAndRenewDynamicTranslator() {
		CheckpointCollectingSession cpcSessionSingleton = CheckpointCollectingSession.getSingleton();
		// 鎸夌洰鍓� 2016-07-18 鐨勫亣璁�, 鍙湁涓�涓敊鐨� TC
		TestCase theOnlyFailedTestCase = cpcSessionSingleton.getTestCaseList().get(0);
		DebugPlugin debugplugin = DebugPlugin.getDefault();
		// 鑻ラ潪绗竴娆ollect, 瑕佸厛 remove 鏃� listener
		// if (debugEventListener != null) {
		// debugplugin.removeDebugEventListener(debugEventListener);
		// }
		DynamicTranslator translator = new DynamicTranslator(cpcSessionSingleton.getProject(), true);
		DebugEventListener debugEventListener = new DebugEventListener(translator);
		debugplugin.addDebugEventListener(debugEventListener);
		// 濡傛灉娌℃湁寮�濮嬫墽琛�, 鎶� trace 鐨勫紩鐢� add 杩涘幓, 浼氭湁闂鍚�... // 涓轰簡璁╅偅涓紩鐢ㄤ笉鍙�, 缁�
		// DynamicTranslator#rootInvocationTrace 瀛楁鍔犱簡 final
		cpcSessionSingleton.addTestTrace(theOnlyFailedTestCase, debugEventListener.getTranslator().getCurrentTrace());
		return debugEventListener;
	}

	/**
	 * 
	 * @param afterLaunch
	 *            afterLaunch.run() will be called after the launch terminates.
	 *            If you don't want to do anything after launch, use null
	 *            argument.
	 */
	private void launchCheckpointCollectingSession(ICallback afterLaunch) {
		CheckpointCollectingSession cpcSessionSingleton = CheckpointCollectingSession.getSingleton();
		// 鎸夌洰鍓� 2016-07-18 鐨勫亣璁�, 鍙湁涓�涓敊鐨� TC
		TestCase theOnlyFailedTestCase = cpcSessionSingleton.getTestCaseList().get(0);
		try {
			ILaunchConfiguration config = cpcSessionSingleton.findLaunchConfiguration(theOnlyFailedTestCase);
			// BugFixer.java l105
			DebugPlugin.getDefault().getLaunchManager().addLaunchListener(new ILaunchesListener2() {
				// input for this anonymous inner class: ILaunchConfiguration
				// config, ICallback afterLaunch
				/**
				 * 涓嶇煡閬撳暐鏃跺�欎細鏈夊涓� launch 涓�璧� terminate... 姣忎釜閮界湅涓�涓嬪氨濂�
				 * 
				 * @param launches
				 */
				@Override // BugFixer.java l230
				public void launchesTerminated(ILaunch[] launches) {
					System.out.println("launchesTerminated: " + config); // 闂寘!

					for (ILaunch launch : launches) {
						if (launch.getLaunchConfiguration().equals(config)) {
							DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this); // 闂寘!
							System.out.println("My ILaunchesListener2 " + this + " is removed");
							if (afterLaunch != null) {
								afterLaunch.run();
							}
							break;
						}
					}
				}

				@Override
				public void launchesRemoved(ILaunch[] launches) {
				}

				@Override
				public void launchesAdded(ILaunch[] launches) {
				}

				@Override
				public void launchesChanged(ILaunch[] launches) {
				}
			});
			config.launch("sdtrace", new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ITestElement <- {ITestCaseElement, ITestElementContainer}
	 */
	private ITestCaseElement peelTestCaseElementContainer(ITestElement element) {
		while (element instanceof ITestElementContainer) { // 鍓ュ紑
			element = ((ITestElementContainer) element).getChildren()[0];
		}
		return (ITestCaseElement) element;
	}

	class DebugEventListener implements IDebugEventSetListener {

		private DynamicTranslator translator;

		public DebugEventListener(DynamicTranslator translator) {
			super();
			this.translator = translator;
		}

		public DynamicTranslator getTranslator() {
			return translator;
		}

		public void setTranslator(DynamicTranslator translator) {
			this.translator = translator;
		}

		@Override
		public void handleDebugEvents(DebugEvent[] events) {
			if (events.length > 1) {
				System.err.println("VERY LONG debug event set: " + events.length);
			}
			DebugEvent event = events[0];
			if (event.getKind() == DebugEvent.SUSPEND) {
				System.out.println("A event of kind SUSPEND: " + event);
				translator.handleNewActions();
			} else if (event.getKind() == DebugEvent.TERMINATE) {
				System.out.println("A event of kind TERMINATE: " + event);
				translator.handleNewActions();
			} else {
				// pass
			}
		}
	}
}
