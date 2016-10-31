package cn.edu.thu.tsmart.tool.da.tracer.action;

public class EnterAction extends TraceAction{

	public EnterAction(String className, String methodName,
			String methodSignature, ActionType type, int label) {
		super(className, methodName, methodSignature, type, label);
	}
	
}
