package cn.edu.thu.tsmart.tool.da.core.suggestion;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.Expression;

import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSite;

public class NPEFix extends FilterableFix{

	public NPEFix(AbstractFixSite fixSite, int startPosition, int originalLength,
			String modifiedString, String fixType, int fixLineNum,
			Expression targetExpr, String targetExprType, String newExprString,
			String passTCExpectedValue) {
		super(fixSite, startPosition, originalLength, modifiedString, fixType,
				fixLineNum, targetExpr, targetExprType, newExprString,
				passTCExpectedValue);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doFix() {
		// TODO Auto-generated method stub
		super.doFix();
	}

	@Override
	public void undoFix() {
		// TODO Auto-generated method stub
		super.doFix();
	}

	@Override
	public IFile[] getModifiedFiles() {
		// TODO Auto-generated method stub
		return super.getModifiedFiles();
	}

	@Override
	public String getFixType() {
		// TODO Auto-generated method stub
		return "NPEFix";
	}

	@Override
	public int getFixLineNum() {
		// TODO Auto-generated method stub
		return super.getFixLineNum();
	}

	@Override
	public String getFileName() {
		// TODO Auto-generated method stub
		return super.getFileName();
	}

}
