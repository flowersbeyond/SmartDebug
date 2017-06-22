package cn.edu.thu.tsmart.tool.da.core.search.strategy.npe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.Logger;
import cn.edu.thu.tsmart.tool.da.core.fl.BasicBlock;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.npe.fixer.NPEFixer;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.npe.fs.NPEFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.npe.fs.NPEFixSiteManager;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.FixGenerator;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSite;
import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;
import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;

import com.ibm.wala.ipa.slicer.Statement;

public class NPEFixGenerator extends FixGenerator{
	private BugFixSession session;
	private NPEFixer npeFixer;
	private List<NPEFixSite> triedNPEFixSite;
	//TODO:
	//private ABFixer abFixer;
	public NPEFixGenerator(BugFixSession session) {		
		this.session = session;
		npeFixer = new NPEFixer(session);
	}
	
	public void init(){
		TestCase tc = session.getExceptionTestCase();
		NPEInfo npeInfo = new NPEInfo(session.getExceptionInfo(), session.getProject());
		Collection<Statement> slice = NPESlicer.doSlicing(tc, session.getAnalysisScope(), session.getCHA(), npeInfo);
		NPEFixSiteManager manager = (NPEFixSiteManager)(session.getFixSiteManager());
		manager.setSlice(slice);
	}
	public ArrayList<Fix> searchFixes(BasicBlock bb){
		//remove all breakpoints just in case:
		try {
			IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
			IBreakpoint[] breakpoints = manager.getBreakpoints();
			DebugPlugin.getDefault().getBreakpointManager().removeBreakpoints(breakpoints, true);
		} catch (CoreException e) {
			e.printStackTrace();
			return new ArrayList<Fix>();			
		}
		TestCase testcase = session.getCoveringTestCase(bb);
		if(testcase == null)
			return null;
		String methodKey = bb.getMethodKey();
		ArrayList<AbstractFixSite> fixSites= session.getFixSiteManager().getFixSitesFromLocation(methodKey, bb.getStartLineNum(), bb.getEndLineNum());
		
		if(fixSites.size() == 0)
			return null;
		AbstractFixSite fs = fixSites.get(0);
		IFile file = fs.getFile();
		String typeName = fs.getQualifiedTypeName();
		ArrayList<TestCase> passTCs = session.getPassCoveringTCs(bb);
		TestCase failTC = session.getFailCoveringTC(bb);
		
		session.getLogger().log(Logger.DEBUG_MODE, "SEARCH_FIX", "search for:" + bb.toString());
		session.getLogger().log(Logger.EXPR_MODE, "SEARCH_FIX", "search for:" + bb.toString());
		//Map<Integer, ArrayList<FilterableFix>> fixes = new HashMap<Integer, ArrayList<FilterableFix>>();
		ArrayList<Fix> filteredFixes = new ArrayList<Fix>();
		for(AbstractFixSite fixSite: fixSites){
			
			if(fixSite instanceof NPEFixSite){
				filteredFixes.addAll(npeFixer.generateFix((NPEFixSite)fixSite));
			}
			
		}
		
//		session.getLogger().log(Logger.EXPR_MODE, "PRE_FILTER_STATUS", Logger.generateFixStatistics(fixes));
//		ArrayList<Fix> preFilteredFixes = session.getFilter().filter(fixes, file, typeName, passTCs, failTC);
//		session.getLogger().log(Logger.EXPR_MODE, "POST_FILTER_STATUS", Logger.generateFixStatistics(preFilteredFixes));
//		filteredFixes.addAll(preFilteredFixes);
		
		session.getLogger().log(Logger.EXPR_MODE, "END_SEARCH_FIX", "search for:" + bb.toString());
		
		return filteredFixes;
	}
	
		
	public void clearCache() {
		if(this.triedNPEFixSite!= null)
			this.triedNPEFixSite.clear();
	}
}
