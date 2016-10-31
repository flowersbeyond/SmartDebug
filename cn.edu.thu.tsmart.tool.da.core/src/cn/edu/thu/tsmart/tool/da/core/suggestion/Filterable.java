package cn.edu.thu.tsmart.tool.da.core.suggestion;

import org.eclipse.jdt.core.dom.Expression;

public interface Filterable {
	
	public boolean hasExpectedValue();
	public String getExpectedValue();
	
	public Expression getTargetExpression();
	public String getNewExprString();
	
	public String getTargetExprType();

}
