package cn.edu.thu.tsmart.tool.da.tracer.trace;

import java.util.ArrayList;

public class InvokeTraceNode extends AbstractCommonTraceNode {

	private String calleeClassName;
	private String calleeMethodName;
	private String calleeMethodSignature;

	public InvokeTraceNode(String className, String methodName, String methodSignature) {
		this.calleeClassName = className;
		this.calleeMethodName = methodName;
		this.calleeMethodSignature = methodSignature;
	}

	private ArrayList<TraceNode> calleeTrace;

	public ArrayList<TraceNode> getCalleeTrace() {
		return calleeTrace;
	}

	public void setCalleeTrace(ArrayList<TraceNode> calleeTrace) {
		this.calleeTrace = calleeTrace;
		for (int i = 0; i < calleeTrace.size(); i++) {
			calleeTrace.get(i).setCallerNode(this);
		}
	}

	private TraceNode callSiteNode;

	public void setCallSiteNode(TraceNode callerNode) {
		this.callSiteNode = callerNode;
	}

	public TraceNode getCallSiteNode() {
		return this.callSiteNode;
	}

	@Override
	public String toString() {
		return calleeClassName + ":" + calleeMethodName + ":" + calleeMethodSignature;
	}
	
	public boolean correspondsToCallee(String className, String methodName, String methodSignature){
		if(this.calleeMethodName.equals(methodName) 
				&& this.calleeMethodSignature.equals(methodSignature)){
			return true;
		}
		return false;
	}

}
