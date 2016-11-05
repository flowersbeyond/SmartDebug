package cn.edu.thu.tsmart.tool.da.ui.handler;

import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElementContainer;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

import cn.edu.thu.tsmart.tool.da.core.Logger;
import cn.edu.thu.tsmart.tool.da.core.SmartDebugPlugin;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.Checkpoint;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.StatusCode;
import cn.edu.thu.tsmart.tool.da.ui.views.FaultLocalizationView;
import cn.edu.thu.tsmart.tool.da.ui.views.SuggestionView;
import cn.edu.thu.tsmart.tool.da.validator.ui.CheckpointView;

public class StepResolveHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("Start Step Resolving Bugs");
		
		
		
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		try {
			PlatformUI.getWorkbench().showPerspective(
					"org.eclipse.debug.ui.DebugPerspective", window);
		} catch (WorkbenchException e1) {
			e1.printStackTrace();
		}
		
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		FaultLocalizationView flView = (FaultLocalizationView) page.findView(FaultLocalizationView.ID);		
		if(flView != null){
			flView.clear();
		}
		
		SuggestionView suggestView = (SuggestionView)page.findView(SuggestionView.ID);
		if(suggestView != null){
			suggestView.clear();
		}
		
		/*TraceView traceView = (TraceView)page.findView(TraceView.ID);
		if(traceView != null){
			traceView.clear();
		}*/
		if(SmartDebugPlugin.getLastFixSession() == null){
		TestRunnerViewPart viewpart = (TestRunnerViewPart) page.findView(TestRunnerViewPart.NAME);
		
		
		ArrayList<TestRunSession> sessions = (ArrayList<TestRunSession>) (JUnitCorePlugin
				.getModel().getTestRunSessions());
		if(sessions.size() > 0){
			ITestElement element = sessions.get(0).getChildren()[0];
			IJavaProject project = sessions.get(0).getLaunchedProject();
			while(element instanceof ITestElementContainer){
				element = ((ITestElementContainer)element).getChildren()[0];
			}
			if(element instanceof ITestCaseElement){
				String testClassName = ((ITestCaseElement) element).getTestClassName();
				try {
					String projDir = project.getProject().getLocation().toOSString();
					Logger.setLogRootDir(projDir + "/data/");
					IType type = project.findType(testClassName);
					SmartDebugPlugin.getDefault().startNewSession(project, type);
				} catch (JavaModelException e) {
					e.printStackTrace();
				}

			}
		}
		}
		
		if(SmartDebugPlugin.getLastFixSession() != null){
		
			CheckpointView view = (CheckpointView)PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage().findView(CheckpointView.ID);
			
			Checkpoint cp = view.getSelectedCheckpoint();
			if(cp != null && cp.getStatus() == StatusCode.FAILED){
				SmartDebugPlugin.getLastFixSession().setFixGoal(cp);
				SmartDebugPlugin.getLastFixSession().clearUpCache();
				SmartDebugPlugin.getLastFixSession().getBugFixer().schedule();
			}
		}
		return null;
	}

}
