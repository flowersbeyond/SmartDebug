package cn.edu.thu.tsmart.tool.da.ui.views;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

import cn.edu.thu.tsmart.tool.da.core.fl.BasicBlock;
import cn.edu.thu.tsmart.tool.da.core.fl.FaultLocalizationListener;
import cn.edu.thu.tsmart.tool.da.core.fl.FaultLocalizer;

import cn.edu.thu.tsmart.tool.da.ui.Activator;

public class FaultLocalizationView extends ViewPart implements FaultLocalizationListener, ISelectionChangedListener{

	public static final String ID = "cn.edu.thu.tsmart.tool.da.ui.faultlocalizationview";
	
	private ListViewer viewer;
	
	public FaultLocalizationView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new ListViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getList().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		FaultLocalizer.registerListener(this);
		//getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(ID, this);
		viewer.addPostSelectionChangedListener(this);
		getSite().setSelectionProvider(viewer);
	}
	
	@Override
	public void dispose(){
		FaultLocalizer.removeListener(this);
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
				ArrayList<?> blockList = (ArrayList<?>)inputElement;
				return blockList.toArray();
			}
			return new Object[]{new Object()};
		}
			
	}
	
	class ViewLabelProvider extends LabelProvider implements ILabelProvider{
		@Override
		public String getText(Object obj){
			if(obj instanceof BasicBlock){
				String text = ((BasicBlock)obj).toString();
				if(text.startsWith("ACT:"))
					text = text.substring(4);
				else if (text.startsWith("DEC:"))
					text = text.substring(4);
				return text;
			}
			return "";
		}
		
		@Override
		public Image getImage(Object obj){			
			/*if(obj instanceof ActionBlock){
				ImageDescriptor id = Activator.getImageDescriptor("icons/actionblock.gif");
				Image img = id.createImage();
				return img;
			}
			else if (obj instanceof DecisionBlock){
				ImageDescriptor id = Activator.getImageDescriptor("icons/decisionblock.gif");
				Image img = id.createImage();
				return img;
			}*/
			return null;
		}
	}

	@Override
	public void localizationFinished(ArrayList<BasicBlock> localizationResult) {
		Display.getDefault().asyncExec(new Runnable(){

			@Override
			public void run() {
				viewer.setInput(localizationResult);
			}
			
		});
		
	}
	
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		// TODO Auto-generated method stub
		ISelection selection = event.getSelection();
		
		if(selection instanceof IStructuredSelection){
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if(element instanceof BasicBlock){
				/*
				 if(element instanceof ActionBlock){
				 	ActionBlock ablock = (ActionBlock)element;
					ArrayList<ASTNode> statements = (ArrayList<ASTNode>) ablock.getStatements();
					if(statements.size() > 0){
						ASTNode stmt = statements.get(0);
						ASTNode root = stmt.getRoot();
						if(root instanceof CompilationUnit){
							try {
								IResource resource;
								resource = ((CompilationUnit) root).getJavaElement().getUnderlyingResource();								
								if(resource instanceof IFile){
									IFile srcFile = (IFile)resource;
									IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
									ITextEditor editor = (ITextEditor) IDE.openEditor(page, srcFile);
									int offset = statements.get(0).getStartPosition();
									ASTNode lastStatement = statements.get(statements.size() - 1);
									int length = lastStatement.getStartPosition() + lastStatement.getLength() - offset;	
									editor.selectAndReveal(offset, length);
								}
							} catch (JavaModelException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (PartInitException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					
				}
				else if (element instanceof DecisionBlock){
					DecisionBlock dblock = (DecisionBlock)element;
					Statement stmt = dblock.getConditionSource();
					ASTNode root = stmt.getRoot();
					if(root instanceof CompilationUnit){
						try {
							IResource resource;
							resource = ((CompilationUnit) root).getJavaElement().getUnderlyingResource();								
							if(resource instanceof IFile){
								IFile srcFile = (IFile)resource;
								IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
								ITextEditor editor = (ITextEditor) IDE.openEditor(page, srcFile);
								int offset = stmt.getStartPosition();
								int length = stmt.getLength();
								editor.selectAndReveal(offset, length);
							}
						} catch (JavaModelException e) {
							e.printStackTrace();
						} catch (PartInitException e) {
							e.printStackTrace();
						}
					}
				}*/
			}
		}
		
	}

	public void clear() {
		if(viewer != null)
			viewer.setInput(new Object());		
	}

}

