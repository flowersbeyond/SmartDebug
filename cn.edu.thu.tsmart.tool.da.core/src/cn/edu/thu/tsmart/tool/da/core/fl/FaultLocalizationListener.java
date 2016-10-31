package cn.edu.thu.tsmart.tool.da.core.fl;

import java.util.ArrayList;

public interface FaultLocalizationListener {

	public void localizationFinished(ArrayList<BasicBlock> localizationResult);
}
