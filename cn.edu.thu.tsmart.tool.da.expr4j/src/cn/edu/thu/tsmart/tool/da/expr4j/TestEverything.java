package cn.edu.thu.tsmart.tool.da.expr4j;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.BugFixer;
import cn.edu.thu.tsmart.tool.da.core.Logger;
import cn.edu.thu.tsmart.tool.da.core.validator.FixValidator;
import cn.edu.thu.tsmart.tool.da.expr4j.oracle.Oracle;
import cn.edu.thu.tsmart.tool.da.expr4j.oracle.OracleManager;

public class TestEverything extends Job{

	public TestEverything() {
		super("Test Everything");
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		try{
			OracleManager.readInOracle();
			Logger.setLogRootDir("D:/Documents/Research/defects4j/experiments/data/");
			
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			for(int i = 0; i < projects.length; i ++){
				IProject proj = projects[i];
				try{
					if(proj.isAccessible() && proj.hasNature(JavaCore.NATURE_ID)){
						IJavaProject javaProject = JavaCore.create(projects[i]);
						Oracle oracle = OracleManager.getOracle(proj.getName());
						String testTypeName = oracle.getQualifiedTestTypeName();
						IType testType = javaProject.findType(testTypeName);
						BugFixSession session = new BugFixSession(javaProject, testType);
						BugFixer fixer = new BugFixer(session);
						FixValidator.registerValidateEventListener(oracle);
						fixer.schedule();
						fixer.join();
						FixValidator.removeValidateEventListener(oracle);
						
					}
					System.gc();
				} catch(CoreException e){
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (Exception e){
					e.printStackTrace();
					System.out.println("Exception happened in proj:" + proj.getName());
				}
			}
		} catch (Exception e){
			e.printStackTrace();
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}
}
