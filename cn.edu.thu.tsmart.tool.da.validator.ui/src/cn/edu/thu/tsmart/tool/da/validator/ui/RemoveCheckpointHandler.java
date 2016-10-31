package cn.edu.thu.tsmart.tool.da.validator.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;

import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointManager;

public class RemoveCheckpointHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CheckpointView view = (CheckpointView)PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().findView(CheckpointView.ID);
		view.removeSelectedCheckpoint();
		view.onUpdateContent(CheckpointManager.getInstance());
		
		return null;
	}

}
