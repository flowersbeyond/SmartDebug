package cn.edu.thu.tsmart.tool.da.core.search.sc.calculator;

import org.eclipse.jdt.core.dom.*;

import cn.edu.thu.tsmart.tool.da.core.search.sc.U;

public class FieldAccessCalculator {

	public static double sim(FieldAccess e1, Expression e2) {
		// if(e2 instanceof ArrayAccess){
		// return sim(e1,(ArrayAccess)e2);
		// }
		// else if(e2 instanceof ArrayCreation){
		// return sim(e1,(ArrayCreation)e2);
		// }
		// else if(e2 instanceof ArrayInitializer){
		// return sim(e1,(ArrayInitializer)e2);
		// }
		// else if(e2 instanceof Assignment){
		// return sim(e1,(Assignment)e2);
		// }
		// else if(e2 instanceof BooleanLiteral){
		// return sim(e1,(BooleanLiteral)e2);
		// }
		// else if(e2 instanceof CastExpression){
		// return sim(e1,(CastExpression)e2);
		// }
		// else if(e2 instanceof CharacterLiteral){
		// return sim(e1,(CharacterLiteral)e2);
		// }
		// else if(e2 instanceof ClassInstanceCreation){
		// return sim(e1,(ClassInstanceCreation)e2);
		// }
		// else if(e2 instanceof ConditionalExpression){
		// return sim(e1,(ConditionalExpression)e2);
		// }
		// else
		if (e2 instanceof FieldAccess) {
			return sim(e1, (FieldAccess) e2);
		}
		// else if(e2 instanceof InfixExpression){
		// return sim(e1,(InfixExpression)e2);
		// }
		// else if(e2 instanceof InstanceofExpression){
		// return sim(e1,(InstanceofExpression)e2);
		// }
		// else if(e2 instanceof MethodInvocation){
		// return sim(e1,(MethodInvocation)e2);
		// }
		// else if(e2 instanceof Name){
		// return sim(e1,(Name)e2);
		// }
		// else if(e2 instanceof NullLiteral){
		// return sim(e1,(NullLiteral)e2);
		// }
		// else if(e2 instanceof NumberLiteral){
		// return sim(e1,(NumberLiteral)e2);
		// }
		// else if(e2 instanceof ParenthesizedExpression){
		// return sim(e1,(ParenthesizedExpression)e2);
		// }
		// else if(e2 instanceof PostfixExpression){
		// return sim(e1,(PostfixExpression)e2);
		// }
		// else if(e2 instanceof PrefixExpression){
		// return sim(e1,(PrefixExpression)e2);
		// }
		// else if(e2 instanceof StringLiteral){
		// return sim(e1,(StringLiteral)e2);
		// }
		// else if(e2 instanceof SuperFieldAccess){
		// return sim(e1,(SuperFieldAccess)e2);
		// }
		// else if(e2 instanceof SuperMethodInvocation){
		// return sim(e1,(SuperMethodInvocation)e2);
		// }
		// else if(e2 instanceof ThisExpression){
		// return sim(e1,(ThisExpression)e2);
		// }
		// else if(e2 instanceof TypeLiteral){
		// return sim(e1,(TypeLiteral)e2);
		// }
		// else if(e2 instanceof VariableDeclarationExpression){
		// return sim(e1,(VariableDeclarationExpression)e2);
		// }
		else {
			return SimilarityConstant.sim(e1.getNodeType(), e2.getNodeType());
		}
	}

	public static double sim(FieldAccess e1, FieldAccess e2) {
		double objectNameSimilarity = U.stringSim(
				e1.getExpression().toString(), e2.getExpression().toString());
		double fieldNameSimilarity = U.stringSim(e1.getName().toString(), e2
				.getName().toString());
		return U.mean(objectNameSimilarity, fieldNameSimilarity);
	}
}
