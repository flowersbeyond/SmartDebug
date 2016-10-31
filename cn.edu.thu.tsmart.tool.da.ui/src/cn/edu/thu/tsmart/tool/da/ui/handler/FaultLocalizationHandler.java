package cn.edu.thu.tsmart.tool.da.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class FaultLocalizationHandler extends AbstractHandler {

	/**
	 * Do FL and println
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("fl handler executed");
		
		// 周一走之前看 FaultLocalizer#localize() 的结论: 好难择出来用啊...
		
		return null;
	}

}
