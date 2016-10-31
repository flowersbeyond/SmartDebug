package cn.edu.thu.tsmart.tool.da.tracer;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import cn.edu.thu.tsmart.tool.da.tracer.util.EclipseUtils;
import cn.edu.thu.tsmart.tool.da.tracer.util.FileUtils;

public class SDTraceLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate implements IExecutableExtension{
	
	//The type of launch being handled.
	protected String launchtype;
	
	//The delegate which will do most of the work.
	protected ILaunchConfigurationDelegate launchdelegate;

	// IExecutableExtension interface:
	@Override
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		launchtype = config.getAttribute("type"); //$NON-NLS-1$
		launchdelegate = getLaunchDelegate(launchtype);
	}

	@SuppressWarnings("deprecation")
	private ILaunchConfigurationDelegate getLaunchDelegate(String launchtype)
			throws CoreException {
		ILaunchConfigurationType type = DebugPlugin.getDefault()
				.getLaunchManager().getLaunchConfigurationType(launchtype);
		if (type == null) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unknown launch type", new Throwable()));
		}
		return type.getDelegate(ILaunchManager.DEBUG_MODE);
	}
	
	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		try {
			// Create working copy launch config
			ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
			
			// init message pipeline
			ServerSocket serversocket = new ServerSocket(0);
			TraceServer server = new TraceServer(serversocket);
			new Thread(server).start();
			
			//parse class names;
			String projectName = configuration.getAttribute(
					IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
					(String) null);
			IProject proj = ResourcesPlugin.getWorkspace().getRoot()
					.getProject(projectName);
			IJavaProject javaproj = null;
			if (proj != null) {
				javaproj = JavaCore.create(proj);
			}
			if(javaproj == null)
				return;
			
			ArrayList<String>classNames = new ArrayList<String>();
			IPackageFragmentRoot[] roots = javaproj.getAllPackageFragmentRoots();
			for(int j = 0; j < roots.length;j ++){
				if(roots[j].getKind() == IPackageFragmentRoot.K_SOURCE){
					IJavaElement[] elements = roots[j].getChildren();
					for(int k = 0; k < elements.length; k ++){
						ICompilationUnit[] cus = ((IPackageFragment)elements[k]).getCompilationUnits();
						for(int l = 0; l < cus.length; l ++){
							IType[] types = cus[l].getAllTypes();
							for(int m = 0; m < types.length; m ++){
								classNames.add(types[m].getFullyQualifiedName());
							}
						}
					}
				}
			}
			
			String projdir = EclipseUtils.getProjectDir(javaproj);
			String configFileDir = projdir + "/sdtrace/classes-config";
			File configFile = FileUtils.ensureFile(configFileDir);
			BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));
			
			//write port to config file
			String portString = serversocket.getLocalPort()+"";
			writer.write(portString);
			writer.newLine();
			//write class names to config file
			for(int i = 0; i < classNames.size(); i ++){
				writer.write(classNames.get(i));
				writer.newLine();
			}
			writer.close();
			
			
			// Add VM arguments
			configFileDir = projdir + "/sdtrace";
			String vmArgs = wc.getAttribute(
					IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "");
			String agentdir = AgentFinder.getAgentDir();
			if (agentdir != null && agentdir.length() > 0) {
				vmArgs += " -javaagent:" + agentdir;
				if(configFileDir.indexOf(' ') != -1){
					configFileDir = "\"" + configFileDir + "\"";
				}
				vmArgs += "=" + configFileDir;
			}
			System.out.println("vmargs:" + vmArgs);
			// launch trace printer thread
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
			// Start launch
			launchdelegate.launch(wc, ILaunchManager.DEBUG_MODE, launch,
					new SubProgressMonitor(monitor, 1));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}

class TraceServer implements Runnable{
	private ServerSocket server;
	public TraceServer(ServerSocket server){
		this.server = server;
	}
	@Override
	public void run() {
		try {
			Socket client = server.accept();
			DataInputStream in = new DataInputStream(client.getInputStream());
			while(true){
				String message = in.readUTF();
				// TODO: should delete this statement. This is only for debug & demo.
				//System.out.println(message);
				TraceRecordContainer.appendTraceMessage(message);
			}
		} catch (IOException e) {
		} finally{
			try {
				server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
}
