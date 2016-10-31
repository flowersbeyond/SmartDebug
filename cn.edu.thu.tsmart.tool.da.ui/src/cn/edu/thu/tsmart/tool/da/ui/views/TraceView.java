package cn.edu.thu.tsmart.tool.da.ui.views;

import java.util.ArrayList;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import cn.edu.thu.tsmart.tool.da.tracer.DynamicTranslator;
import cn.edu.thu.tsmart.tool.da.tracer.ITraceEventListener;
import cn.edu.thu.tsmart.tool.da.tracer.trace.InvokeTraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.TraceNode;

public class TraceView extends ViewPart implements ITraceEventListener{
	public static final String ID = "cn.edu.thu.thss.tsmart.tool.da.trace.views.TraceView";
	
	private TreeViewer viewer;
	private ArrayList<InvokeTraceNode> traceRoots;
	
	class ViewContentProvider implements ITreeContentProvider{

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if(inputElement instanceof ArrayList<?>)
				return ((ArrayList)inputElement).toArray();
			return new Object[]{new Object()};
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if(parentElement instanceof InvokeTraceNode){
				return ((InvokeTraceNode) parentElement).getCalleeTrace().toArray();
			} else if(parentElement instanceof TraceNode){
				return ((TraceNode) parentElement).getCalleeList().toArray();
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			if(element instanceof InvokeTraceNode){
				return ((InvokeTraceNode) element).getCallSiteNode();
			} else if (element instanceof TraceNode){
				return ((TraceNode) element).getCallerNode();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if(element instanceof InvokeTraceNode){
				return (!((InvokeTraceNode) element).getCalleeTrace().isEmpty());
			} else if (element instanceof TraceNode){
				return (!((TraceNode) element).getCalleeList().isEmpty());
			}
			return false;
		}

		/*
		@Override
		public void dispose() {			
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if(inputElement instanceof ArrayList<?>)
				return ((ArrayList)inputElement).toArray();
			return new Object[]{new Object()};
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if(parentElement instanceof ASTTrace){
				ASTTrace astTrace = (ASTTrace) parentElement;
				ArrayList<ASTTraceNode> nodes = astTrace.getTrace();
				return nodes.toArray();
			}
			else if(parentElement instanceof DummyASTTraceNode){
				ASTTrace subTrace = ((DummyASTTraceNode) parentElement).getSubTrace();
				return new Object[]{subTrace};
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			if(element instanceof ASTTraceNode){
				return ((ASTTraceNode) element).getParentTrace();
			}
			else if (element instanceof ASTTrace){
				return ((ASTTrace) element).getParentDummyNode();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if(element instanceof DummyASTTraceNode){
				if(((DummyASTTraceNode) element).getSubTrace() != null)
					return true;
			} else if (element instanceof ASTTrace){
				return true;
			}
			return false;
		}
		*/
		
	}
	
	class ViewLabelProvider extends LabelProvider implements ILabelProvider{
		
		/*
		@Override
		public String getText(Object obj){
			
			if(obj instanceof ASTTrace){
				MethodDeclaration md = ((ASTTrace) obj).getMethodDeclaration();
				IMethodBinding mb = md.resolveBinding();
				StringBuffer text = new StringBuffer("");
				text.append("Call method: ");
				text.append(mb.getDeclaringClass().getQualifiedName());
				text.append(":");
				text.append(mb.getName());
				
				return text.toString();
			} else if(obj instanceof DecisionASTTraceNode){
				ASTNode node = ((DecisionASTTraceNode) obj).getASTNode();
				String text = "? " + node.toString() + " ? :" + ((DecisionASTTraceNode) obj).getValue();
				return text;
			} else if(obj instanceof DummyASTTraceNode){
				ASTNode node = ((DummyASTTraceNode) obj).getASTNode();
				String text = "`-> " + node.toString();
				return text;
			} else if(obj instanceof ReturnASTTraceNode){
				ASTNode node = ((ReturnASTTraceNode)obj).getASTNode();
				String text = "<- " + node.toString() + " : = " +((ReturnASTTraceNode)obj).getReturnValue();
				return text;
			} else if(obj instanceof SuspendASTTraceNode){
				SuspendASTTraceNode node = (SuspendASTTraceNode)obj;
				String text = "CP: " + node.getClassName() + ": " + node.getMethodName() + ": Line" + node.getLineNum();
				return text;
			} else if(obj instanceof ASTTraceNode){
				ASTNode node = ((ASTTraceNode)obj).getASTNode();
				String text = node.toString();
				return text;
			}
			return "";
		}*/
		
		@Override
		public String getText(Object obj){
			return obj.toString();
		}
		
		@Override
		public Image getImage(Object obj){
			return PlatformUI.getWorkbench()
					.getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}
	
	public TraceView() {

	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setInput(traceRoots);
		DynamicTranslator.registerTraceEventListener(this);
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}


	@Override
	public void handleEvent(String eventCause, String eventKind,
			DynamicTranslator dynamicTranslator) {
		Display.getDefault().asyncExec(new Runnable(){

			@Override
			public void run() {
				viewer.setInput(dynamicTranslator.getCurrentTrace());
				viewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);			
			}
			
		});
				
	}

	public void clear() {
		if(viewer != null)
			viewer.setInput(new Object());
	}

}
