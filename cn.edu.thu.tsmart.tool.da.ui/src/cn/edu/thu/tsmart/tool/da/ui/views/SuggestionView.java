package cn.edu.thu.tsmart.tool.da.ui.views;

import java.util.ArrayList;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;
import cn.edu.thu.tsmart.tool.da.core.suggestion.ISuggestionListener;
import cn.edu.thu.tsmart.tool.da.core.suggestion.SuggestionManager;
import cn.edu.thu.tsmart.tool.da.ui.Activator;


public class SuggestionView extends ViewPart implements ISuggestionListener, ISelectionChangedListener{
	
public static final String ID = "cn.edu.thu.tsmart.tool.da.ui.views.suggestionview";
	
	private ListViewer viewer;
	
	public SuggestionView(){
		
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new ListViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getList().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		SuggestionManager.registerSuggestionListener(this);
		//getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(ID, this);
		viewer.addPostSelectionChangedListener(this);
		getSite().setSelectionProvider(viewer);
	}
	
	@Override
	public void dispose(){
		SuggestionManager.removeSuggestionListener(this);
		viewer.removePostSelectionChangedListener(this);
	}
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	
	class ViewContentProvider extends ArrayContentProvider{
		@Override
		public Object[] getElements(Object inputElement) {
			if(inputElement instanceof ArrayList<?>){
				ArrayList<?> suggestionList = (ArrayList<?>)inputElement;
				return suggestionList.toArray();
			}
			return new Object[]{new Object()};
		}
			
	}
	
	class ViewLabelProvider extends LabelProvider implements ILabelProvider{
		@Override
		public String getText(Object obj){
			
			if(obj instanceof Fix){
				return ((Fix)obj).toString();
			}
			return "";
		}
		
		@Override
		public Image getImage(Object obj){			
			if(obj instanceof Fix){
				ImageDescriptor id = Activator.getImageDescriptor("icons/suggestion.gif");
				Image img = id.createImage();
				return img;
			}
			return null;
		}
	}


	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		// TODO Auto-generated method stub
		ISelection selection = event.getSelection();
		
		if(selection instanceof IStructuredSelection){
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if(element instanceof Fix){
				Fix suggestion = (Fix)element;
			}
		}
		
	}

	@Override
	public void suggestionChanged(ArrayList<Fix> suggestions) {
		Display.getDefault().asyncExec(new Runnable(){

			@Override
			public void run() {
				viewer.setInput(suggestions);	
			}
			
		});
			
	}

	public void applySelectedSuggestion() {
		ISelection selection = viewer.getSelection();
		if(selection instanceof IStructuredSelection){
			Object element = ((IStructuredSelection) selection).getFirstElement();
			if(element instanceof Fix){
				((Fix) element).doFix();
			}
		}
		
	}

	public void clear() {
		if(viewer!= null)
			viewer.setInput(new ArrayList<Object>());
		
	}

}
