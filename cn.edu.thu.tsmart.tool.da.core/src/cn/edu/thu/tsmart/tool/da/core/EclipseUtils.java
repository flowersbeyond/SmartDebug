package cn.edu.thu.tsmart.tool.da.core;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.eval.EvaluationManager;
import org.eclipse.jdt.debug.eval.IAstEvaluationEngine;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
import org.eclipse.jdt.debug.eval.IEvaluationResult;

public class EclipseUtils {
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
	
	public static  IJavaValue evaluateExpr(String newExprString, IJavaThread thread,
			IJavaStackFrame stackframe, IJavaProject project) {
		
		IAstEvaluationEngine evalEngine = EvaluationManager.newAstEvaluationEngine(project, (IJavaDebugTarget) thread.getDebugTarget());
		Object evalLock = new Object();
		EvaluationListener listener = new EvaluationListener(evalLock);
		
		Timer timer = new Timer();
		EvaluationTimeoutTask timeoutTask = new EvaluationTimeoutTask(evalLock);
		synchronized(evalLock){
			try {
				evalEngine.evaluate(newExprString, stackframe, listener, DebugEvent.EVALUATION, false);
				timer.schedule(timeoutTask, 200);
				evalLock.wait();
			} catch (DebugException e) {
				e.printStackTrace();
				return null;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		IJavaValue value = null;
		if(!timeoutTask.evaluationTimeOut()){
			timeoutTask.cancel();
			IEvaluationResult result = listener.getResult();
			if(result.getErrorMessages().length == 0 && result.getException() == null){
				value = result.getValue();
			}
		}
		evalEngine.dispose();
		return value;
	}
}

class EvaluationListener implements IEvaluationListener{
	private IEvaluationResult result;
	private Object lock;
	
	public EvaluationListener(Object lock){
		this.lock = lock;
	}
	
	public IEvaluationResult getResult(){
		return result;
	}
	@Override
	public void evaluationComplete(IEvaluationResult result) {
		this.result = result;
		synchronized(lock){
			lock.notifyAll();
		}
	}
	
}

class EvaluationTimeoutTask extends TimerTask{
	Object lock;
	boolean timeout = false;
	public EvaluationTimeoutTask(Object lock){
		this.lock = lock;
	}
	
	public boolean evaluationTimeOut(){
		return timeout;
	}
	
	@Override
	public void run(){
		timeout = true;
		synchronized(lock){
			lock.notifyAll();
		}
	}
}
