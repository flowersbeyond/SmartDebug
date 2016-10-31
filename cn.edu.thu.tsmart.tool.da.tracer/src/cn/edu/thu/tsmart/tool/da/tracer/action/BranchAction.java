package cn.edu.thu.tsmart.tool.da.tracer.action;

public class BranchAction extends TraceAction{
	
	public static int IF_FALL_THROUGH = 0;
	public static int IF_BRANCH = 1;
	public static int GOTO = 2;
	public static int SWITCH_TO = 3;
	
	private int branchtype;
	
	public BranchAction(String className, String methodName,
			String methodSignature, ActionType type, int label, int branchtype) {
		super(className, methodName, methodSignature, type, label);
		this.branchtype = branchtype;
	}
	
	public int getBranchType(){
		return branchtype;
	}
	
}
