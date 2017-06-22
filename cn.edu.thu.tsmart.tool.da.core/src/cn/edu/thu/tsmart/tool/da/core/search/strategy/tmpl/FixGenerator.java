package cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl;

import java.util.ArrayList;

import cn.edu.thu.tsmart.tool.da.core.fl.BasicBlock;
import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;

public abstract class FixGenerator {

	public abstract ArrayList<Fix> searchFixes(BasicBlock bb);

	public abstract void clearCache();

	public void init() {}

}
