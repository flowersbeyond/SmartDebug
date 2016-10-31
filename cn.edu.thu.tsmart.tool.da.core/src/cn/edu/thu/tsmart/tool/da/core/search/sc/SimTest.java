package cn.edu.thu.tsmart.tool.da.core.search.sc;

import org.eclipse.jdt.core.dom.CompilationUnit;

public class SimTest {

	// 入门博文 http://blog.csdn.net/lovelion/article/details/19050155
	// 乱七八糟 http://blog.csdn.net/flying881114/article/details/6187061

	public SimTest(String path) {
		CompilationUnit comp = JdtAstUtil.getCompilationUnit(path);

		SimTestVisitor visitor = new SimTestVisitor();
		comp.accept(visitor);

		// ITypeRoot tr=comp.getTypeRoot();
	}

	public static void main(String[] args) {
		// new
		// SimTest("src/cn/edu/thu/tsmart/tool/da/core/fix/sc/DemoSimSource.java");
		new SimTest("src/cn/edu/thu/tsmart/tool/da/core/fix/sc/DemoBinding.java");
	}
}