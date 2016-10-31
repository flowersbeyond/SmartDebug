package cn.edu.thu.tsmart.tool.da.core.search.fixSite;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.CompilationUnit;

public abstract class FixSite {
	
	private boolean visited = false;
	private int lineNumStart;
	private int lineNumEnd;
	private IFile file;
	private String qualifiedTypeName;
	
	public FixSite(IFile file, String qualifiedTypeName, int lineNumStart, int lineNumEnd){
		this.file = file;
		this.lineNumStart = lineNumStart;
		this.lineNumEnd = lineNumEnd;
		this.qualifiedTypeName = qualifiedTypeName;
	}
	
	public void markAsVisited(){
		this.visited = true;
	}
	
	public void clearVisitFlag(){
		this.visited = false;
	}
	
	public boolean isVisited(){
		return visited;
	}
	
	public boolean coverLineNum(int startLine, int endLine){
		if(lineNumEnd < startLine || lineNumStart > endLine)
			return false;
		return true;
	}
	
	public IFile getFile(){
		return file;
	}

	public String getQualifiedTypeName() {
		return qualifiedTypeName;
	}
	
	public abstract CompilationUnit getCompilationUnit();
}
