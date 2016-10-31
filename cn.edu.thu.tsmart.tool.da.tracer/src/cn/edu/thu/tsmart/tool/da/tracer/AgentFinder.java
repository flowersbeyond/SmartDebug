package cn.edu.thu.tsmart.tool.da.tracer;
import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.ui.IStartup;
import org.osgi.framework.Bundle;


public class AgentFinder implements IStartup {
	
	private static String agentDir = "";

	@Override
	public void earlyStartup() {
		Bundle b = Activator.getDefault().getBundle();
		try {
			File bundleDir = FileLocator.getBundleFile(b);
			System.out.println("find bundle:" + bundleDir.getAbsolutePath());
			File jarFile = new File(bundleDir, "lib/agent.jar");
			agentDir = jarFile.getAbsolutePath();
			agentDir = "\"" + agentDir + "\"";
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getAgentDir(){
		return agentDir;
	}
}
