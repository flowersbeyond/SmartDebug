package cn.edu.thu.tsmart.tool.da.tracer.action;

public class InvokeAction extends TraceAction{

	private String calleeClassName;
	private String calleeMethodName;
	private String calleeMethodSignature;
	
	public InvokeAction(String className, String methodName,
			String methodSignature, ActionType type, int label,
			String calleeClassName, String calleeMethodName, String calleeMethodSignature) {
		super(className, methodName, methodSignature, type, label);
		this.calleeClassName = calleeClassName;
		this.calleeMethodName = calleeMethodName;
		this.calleeMethodSignature = calleeMethodSignature;
	}
	
	public String getCalleeClassName(){
		return calleeClassName;
	}
	public String getCalleeMethodName(){
		return calleeMethodName;
	}
	
	public String getCalleeMethodSignature(){
		return calleeMethodSignature;
	}

}
