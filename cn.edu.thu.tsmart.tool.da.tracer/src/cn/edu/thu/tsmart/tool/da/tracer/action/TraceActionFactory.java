package cn.edu.thu.tsmart.tool.da.tracer.action;

import cn.edu.thu.tsmart.tool.da.tracer.Instrumentor;

public class TraceActionFactory {
	public static TraceAction parse(String actionString){
		if(!actionString.startsWith("[TRACE:]")){
			return null;
		}
		String rawString = actionString.substring(Instrumentor.traceprefix.length());
		String [] parts = rawString.split(":");
		String className = parts[0];
		String methodName = parts[1];
		String methodSignature = parts[2];
		String actionTypeString = parts[3];
		int label = Integer.parseInt(parts[4]);
		if(actionTypeString.equals(Instrumentor.ENTER)){
			return new EnterAction(className, methodName, methodSignature,
					ActionType.ENTER_METHOD,label);
		}
		if(actionTypeString.equals(Instrumentor.EXIT)){
			return new ExitAction(className, methodName, methodSignature,
					ActionType.EXIT_METHOD,label);
		}
		if(actionTypeString.equals(Instrumentor.IF_BRANCH_TO)){
			return new BranchAction(className, methodName, methodSignature,
					ActionType.BRANCH,label, BranchAction.IF_BRANCH);
		}
		if(actionTypeString.equals(Instrumentor.IF_BRANCH_FALL_THROUGH)){
			return new BranchAction(className, methodName, methodSignature,
					ActionType.BRANCH,label, BranchAction.IF_FALL_THROUGH);
		}
		if(actionTypeString.equals(Instrumentor.GOTO)){
			return new BranchAction(className, methodName, methodSignature,
					ActionType.BRANCH,label, BranchAction.GOTO);
		}
		if(actionTypeString.equals(Instrumentor.SWITCH)){
			return new BranchAction(className, methodName, methodSignature,
					ActionType.BRANCH,label, BranchAction.SWITCH_TO);
		}
		if(actionTypeString.equals(Instrumentor.INVOKE)){
			String calleeClassName = parts[5];
			String calleeMethodName = parts[6];
			String calleeMethodSignature = parts[7];
			return new InvokeAction(className, methodName, methodSignature,
					ActionType.INVOKE_METHOD, label, 
					calleeClassName, calleeMethodName, calleeMethodSignature);
		}
		return null;
	}
}
