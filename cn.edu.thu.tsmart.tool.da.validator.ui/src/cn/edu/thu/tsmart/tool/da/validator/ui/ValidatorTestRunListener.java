package cn.edu.thu.tsmart.tool.da.validator.ui;

import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestRunSession;

import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;

public class ValidatorTestRunListener extends TestRunListener{

	private static String testClassName = "";
	private static String testMethodName = "";
	
	/**
	 * A test run session has been launched. The test tree is not available yet.
	 * <p>
	 * Important: The implementor of this method must not keep a reference to the session element
	 * after {@link #sessionFinished(ITestRunSession)} has finished.
	 * </p>
	 *
	 * @param session the session that has just been launched
	 * @since 3.6
	 */
	@Override
	public void sessionLaunched(ITestRunSession session) {
	}
	
	/**
	 * A test run session has started. The test tree can be accessed through the session element.
	 * <p>
	 * Important: The implementor of this method must not keep a reference to the session element
	 * after {@link #sessionFinished(ITestRunSession)} has finished.
	 * </p>
	 *
	 * @param session the session that has just started.
	 */
	@Override
	public void sessionStarted(ITestRunSession session) {
	}

	/**
	 * A test run session has finished. The test tree can be accessed through the session element.
	 *
	 * <p>
	 * Important: The implementor of this method must not keep the session element when the method is finished.
	 * </p>
	 *
	 * @param session the test
	 */
	@Override
	public void sessionFinished(ITestRunSession session) {
	}

	/**
	 * A test case has started. The result can be accessed from the element.
	 * <p>
	 * Important: The implementor of this method must not keep a reference to the test case element
	 * after {@link #sessionFinished(ITestRunSession)} has finished.
	 * </p>
	 * @param testCaseElement the test that has started to run
	 */
	@Override
	public void testCaseStarted(ITestCaseElement testCaseElement) {
		ValidatorTestRunListener.testClassName = testCaseElement.getTestClassName();
		ValidatorTestRunListener.testMethodName = testCaseElement.getTestMethodName();
		//System.out.println(testCaseElement.getTestClassName() + ":" + testCaseElement.getTestMethodName() + " started");
		
		/*IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
		TestCase tc = Validator.getInstance().getTestCase(testClassName, testMethodName);
		ArrayList<Checkpoint> checkpoints = Validator.getInstance().getConditionForTestCase(tc);
		Checkpoint[] cps= new Checkpoint[checkpoints.size()];
		try {
			manager.addBreakpoints(checkpoints.toArray(cps));
		} catch (CoreException e) {
			e.printStackTrace();
		}*/
	}

	/**
	 * A test case has ended. The result can be accessed from the element.
	 * <p>
	 * Important: The implementor of this method must not keep a reference to the test case element
	 * after {@link #sessionFinished(ITestRunSession)} has finished.
	 * </p>
	 *
	 * @param testCaseElement the test that has finished running
	 */
	@Override
	public void testCaseFinished(ITestCaseElement testCaseElement) {
		ValidatorTestRunListener.testClassName = "";
		ValidatorTestRunListener.testMethodName = "";
		//System.out.println(testCaseElement.getTestClassName() + ":" + testCaseElement.getTestMethodName() + " finished");
		/*
		IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
		TestCase tc = Validator.getInstance().getTestCase(testClassName, testMethodName);
		ArrayList<Checkpoint> checkpoints = Validator.getInstance().getConditionForTestCase(tc);
		Checkpoint[] cps= new Checkpoint[checkpoints.size()];
		try {
			manager.removeBreakpoints((checkpoints.toArray(cps)), false);
		} catch (CoreException e) {
			e.printStackTrace();
		}*/
	}
	

	public static TestCase getCurrentRunningTestCase(){
		if(testClassName == "" || testMethodName == ""){
			return null;
		}
		return new TestCase(testClassName, testMethodName);
	}

}
