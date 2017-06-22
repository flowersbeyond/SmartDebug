package cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.Logger;
import cn.edu.thu.tsmart.tool.da.core.fl.BasicBlock;
import cn.edu.thu.tsmart.tool.da.core.search.Filter;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fixer.ConditionExprFixer;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fixer.ExpressionFixer;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fixer.IfInserter;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fixer.MethodFixer;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.ConditionFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.InsertStopFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.StatementFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.FixGenerator;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSite;
import cn.edu.thu.tsmart.tool.da.core.suggestion.FilterableFix;
import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;
import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;


public class GnrFixGenerator extends FixGenerator{

	BugFixSession session;
	private Set<AbstractFixSite> triedStatementFixSite = new HashSet<AbstractFixSite>();	
	private Set<AbstractFixSite> triedConditionFixSite = new HashSet<AbstractFixSite>();
	private Set<AbstractFixSite> triedInsertStopFixSite = new HashSet<AbstractFixSite>();
	
	
	//Fix generation strategies:
//	private BranchFixer branchFixer;
//	private LoopFixer loopFixer;
//	private SequenceFixer seqFixer;
	private IfInserter ifInserter;
	private MethodFixer methodFixer;
	private ExpressionFixer exprFixer;
	private ConditionExprFixer condFixer;
	
	
	
	
	

	public GnrFixGenerator(BugFixSession session) {
		this.session = session;
		
//		this.branchFixer = new BranchFixer();
//		this.loopFixer = new LoopFixer();
//		this.seqFixer = new SequenceFixer();
		this.ifInserter = new IfInserter(session);
		this.methodFixer = new MethodFixer(session);
		this.exprFixer = new ExpressionFixer(session);
		this.condFixer = new ConditionExprFixer(session);
		
		
		
	}
	
	/*public void run(SubProgressMonitor subPM){
		try{
			subPM.beginTask("Generating Fix Candidates...", suspiciousBlocks.size() * 2);
			Filter.setSession(session);
			for(BasicBlock bb: suspiciousBlocks){
				
				addNewFixCandidates(filteredFixes);
			}
			
			
			
		}finally{
			subPM.done();
		}
	}*/

	
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
		Map<Integer, ArrayList<FilterableFix>> fixes = new HashMap<Integer, ArrayList<FilterableFix>>();
		ArrayList<Fix> filteredFixes = new ArrayList<Fix>();
		for(AbstractFixSite fixSite: fixSites){
			
			
				if(fixSite instanceof StatementFixSite){
					if(triedStatementFixSite.contains(fixSite))
						continue;
					/*
					//structure modifications
					fixes.addAll(branchFixer.generateFix((StatementFixSite) fixSite));
					fixes.addAll(seqFixer.generateFix((StatementFixSite)fixSite));
					fixes.addAll(loopFixer.generateFix((StatementFixSite)fixSite));
					*/
					
					
											
					//method fixes
					ArrayList<Fix> methodFixes = methodFixer.generateFix((StatementFixSite)fixSite);
					filteredFixes.addAll(methodFixes);
					session.getLogger().log(Logger.DATA_MODE, Logger.GEN_METHOD_FIX, methodFixes.size() + "");
					session.getLogger().log(Logger.EXPR_MODE, Logger.GEN_METHOD_FIX, methodFixes.size() + "");
					session.getLogger().log(Logger.DEBUG_MODE, Logger.GEN_METHOD_FIX, methodFixes.size() + "");
					session.getLogger().log(Logger.DEBUG_MODE, Logger.METHOD_FIX_DETAIL, Logger.generateMethodFixSummary(methodFixes));
					Map<Integer, ArrayList<FilterableFix>> ifInsertedFixes = ifInserter.generateFix((StatementFixSite) fixSite);
					
					//then generate expression fixes
					//expression fixes will be ordered:
					//	temporarily : method param fixer first, finally general fixers
					Map<Integer, ArrayList<FilterableFix>> exprFixes = exprFixer.generateFixForStatementBlocks(((StatementFixSite)fixSite));
					
					Filter.merge(fixes, ifInsertedFixes);
					Filter.merge(fixes, exprFixes);
					
					triedStatementFixSite.add(fixSite);
					
					/*
					SymExpressionFixer symExprFixer = new SymExpressionFixer(session, testcase, null);
					fixes.addAll(symExprFixer.generateFix((StatementFixSite)fixSite));*/
				
				}
				else if (fixSite instanceof ConditionFixSite){
					if(triedConditionFixSite.contains(fixSite))
						continue;
					Map<Integer, ArrayList<FilterableFix>> conditionFixes = condFixer.generateFix((ConditionFixSite)fixSite);
					Filter.merge(fixes, conditionFixes);
					triedConditionFixSite.add(fixSite);
				} else if(fixSite instanceof InsertStopFixSite){
					if(triedInsertStopFixSite.contains(fixSite))
						continue;
					Map<Integer, ArrayList<FilterableFix>> ifInsertedFixes = ifInserter.generateFix((InsertStopFixSite)fixSite);
					Filter.merge(fixes, ifInsertedFixes);
					triedInsertStopFixSite.add(fixSite);
				}
				
		}
		
		session.getLogger().log(Logger.EXPR_MODE, "PRE_FILTER_STATUS", Logger.generateFixStatistics(fixes));
		ArrayList<Fix> preFilteredFixes = session.getFilter().filter(fixes, file, typeName, passTCs, failTC);
		session.getLogger().log(Logger.EXPR_MODE, "POST_FILTER_STATUS", Logger.generateFixStatistics(preFilteredFixes));
		filteredFixes.addAll(preFilteredFixes);
		
		session.getLogger().log(Logger.EXPR_MODE, "END_SEARCH_FIX", "search for:" + bb.toString());
		
		return filteredFixes;
	}
	
	/*
	private void printBasicBlockInfo(BasicBlock block){
		String methodKey = block.getMethodKey();
		SSACFG cfg = CFGCache.get(methodKey);
		IMethod method = cfg.getMethod();
		if(method instanceof ShrikeCTMethod){
			ShrikeCTMethod shrikeMethod = (ShrikeCTMethod)method;
			
			try {
				IInstruction[] instrs = shrikeMethod.getInstructions();
				SSACFG.BasicBlock ssabb = block.getSSABasicBlock();
				List<SSAInstruction> ssainstrs = ssabb.getAllInstructions();
				for(SSAInstruction ssainstr: ssainstrs){
					System.out.println(ssainstr.toString());
				}
				
				SourcePosition srcpos = shrikeMethod.getSourcePosition(block.getSSABasicBlock().getFirstInstructionIndex());
				int linestart = srcpos.getFirstLine();
				int lineend = srcpos.getLastLine();
				int colstart = srcpos.getFirstCol();
				int colend = srcpos.getLastCol();
				int offsetstart = srcpos.getFirstOffset();
				int offsetend = srcpos.getLastOffset();
				System.out.println(block.getClassName() + ": line start:" + linestart + "end:" + lineend + " | "
						+ "col start:" + colstart + " end:" + colend + " | "
						+ "offset start:" + offsetstart + " end:" + offsetend);
				
			} catch (InvalidClassFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	*/
	
	public void clearCache() {
		if(this.triedConditionFixSite!= null)
			this.triedConditionFixSite.clear();
		if(this.triedInsertStopFixSite!= null)
			this.triedInsertStopFixSite.clear();
		if(this.triedStatementFixSite!= null)
			this.triedStatementFixSite.clear();
	}

}
