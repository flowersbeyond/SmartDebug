package cn.edu.thu.tsmart.tool.da.expr4j.oracle;

import cn.edu.thu.tsmart.tool.da.core.fl.BasicBlock;
import cn.edu.thu.tsmart.tool.da.core.validator.ValidateEventListener;


public class Oracle implements ValidateEventListener{
	private String versionName;
	private String modifiedFileName;
	private String qualifiedTestName;
	private int lineNum;
	private String fixType;
	
	public Oracle(String versionName, String qualifiedTestName, String modifiedFileName, int lineNum, String fixType){
		this.versionName = versionName;
		this.qualifiedTestName = qualifiedTestName;
		this.modifiedFileName = modifiedFileName;
		this.lineNum = lineNum;
		this.fixType = fixType;
	}
	public boolean confirmValidateResult(String fileName, int lineNum){
		return checkOK(fileName, lineNum);
	}
	private boolean checkOK(String fileName, int lineNum){
		if(fileName.equals(this.modifiedFileName) && lineNum >= this.lineNum - 2 && lineNum <= this.lineNum + 2){
			return true;
		}
		return false;
	}
	
	public String getQualifiedTestTypeName(){
		return this.qualifiedTestName;
	}
	@Override
	public boolean confirmBasicBlock(BasicBlock bb) {
		int startLineNum = bb.getStartLineNum();
		int endLineNum = bb.getEndLineNum();
		String className = bb.getClassName();
		className = className.replaceAll("/", ".");
		if(modifiedFileName.startsWith(className) && lineNum >= startLineNum && lineNum <= endLineNum)
			return true;
		if(this.versionName.contains("118") || this.versionName.contains("92") || this.versionName.contains("93"))
			if(lineNum >= startLineNum && lineNum <= endLineNum)
				return true;
		
		return false;
	}
}
