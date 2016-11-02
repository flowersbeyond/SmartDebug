package cn.edu.thu.tsmart.tool.da.core.validator.cp;

public class ConditionItem {
	
	private String hitCondition;
	private String expectation;
	private StatusCode status;	
	private int hitCount;
	private int failHitCount = -1;
	
	
	public ConditionItem(String hitCondition, String expectation){
		this.hitCondition = hitCondition;
		this.expectation = expectation;
		this.status = StatusCode.UNKNOWN;
	}

	
	public void setPassed(){
		this.status = StatusCode.PASSED;
	}
	
	public void setFailed(){
		this.status = StatusCode.FAILED;
	}
	
	public void setUnknown(){
		this.status = StatusCode.UNKNOWN;
	}
	
	public String getHitCondition() {
		return hitCondition;
	}

	public void setHitCondition(String hitCondition) {
		this.hitCondition = hitCondition;
	}

	public void setHitCount(int count){
		this.hitCount = count;
	}
	
	public int getHitCount(){
		return this.hitCount;
	}
	
	public void increaseHitCount(){
		this.hitCount ++;
	}
	
	public String getExpectation() {
		return expectation;
	}

	public void setExpecation(String conditionExpr) {
		this.expectation = conditionExpr;
	}
	
	public StatusCode getStatus(){
		return status;
	}
	
	public int getFailHitTime(){
		return this.failHitCount;
	}


	public void setFailHitTime(int hitCount) {
		this.failHitCount = hitCount;
	}


	public void setOutOfDate() {
		this.status = StatusCode.OUT_OF_DATE;
		
	}

}
