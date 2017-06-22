package cn.edu.thu.tsmart.tool.da.core;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class SmartDebugPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "cn.edu.thu.tsmart.tool.da.fix"; //$NON-NLS-1$

	// The shared instance
	private static SmartDebugPlugin plugin;
	
	
	private static BugFixSession currentFixSession;
	//private ArrayList<BugFixSession> currentFixSessions = new ArrayList<BugFixSession>();

	public static boolean USE_FILTRATION = false;
	
	/** 
	 * // return last one. null if no FixSession.<br>
	 * 完全是 (临时的, 目测只在少量情况下正确的) workaround! 详询 getLastFixSession 的 call hierarchy */
	public static BugFixSession getLastFixSession(){
		try{
			return currentFixSession;
			//return plugin.currentFixSessions.get(plugin.currentFixSessions.size()-1);
		}
		catch(java.lang.ArrayIndexOutOfBoundsException e){
			return null;
		}
	}
	
	// 2016-06-22 始终是1
	public static int getFixSessionsSize(){
		//return plugin.currentFixSessions.size();
		return 1;
	}
	
	public static int getLastFixSessionMethodsSize(){
		//return plugin.currentFixSessions.get(plugin.currentFixSessions.size()-1).getTestMethods().size();
		return currentFixSession.getTestMethods().size();
	}
	
	/**
	 * The constructor
	 */
	public SmartDebugPlugin(){
	}
	
	public BugFixSession findBugFixSession(IJavaProject project, IType testsType){
		/*for(BugFixSession session: currentFixSessions){
			if(session.getProject().equals(project) && session.getTestsType().equals(testsType)){
				return session;
			}
		}*/
		if(currentFixSession.getProject().equals(project) && currentFixSession.getTestsType().equals(testsType))
			return currentFixSession;
		return null;
	}
	
	public void startNewSession(IJavaProject project, IType testsType){
		BugFixSession session = new BugFixSession(project, testsType);
		//currentFixSessions.add(session);
		currentFixSession = session;
		System.gc();
		
	}
	
	public void cancelCurrentSession() {
		/*if(currentFixSessions.size() >= 1){
			BugFixSession session = currentFixSessions.get(currentFixSessions.size() - 1);
			session.getBugFixer().stopAnalysis();
		}*/
		if(currentFixSession!= null){
			currentFixSession.getBugFixer().stopAnalysis();
		}
	}
	
	public void continueLastSession() {
		/*if(currentFixSessions.size() >= 1){
			BugFixSession session = currentFixSessions.get(currentFixSessions.size() - 1);
			session.getBugFixer().schedule();
		}*/
		if(currentFixSession != null){
			currentFixSession.getBugFixer().schedule();
		}
	}


	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		/*for(BugFixSession session: currentFixSessions){
			session.getBugFixer().stopAnalysis();
		}
		currentFixSessions.clear();*/
		if(currentFixSession != null)
			currentFixSession.getBugFixer().stopAnalysis();
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static SmartDebugPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
}
