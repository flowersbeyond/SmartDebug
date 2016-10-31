package cn.edu.thu.tsmart.tool.da.core.validator;

import cn.edu.thu.tsmart.tool.da.core.fl.BasicBlock;

public interface ValidateEventListener {
	public boolean confirmValidateResult(String fileName, int lineNum);

	public boolean confirmBasicBlock(BasicBlock bb);
}
