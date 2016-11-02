package cn.edu.thu.tsmart.tool.da.core.validator;

import cn.edu.thu.tsmart.tool.da.core.validator.cp.StatusCode;

public class TestCase {
	/**
	 * qualified name
	 */
	private String className;
	private String methodName;
	private StatusCode status;

	public TestCase(String className, String methodName) {
		this.className = className;
		this.methodName = methodName;
		this.status = StatusCode.UNKNOWN;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof TestCase) {
			TestCase tc = (TestCase) object;
			if (tc.getClassName().equals(this.className) && tc.getMethodName().equals(this.getMethodName()))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (className + methodName).hashCode();
	}
	
	public StatusCode getStatus(){
		return this.status;
	}

	public void setStatus(StatusCode status) {
		this.status = status;		
	}
}
