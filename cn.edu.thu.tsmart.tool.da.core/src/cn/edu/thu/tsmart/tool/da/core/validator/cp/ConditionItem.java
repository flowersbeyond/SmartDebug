package cn.edu.thu.tsmart.tool.da.core.validator.cp;

public class ConditionItem {
	
	private int hitCount;
	private String conditionExpr;
	
	public ConditionItem(int hitCount, String conditionExpr){
		this.hitCount = hitCount;
		this.conditionExpr = conditionExpr;
	}

	public int getHitCount() {
		return hitCount;
	}

	public void setHitCount(int hitCount) {
		this.hitCount = hitCount;
	}

	public String getConditionExpr() {
		return conditionExpr;
	}

	public void setConditionExpr(String conditionExpr) {
		this.conditionExpr = conditionExpr;
	}

}
