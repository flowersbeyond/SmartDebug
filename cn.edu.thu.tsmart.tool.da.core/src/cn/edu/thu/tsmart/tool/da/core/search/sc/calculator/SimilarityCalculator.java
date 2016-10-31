package cn.edu.thu.tsmart.tool.da.core.search.sc.calculator;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.StringLiteral;


public class SimilarityCalculator {

    /**
     * @param
     * e1 要换掉的表达式
     * e2 要换成的表达式
     * @return [0,1], 越高越可能换. == 0 表示作者认为不可能这样换...
     */
    public static double calculateSimilarity(Expression e1, Expression e2) {
      return sim(e1,e2);
    }

    /**
     * 内部使用... 一个调度器
     * */
    static double sim(Expression e1, Expression e2){
        if (e1 instanceof ArrayAccess) {
            return ArrayAccessCalculator.sim((ArrayAccess) e1, e2);
        } else if (e1 instanceof ArrayCreation) {
            return ArrayCreationCalculator.sim((ArrayCreation) e1, e2);
        } else if (e1 instanceof ArrayInitializer) {
            return ArrayInitializerCalculator.sim((ArrayInitializer) e1, e2);
        } else if (e1 instanceof Assignment) {
            return AssignmentCalculator.sim((Assignment) e1, e2);
        } else if (e1 instanceof BooleanLiteral) {
            return BooleanLiteralCalculator.sim((BooleanLiteral) e1, e2);
        } else if (e1 instanceof CastExpression) {
            return CastExpressionCalculator.sim((CastExpression) e1, e2);
        } else if (e1 instanceof CharacterLiteral) {
            return CharacterLiteralCalculator.sim((CharacterLiteral) e1, e2);
        } else if (e1 instanceof ClassInstanceCreation) {
            return ClassInstanceCreationCalculator.sim((ClassInstanceCreation) e1, e2);
        } else if (e1 instanceof ConditionalExpression) {
            return ConditionalExpressionCalculator.sim((ConditionalExpression) e1, e2);
        } else if (e1 instanceof FieldAccess) {
            return FieldAccessCalculator.sim((FieldAccess) e1, e2);
        } else if (e1 instanceof InfixExpression) {
            return InfixExpressionCalculator.sim((InfixExpression) e1, e2);
        } else if (e1 instanceof InstanceofExpression) {
            return InstanceofExpressionCalculator.sim((InstanceofExpression) e1, e2);
        } else if (e1 instanceof MethodInvocation) {
            return MethodInvocationCalculator.sim((MethodInvocation) e1, e2);
        } else if (e1 instanceof Name) {
            return NameCalculator.sim((Name) e1, e2);
        } else if (e1 instanceof NullLiteral) {
            return NullLiteralCalculator.sim((NullLiteral) e1, e2);
        } else if (e1 instanceof NumberLiteral) {
            return NumberLiteralCalculator.sim((NumberLiteral) e1, e2);
        } else if (e1 instanceof ParenthesizedExpression) {
            return ParenthesizedExpressionCalculator.sim((ParenthesizedExpression) e1, e2);
        } else if (e1 instanceof PostfixExpression) {
            return PostfixExpressionCalculator.sim((PostfixExpression) e1, e2);
        } else if (e1 instanceof PrefixExpression) {
            return PrefixExpressionCalculator.sim((PrefixExpression) e1, e2);
        } else if (e1 instanceof StringLiteral) {
            return StringLiteralCalculator.sim((StringLiteral) e1, e2);
        } else if (e1 instanceof SuperFieldAccess) {
            return SuperFieldAccessCalculator.sim((SuperFieldAccess) e1, e2);
        } else if (e1 instanceof SuperMethodInvocation) {
            return SuperMethodInvocationCalculator.sim((SuperMethodInvocation) e1, e2);
		} else if (e1 instanceof ThisExpression) {
            return ThisExpressionCalculator.sim((ThisExpression) e1, e2);
        } else if (e1 instanceof TypeLiteral) {
            return TypeLiteralCalculator.sim((TypeLiteral) e1, e2);
        } else if (e1 instanceof VariableDeclarationExpression) {
            return VariableDeclarationExpressionCalculator.sim((VariableDeclarationExpression) e1, e2);
        } else {
            return -1; // 完备后不应出现. 抛个异常?
        }
    }


}
