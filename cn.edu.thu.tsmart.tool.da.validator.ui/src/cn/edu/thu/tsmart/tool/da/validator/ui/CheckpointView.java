package cn.edu.thu.tsmart.tool.da.validator.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.Checkpoint;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointManager;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.ConditionItem;
import cn.edu.thu.tsmart.tool.da.validator.Activator;

public class CheckpointView extends ViewPart {
	public static final String ID = "cn.thu.edu.thss.tsmart.tool.da.validator.CheckpointView";
	
	private TreeViewer viewer;
	private CheckpointManager cpmanager;

	public void onUpdateContent(CheckpointManager cpmanager){
		this.cpmanager = cpmanager;
		viewer.setInput(cpmanager);
		viewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
	}
	
	class ViewContentProvider implements ITreeContentProvider{

		@Override
		public void dispose() {			
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return cpmanager.getHandledTestCases().toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if(parentElement instanceof TestCase){
				return cpmanager.getConditionForTestCase((TestCase)parentElement).toArray();
			}
			if(parentElement instanceof Checkpoint){
				return ((Checkpoint)parentElement).getConditions().toArray();
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof Checkpoint){
				return ((Checkpoint) element).getOwnerTestCase();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if(element instanceof TestCase){
				return true;
			} else if (element instanceof Checkpoint){
				return true;
			}
			return false;
		}
		
	}
	
	class ViewLabelProvider extends LabelProvider implements ILabelProvider{
		
		@Override
		public String getText(Object obj){
			String text = "";
			if(obj instanceof TestCase){
				text = ((TestCase) obj).getClassName() 
						+ ": " + ((TestCase)obj).getMethodName();
			}
			else if(obj instanceof Checkpoint){
				Checkpoint cp = (Checkpoint)obj;
				try {
					text = cp.getTypeName() + ": Line " + cp.getLineNumber();
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
			else if(obj instanceof ConditionItem){
				ConditionItem item = (ConditionItem)obj;
				if(!item.getHitCount().equals("")){
					text = " Hit condition: " + item.getHitCount() + "; Expectation: "+ item.getConditionExpr();
				} else
					text = "";
			}
			return text;
		}
	
	
		@Override
		public Image getImage(Object obj){
			if(obj instanceof TestCase){
				return PlatformUI.getWorkbench()
						.getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
			}
			else if (obj instanceof Checkpoint){
				Checkpoint cp = (Checkpoint)obj;
				if(cp.getConsitionSatisfied()){
					ImageDescriptor id = Activator.getImageDescriptor("icons/Checkpoint-pass.png");
					Image img = id.createImage();
					return img;
				}
				else{
					ImageDescriptor id = Activator.getImageDescriptor("icons/checkpoint.gif");
					Image img = id.createImage();
					return img;
				}
			}
			return null;
		}
	}
	
	
	public CheckpointView() {

	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		if(cpmanager == null)
			cpmanager = CheckpointManager.getInstance();
		viewer.setInput(cpmanager);
		viewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	public void removeSelectedCheckpoint(){
		ISelection selection = this.viewer.getSelection();
		if(selection instanceof TreeSelection){
			TreeSelection treeSel = (TreeSelection)selection;
			Object obj = treeSel.getFirstElement();
			if(obj instanceof Checkpoint){
				IMarker marker = ((Checkpoint)obj).getMarker();
				try {
					marker.delete();
				} catch (CoreException e) {
					e.printStackTrace();
				}
				CheckpointManager.getInstance().removeCheckpoint((Checkpoint)obj);
			}
			
		}		
	}

}
