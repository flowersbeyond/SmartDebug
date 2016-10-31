package cn.edu.thu.tsmart.tool.da.tracer.action;

public class SuspendAction extends TraceAction{
	
	private int hitLineNum;

	public SuspendAction(String className, String methodName,
			String methodSignature, ActionType type, int label, int hitLineNum) {
		super(className, methodName, methodSignature, type, -1);
		this.hitLineNum = hitLineNum;
	}
	
	public int getHitLineNum(){
		return hitLineNum;
	}

}
