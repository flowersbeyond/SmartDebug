package cn.edu.thu.tsmart.tool.da.tracer.action;

public class TraceAction {
	protected String className;
	protected String methodName;
	protected String methodSignature;
	protected ActionType type;	
	protected int label;
	
	public TraceAction(String className, String methodName, String methodSignature, 
			ActionType type, int label){
		this.className = className;
		this.methodName = methodName;
		this.methodSignature = methodSignature;
		this.type = type;
		this.label = label;
	}
	
	public String getClassName(){
		return className;
	}
	
	public String getMethodName(){
		return methodName;
	}
	
	public String getMethodSignature(){
		return methodSignature;
	}
	
	public int getInstructionIndex(){
		return label;
	}
}
