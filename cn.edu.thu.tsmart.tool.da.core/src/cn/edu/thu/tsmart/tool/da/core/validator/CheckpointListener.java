package cn.edu.thu.tsmart.tool.da.core.validator;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaBreakpointListener;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaPrimitiveValue;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.eval.IAstEvaluationEngine;
import org.eclipse.jdt.debug.eval.ICompiledExpression;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
import org.eclipse.jdt.debug.eval.IEvaluationResult;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.internal.debug.core.JavaDebugUtils;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugTarget;
import org.eclipse.jdt.internal.debug.core.model.JDIThread;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement.Result;

import cn.edu.thu.tsmart.tool.da.core.validator.cp.Checkpoint;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.ConditionItem;


/**
 * Listens for evaluation completion for condition evaluation. If an
 * evaluation evaluates <code>true</code> or has an error, this checkpoint
 * will resume the thread in which the breakpoint was hit. If the
 * evaluation returns <code>false</code>, the thread is suspended.
 */
class EvaluationListener implements IEvaluationListener{

	/**
	 * Lock for synchronizing evaluation
	 */
	private Object fLock = new Object();

	/**
	 * The checkpoint that was hit
	 */
	private Checkpoint fCheckpoint;

	/**
	 * Result of the vote
	 */
	private int fVote;

	EvaluationListener(Checkpoint checkpoint) {
		fCheckpoint = checkpoint;
	}

	@Override
	public void evaluationComplete(IEvaluationResult result) {
		fVote = determineVote(result);
		synchronized (fLock) {
			fLock.notifyAll();
		}
	}

	/**
	 * Processes the result to determine whether to suspend or resume.
	 * 
	 * @param result
	 *            evaluation result
	 * @return vote
	 */
	private int determineVote(IEvaluationResult result) {
		if (result.isTerminated()) {
			// indicates the user terminated the evaluation
			return IJavaBreakpointListener.DONT_CARE;
		}
		JDIThread thread = (JDIThread) result.getThread();
		if (result.hasErrors()) {
			return IJavaBreakpointListener.DONT_CARE;
		} 
		try {
			IValue value = result.getValue();
				if (value instanceof IJavaPrimitiveValue) {
					// Suspend when the condition evaluates true
					IJavaPrimitiveValue javaValue = (IJavaPrimitiveValue) value;
					if (javaValue.getJavaType().getName()
							.equals("boolean")) { //$NON-NLS-1$
						if (javaValue.getBooleanValue()) {
							return IJavaBreakpointListener.DONT_SUSPEND;
						} 
						return IJavaBreakpointListener.SUSPEND;
					}
				}
				// result was not boolean
				System.out.println("condition expression not boolean");
				return IJavaBreakpointListener.DONT_SUSPEND;
		} catch (DebugException e) {
			// Suspend when an error occurs
			JDIDebugPlugin.log(e);
			return IJavaBreakpointListener.DONT_SUSPEND;
		}
	}

	/**
	 * Result of the conditional expression evaluation - to resume or not
	 * resume, that is the question.
	 * 
	 * @return vote result
	 */
	int getVote() {
		return fVote;
	}

	/**
	 * Returns the lock object to synchronize this evaluation.
	 * 
	 * @return lock object
	 */
	Object getLock() {
		return fLock;
	}
}
public class CheckpointListener extends TestRunListener implements IJavaBreakpointListener, ILaunchesListener2{

	protected ILaunchConfiguration config;

	protected Checkpoint failedCheckpoint = null;
	protected boolean testcaseFailed = false;
	
	protected boolean TEST_FINISHED_FLAG = false;
	protected boolean LAUNCH_TERMINATED_FLAG = false;
	protected boolean CHECKPOINT_VIOLATED_FLAG = false;
	
	protected Object lock = new Object();
	
	private Map<IBreakpoint, Checkpoint> bpcpMap;
	public Object getLock() {
		return lock;
	}
	public boolean junitTestCaseFailed(){
		return testcaseFailed;
	}

	public Checkpoint getFailedCheckpoint(){
		return failedCheckpoint;
	}
	public CheckpointListener(ILaunchConfiguration config, ArrayList<Checkpoint> cps, Map<IBreakpoint, Checkpoint> bpcpMap) {
		this.config = config;
		this.bpcpMap = bpcpMap;
	}

	@Override
	public void addingBreakpoint(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint) {		
	}

	@Override
	public int installingBreakpoint(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint, IJavaType type) {
		return 0;
	}

