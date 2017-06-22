package cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSite;

public class StatementFixSite extends AbstractFixSite{

	private List<? extends ASTNode> statements;
	
	public StatementFixSite(IFile file, String qualifiedTypeName, int lineNumStart, int lineNumEnd, List<? extends ASTNode> statements) {
		super(file, qualifiedTypeName, lineNumStart, lineNumEnd);
		this.statements = statements;
	}
	
	public List<? extends ASTNode> getStatements(){
		return statements;
	}

	@Override
	public String toString(){
		String str = "";
		for(ASTNode node: statements){
			str += node.toString() + "\n";
		}
		return str;
	}
	
	@Override
	public CompilationUnit getCompilationUnit(){
		if(statements == null || statements.size() == 0)
			return null;
		ASTNode root = statements.get(0).getRoot();
		if(root instanceof CompilationUnit)
			return (CompilationUnit)root;
		return null;
	}
}
