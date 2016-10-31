package cn.edu.thu.tsmart.tool.da.core.validator;

public class TestCase {
	/**
	 * qualified name
	 */
	private String className;
	private String methodName;

	public TestCase(String className, String methodName) {
		this.className = className;
		this.methodName = methodName;
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

	/**
	 * 因为要用 HashMap 吖. 碰撞了没事, hashCode 相等之后还会用 equals 精确判定的. 随便设计了一个. <br>
	 * 只要满足 contract: a.equals(b) -> a.hashCode()==b.hashCode() 就可以 (逆命题不必成立).
	 * <br>
	 * @see <a href=
	 *      "http://boxingp.github.io/blog/2015/02/24/use-equals-and-hashcode-methods-in-java-correctly/">
	 *      一篇讲 equals 和 hashCode 的文章</a>
	 * 
	 */
	@Override
	public int hashCode() {
		return (className + methodName).hashCode();
	}
}
