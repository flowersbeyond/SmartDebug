package cn.edu.thu.tsmart.tool.da.core.search.sc.calculator;

import org.eclipse.jdt.core.dom.*;

import cn.edu.thu.tsmart.tool.da.core.search.sc.U;

/**
 * 2015-12-22 14:35:07 都有算法了..
 * */
public class BooleanLiteralCalculator {

	public static double sim(BooleanLiteral e1, Expression e2) {
		if (e2 instanceof ArrayAccess) {
			// return sim(e1,(ArrayAccess)e2);
			return 0.3;
		} else if (e2 instanceof ArrayCreation) {
			// return sim(e1,(ArrayCreation)e2);
			return 0;
		} else if (e2 instanceof ArrayInitializer) {
			// return sim(e1,(ArrayInitializer)e2);
			return 0;
		} else if (e2 instanceof Assignment) {
			// return sim(e1,(Assignment)e2);
			return 0.1;
		} else if (e2 instanceof BooleanLiteral) {
			// return sim(e1,(BooleanLiteral)e2);
			return 1;
		} else if (e2 instanceof CastExpression) {
			// return sim(e1,(CastExpression)e2);
			return 0;
		} else if (e2 instanceof CharacterLiteral) {
			// return sim(e1,(CharacterLiteral)e2);
			return 0;
		} else if (e2 instanceof ClassInstanceCreation) {
			// return sim(e1,(ClassInstanceCreation)e2);
			return 0;
		} else if (e2 instanceof ConditionalExpression) {
			// return sim(e1,(ConditionalExpression)e2);
			ConditionalExpression ce = (ConditionalExpression) e2;
			return U.mean(SimilarityCalculator.sim(e1, ce.getThenExpression()),
					SimilarityCalculator.sim(e1, ce.getElseExpression()));
		} else if (e2 instanceof FieldAccess) {
			// return sim(e1,(FieldAccess)e2);
			return 0.5;
		} else if (e2 instanceof InfixExpression) {
			// return sim(e1,(InfixExpression)e2);
			InfixExpression ie = (InfixExpression) e2;
			return U.canInfixOpReturnBoolean(ie.getOperator()) ? 0.9 : 0;
		} else if (e2 instanceof InstanceofExpression) {
			// return sim(e1,(InstanceofExpression)e2);
			return 0.8;
		} else if (e2 instanceof MethodInvocation) {
			// return sim(e1,(MethodInvocation)e2);
			return 0.5;
		} else if (e2 instanceof Name) {
			// return sim(e1,(Name)e2);
			return 0.5;
		} else if (e2 instanceof NullLiteral) {
			// return sim(e1,(NullLiteral)e2);
			return 0.6;
		} else if (e2 instanceof NumberLiteral) {
			// return sim(e1,(NumberLiteral)e2);
			return 0;
		} else if (e2 instanceof ParenthesizedExpression) {
			// return sim(e1,(ParenthesizedExpression)e2);
			return 0;
		} else if (e2 instanceof PostfixExpression) {
			// return sim(e1,(PostfixExpression)e2);
			return 0;
		} else if (e2 instanceof PrefixExpression) {
			// return sim(e1,(PrefixExpression)e2);
			return 0;
		} else if (e2 instanceof StringLiteral) {
			// return sim(e1,(StringLiteral)e2);
			return 0;
		} else if (e2 instanceof SuperFieldAccess) {
			// return sim(e1,(SuperFieldAccess)e2);
			return 0.5;
		} else if (e2 instanceof SuperMethodInvocation) {
			// return sim(e1,(SuperMethodInvocation)e2);
			return 0.5;
		} else if (e2 instanceof ThisExpression) {
			// return sim(e1,(ThisExpression)e2);
			return 0;
		} else if (e2 instanceof TypeLiteral) {
			// return sim(e1,(TypeLiteral)e2);
			return 0;
		} else if (e2 instanceof VariableDeclarationExpression) {
			// return sim(e1,(VariableDeclarationExpression)e2);
			return 0;
		} else {
			return SimilarityConstant.sim(e1.getNodeType(), e2.getNodeType());
		}
	}

}
