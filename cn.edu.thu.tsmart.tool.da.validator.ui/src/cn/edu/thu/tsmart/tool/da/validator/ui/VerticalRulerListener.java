package cn.edu.thu.tsmart.tool.da.validator.ui;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.debug.core.breakpoints.ValidBreakpointLocationLocator;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.Checkpoint;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointManager;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.ConditionItem;

public class VerticalRulerListener implements MouseListener {

	private IVerticalRulerInfo verticalRuler;
	private IFile file;

	public VerticalRulerListener(IVerticalRulerInfo ruler,
			AbstractTextEditor editor) {
		this.verticalRuler = ruler;
		this.file = (IFile) editor.getEditorInput().getAdapter(IFile.class);

	}

	@SuppressWarnings("restriction")
	@Override
	public void mouseDown(MouseEvent e) {
		TestCase currentTestCase = ValidatorTestRunListener.getCurrentRunningTestCase();
		if(currentTestCase != null){
			if((e.stateMask & SWT.CTRL) != 0 && (e.stateMask & SWT.SHIFT) == 0){
				try {						
					if(file.isAccessible()){
						IJavaElement element = JavaCore.create(file);
						if(element instanceof ICompilationUnit){
							
							//get the real line number
							ICompilationUnit unit = (ICompilationUnit)element;
							ASTParser parser = ASTParser.newParser(AST.JLS4);
					        parser.setKind(ASTParser.K_COMPILATION_UNIT);
					        parser.setSource(unit);
					        parser.setResolveBindings(true);
					        parser.setEnvironment(null, null, null, true);
					        parser.setBindingsRecovery(true);
					        final CompilationUnit ast = (CompilationUnit) parser.createAST(null);
							int lineNum = verticalRuler.toDocumentLineNumber(e.y) + 1;
							ValidBreakpointLocationLocator locator = new ValidBreakpointLocationLocator(
									ast, lineNum, false, true);
							ast.accept(locator);
							int realLineNum = locator.getLineLocation();
												
							//check if we already have a checkpoint at this line in this file
							CheckpointDialog dialog;
							Checkpoint existingCheckpoint = CheckpointManager.getInstance().findExistingCheckpoint(file, currentTestCase, realLineNum);
							//open a proper edit dialog
							if(existingCheckpoint != null)
								dialog = new CheckpointDialog(Display.getDefault().getActiveShell(), existingCheckpoint.getConditions());
							else
								dialog = new CheckpointDialog(Display.getDefault().getActiveShell(), new ArrayList<ConditionItem>());
							
							if(dialog.open() == Window.OK){
								if(existingCheckpoint != null){
									existingCheckpoint.setConditions(dialog.getConditions());
								} else {
									//create a new checkpoint here.
									IDocumentProvider provider = new TextFileDocumentProvider();
									provider.connect(file);
									
									Checkpoint checkpoint = new Checkpoint(currentTestCase, file, locator.getFullyQualifiedTypeName(), realLineNum);
									
									checkpoint.setConditions(dialog.getConditions());
									CheckpointManager.getInstance().addCheckpoint(currentTestCase, checkpoint);
									CheckpointManager.getInstance().setOutOfSync();
								}
								
								Display.getDefault().asyncExec(new Runnable(){
									@Override
									public void run(){
										CheckpointView view = (CheckpointView)PlatformUI.getWorkbench()
												.getActiveWorkbenchWindow().getActivePage().findView(CheckpointView.ID);
										if(view != null){
											view.onUpdateContent(CheckpointManager.getInstance());
										}
									}
								});
							}
						}	
					}
				} catch (CoreException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} 
			}
		}
	}

	@Override
	public void mouseUp(MouseEvent e) {}

	@Override
	public void mouseDoubleClick(MouseEvent e) {}

}
