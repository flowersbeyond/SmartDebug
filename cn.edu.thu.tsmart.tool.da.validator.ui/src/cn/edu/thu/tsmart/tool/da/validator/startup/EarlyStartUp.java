package cn.edu.thu.tsmart.tool.da.validator.startup;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointManager;
import cn.edu.thu.tsmart.tool.da.validator.ui.CheckpointView;
import cn.edu.thu.tsmart.tool.da.validator.ui.EditorPartListener;

public class EarlyStartUp implements IStartup {

	@Override
	public void earlyStartup() {
		CheckpointManager.getInstance().restoreCheckpoints();
		Display.getDefault().asyncExec(new Runnable(){

			@Override
			public void run() {
				IPartService service = (IPartService) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService();
				service.addPartListener(new EditorPartListener());
				
				CheckpointView view = (CheckpointView)PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage().findView(CheckpointView.ID);
				if(view != null){
					view.onUpdateContent(CheckpointManager.getInstance());
				}
			}
		});
		
	}
	
}
