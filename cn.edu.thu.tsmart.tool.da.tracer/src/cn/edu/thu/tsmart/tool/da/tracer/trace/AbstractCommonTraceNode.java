package cn.edu.thu.tsmart.tool.da.tracer.trace;

/**
 * 2016-07-15 引入. <br>
 * Motivation: 用 List 或 Map 给 Trace 链上每一结点编号, 因此想让 TraceNode 和 InvokeTraceNode
 * 有同一父类或实现同一接口. <br>
 * 请特别注意重写过的 equals() 和 hashCode() 方法.
 * 
 * @author LI Tianchi
 *
 */
public abstract class AbstractCommonTraceNode {
	private static final int ID_NOT_ASSIGNED = -1;
	int id = ID_NOT_ASSIGNED;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/**
	 * 至少被用于以下两方法
	 * 
	 * @see cn.edu.thu.tsmart.tool.da.core.CheckpointCollectingSession#findCheckpointFromTraceNode(TestCase
	 *      tc, TraceNode node)
	 * @see cn.edu.thu.tsmart.tool.da.core.CheckpointCollectingSession#assignCheckpointToTraceNode(CheckpointOnNode
	 *      cp, TestCase tc, TraceNode node)
	 */
	@Override
	public boolean equals(Object that) {
		return that instanceof AbstractCommonTraceNode && ((AbstractCommonTraceNode) that).id == this.id;
	}

	@Override
	public int hashCode() {
		return id;
	}

}
