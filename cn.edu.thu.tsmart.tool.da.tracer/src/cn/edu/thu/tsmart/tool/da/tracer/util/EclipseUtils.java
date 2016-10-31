package cn.edu.thu.tsmart.tool.da.tracer.util;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;


public class EclipseUtils {
	public static String getProjectDir(IJavaProject project) throws JavaModelException{
		String projectdir = project.getUnderlyingResource().getLocation().toOSString() + "/";
		return projectdir;
	}
}
