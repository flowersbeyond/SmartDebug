package cn.edu.thu.tsmart.tool.da.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaBreakpointListener;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement.Result;

import cn.edu.thu.tsmart.tool.da.core.validator.cp.Checkpoint;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.ConditionItem;
import cn.edu.thu.tsmart.tool.da.core.validator.cp.StatusCode;

public class CheckpointConditionRefresher extends TestRunListener implements IJavaBreakpointListener, ILaunchesListener2{

	protected ILaunchConfiguration config;

	protected ConditionItem failedConditionItem = null;
	protected boolean testcaseFailed = false;
	
	protected boolean TEST_FINISHED_FLAG = false;
	protected boolean LAUNCH_TERMINATED_FLAG = false;
	protected boolean CHECKPOINT_VIOLATED_FLAG = false;
	
	private Object lock;
	private Map<IBreakpoint, Checkpoint> bpcpMap;
	private ArrayList<Checkpoint> cps;
	private Set<ConditionItem> passedConditions;
	
	public CheckpointConditionRefresher(ILaunchConfiguration config, ArrayList<Checkpoint> cps, Map<IBreakpoint, Checkpoint> bpcpMap, Object lock) {
		this.config = config;
		this.bpcpMap = bpcpMap;
		this.lock = lock;
		this.passedConditions = new HashSet<ConditionItem>();
		this.cps = cps;
		clearCheckpointStatus();
	}
	
	private void clearCheckpointStatus(){
		for(Checkpoint cp: cps){
			cp.setStatus(StatusCode.UNKNOWN);
			for(ConditionItem item: cp.getConditions()){
				item.setUnknown();
				item.setHitCount(0);
			}
		}
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
				ArrayList<ConditionItem> conditions = cp.getConditions();
				for(ConditionItem item: conditions){
					int hitConditionSatisfied = evaluate(item.getHitCondition(), thread);
					if(hitConditionSatisfied == -1){
						// compilation error, item out of date
						item.setOutOfDate();
						continue;
					} else if (hitConditionSatisfied == 1){
						item.increaseHitCount();
						int expectationSatisfied = evaluate(item.getExpectation(), thread);
						if(expectationSatisfied == -1){
							//compilation error, item out of date
							item.setOutOfDate();
							continue;
						}
						
						if(expectationSatisfied == 1){
							passedConditions.add(item);
						} else {
							this.failedConditionItem = item;
							item.setFailed();
							item.setFailHitTime(item.getHitCount());
							this.testcaseFailed = true;
							if(passedConditions.contains(item)){
								passedConditions.remove(item);
							}						
							
							this.CHECKPOINT_VIOLATED_FLAG = true;
							thread.getLaunch().terminate();
						}
					}
				}
				
				return IJavaBreakpointListener.DONT_SUSPEND;
			
			} 
		} catch (CoreException e) {
			e.printStackTrace();
		}
			
		return IJavaBreakpointListener.DONT_SUSPEND;
	}
	
	private void cleanUp(){
		for(ConditionItem passitem: passedConditions){
			if(passitem.getHitCondition().equals(Checkpoint.HIT_ALWAYS)){
				passitem.setUnknown();
			} else {
				passitem.setPassed();
			}
		}
		
		Checkpoint failCheckpoint = null;
		for(Checkpoint checkpoint: this.cps){
			boolean allpass = true;								
			for(ConditionItem i: checkpoint.getConditions()){
				if(i.getStatus() != StatusCode.PASSED){
					allpass = false;
				}
				if(i.equals(failedConditionItem)){
					failCheckpoint = checkpoint;
				}
			}
			if(allpass)
				checkpoint.setStatus(StatusCode.PASSED);
		}
		if(failCheckpoint != null)
			failCheckpoint.setStatus(StatusCode.FAILED);
		
	}

	private int evaluate(String hitCondition, IJavaThread thread) {
		IJavaProject project = SmartDebugPlugin.getLastFixSession().getProject();
		try{
		IJavaValue value = EclipseUtils.evaluateExpr(hitCondition, thread, (IJavaStackFrame)thread.getTopStackFrame(), project);
		if(value != null){
			if(value.getValueString().equals("true"))
				return 1;
			else if (value.getValueString().equals("false"))
				return 0;
		}
		} catch (DebugException e){
			e.printStackTrace();
			return -1;
		}
		return -1;
	}

	@Override
	public void breakpointRemoved(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint) {}

	@Override
	public void breakpointHasRuntimeException(IJavaLineBreakpoint breakpoint,
			DebugException exception) {}

	@Override
	public void breakpointHasCompilationErrors(IJavaLineBreakpoint breakpoint,
			Message[] errors) {}
	
	@Override
	public void launchesRemoved(ILaunch[] launches) {}

	@Override
	public void launchesAdded(ILaunch[] launches) {}

	@Override
	public void launchesChanged(ILaunch[] launches) {}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		for(int i = 0; i < launches.length; i ++){
			ILaunch launch = launches[i];
			if(launch.getLaunchConfiguration().equals(config)){
				
				cleanUp();
				
				if(this.TEST_FINISHED_FLAG == true || CHECKPOINT_VIOLATED_FLAG == true){
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
		} else{
			this.TEST_FINISHED_FLAG = true;
		}
	}

	public boolean getTestPassed() {
		return !testcaseFailed;
	}

	public ConditionItem getFailedExpectation() {
		return this.failedConditionItem;
	}
}
