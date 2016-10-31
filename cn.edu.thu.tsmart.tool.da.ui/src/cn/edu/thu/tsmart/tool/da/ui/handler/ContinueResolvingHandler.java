package cn.edu.thu.tsmart.tool.da.ui.handler;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import cn.edu.thu.tsmart.tool.da.core.SmartDebugPlugin;


public class ContinueResolvingHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		SmartDebugPlugin.getDefault().continueLastSession();
		return null;
	}

}
