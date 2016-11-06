package cn.edu.thu.tsmart.tool.da.validator.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.part.ViewPart;

import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.Checkpoint;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointManager;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.ConditionItem;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.StatusCode;
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
			//viewer.setInput(newInput);
			return;
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
	
	class TableLabelProvider implements ITableLabelProvider {
		public Image getColumnImage(Object element, int columnIndex){
			switch (columnIndex) {
			case 0: {
				StatusCode status = null;
				if(element instanceof TestCase){
					status = ((TestCase) element).getStatus();
				} else if (element instanceof Checkpoint){
					status = ((Checkpoint) element).getStatus();
				} else if (element instanceof ConditionItem){
					status = ((ConditionItem) element).getStatus();
				}
				if(status != null){
					switch(status){
						case PASSED:{				
							ImageDescriptor id = Activator.getImageDescriptor("icons/pass.png");
							Image img = id.createImage();
							return img;
						}
						case FAILED:{
							ImageDescriptor id = Activator.getImageDescriptor("icons/fail.png");
							Image img = id.createImage();
							return img;
						}
						case UNKNOWN:{
							ImageDescriptor id = Activator.getImageDescriptor("icons/unknown.jpg");
							Image img = id.createImage();
							return img;
						}
					}
				}
			}
			}
			return null;
		}
		
		public String getColumnText(Object element, int columnIndex){
			switch (columnIndex) {
			case 0: {
				String text = "";
				if(element instanceof TestCase){
					text = ((TestCase) element).getClassName() 
							+ ": " + ((TestCase)element).getMethodName();
				}
				else if(element instanceof Checkpoint){
					Checkpoint cp = (Checkpoint)element;
					text = cp.getTypeName() + ": Line " + cp.getLineNumber();
				}
				else if(element instanceof ConditionItem){
					text = "Specification:";
				}
				return text;
			}
			case 1:
				if(element instanceof ConditionItem){
					return ((ConditionItem) element).getHitCondition();
				}
			case 2:
				if(element instanceof ConditionItem){
					return ((ConditionItem) element).getExpectation();
				}
			
			case 3:
				if(element instanceof ConditionItem){
					int failHitTime = ((ConditionItem)element).getFailHitTime();
					if(failHitTime < 1)
						return "";
					else
						return failHitTime - 1 + "";
				}
			}
			return null;
		}

		@Override
		public void addListener(ILabelProviderListener listener) {}

		@Override
		public void dispose() {}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {}
	}
	
	public CheckpointView() {

	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		if(cpmanager == null)
			cpmanager = CheckpointManager.getInstance();
		
		Tree cpTree = viewer.getTree();
		cpTree.setHeaderVisible(true);
		
		TreeViewerColumn column1 = new TreeViewerColumn(viewer, SWT.LEFT);
		column1.getColumn().setAlignment(SWT.LEFT);
		column1.getColumn().setText("Debug Process");
		column1.getColumn().setWidth(300);		
		
		TreeViewerColumn column2 = new TreeViewerColumn(viewer, SWT.RIGHT);
		column2.getColumn().setAlignment(SWT.LEFT);
		column2.getColumn().setText("Hit Condition");
		column2.getColumn().setWidth(150);
		column2.setEditingSupport(new CheckpointViewEditSupport(viewer, "Hit Condition", cpmanager));
		
		TreeViewerColumn column3 = new TreeViewerColumn(viewer, SWT.RIGHT);
		column3.getColumn().setAlignment(SWT.LEFT);
		column3.getColumn().setText("Expectation");
		column3.getColumn().setWidth(150);
		column3.setEditingSupport(new CheckpointViewEditSupport(viewer, "Expectation", cpmanager));
		
		TreeViewerColumn column4 = new TreeViewerColumn(viewer, SWT.RIGHT);
		column4.getColumn().setAlignment(SWT.LEFT);
		column4.getColumn().setText("Last Correct At");
		column4.getColumn().setWidth(150);
		

		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new TableLabelProvider());
		
		
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
	
	public Checkpoint getSelectedCheckpoint(){
		ISelection selection = this.viewer.getSelection();
		if(selection instanceof TreeSelection){
			TreeSelection treeSel = (TreeSelection)selection;
			Object obj = treeSel.getFirstElement();
			if(obj instanceof Checkpoint){
				return (Checkpoint)obj;
			}
		}	
		
		return null;
	}

	public void refresh() {
		if(cpmanager != null)
			this.onUpdateContent(cpmanager);
	}
}

class CheckpointViewEditSupport extends EditingSupport{

	private CellEditor editor;
	private String propertyKey;
	private TreeViewer viewer;
	private CheckpointManager cpManager;
	
	private static String[] propertyKeys = {"Hit Condition", "Expectation"};
	public CheckpointViewEditSupport(TreeViewer viewer, String propertyKey, CheckpointManager cpManager) {
		super(viewer);
		this.viewer = viewer;
		this.cpManager = cpManager;
		this.propertyKey = propertyKey;
		this.editor = new TextCellEditor(viewer.getTree());		
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return editor;
	}

	@Override
	protected boolean canEdit(Object element) {
		if(element instanceof ConditionItem)
			return true;
		return false;
	}

	@Override
	protected Object getValue(Object element) {
		if (element instanceof ConditionItem) {
			if(propertyKey.equals(propertyKeys[0])){
				return ((ConditionItem) element).getHitCondition() + "";
			} else if(propertyKey.equals(propertyKeys[1])){
				return ((ConditionItem) element).getExpectation();
			}
		}
		return null;
	}

	@Override
	protected void setValue(Object element, Object value) {
		if (element instanceof ConditionItem) {
			if(propertyKey.equals(propertyKeys[0])){			
				((ConditionItem) element).setHitCondition((String)value);			
			} else if(propertyKey.equals(propertyKeys[1])){
				((ConditionItem) element).setExpecation((String) value);
			}
			((ConditionItem)element).getOwningCheckpoint().update();
			refresh();
		}
		cpManager.setOutOfSync();
	}
	
	private void refresh(){
		viewer.setInput(cpManager);
	}
	
}
