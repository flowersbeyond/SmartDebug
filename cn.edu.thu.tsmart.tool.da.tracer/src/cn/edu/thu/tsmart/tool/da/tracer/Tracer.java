package cn.edu.thu.tsmart.tool.da.tracer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaBreakpointListener;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class Tracer {
	
	private static IJavaProject project;
	
	public static IJavaProject getCurrentProject(){
		return project;
	}
	
	public void traceCurrentProject(){
		// 鍘诲彇寰梬s閲屾墍鏈塸roject
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		
		IJavaProject javaproj = null;
		if(projects.length == 0)
			return;
		try {
			for(int i = 0; i < projects.length; i ++){
				if(projects[i].isOpen() && projects[i].hasNature(JavaCore.NATURE_ID)){
					javaproj = JavaCore.create(projects[i]);
					break;
				}
			}
			
			
			if (javaproj == null)
				return;
			project = javaproj; // 鎵惧埌浜嗙涓�涓猨ava project, 璁版垚涓�涓叏灞�鍙橀噺
			
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager
					.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
			
			ILaunchConfiguration[] configurations = manager
					.getLaunchConfigurations(type);
			ILaunchConfiguration currentConfig = null;
			for (int i = 0; i < configurations.length; i++) {
				ILaunchConfiguration configuration = configurations[i];
				String projectName = configuration.getAttribute(
						IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
				if (javaproj.getProject().getName().equals(projectName)) {
					currentConfig = configuration;
					break;
				}
			}
			if(currentConfig != null){
				TraceProcessListener listener = new TraceProcessListener(currentConfig, new DynamicTranslator(javaproj, true));
				JDIDebugModel.addJavaBreakpointListener(listener);
				DebugPlugin.getDefault().getLaunchManager().addLaunchListener(listener);			
				currentConfig.launch("sdtrace", new NullProgressMonitor());
			}
			
		} catch (CoreException e) {
			e.printStackTrace();
			return;
		}
	}

}

class TraceProcessListener implements IJavaBreakpointListener, ILaunchesListener2{

	private DynamicTranslator translator;
	private ILaunchConfiguration config;
	public TraceProcessListener (ILaunchConfiguration config, DynamicTranslator translator){
		this.translator = translator;
		this.config = config;
	}
	@Override
	public void launchesRemoved(ILaunch[] launches) {
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
	}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		for(int i = 0; i < launches.length; i ++){
			ILaunch launch = launches[i];
			if(launch.getLaunchConfiguration().equals(config)){
				translator.handleNewActions();
				translator.fireTraceEvent(ITraceEventListener.LAUNCH_TERMINATED);	
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
		try{
			IJavaStackFrame stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
			
			String className = stackFrame.getDeclaringTypeName();
			String methodName = stackFrame.getMethodName();
			int lineNumber = stackFrame.getLineNumber();
		
			translator.addSuspendAction(className, methodName, "methodSignature", lineNumber);
			translator.handleNewActions();
			translator.fireTraceEvent(ITraceEventListener.HIT_BREAKPOINT);
			return DONT_CARE;
		}
		catch (DebugException e){
			e.printStackTrace();
			return DONT_CARE;
		}
	}

	@Override
	public void breakpointRemoved(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint) {		
	}

	@Override
	public void breakpointHasRuntimeException(IJavaLineBreakpoint breakpoint,
			DebugException exception) {		
	}

	@Override
	public void breakpointHasCompilationErrors(IJavaLineBreakpoint breakpoint,
			Message[] errors) {
	}
	
}