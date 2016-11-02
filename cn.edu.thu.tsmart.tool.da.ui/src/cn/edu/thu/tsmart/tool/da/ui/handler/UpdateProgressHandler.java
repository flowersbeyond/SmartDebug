package cn.edu.thu.tsmart.tool.da.ui.handler;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import cn.edu.thu.tsmart.tool.da.core.SmartDebugPlugin;
import cn.edu.thu.tsmart.tool.da.validator.ui.CheckpointView;


public class UpdateProgressHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		SmartDebugPlugin.getLastFixSession().getBugFixer().updateDebugProcess();
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		CheckpointView view = (CheckpointView) page.findView("cn.thu.edu.thss.tsmart.tool.da.validator.CheckpointView");
		if(view != null)
			view.refresh();
		return null;
	}

}
