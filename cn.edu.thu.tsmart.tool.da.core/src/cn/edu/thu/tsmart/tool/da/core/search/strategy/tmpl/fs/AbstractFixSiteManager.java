package cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs;

import java.util.ArrayList;


public abstract class AbstractFixSiteManager {

	public abstract ArrayList<AbstractFixSite> getFixSitesFromLocation(String methodKey,
			int startLineNum, int endLineNum);

	public abstract void  clearCache();
	
}