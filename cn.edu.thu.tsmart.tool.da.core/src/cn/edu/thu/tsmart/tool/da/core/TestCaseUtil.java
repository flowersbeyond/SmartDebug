package cn.edu.thu.tsmart.tool.da.core;

import java.util.ArrayList;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

/**
 * 
 * @author LI Tianchi
 *
 */
public class TestCaseUtil {
	/**
	 * find all test methods in a @param testClass.
	 * if the test class is a subclass of TestCase, then 
	 * its test methods do not need to be annotated with "@Test".
	 * so we find all methods that are named with "testXXX" and has no parameters.
	 * 
	 * for standard test classes, we find the methods annotated with "@Test".
	 * 
	 * @param testClass
	 * @return
	 */
	public static ArrayList<IMethod> getTestMethods(IType testClass) {
		try {
			
			String superClassName = testClass.getSuperclassName();
			ArrayList<IMethod> testMethods = new ArrayList<IMethod>();
			if(superClassName != null && superClassName.indexOf("TestCase") != -1){
				// we now have a test class that extends TestCase.
				// therefore every method that starts with "test" is a valid test method.
				IMethod[] allMethods = testClass.getMethods();
				for(int i = 0; i < allMethods.length; i ++){
					IMethod method = allMethods[i];
					String methodName = method.getElementName();
					if(methodName.startsWith("test") && methodName.length() > 4 && method.getParameterNames().length == 0){
						testMethods.add(method);
					}
				}
			} else{
				IMethod[] allMethods = testClass.getMethods();
				
				for (int i = 0; i < allMethods.length; i++) {
					IAnnotation[] annots = allMethods[i].getAnnotations();
					for (int j = 0; j < annots.length; j++) {
						if (annots[j].getElementName().equals("Test"))
							testMethods.add(allMethods[i]);
					}
				}
			}
			return testMethods;
		} catch (JavaModelException e) {
			e.printStackTrace();
			return null;
		}

	}
}
