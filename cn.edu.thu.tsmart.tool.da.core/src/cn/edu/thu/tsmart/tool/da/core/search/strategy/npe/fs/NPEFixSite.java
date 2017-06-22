package cn.edu.thu.tsmart.tool.da.core.search.strategy.npe.fs;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSite;

public class NPEFixSite extends AbstractFixSite{

	public NPEFixSite(IFile file, String qualifiedTypeName, int lineNumStart,
			int lineNumEnd, ASTNode expr) {
		super(file, qualifiedTypeName, lineNumStart, lineNumEnd);
		this.expr = expr;
		
	}
	private ASTNode expr;
	
	public ASTNode getExpr(){
		return expr;
	}

	@Override
	public CompilationUnit getCompilationUnit() {
		//TODO:
		return null;
	}
	
}
