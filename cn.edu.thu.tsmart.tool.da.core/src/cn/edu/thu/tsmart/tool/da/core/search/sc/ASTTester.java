package cn.edu.thu.tsmart.tool.da.core.search.sc;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class ASTTester {

	public static void main(String[] args) {
		String path = "src/cn/edu/thu/tsmart/tool/da/core/fix/sc/Apple.java";

		byte[] input = null;
		try {
			BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(path));
			input = new byte[bufferedInputStream.available()];
			bufferedInputStream.read(input);
			bufferedInputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String str = new String(input);

		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		parser.setBindingsRecovery(true);

		@SuppressWarnings("rawtypes")
		Hashtable options = JavaCore.getOptions();
		parser.setCompilerOptions(options);

		String unitName = "Apple.java";
		parser.setUnitName(unitName);

		String[] sources = { "src/cn/edu/thu/tsmart/tool/da/core/fix/sc" };
		String[] classpath = {};// {".:/usr/local/lib/ST-4.0.8.jar:/usr/local/lib/antlr-4.5-complete.jar:"};

		parser.setEnvironment(classpath, sources, new String[] { "UTF-8" }, true);
		parser.setSource(str.toCharArray());

		CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		if (cu.getAST().hasBindingsRecovery()) {
			System.out.println("Binding activated.");
		}

		TypeFinderVisitor v = new TypeFinderVisitor();
		cu.accept(v);
	}
}

class TypeFinderVisitor extends ASTVisitor {

	// public boolean visit(VariableDeclarationStatement node) {
	// for (Iterator iter = node.fragments().iterator(); iter.hasNext();) {
	// System.out.println("------------------");
	//
	// VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter
	// .next();
	// IVariableBinding binding = fragment.resolveBinding();
	//
	// System.out.println("binding variable declaration: "
	// + binding.getVariableDeclaration());
	// System.out.println("binding: " + binding);
	// }
	// return true;
	// }
	//
	// @Override
	// public boolean visit(Assignment node) {
	// System.out.println("Assignment\t" + node);
	// ITypeBinding binding = node.resolveTypeBinding();
	// System.out.println("\tbinding\t"+binding);
	// System.out.println("\tbinding.getName()\t"+binding.getName());
	// System.out.println("\tbinding.getBinaryName()\t"+binding.getBinaryName());
	// return true;
	// }

	@Override
	public boolean visit(ArrayAccess node) {
		System.out.println("ArrayAccess\t" + node);
		ITypeBinding binding = node.resolveTypeBinding();
		System.out.println(binding);
		System.out.println(binding.getTypeDeclaration());

		Expression e = (Expression) node;
		System.out.println("e.getNodeType() " + e.getNodeType());
		System.out.println("e.getNodeType()==ASTNode.ARRAY_ACCESS " + (e.getNodeType() == ASTNode.ARRAY_ACCESS));

		/* ArraƒyAccess iaa[2][2] ArrayAccess iaa[2] 两次 */
		return true;
		// return false;
	}

}