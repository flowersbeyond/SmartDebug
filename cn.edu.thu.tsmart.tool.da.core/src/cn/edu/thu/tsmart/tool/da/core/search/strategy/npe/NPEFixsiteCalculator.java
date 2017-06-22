package cn.edu.thu.tsmart.tool.da.core.search.strategy.npe;

import java.util.ArrayList;

import com.ibm.wala.ipa.slicer.Statement;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.npe.fs.NPEFixSite;

public class NPEFixsiteCalculator {
	
	private BugFixSession session;

	public NPEFixsiteCalculator(BugFixSession session){
		this.session = session;
	}
	
	public ArrayList<NPEFixSite> sliceFixSites(String NPELocStr){
		//compute slicing
		/*Statement npeStmt = locateNPEStmt(NPELocStr);
		List<Statement> usefulStmts = slice();
		
		return computeNPEFixSite(usefulStmts);*/
		return null;
	}
}
