package cn.edu.thu.tsmart.tool.da.tracer.util;

import java.util.ArrayList;

import cn.edu.thu.tsmart.tool.da.tracer.trace.InvokeTraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.TraceNode;

public class PrettyPrinter {
	
	public static void prettyPrint(ArrayList<TraceNode> trace){
		for(int i = 0; i < trace.size(); i ++){
			prettyPrint(trace.get(i), 0);
		}
	}
	
	private static void prettyPrint(TraceNode node, int indent){
		String indentStr = "";
		for(int i = 0; i < indent; i ++)
			indentStr += "\t";
		
		System.out.println(indentStr + node.toString());
		ArrayList<InvokeTraceNode> invokelist = node.getCalleeList();
		for(int i = 0; i < invokelist.size(); i ++){
			System.out.println(indentStr + "`-" + invokelist.get(i).toString());
			ArrayList<TraceNode> calleetrace = invokelist.get(i).getCalleeTrace();
			for(int j = 0; j < calleetrace.size(); j ++){
				prettyPrint(calleetrace.get(j), indent + 1);
			}
		}
	}
	
	

}
