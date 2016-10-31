package cn.edu.thu.tsmart.tool.da.core.suggestion;

import java.util.List;

import org.eclipse.core.resources.IFile;

public class FilterableSetFix extends Fix{
	
	List<? extends Fix> fixset;
	public FilterableSetFix(List<? extends Fix> fixset){
		this.fixset = fixset;
	}

	@Override
	public void doFix() {
		fixset.get(0).doFix();
	}

	@Override
	public void undoFix() {
		fixset.get(0).undoFix();
		
	}

	@Override
	public IFile[] getModifiedFiles() {
		return fixset.get(0).getModifiedFiles();
	}

	@Override
	public String getFixType() {
		return fixset.get(0).getFixType();
	}

	@Override
	public int getFixLineNum() {
		return fixset.get(0).getFixLineNum();
	}

	@Override
	public String getFileName() {
		return fixset.get(0).getFileName();
	}

	public int size() {
		
		return fixset.size();
	}

	public List<? extends Fix> getFixes() {
		return fixset;
	}

}
