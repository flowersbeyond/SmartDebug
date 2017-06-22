package cn.edu.thu.tsmart.tool.da.validator.ui;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import cn.edu.thu.tsmart.tool.da.core.validator.cp.Checkpoint;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointManager;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.ConditionItem;

public class CheckpointDialog extends Dialog {

	private static class ContentProvider implements IStructuredContentProvider {
		
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ArrayList<?>)
				return ((ArrayList<?>) inputElement).toArray();
			return new Object[0];
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	private TableViewer tableViewer;
	private ArrayList<ConditionItem> conditions;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public CheckpointDialog(Shell parentShell, ArrayList<ConditionItem> conditions){
		super(parentShell);
		setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE);
		
		this.conditions = new ArrayList<ConditionItem>();
		for(ConditionItem item: conditions){
			ConditionItem copyItem = new ConditionItem(item.getHitCondition(), item.getExpectation());
			this.conditions.add(copyItem);
		}
		if(conditions.size() == 0){
			ConditionItem exampleItem = new ConditionItem("", "");
			this.conditions.add(exampleItem);
		}
		else{
			ConditionItem lastItem = this.conditions.get(this.conditions.size() - 1);
			if(!(lastItem.getHitCondition().equals("") && lastItem.getExpectation().equals(""))){
				ConditionItem exampleItem = new ConditionItem("", "");
				this.conditions.add(exampleItem);
			}
		}
	
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) container.getLayout();
		gridLayout.horizontalSpacing = 3;
		gridLayout.verticalSpacing = 2;

		Label lblGuideText = new Label(container, SWT.NONE);
		GridData gd_lblGuideText = new GridData(SWT.LEFT, SWT.FILL, false,
				false, 1, 1);
		gd_lblGuideText.heightHint = 30;
		lblGuideText.setLayoutData(gd_lblGuideText);
		lblGuideText.setText("Please enter your expectations of the program:");

		tableViewer = new TableViewer(container, SWT.BORDER
				| SWT.FULL_SELECTION);
		tableViewer
				.setColumnProperties(new String[] { "Hit Count", "Condition" });
		Table table = tableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData gd_table = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
		gd_table.widthHint = 400;
		gd_table.heightHint = 175;
		table.setLayoutData(gd_table);
		tableViewer.setContentProvider(new ContentProvider());
		// tableViewer.setLabelProvider(new TableLabelProvider());

		TableViewerColumn hitCountColumn = new TableViewerColumn(tableViewer,
				SWT.NONE);
		TableColumn tblclmnHitCount = hitCountColumn.getColumn();
		tblclmnHitCount.setWidth(150);
		tblclmnHitCount.setText("Hit Condition");
		hitCountColumn.setEditingSupport(new ConditionItemEditSupport(tableViewer, "Hit Condition", conditions));
		hitCountColumn.setLabelProvider(new ColumnLabelProvider() {
			public Image getImage(Object element) {
				return null;
			}

			public String getText(Object element) {
				if (element != null) {
					if (element instanceof ConditionItem) {
						String hitCondition = ((ConditionItem) element).getHitCondition();
						if (hitCondition.equals("")) {
							return "New Specification";
						} else
							return ((ConditionItem) element).getHitCondition() + "";
					}
					return element.toString();
				}
				return "";
			}
		});

		TableViewerColumn conditionColumn = new TableViewerColumn(tableViewer,
				SWT.NONE);
		TableColumn tblclmnCondition = conditionColumn.getColumn();
		tblclmnCondition.setWidth(247);
		tblclmnCondition.setText("Expectation");
		conditionColumn.setEditingSupport(new ConditionItemEditSupport(tableViewer, "Expectation", conditions));
		conditionColumn.setLabelProvider(new ColumnLabelProvider() {
			public Image getImage(Object element) {
				return null;
			}

			public String getText(Object element) {
				if (element != null) {
					if (element instanceof ConditionItem) {
						if(!((ConditionItem) element).getHitCondition().equals(""))
							return ((ConditionItem) element).getExpectation();
					}
				}
				return "";
			}
		});

		tableViewer.setInput(conditions);
		//tableViewer.setCellModifier(new ConditionItemCellModifier(tableViewer));

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 400);
	}

	public ArrayList<ConditionItem> getConditions() {
		return conditions;
	}
	
	public void setConditions(ArrayList<ConditionItem> conditions) {
		this.conditions = conditions;
	}

}

class ConditionItemEditSupport extends EditingSupport{

	private CellEditor editor;
	private String propertyKey;
	private TableViewer tableViewer;
	private ArrayList<ConditionItem> conditions;
	
	private static String[] propertyKeys = {"Hit Condition", "Expectation"};
	public ConditionItemEditSupport(TableViewer viewer, String propertyKey, ArrayList<ConditionItem> conditions) {
		super(viewer);
		this.tableViewer = viewer;
		this.propertyKey = propertyKey;
		this.editor = new TextCellEditor(viewer.getTable());
		this.conditions = conditions;
		
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return editor;
	}

	@Override
	protected boolean canEdit(Object element) {
		return true;
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
				try {
					ConditionItem citem = (ConditionItem) element;
					if (value.equals("New Condition")) {
						citem.setHitCondition("");
					} else if (value.equals("Always")) {
						citem.setHitCondition(Checkpoint.HIT_ALWAYS);
					} else{
						citem.setHitCondition((String)value);
					}
				} catch (NumberFormatException e) {
					((ConditionItem) element).setHitCondition("");
				}
			} else if(propertyKey.equals(propertyKeys[1])){
				((ConditionItem) element).setExpecation((String) value);
			}
		}
		
		ArrayList<ConditionItem> removedItems = new ArrayList<ConditionItem>();
		for(ConditionItem item: conditions){
			if(item.getHitCondition().equals("")){
				removedItems.add(item);
			}
		}
		conditions.removeAll(removedItems);
		conditions.add(new ConditionItem("", "true"));
		tableViewer.refresh();
		CheckpointManager.getInstance().setOutOfSync();
	}
	
}

