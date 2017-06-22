package cn.edu.thu.tsmart.tool.da.core;

import java.util.ArrayList;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import cn.edu.thu.tsmart.tool.da.core.fl.BasicBlock;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.GnrFixGenerator;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.FixGenerator;
import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;
import cn.edu.thu.tsmart.tool.da.core.validator.FixValidator;
//import cn.edu.thu.tsmart.tool.da.tracer.ITraceEventAllDoneListener;

public class SearchEngine extends Job{
	private static String NAME = "Resolve Bug Job";
	
	private FixGenerator fixGenerator;
	private FixValidator fixValidator;
	private BugFixSession session;
	public SearchEngine(BugFixSession session, FixGenerator fixGenerator){
		super(SearchEngine.NAME);
		this.session = session;
		this.fixGenerator = fixGenerator;
		setUser(true);
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final int ticks = 100000;
		int allworkload = 100000;
		monitor.beginTask("SmartDebugger Resolving Bugs...", ticks);
		try {
			// we just entered into a new fix session, everything needs to
			// be initialized
			session.getLogger().log(Logger.DATA_MODE, Logger.INIT, "initialize started...");
			session.initFixSession(new SubProgressMonitor(monitor, 5000));
			session.getLogger().log(Logger.DATA_MODE, Logger.INIT_FINISHED, "initialization finished...");
			
			
			if (fixValidator == null) {
				fixValidator = new FixValidator(session);
				allworkload = allworkload - 5000;
			}
			
			fixGenerator.init();
			
			int allSuspiciousBlockNum = session.getSuspectList().size();
			session.getLogger().log(Logger.DATA_MODE, Logger.FL_TOTAL, allSuspiciousBlockNum + "" );
			
			int suspiciousBlockLeft = allSuspiciousBlockNum - session.getBBProgress();
			double eachBBWorkload = (double)allworkload / (double)suspiciousBlockLeft;
			
			session.getLogger().log(Logger.DATA_MODE, Logger.BEGIN_SEARCH, session.getBBProgress() + "");
			boolean shouldstop = false;
			while(session.getBBProgress() < allSuspiciousBlockNum){
				if(shouldstop)
					break;
				int newBBProgress = 0;
				int bbProgressCount = session.getBBProgress();
				for(int i = bbProgressCount; i < allSuspiciousBlockNum; i ++){
					BasicBlock bb = session.getSuspectList().get(i);
					
					session.getLogger().log(Logger.DATA_MODE, Logger.SEARCH_FIX, i + "");
					ArrayList<Fix> fixes = fixGenerator.searchFixes(bb);
					if(fixes != null){
						session.getCandidateQueue().appendNewFixes(fixes);
						session.getLogger().log(Logger.DATA_MODE, Logger.END_SEARCH_FIX, i + "");
					} else {
						session.getLogger().log(Logger.DATA_MODE, Logger.IGNORE_BB, i + "");
					}
					session.increaseBBProgress();
					
					if(session.getCandidateQueue().getSize() > 30){
						newBBProgress = i + 1 - bbProgressCount;
						break;
					}
					monitor.worked((int)(eachBBWorkload / 5));
					if(monitor.isCanceled()){
						return Status.CANCEL_STATUS;
					}							
				}
				
				
				double fixValidationProgressShare = eachBBWorkload * newBBProgress / 5 * 4;
				int eachValidationProgress = (int)(fixValidationProgressShare / session.getCandidateQueue().getSize());
				
				session.getLogger().log(Logger.DATA_MODE, "BEGIN_VALIDATE", session.getCandidateQueue().getSize() + "");
				while(session.getCandidateQueue().hasNextFix()){
					Fix fix = session.getCandidateQueue().getNextFix();
					boolean findFix = fixValidator.validate(fix);
					if(findFix){
						session.getLogger().log(Logger.DATA_MODE, Logger.FIX_DONE, "success");
						try {
							session.getProject().getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
						} catch (CoreException e) {
							e.printStackTrace();
						}
						return Status.OK_STATUS;
					}
					monitor.worked(eachValidationProgress);
					if(monitor.isCanceled()){
						try {
							session.getProject().getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
						} catch (CoreException e) {
							e.printStackTrace();
						}
						return Status.CANCEL_STATUS;
					}
				}
				try {
					session.getProject().getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
				} catch (CoreException e) {
					e.printStackTrace();
				}
				session.getLogger().log(Logger.DATA_MODE, "END_VALIDATE", "");
				if(monitor.isCanceled()){
					return Status.CANCEL_STATUS;
				}
			}

		} finally {
			monitor.done();
		}

		return Status.OK_STATUS;
	}

	public void stopAnalysis(){
		this.cancel();
	}


	public void clearCache() {
		if(this.fixGenerator != null)
			this.fixGenerator.clearCache();
		if(this.fixValidator != null)
			this.fixValidator.clearCache();
	}
}