	@Override
	public void breakpointInstalled(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint) {
	}
	
	
	@Override
	public int breakpointHit(IJavaThread thread, IJavaBreakpoint breakpoint) {
		
		try {
			Checkpoint cp = bpcpMap.get(breakpoint);
			if(cp != null && thread.getLaunch().getLaunchConfiguration().equals(config)){
				int checkResult = checkCheckpoint(cp, thread);
						
				if(checkResult == IJavaBreakpointListener.SUSPEND){
					this.failedCheckpoint = cp;
					this.CHECKPOINT_VIOLATED_FLAG = true;
					thread.getLaunch().terminate();
				}
				return checkResult;
			
			} 
		} catch (CoreException e) {
			e.printStackTrace();
		}
			
		return IJavaBreakpointListener.DONT_SUSPEND;
	}
	
	protected int checkCheckpoint(Checkpoint cp, IJavaThread thread){
		
		/*try{
			ArrayList<ConditionItem> conditions = cp.getConditions();
			
			for(ConditionItem condition: conditions){
				String hitCondition = condition.getHitCondition();
				if(checkSatisfied(hitCondition, thread)){
					if(!checkExpectation(condition.getExpectation(), thread)){
						condition.setFailed();
					}
					else {
						
					}
				}
			}
			
			for(ConditionItem item: conditions){
				
				if(item.getHitCondition() == Checkpoint.HIT_ALWAYS
						|| item.getHitCondition() == currentHitCount){
					
					EvaluationListener listener = new EvaluationListener(
							cp);
					IJavaStackFrame frame = (IJavaStackFrame) thread
							.getTopStackFrame();
					IJavaProject project = JavaDebugUtils.resolveJavaProject(frame);
					if (project == null) {
						System.out.println("cannot initiate evaluation listener");
						return IJavaBreakpointListener.DONT_CARE;
					}
					IJavaDebugTarget target = (IJavaDebugTarget) thread
							.getDebugTarget();
					IAstEvaluationEngine engine = getEvaluationEngine(target,
							project);
					if (engine == null) {
						// If no engine is available, suspend
						return DONT_CARE;
					}
					ICompiledExpression expression = engine.getCompiledExpression(item.getExpectation(), frame);
						
					if (expression.hasErrors()) {
						System.out.println("condition expression compile failed, skip");
						return IJavaBreakpointListener.DONT_SUSPEND;
					}
					Object lock = listener.getLock();
					synchronized (lock) {
						engine.evaluateExpression(expression, frame, listener,
								DebugEvent.EVALUATION_IMPLICIT, false);
						// TODO: timeout?
						try {
							lock.wait();
						} catch (InterruptedException e) {
							return DONT_SUSPEND;
						}
					}
					return listener.getVote();
				}
			}
			return DONT_CARE;
			
		}catch(CoreException e){
			return DONT_CARE;
		}
		*/
		return DONT_CARE;
	}

	@Override
	public void breakpointRemoved(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint) {
		// TODO Auto-generated method stub

	}

	@Override
	public void breakpointHasRuntimeException(IJavaLineBreakpoint breakpoint,
			DebugException exception) {
		// TODO Auto-generated method stub

	}

	@Override
	public void breakpointHasCompilationErrors(IJavaLineBreakpoint breakpoint,
			Message[] errors) {
		// TODO Auto-generated method stub

	}

	
	/**
	 * Returns an evaluation engine for evaluating this breakpoint's condition
	 * in the given target and project context.
	 * @param vm the VM to get an evaluation engine for
	 * @param project the project context
	 * @return a new {@link IAstEvaluationEngine}
	 */
	private IAstEvaluationEngine getEvaluationEngine(IJavaDebugTarget vm, IJavaProject project) {
		return ((JDIDebugTarget) vm).getEvaluationEngine(project);
	}

	
	@Override
	public void launchesRemoved(ILaunch[] launches) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		for(int i = 0; i < launches.length; i ++){
			ILaunch launch = launches[i];
			if(launch.getLaunchConfiguration().equals(config)){
				if(this.TEST_FINISHED_FLAG == true){
					synchronized(lock){
						lock.notifyAll();
					}
				} else
					this.LAUNCH_TERMINATED_FLAG = true;
			}
		}
		
	}
	
	@Override
	public void testCaseFinished(ITestCaseElement testCaseElement) {
		Result result = testCaseElement.getTestResult(false);
		if(result.equals(Result.ERROR) || result.equals(Result.FAILURE)){
			this.testcaseFailed = true;
		}
		if(this.LAUNCH_TERMINATED_FLAG == true){
			synchronized(lock){
				lock.notifyAll();
			}
		} else
			this.TEST_FINISHED_FLAG = true;
	}
}
