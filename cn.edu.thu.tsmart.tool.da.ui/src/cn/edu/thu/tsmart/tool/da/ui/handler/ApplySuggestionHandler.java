package cn.edu.thu.tsmart.tool.da.ui.handler;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;

import cn.edu.thu.tsmart.tool.da.ui.views.SuggestionView;


public class ApplySuggestionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// TODO Auto-generated method stub
		SuggestionView view = (SuggestionView)PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().findView(SuggestionView.ID);
		view.applySelectedSuggestion();
		
		System.out.println("ApplySuggestion");
		return null;
	}

}
