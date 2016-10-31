package cn.edu.thu.tsmart.tool.da.core.search.fixSite;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class ConditionFixSite extends FixSite{

	private ASTNode conditionExpr;
	private ASTNode wrappingStmt;
	private boolean switchCondition;
	
	public ConditionFixSite(IFile file, String qualifiedTypeName, int lineNumStart, int lineNumEnd,
			ASTNode conditionExpr, ASTNode wrappingStmt, boolean switchCondition) {
		super(file, qualifiedTypeName, lineNumStart, lineNumEnd);
		this.conditionExpr = conditionExpr;
		this.wrappingStmt = wrappingStmt;
		this.switchCondition = switchCondition;
	}
	
	public ASTNode getConditionExpression(){
		return conditionExpr;
	}
	
	public ASTNode getWrappingStatement(){
		return wrappingStmt;
	}
	
	public boolean isSwitchCondition(){
		return switchCondition;
	}
	
	@Override
	public String toString(){
		String str = "";
		str += conditionExpr.toString() + " from:\n";
		str += wrappingStmt.toString() + "\n";
		return str;
	}

	@Override
	public CompilationUnit getCompilationUnit() {
		ASTNode node = wrappingStmt.getRoot();
		if(node instanceof CompilationUnit)
			return (CompilationUnit)node;
		return null;
	}
}
