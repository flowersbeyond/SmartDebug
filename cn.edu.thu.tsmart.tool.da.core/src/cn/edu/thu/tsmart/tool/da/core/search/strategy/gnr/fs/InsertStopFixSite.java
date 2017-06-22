package cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;

import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSite;

public class InsertStopFixSite extends AbstractFixSite {

	private Statement stmt;
	public InsertStopFixSite(IFile file, String qualifiedTypeName,
			int lineNumStart, int lineNumEnd, Statement stmt) {
		super(file, qualifiedTypeName, lineNumStart, lineNumEnd);
		this.stmt = stmt;
	}
	
	public Statement getStatement(){
		return stmt;
	}
	
	public String toString(){
		return "Insert before:" + stmt.toString();
	}

	@Override
	public CompilationUnit getCompilationUnit() {
		ASTNode node = stmt.getRoot();
		if(node instanceof CompilationUnit)
			return (CompilationUnit)node;
		return null;
	}

}
