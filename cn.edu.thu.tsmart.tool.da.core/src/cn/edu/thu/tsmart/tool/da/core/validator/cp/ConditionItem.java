package cn.edu.thu.tsmart.tool.da.core.validator.cp;

public class ConditionItem {
	
	private String hitCondition;
	private String expectation;
	
	public ConditionItem(String hitCondition, String expectation){
		this.hitCondition = hitCondition;
		this.expectation = expectation;
	}

	public String getHitCount() {
		return hitCondition;
	}

	public void setHitCount(String hitCount) {
		this.hitCondition = hitCount;
	}

	public String getConditionExpr() {
		return expectation;
	}

	public void setConditionExpr(String conditionExpr) {
		this.expectation = conditionExpr;
	}

}
