package cn.edu.thu.tsmart.tool.da.ui.views;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import cn.edu.thu.tsmart.tool.da.core.CheckpointCollectingSession;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointOnNode;
import cn.edu.thu.tsmart.tool.da.tracer.DynamicTranslator;
import cn.edu.thu.tsmart.tool.da.tracer.ITraceEventListener;
import cn.edu.thu.tsmart.tool.da.tracer.trace.AbstractCommonTraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.InvokeTraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.TraceNode;
import cn.edu.thu.tsmart.tool.da.ui.handler.CollectCheckpointsHandler;

public class TraceAndCheckpointView extends ViewPart
		implements ITraceEventListener/* , ITraceEventAllDoneListener */ {
	/**
	 * 就是给自己看的 吧...
	 */
	public static final String ID = "cn.edu.thu.tsmart.tool.da.ui.views.TraceAndCheckpointView";

	private TreeViewer viewer;
	/**
	 * Trace链的最后一个TraceNode. 意图是: 只让它的条件可编辑. <br>
	 * Keeps null when there's no TraceNode.
	 */
	private TraceNode lastTraceNodeCache = null;

	class TestMethodWithTrace {

	}

	class ViewContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ArrayList<?>)
				return ((ArrayList) inputElement).toArray();
			return new Object[] { new Object() };
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof InvokeTraceNode) {
				return ((InvokeTraceNode) parentElement).getCalleeTrace().toArray();
			} else if (parentElement instanceof TraceNode) {
				return ((TraceNode) parentElement).getCalleeList().toArray();
				// }else if(parentElement instanceof ArrayList<?>){
				// return getChildren(((ArrayList)parentElement).get(0));
			} else {
				return null;
			}
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof InvokeTraceNode) {
				return ((InvokeTraceNode) element).getCallSiteNode();
			} else if (element instanceof TraceNode) {
				return ((TraceNode) element).getCallerNode();
			} else {
				// 不处理 ArrayList<InvokeTraceNode> 那货的parent会怎样?(反正没有parent)
				return null;
			}
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof InvokeTraceNode) {
				return (!((InvokeTraceNode) element).getCalleeTrace().isEmpty());
			} else if (element instanceof TraceNode) {
				return (!((TraceNode) element).getCalleeList().isEmpty());
				// }else if(element instanceof ArrayList<?>){
				// return !((ArrayList)element).isEmpty();
			} else {
				return false;
			}
		}
	}

	class ViewLabelProvider extends LabelProvider {

		@Override
		public String getText(Object obj) {
			return obj.toString();
		}

		@Override
		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	public TraceAndCheckpointView() {
		// 不显式写一个, view 内容就不出来 ( ´_ゝ`)
	}

	@Override
	public void createPartControl(Composite parent) {

		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());

		// 加列 via
		// http://www.vogella.com/tutorials/EclipseJFaceTree/article.html#jfacecolumnfilebrowser
		// 第1列: [Invoke]TraceNode 信息
		TreeViewerColumn traceNodeColumn = new TreeViewerColumn(viewer, SWT.NONE);
		traceNodeColumn.getColumn().setWidth(550);
		traceNodeColumn.getColumn().setText("TraceNode");
		traceNodeColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return element.toString(); // 仿佛知道了一点 什么叫类型擦除... TODO
			}
		});

		// 第2列: Node id
		TreeViewerColumn idColumn = new TreeViewerColumn(viewer, SWT.NONE);
		idColumn.getColumn().setWidth(30);
		idColumn.getColumn().setText("ID");
		idColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AbstractCommonTraceNode) {
					return ((AbstractCommonTraceNode) (element)).getId() + "";
				} else {
					return "";
				}
			}
		});

		// 第3列: Checkpoint condition
		TreeViewerColumn checkpointColumn = new TreeViewerColumn(viewer, SWT.NONE);
		checkpointColumn.getColumn().setWidth(200);
		checkpointColumn.getColumn().setText("Expectation expression");
		checkpointColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof TraceNode) {
					if (CheckpointCollectingSession.isInCollectingProgress()) {
						CheckpointCollectingSession cpCollectingSession = CheckpointCollectingSession.getSingleton();
						CheckpointOnNode checkpoint = cpCollectingSession
								.findCheckpointFromTraceNodeOfTheOnlyTestCase((TraceNode) element);
						if (checkpoint != null) {
							return checkpoint.getConditionString();
						} else {
							return "true";
						}
					} else {
						return "Not In Collecting Progress";
					}
				} else {
					// Only TraceNode has checkpoint. InvokeTraceNode doesn't.
					return "-";
				}
			}
			// 如果要实现 getImage 方法
			// http://www.vogella.com/tutorials/EclipseJFaceTable/article.html#jfacetable_labelprovider
		});
		// 添加编辑能力.
		checkpointColumn.setEditingSupport(new checkpointConditionEditingSupport(viewer));

		// 表头. via
		// http://www.java2s.com/Code/Java/SWT-JFace-Eclipse/SWTTreeWithMulticolumns.htm
		viewer.getTree().setHeaderVisible(true);
		System.err.println("DynamicTranslator.registerTraceEventListener(this);");
		DynamicTranslator.registerTraceEventListener(this);
	}

	/**
	 * 为 CheckpointCondition 一列添加编辑能力.
	 */
	class checkpointConditionEditingSupport extends EditingSupport {
		/**
		 * 这个编辑器属于哪个 viewer. "例行公事"--xking. 现在实际上是个 TreeViewer. 以后说不定会用于
		 * TableViewer, 所以写(祖父)基类 ColumnViewer
		 */
		private ColumnViewer viewer;
		private CellEditor editor;

		public checkpointConditionEditingSupport(ColumnViewer viewer) {
			super(viewer);
			this.viewer = viewer;
			/*
			 * 看起来: 每个viewer有一个control. <br> 更准确一点:
			 * org.eclipse.jface.viewers.Viewer#getControl() 说: <br> the primary
			 * control associated with this viewer. <br> 根据 TreeViewer 代码,
			 * TreeViewer#getControl() 同 TreeViewer#getTree()
			 */
			this.editor = new TextCellEditor((Composite) viewer.getControl());
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (element instanceof TraceNode) {
				if (CheckpointCollectingSession.isInCollectingProgress()) {
					CheckpointCollectingSession cpCollectingSession = CheckpointCollectingSession.getSingleton();
					CheckpointOnNode checkpoint = cpCollectingSession
							.findCheckpointFromTraceNodeOfTheOnlyTestCase((TraceNode) element);
					if (checkpoint == null) {
						// 条件非空非true, 才加 CP. 空条件则认为误点, 不加 CP; 若原来有 CP, 则移除 CP.
						if (!value.equals("") && !value.equals("true")) {
							TraceNode node = (TraceNode) element;
							IFile file = null;
							try {// TODO 干啥的??? 2016-07-17 10:28:46
								file = (IFile) (cpCollectingSession.getProject()
										.findType(node.getClassName().replaceAll("/", ".")).getCompilationUnit()
										.getCorrespondingResource());
							} catch (JavaModelException e) {
								e.printStackTrace();
							}
							checkpoint = new CheckpointOnNode(file); // TODO
																		// 详细填充
																		// //
																		// TODO
																		// 可能使用
																		// CheckpointUtils.createCheckpoint()

							cpCollectingSession.assignCheckpointToTraceNodeOfTheOnlyTestCase(checkpoint,
									(TraceNode) element);
							checkpoint.setConditionString(value.toString());
						} else {
							/* 空条件认为误点 不加CP */}
					} else {
						// 输入空条件, 若原来有 CP, 则移除 CP.
						if (!value.equals("") && !value.equals("true")) {
							checkpoint.setConditionString(value.toString());
						} else {
							cpCollectingSession.removeCheckpointFromTraceNodeOfTheOnlyTestCase((TraceNode) element);
						}
					}
				} else {
					// not in a cpCollectingSession
				}

			} else {
				// Only TraceNode has checkpoint. InvokeTraceNode doesn't.
			}
			// 数据改变之后, 这里让viewer中的显示更新一下.
			// via
			// http://www.vogella.com/tutorials/EclipseJFaceTableAdvanced/article.html#jfacetable_editor
			viewer.update(element, null);
		}

		@Override
		protected Object getValue(Object element) {
			if (element instanceof TraceNode) {
				if (CheckpointCollectingSession.isInCollectingProgress()) {
					CheckpointCollectingSession cpCollectingSession = CheckpointCollectingSession.getSingleton();
					CheckpointOnNode checkpoint = cpCollectingSession
							.findCheckpointFromTraceNodeOfTheOnlyTestCase((TraceNode) element);
					if (checkpoint != null) {
						return checkpoint.getConditionString();
					} else {
						return "";
					}
				} else {
					return "";
				}
			} else {
				// Only TraceNode has checkpoint. InvokeTraceNode doesn't.
				return "";
			}
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return editor;
		}

		@Override
		protected boolean canEdit(Object element) {
			if (element instanceof TraceNode) {
				if (((TraceNode) element).equals(lastTraceNodeCache)) {
					return true;
				}
			}
			return false;
		}

	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void handleEvent(String eventCause, String eventKind, DynamicTranslator dynamicTranslator) {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				System.out.println("handle ITraceEvent");
				// 在 setInput 之前 setAutoExpandLevel.
				viewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
				ArrayList<InvokeTraceNode> currentTrace = dynamicTranslator.getCurrentTrace(); 
				lastTraceNodeCache = calculateLastOnTree(currentTrace);
				CollectCheckpointsHandler.assignIDtoTrace(currentTrace, false);
				viewer.setInput(currentTrace);
			}
		});

	}

	/**
	 * 递归地找到 Trace 树的最后一个结点 <br>
	 * 
	 * @param currentTrace
	 *            是 InvokeTraceNode 们
	 * @return The last TraceNode on trace. <b>null</b> if not existing, or the
	 *         last node is an InvokeTraceNode instead of TraceNode.
	 */
	private static TraceNode calculateLastOnTree(ArrayList<InvokeTraceNode> currentTrace) {
		if (currentTrace.isEmpty()) {
			return null;
		} else {
			InvokeTraceNode lastITN = currentTrace.get(currentTrace.size() - 1);
			ArrayList<TraceNode> tns = lastITN.getCalleeTrace();
			if (tns.isEmpty()) {
				return null;
			} else {
				TraceNode lastTN = tns.get(tns.size() - 1);
				ArrayList<InvokeTraceNode> calleeList = lastTN.getCalleeList();
				if (calleeList.isEmpty()) {
					return lastTN;
				} else {
					return calculateLastOnTree(calleeList);
				}
			}
		}
	}

	

	public void clear() {
		if (viewer != null)
			viewer.setInput(new Object());
	}

	// @Override // ITraceEventAllDoneListener
	// public void handleEvent() {
	// Display.getDefault().asyncExec(new Runnable() {
	// @Override
	// public void run() {
	//// viewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
	//// viewer.setInput(traceRoots);
	// viewer.setInput(null);
	// System.out.println("ITraceEventAllDoneListener handleEvent 触发");
	// }
	// });
	// }

}
