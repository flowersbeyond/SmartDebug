package cn.edu.thu.tsmart.tool.da.core.search.sc.calculator;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * 依据 valueTypeVector (见 similarityDesign.md, valueTypeVector.csv) 计算, 由 valueTypeVectorCalculator.js 生成代码
 * */
public class SimilarityConstant {
	
	public static double sim(int type1, int type2){
		if(type1 == ASTNode.ARRAY_ACCESS){
			if(type2 == ASTNode.ARRAY_ACCESS) return 1;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.25;
			if(type2 == ASTNode.ASSIGNMENT) return 1;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.25;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.875;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.5;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.25;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 1;
			if(type2 == ASTNode.FIELD_ACCESS) return 1;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.25;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.875;
			if(type2 == ASTNode.NULL_LITERAL) return 0.25;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.5;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 1;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.STRING_LITERAL) return 0.25;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 1;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.875;
		}
		if(type1 == ASTNode.ARRAY_CREATION){
			if(type2 == ASTNode.ARRAY_ACCESS) return 0.25;
			if(type2 == ASTNode.ARRAY_CREATION) return 1;
			if(type2 == ASTNode.ASSIGNMENT) return 0.25;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.75;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.375;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.5;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.75;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 0.25;
			if(type2 == ASTNode.FIELD_ACCESS) return 0.25;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.375;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.75;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.125;
			if(type2 == ASTNode.NULL_LITERAL) return 0.75;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.5;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 0.25;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.375;
			if(type2 == ASTNode.STRING_LITERAL) return 0.75;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.25;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.125;
		}
		if(type1 == ASTNode.ASSIGNMENT){
			if(type2 == ASTNode.ARRAY_ACCESS) return 1;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.25;
			if(type2 == ASTNode.ASSIGNMENT) return 1;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.25;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.875;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.5;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.25;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 1;
			if(type2 == ASTNode.FIELD_ACCESS) return 1;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.25;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.875;
			if(type2 == ASTNode.NULL_LITERAL) return 0.25;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.5;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 1;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.STRING_LITERAL) return 0.25;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 1;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.875;
		}
		if(type1 == ASTNode.BOOLEAN_LITERAL){
			if(type2 == ASTNode.ARRAY_ACCESS) return 0.25;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.75;
			if(type2 == ASTNode.ASSIGNMENT) return 0.25;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 1;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.375;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.5;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 1;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 0.25;
			if(type2 == ASTNode.FIELD_ACCESS) return 0.25;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 1;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.125;
			if(type2 == ASTNode.NULL_LITERAL) return 0.75;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.5;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 0.25;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.STRING_LITERAL) return 0.75;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.25;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.125;
		}
		if(type1 == ASTNode.CAST_EXPRESSION){
			if(type2 == ASTNode.ARRAY_ACCESS) return 0.875;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.375;
			if(type2 == ASTNode.ASSIGNMENT) return 0.875;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.375;
			if(type2 == ASTNode.CAST_EXPRESSION) return 1;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.625;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.375;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 0.875;
			if(type2 == ASTNode.FIELD_ACCESS) return 0.875;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.75;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.375;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.75;
			if(type2 == ASTNode.NULL_LITERAL) return 0.125;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.625;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 0.875;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.75;
			if(type2 == ASTNode.STRING_LITERAL) return 0.375;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.875;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.75;
		}
		if(type1 == ASTNode.CHARACTER_LITERAL){
			if(type2 == ASTNode.ARRAY_ACCESS) return 0.5;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.5;
			if(type2 == ASTNode.ASSIGNMENT) return 0.5;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.5;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.625;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 1;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.5;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 0.5;
			if(type2 == ASTNode.FIELD_ACCESS) return 0.5;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.5;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.375;
			if(type2 == ASTNode.NULL_LITERAL) return 0.5;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.75;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 0.5;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.75;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.STRING_LITERAL) return 0.75;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.5;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.375;
		}
		if(type1 == ASTNode.CLASS_INSTANCE_CREATION){
			if(type2 == ASTNode.ARRAY_ACCESS) return 0.25;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.75;
			if(type2 == ASTNode.ASSIGNMENT) return 0.25;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 1;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.375;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.5;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 1;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 0.25;
			if(type2 == ASTNode.FIELD_ACCESS) return 0.25;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 1;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.125;
			if(type2 == ASTNode.NULL_LITERAL) return 0.75;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.5;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 0.25;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.STRING_LITERAL) return 0.75;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.25;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.125;
		}
		if(type1 == ASTNode.CONDITIONAL_EXPRESSION){
			if(type2 == ASTNode.ARRAY_ACCESS) return 1;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.25;
			if(type2 == ASTNode.ASSIGNMENT) return 1;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.25;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.875;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.5;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.25;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 1;
			if(type2 == ASTNode.FIELD_ACCESS) return 1;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.25;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.875;
			if(type2 == ASTNode.NULL_LITERAL) return 0.25;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.5;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 1;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.STRING_LITERAL) return 0.25;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 1;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.875;
		}
		if(type1 == ASTNode.FIELD_ACCESS){
			if(type2 == ASTNode.ARRAY_ACCESS) return 1;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.25;
			if(type2 == ASTNode.ASSIGNMENT) return 1;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.25;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.875;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.5;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.25;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 1;
			if(type2 == ASTNode.FIELD_ACCESS) return 1;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.25;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.875;
			if(type2 == ASTNode.NULL_LITERAL) return 0.25;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.5;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 1;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.STRING_LITERAL) return 0.25;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 1;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.875;
		}
		if(type1 == ASTNode.INFIX_EXPRESSION){
			if(type2 == ASTNode.ARRAY_ACCESS) return 0.625;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.375;
			if(type2 == ASTNode.ASSIGNMENT) return 0.625;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.625;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.75;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.625;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.625;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 0.625;
			if(type2 == ASTNode.FIELD_ACCESS) return 0.625;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 1;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.625;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.5;
			if(type2 == ASTNode.NULL_LITERAL) return 0.375;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.625;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 0.625;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.75;
			if(type2 == ASTNode.STRING_LITERAL) return 0.625;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.625;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.5;
		}
		if(type1 == ASTNode.INSTANCEOF_EXPRESSION){
			if(type2 == ASTNode.ARRAY_ACCESS) return 0.25;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.75;
			if(type2 == ASTNode.ASSIGNMENT) return 0.25;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 1;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.375;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.5;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 1;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 0.25;
			if(type2 == ASTNode.FIELD_ACCESS) return 0.25;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 1;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.125;
			if(type2 == ASTNode.NULL_LITERAL) return 0.75;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.5;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 0.25;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.STRING_LITERAL) return 0.75;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.25;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.125;
		}
		if(type1 == ASTNode.METHOD_INVOCATION){
			if(type2 == ASTNode.ARRAY_ACCESS) return 0.875;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.125;
			if(type2 == ASTNode.ASSIGNMENT) return 0.875;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.125;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.75;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.375;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.125;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 0.875;
			if(type2 == ASTNode.FIELD_ACCESS) return 0.875;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.125;
			if(type2 == ASTNode.METHOD_INVOCATION) return 1;
			if(type2 == ASTNode.NULL_LITERAL) return 0.125;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.375;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 0.875;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.375;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.STRING_LITERAL) return 0.125;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.875;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 1;
		}
		if(type1 == ASTNode.NULL_LITERAL){
			if(type2 == ASTNode.ARRAY_ACCESS) return 0.25;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.75;
			if(type2 == ASTNode.ASSIGNMENT) return 0.25;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.75;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.125;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.5;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.75;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 0.25;
			if(type2 == ASTNode.FIELD_ACCESS) return 0.25;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.375;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.75;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.125;
			if(type2 == ASTNode.NULL_LITERAL) return 1;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.5;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 0.25;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.375;
			if(type2 == ASTNode.STRING_LITERAL) return 0.75;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.25;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.125;
		}
		if(type1 == ASTNode.NUMBER_LITERAL){
			if(type2 == ASTNode.ARRAY_ACCESS) return 0.5;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.5;
			if(type2 == ASTNode.ASSIGNMENT) return 0.5;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.5;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.625;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.75;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.5;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 0.5;
			if(type2 == ASTNode.FIELD_ACCESS) return 0.5;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.5;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.375;
			if(type2 == ASTNode.NULL_LITERAL) return 0.5;
			if(type2 == ASTNode.NUMBER_LITERAL) return 1;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 0.5;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 1;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.875;
			if(type2 == ASTNode.STRING_LITERAL) return 0.5;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.5;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.375;
		}
		if(type1 == ASTNode.PARENTHESIZED_EXPRESSION){
			if(type2 == ASTNode.ARRAY_ACCESS) return 1;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.25;
			if(type2 == ASTNode.ASSIGNMENT) return 1;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.25;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.875;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.5;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.25;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 1;
			if(type2 == ASTNode.FIELD_ACCESS) return 1;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.25;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.875;
			if(type2 == ASTNode.NULL_LITERAL) return 0.25;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.5;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 1;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.STRING_LITERAL) return 0.25;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 1;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.875;
		}
		if(type1 == ASTNode.POSTFIX_EXPRESSION){
			if(type2 == ASTNode.ARRAY_ACCESS) return 0.5;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.5;
			if(type2 == ASTNode.ASSIGNMENT) return 0.5;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.5;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.625;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.75;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.5;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 0.5;
			if(type2 == ASTNode.FIELD_ACCESS) return 0.5;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.5;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.375;
			if(type2 == ASTNode.NULL_LITERAL) return 0.5;
			if(type2 == ASTNode.NUMBER_LITERAL) return 1;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 0.5;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 1;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.875;
			if(type2 == ASTNode.STRING_LITERAL) return 0.5;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.5;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.375;
		}
		if(type1 == ASTNode.PREFIX_EXPRESSION){
			if(type2 == ASTNode.ARRAY_ACCESS) return 0.625;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.375;
			if(type2 == ASTNode.ASSIGNMENT) return 0.625;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.625;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.75;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.625;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.625;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 0.625;
			if(type2 == ASTNode.FIELD_ACCESS) return 0.625;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.75;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.625;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.5;
			if(type2 == ASTNode.NULL_LITERAL) return 0.375;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.875;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 0.625;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.875;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 1;
			if(type2 == ASTNode.STRING_LITERAL) return 0.375;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.625;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.5;
		}
		if(type1 == ASTNode.STRING_LITERAL){
			if(type2 == ASTNode.ARRAY_ACCESS) return 0.25;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.75;
			if(type2 == ASTNode.ASSIGNMENT) return 0.25;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.75;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.375;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.75;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.75;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 0.25;
			if(type2 == ASTNode.FIELD_ACCESS) return 0.25;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.75;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.125;
			if(type2 == ASTNode.NULL_LITERAL) return 0.75;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.5;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 0.25;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.375;
			if(type2 == ASTNode.STRING_LITERAL) return 1;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.25;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.125;
		}
		if(type1 == ASTNode.SUPER_FIELD_ACCESS){
			if(type2 == ASTNode.ARRAY_ACCESS) return 1;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.25;
			if(type2 == ASTNode.ASSIGNMENT) return 1;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.25;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.875;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.5;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.25;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 1;
			if(type2 == ASTNode.FIELD_ACCESS) return 1;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.25;
			if(type2 == ASTNode.METHOD_INVOCATION) return 0.875;
			if(type2 == ASTNode.NULL_LITERAL) return 0.25;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.5;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 1;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.625;
			if(type2 == ASTNode.STRING_LITERAL) return 0.25;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 1;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 0.875;
		}
		if(type1 == ASTNode.SUPER_METHOD_INVOCATION){
			if(type2 == ASTNode.ARRAY_ACCESS) return 0.875;
			if(type2 == ASTNode.ARRAY_CREATION) return 0.125;
			if(type2 == ASTNode.ASSIGNMENT) return 0.875;
			if(type2 == ASTNode.BOOLEAN_LITERAL) return 0.125;
			if(type2 == ASTNode.CAST_EXPRESSION) return 0.75;
			if(type2 == ASTNode.CHARACTER_LITERAL) return 0.375;
			if(type2 == ASTNode.CLASS_INSTANCE_CREATION) return 0.125;
			if(type2 == ASTNode.CONDITIONAL_EXPRESSION) return 0.875;
			if(type2 == ASTNode.FIELD_ACCESS) return 0.875;
			if(type2 == ASTNode.INFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.INSTANCEOF_EXPRESSION) return 0.125;
			if(type2 == ASTNode.METHOD_INVOCATION) return 1;
			if(type2 == ASTNode.NULL_LITERAL) return 0.125;
			if(type2 == ASTNode.NUMBER_LITERAL) return 0.375;
			if(type2 == ASTNode.PARENTHESIZED_EXPRESSION) return 0.875;
			if(type2 == ASTNode.POSTFIX_EXPRESSION) return 0.375;
			if(type2 == ASTNode.PREFIX_EXPRESSION) return 0.5;
			if(type2 == ASTNode.STRING_LITERAL) return 0.125;
			if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.875;
			if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 1;
		}
		return 0;
	}
	
}
