package cn.edu.thu.tsmart.tool.da.core.validator;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.junit.JUnitCore;

import cn.edu.thu.tsmart.tool.da.core.validator.cp.Checkpoint;

public class TestCaseValidator{

	private boolean validateresult;
	private Checkpoint failingCheckpoint;
	
	//private BugFixSession session;
	
	public TestCaseValidator() {
		//this.session = session;
	}


	public void validate(ILaunchConfiguration tcLaunchConfig, ArrayList<Checkpoint> cps) {
		
	try {			
			IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
			IBreakpoint[] bps = manager.getBreakpoints();
			ArrayList<IBreakpoint> enabledBreakpoints = new ArrayList<IBreakpoint>();
			for(IBreakpoint bp: bps){
				if(bp.isEnabled()){
					enabledBreakpoints.add(bp);
					bp.setEnabled(false);
				}
					
			}
			
			Checkpoint[] cpsArray = new Checkpoint[cps.size()];
			cpsArray = cps.toArray(cpsArray);
			for(int i = 0; i < cpsArray.length; i ++){
				cpsArray[i].ensureCPMarker();
			}
			manager.addBreakpoints(cpsArray);
			
			CheckpointListener listener = new CheckpointListener(tcLaunchConfig, cps);
			
			JDIDebugModel.addJavaBreakpointListener(listener);
			DebugPlugin.getDefault().getLaunchManager().addLaunchListener(listener);
			JUnitCore.addTestRunListener(listener);
			
			Object lock = listener.getLock();
			Timer timer = new Timer();
			TestCaseValidateTimeoutTask timeoutTask = new TestCaseValidateTimeoutTask(lock);
			ILaunch launch = null;
			synchronized (lock) {
				launch = tcLaunchConfig.launch(ILaunchManager.DEBUG_MODE, new NullProgressMonitor());
				//DebugUIPlugin.launchInBackground(tcLaunchConfig, ILaunchManager.DEBUG_MODE);
				timer.schedule(timeoutTask , 5000);
				lock.wait();				
			}
			if(!timeoutTask.validateTimeOut()){
				timer.cancel();
				Checkpoint cp = listener.getFailedCheckpoint();
				if(cp == null && !listener.junitTestCaseFailed()){
					this.validateresult = true;
					System.out.println(tcLaunchConfig.toString() + ": passed");
				} else {
					this.validateresult = false;
					this.failingCheckpoint = cp;
					System.out.println(tcLaunchConfig.toString() + ": failed");
					
				}
			
				JUnitCore.removeTestRunListener(listener);
				JDIDebugModel.removeJavaBreakpointListener(listener);
				DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(listener);
				manager.removeBreakpoints(cpsArray, false);
				for(IBreakpoint bp: enabledBreakpoints){
					bp.setEnabled(true);
				}
				for(int i = 0; i < cpsArray.length; i ++){
					cpsArray[i].ensureCPMarker();
				}
			} else{
				this.validateresult = false;
				JUnitCore.removeTestRunListener(listener);
				JDIDebugModel.removeJavaBreakpointListener(listener);
				DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(listener);
				
				if(launch != null)
					launch.terminate();
				manager.removeBreakpoints(cpsArray, false);
				for(IBreakpoint bp: enabledBreakpoints){
					bp.setEnabled(true);
				}
				for(int i = 0; i < cpsArray.length; i ++){
					cpsArray[i].ensureCPMarker();
				}	
			}
			for(int i = 0; i < cpsArray.length; i ++){
				cpsArray[i].ensureCPMarker();
			}
		
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	public boolean getValidationResult(){
		return validateresult;
	}
	
	public Checkpoint getFailingCheckpoint(){
		return failingCheckpoint;
	}

}

class TestCaseValidateTimeoutTask extends TimerTask{
	Object lock;
	boolean timeout = false;
	public TestCaseValidateTimeoutTask(Object lock){
		this.lock = lock;
	}

	public boolean validateTimeOut(){
		return timeout;
	}
	@Override
	public void run() {
		timeout = true;
		synchronized(lock){
			lock.notifyAll();
		}
	}
	
}
