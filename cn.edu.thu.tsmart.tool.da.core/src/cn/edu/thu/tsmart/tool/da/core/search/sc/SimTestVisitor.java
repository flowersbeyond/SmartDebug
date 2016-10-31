package cn.edu.thu.tsmart.tool.da.core.search.sc;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class SimTestVisitor extends ASTVisitor {
	//
	// private List<StringLiteral> ss;
	// private NullLiteral aNull;
	//
	//
	// public SimTestVisitor(){
	// ss=new ArrayList<StringLiteral>();
	// }
	//
	// @Override
	// public boolean visit(FieldDeclaration node) {
	// for (Object obj: node.fragments()) {
	// VariableDeclarationFragment v = (VariableDeclarationFragment)obj;
	// System.out.println("FieldDeclaration:\t" + v.getName());
	// }
	// return true;
	// }
	//
	// @Override
	// public boolean visit(MethodDeclaration node) {
	// System.out.println("MethodDeclaration:\t" + node.getName());
	// return true;
	// }
	//
	// @Override
	// public boolean visit(TypeDeclaration node) {
	// System.out.println("TypeDeclaration visit:\t" + node.getName());
	// return true;
	// }
	//
	// @Override
	// public void endVisit(TypeDeclaration node) {
	// System.out.println("TypeDeclaration endV:\t" + node.getName());
	//
	//// StringLiteral sl1=ss.get(0),sl2=ss.get(1);
	//// System.out.println(sl1.getLiteralValue()+" 与 "+sl2.getLiteralValue()+"
	// 相似度为 "+ SimilarityCalculator.calculateSimilarity(sl1, sl2));
	//// System.out.println(sl1.getLiteralValue()+" 与 NullLiteral 相似度为
	// "+SimilarityCalculator.calculateSimilarity(sl1, aNull));
	//
	// }
	//
	// @Override
	// public boolean visit(StringLiteral node) {
	// System.out.println("StringLiteral visit:\t" + node.getLiteralValue());
	// ss.add(node);
	// return true;
	// }
	//
	// @Override
	// public void endVisit(StringLiteral node) {
	// System.out.println("StringLiteral endV:\t" + node.getLiteralValue());
	// }
	//
	// @Override
	// public boolean visit(NullLiteral node) {
	// System.out.println("NullLiteral visit:\t"+node );
	// aNull=node;
	// return true;
	// }
	//
	// @Override
	// public boolean visit(ArrayInitializer node) {
	// System.out.println("ArrayInitializer visit:\t"+node );
	// return true;
	// }
	//
	// @Override
	// public boolean visit(ParenthesizedExpression node) {
	// System.out.println("ParenthesizedExpression visit:\t"+node );
	// return true;
	// }
	//
	// @Override
	// public boolean visit(QualifiedName node) {
	// return true;
	// }
	//
	// @Override
	// public boolean visit(SimpleName node) {
	//// System.out.println("simplename "+node);
	//// System.out.println("\t\tnode.resolveBinding() "+node.resolveBinding());
	//// System.out.println("\t\tnode.resolveTypeBinding()
	// "+node.resolveTypeBinding());
	// return true;
	// }
	//
	// @Override
	// public boolean visit(Assignment node) {
	// System.out.println("Assignment\t"+node);
	// Expression l=node.getLeftHandSide(),r=node.getRightHandSide();
	// System.out.println(l.resolveTypeBinding());
	// System.out.println(r);
	// return true;
	// }

	@Override
	public boolean visit(VariableDeclarationExpression node) {
		System.out.println("VariableDeclarationExpression\t" + node);
		// Interesting.
		// List<VariableDeclarationFragment>->List->ASTNode.NodeList->AbstractList->extends
		// AbstractCollection<E> implements List<E>
		@SuppressWarnings("unchecked")
		List<VariableDeclarationFragment> fragments = node.fragments();
		for (VariableDeclarationFragment f : fragments) {
			System.out.println("VariableDeclarationFragment\t" + f);
			System.out.println(f.resolveBinding());
		}
		// Expression l=node.getLeftHandSide(),r=node.getRightHandSide();
		// System.out.println(l.resolveTypeBinding());
		// System.out.println(r);
		return true;
	}

}