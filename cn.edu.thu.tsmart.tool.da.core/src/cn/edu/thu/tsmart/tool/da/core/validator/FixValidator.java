package cn.edu.thu.tsmart.tool.da.core.validator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.Logger;
import cn.edu.thu.tsmart.tool.da.core.fl.BasicBlock;
import cn.edu.thu.tsmart.tool.da.core.suggestion.FilterableSetFix;
import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;
import cn.edu.thu.tsmart.tool.da.core.suggestion.SuggestionManager;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.Checkpoint;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.CheckpointManager;

public class FixValidator{
	
	private BugFixSession session;
	private NullProgressMonitor compilerPM;
	private SuggestionManager suggestionManager;
	private int validateCount = 0;
	
	private static ArrayList<ValidateEventListener> validateEventListeners = new ArrayList<ValidateEventListener>();
	
	public FixValidator(BugFixSession session) {
		this.session = session;
		this.compilerPM = new NullProgressMonitor();
		suggestionManager = new SuggestionManager();
	}

	/*public IStatus run(SubProgressMonitor monitor) {
		try {
			int totalLeft = session.getCandidateQueue().getSize();
			if (totalLeft > 200)
				totalLeft = 200;
			monitor.beginTask("Validating Fix Candidates...", totalLeft);
			int itemcount = 200;
			for (int i = 0; i < itemcount; i++) {
				Fix fix = session.getCandidateQueue().getNextFix();
				if (fix == null) {
					// we have checked all generated candidates
					break;
				}
				validate(fix);
				monitor.worked(1);
				if (monitor.isCanceled()){					
					CheckpointManager.getInstance().ensureAllCPMarkers();
					return Status.CANCEL_STATUS;
				}
			}
		} finally {
			monitor.done();
		}
		CheckpointManager.getInstance().ensureAllCPMarkers();
		return Status.OK_STATUS;
	}*/
	
	public static void registerValidateEventListener(ValidateEventListener listener){
		validateEventListeners.add(listener);
	}
	
	public static void removeValidateEventListener(ValidateEventListener listener){
		if(validateEventListeners.contains(listener)){
			validateEventListeners.remove(listener);
		}
	}
	
	public boolean isProperBasicBlock(BasicBlock bb ){
		
		for(ValidateEventListener listener: validateEventListeners){
			ValidateEventListener oracle = listener;
			if(oracle.confirmBasicBlock(bb)){
				return true;
			}
		}
		
		return false;
	}
	
	public boolean validate(Fix fix){
		if(fix instanceof FilterableSetFix){
			this.validateCount += ((FilterableSetFix)fix).size();
		} else {
			this.validateCount ++;
		}
		
		if(fix instanceof FilterableSetFix){
			FilterableSetFix setFix = (FilterableSetFix)fix;
			List<? extends Fix> fixes = setFix.getFixes();
			boolean expr = false;
			boolean cond = false;
			boolean cont = false;
			boolean brek = false;
			boolean ret = false;
			
			boolean result = false;
			for(Fix f : fixes){
				if(!expr && f.getFixType().equals(Fix.EXPR_CHANGE)){
					result = result || validateInner(f);
					expr = true;
				}
				if(!cond && f.getFixType().equals(Fix.COND_EXPR_CHANGE)){
					result = result || validateInner(f);
					cond = true;
				}
				if(!cont && f.getFixType().equals(Fix.IF_CONTINUE)){
					result = result || validateInner(f);
					cont = true;
				}
				if(!brek && f.getFixType().equals(Fix.IF_BREAK)){
					result = result || validateInner(f);
					brek = true;
				}
				if(!ret && f.getFixType().equals(Fix.IF_RETURN)){
					result = result || validateInner(f);
					ret = true;
				}
				
			}
			
			return result;
		} else {
			return validateInner(fix);
		}
	
	}
	
	private boolean validateInner(Fix fix){
		Object breakpointMutualLock = session.getBreakpointMutualLock();
		synchronized(breakpointMutualLock){
			//System.out.println("start apply fix");
			fix.doFix();
			System.out.println(fix.toString());
			//System.out.println("fix applied");
			
			IJavaProject currentProject = session.getProject();
			IProject proj = currentProject.getProject();
			try {
				proj.build(IncrementalProjectBuilder.FULL_BUILD, compilerPM);
				
				boolean compilationSuccess = true;
				IFile[] modifiedFiles = fix.getModifiedFiles();
				for(IFile file: modifiedFiles){
					IMarker[] markers =
							 file.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER,
							 true, IResource.DEPTH_INFINITE);
					for(int i = 0; i < markers.length; i ++){
						int severity = markers[i].getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
						if(severity == IMarker.SEVERITY_ERROR){
							compilationSuccess = false;
							break;
						}
					}
				}
				//System.out.println("compilation finished");
				//boolean compilationSuccess = monitor.getCompilationSuccess();
				if(!compilationSuccess){//!monitor.getCompilationSuccess()){
					fix.undoFix();
					//System.out.println("fix failed, dis-applied");
					synchronized(this){
						wait(800);
					}
					return false;
				}
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 	
			
			if(session.getFixGoal() == null){
				return validateForAll(fix);
			} else{
				return validateForCheckpoint(fix, session.getFixGoal());
			}
			
			
		}
	}
	
