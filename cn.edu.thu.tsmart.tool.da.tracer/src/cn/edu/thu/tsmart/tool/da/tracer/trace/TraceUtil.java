package cn.edu.thu.tsmart.tool.da.tracer.trace;

import java.util.ArrayList;

import com.ibm.wala.ssa.SSACFG;

/**
 * 看起来有用 TODO 问问咋用 想想能不能用
 *
 */
public class TraceUtil {

	/**
	 * 原来... 已经有了...
	 * 学姐: 可能不太好
	 * 
	 * @param node
	 * @param trace
	 * @return
	 */
	public static int getHitCount(TraceNode node, ArrayList<InvokeTraceNode> trace) {
		ArrayList<SSACFG.BasicBlock> blocksite = node.getBlockTrace();

		// TODO: optimize it:
		ArrayList<TraceNode> flattenedTrace = flattenTrace(trace);
		int hitCount = 0;
		for (TraceNode n : flattenedTrace) {
			if (n == node)
				break;
			if (n.getMethodKey().equals(node.getMethodKey())) {
				ArrayList<SSACFG.BasicBlock> blocks = n.getBlockTrace();
				if (blocks == null && blocksite == null) {
					hitCount++;
				} else {
					if (blocks != null && blocksite != null) {
						if (blocks.size() == 0 && blocksite.size() == 0)
							hitCount++;
						else {
							SSACFG.BasicBlock targetblock = blocksite.get(0);
							SSACFG.BasicBlock thisblock = blocks.get(0);
							if (targetblock == thisblock) {
								hitCount++;
							}
						}
					}
				}
			}
		}
		return hitCount;
	}

	public static ArrayList<TraceNode> flattenTrace(ArrayList<InvokeTraceNode> trace) {
		ArrayList<TraceNode> flatTrace = new ArrayList<TraceNode>();
		for (InvokeTraceNode invokenode : trace) {
			flatTrace.addAll(flattenTrace(invokenode));
		}

		return flatTrace;
	}

	private static ArrayList<TraceNode> flattenTrace(InvokeTraceNode invokenode) {
		ArrayList<TraceNode> flatTrace = new ArrayList<TraceNode>();
		ArrayList<TraceNode> subtrace = invokenode.getCalleeTrace();
		for (TraceNode node : subtrace) {
			flatTrace.add(node);
			ArrayList<InvokeTraceNode> callees = node.getCalleeList();
			for (InvokeTraceNode callee : callees) {
				flatTrace.addAll(flattenTrace(callee));
			}
		}
		return flatTrace;
	}

}
