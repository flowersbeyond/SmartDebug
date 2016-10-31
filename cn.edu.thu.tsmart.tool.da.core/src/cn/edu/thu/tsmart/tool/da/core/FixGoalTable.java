package cn.edu.thu.tsmart.tool.da.core;

import java.util.HashMap;

import org.eclipse.jdt.core.IMethod;

import cn.edu.thu.tsmart.tool.da.core.validator.cp.Checkpoint;

public class FixGoalTable {
	
	
	private HashMap<IMethod, FixGoal> table = new HashMap<IMethod, FixGoal>();
	
	public void registerFixGoal(IMethod testMethod, Checkpoint resolveGoalCP, boolean testPassed){
		table.put(testMethod, new FixGoal(resolveGoalCP, testPassed));
	}
	
	public boolean methodShouldPass(IMethod testMethod){
		FixGoal goal = table.get(testMethod);
		if(goal != null && goal.testShouldPass()){
			return true;
		}
		return false;
	}

}
class FixGoal{
	private Checkpoint resolveGoalCP;
	
	private boolean testPassed;
	public FixGoal(Checkpoint resolveGoalCP, boolean testPassed){
		this.resolveGoalCP = resolveGoalCP;
		this.testPassed = testPassed;
	}
	public Checkpoint getResolveGoalCP() {
		return resolveGoalCP;
	}
	public boolean testShouldPass() {
		return testPassed;
	}
}