	private boolean validateForAll(Fix fix){
		ArrayList<IMethod> methods = session.getTestMethods();
		
		ArrayList<TestCase> failedTestCases = new ArrayList<TestCase>();
		ArrayList<TestCase> passedTestCases = new ArrayList<TestCase>();
		boolean fixSuccess = true;
		for(IMethod method: methods){
			
			TestCase tc = new TestCase(method.getDeclaringType().getFullyQualifiedName(), method.getElementName());
			boolean pass = session.getTestResult(tc);
			if(pass)
				passedTestCases.add(tc);
			else
				failedTestCases.add(tc);
		}
		for(TestCase tc: failedTestCases){
			ArrayList<Checkpoint> cps = CheckpointManager.getInstance().getConditionForTestCase(tc);
			if(cps == null)
				cps = new ArrayList<Checkpoint>();
			ILaunchConfiguration config = session.findLaunchConfiguration(tc);
			if(config != null){
				TestCaseValidator validator = new TestCaseValidator();
				validator.validate(config, cps);
				if(validator.getValidationResult() == false){
					fixSuccess = false;
					break;
				}
			}
		}
		if(fixSuccess){
			for(TestCase tc: passedTestCases){
				ILaunchConfiguration config = session.findLaunchConfiguration(tc);
				if(config != null){
					TestCaseValidator validator = new TestCaseValidator();
					validator.validate(config, new ArrayList<Checkpoint>());
					if(validator.getValidationResult() == false){
						fixSuccess = false;
						break;
					}
				}
			}
		}
		
		fix.undoFix();
		
		if(fixSuccess){
			suggestionManager.confirmFix(fix);
			session.getLogger().log(Logger.DATA_MODE, Logger.FIX_PLAUSIBLE, this.validateCount + ":" + fix.toString());
			
			for(ValidateEventListener listener: validateEventListeners){
				ValidateEventListener oracle = listener;
				if(oracle.confirmValidateResult(fix.getFileName(), fix.getFixLineNum())){
					session.getLogger().log(Logger.DATA_MODE, Logger.FIX_SUCCESS, this.validateCount + ":" + fix.toString());
					return true;
				}
			}
		}
				
		System.gc();
		//breakpointMutualLock.notifyAll();
		return false;
	}
	
	private boolean validateForCheckpoint(Fix fix, Checkpoint cp){
		if(fix.toString().contains("i<n")){
			int i = 0;
		}
		ArrayList<IMethod> methods = session.getTestMethods();
		
		ArrayList<TestCase> failedTestCases = new ArrayList<TestCase>();
		ArrayList<TestCase> passedTestCases = new ArrayList<TestCase>();
		boolean fixSuccess = true;
		
		TestCase targetTC = cp.getOwnerTestCase();
		ArrayList<Checkpoint> targetCPs = CheckpointManager.getInstance().getConditionForTestCase(targetTC);
		ILaunchConfiguration targetConfig = session.findLaunchConfiguration(targetTC);
		if(targetConfig != null){
			TestCaseValidator validator = new TestCaseValidator();
			validator.validate(targetConfig, targetCPs);
			if(validator.getValidationResult() == false){
				fix.undoFix();
				return false;
			}
		}
		
		
		int score = 0;
		for(IMethod method: methods){			
			TestCase tc = new TestCase(method.getDeclaringType().getFullyQualifiedName(), method.getElementName());
			boolean pass = session.getTestResult(tc);
			if(pass)
				passedTestCases.add(tc);
			else if (!tc.equals(targetTC))
				failedTestCases.add(tc);
		}
		
		
		
		for(TestCase tc: passedTestCases){
			ILaunchConfiguration config = session.findLaunchConfiguration(tc);
			if(config != null){
				TestCaseValidator validator = new TestCaseValidator();
				validator.validate(config, new ArrayList<Checkpoint>());
				if(validator.getValidationResult() == true){
					score ++;
				}
			}
		}
		
		for(TestCase tc: failedTestCases){
			ArrayList<Checkpoint> cps = CheckpointManager.getInstance().getConditionForTestCase(tc);
			if(cps == null)
				cps = new ArrayList<Checkpoint>();
			ILaunchConfiguration config = session.findLaunchConfiguration(tc);
			if(config != null){
				TestCaseValidator validator = new TestCaseValidator();
				validator.validate(config, cps);
				if(validator.getValidationResult() == true){
					score ++;
				}
			}
		}
		
		fix.undoFix();
		fix.setScore(score);
		suggestionManager.confirmFix(fix);
		session.getLogger().log(Logger.DATA_MODE, Logger.FIX_PLAUSIBLE, this.validateCount + ":" + fix.toString());				
		System.gc();
		//breakpointMutualLock.notifyAll();
		return false;
	}
	
	public void clearCache(){
		this.suggestionManager.clearCache();
	}
}


class CompilationProgressMonitor extends NullProgressMonitor{
	
	private Object notifyLock;
	private IFile[] modifiedFiles;
	private boolean compilationSuccess = true;
	
	private int notifiedTime = 0;
	public CompilationProgressMonitor(Object notifyLock, IFile[] modifiedFiles){
		this.notifyLock = notifyLock;
		this.modifiedFiles = modifiedFiles;
	}
	
	public boolean getCompilationSuccess(){
		return compilationSuccess;
	}
	
	@Override
	public void done(){
		for(IFile file: modifiedFiles){
			try {
				IMarker[] markers =
						 file.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER,
						 true, IResource.DEPTH_INFINITE);
				if(markers.length > 0){
					compilationSuccess = false;
					if(notifiedTime == 0){
						synchronized(notifyLock){
							notifyLock.notifyAll();
						}
						notifiedTime ++;
					}
					return;
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		if(notifiedTime == 0){
			synchronized(notifyLock){
				notifyLock.notifyAll();
			}
			notifiedTime ++;
		}
		
	}
}
