package cn.edu.thu.tsmart.tool.da.core.search.sc;

import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

public class U {

	public static class K {
		public static final double StringLiteral_NullLiteral = 0.5;
	}

	/**
	 * @return 算术平均值
	 */
	public static double mean(double... args) {
		if (args.length == 0) {
			System.err.println("U.mean(double... args) 传入 0 个参数, 这不正常, 请检查");
			return 0;
		} // 不返回0掩盖错误, 让异常发生好了... 算了TODO
		double sum = 0;
		for (double d : args) {
			sum += d;
		}
		return sum / args.length;
	}

	/**
	 * 字符串相似度 第1版本: 若编辑距离==0, 则1; 否则为编辑距离的倒数
	 */
	public static double stringSim(String s1, String s2) {
		int levenshteinDistance = computeLevenshteinDistance(s1, s2);
		if (levenshteinDistance == 0) {
			return 1.0;
		} else {
			return 2 - (double) levenshteinDistance / ((double) s1.length() + 1);
		}
	}

	/**
	 * via https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings
	 * /Levenshtein_distance#Java
	 * 
	 * @return 字符串编辑距离, 值域 [0, max(lhs.length, rhs.length)]
	 */
	public static int computeLevenshteinDistance(CharSequence lhs, CharSequence rhs) {
		int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];

		for (int i = 0; i <= lhs.length(); i++)
			distance[i][0] = i;
		for (int j = 1; j <= rhs.length(); j++)
			distance[0][j] = j;

		for (int i = 1; i <= lhs.length(); i++)
			for (int j = 1; j <= rhs.length(); j++)
				distance[i][j] = minimum(distance[i - 1][j] + 1, distance[i][j - 1] + 1,
						distance[i - 1][j - 1] + ((lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1));

		return distance[lhs.length()][rhs.length()];
	}

	public static int minimum(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}

	/**
	 * @return 两个整数的相似度
	 */
	public static double intSim(int i1, int i2) {
		int diff = i1 - i2;
		if (Math.abs(diff) < 1) {
			return 1.0;
		} else {
			return 1.0 / Math.abs(diff);
		}
	}

	/**
	 * @return 两个浮点数的相似度
	 */
	public static double doubleSim(double d1, double d2) {
		double diff = d1 - d2;
		if (Math.abs(diff) < 1) {
			return 1.0;
		} else {
			return 1.0 / Math.abs(diff);
		}
	}

	/**
	 * 夫中缀算符, 有的能返回数, 有的能返回布尔, & | ^ 都能.
	 * 
	 * @return true iff op 能返回布尔.
	 */
	public static boolean canInfixOpReturnBoolean(InfixExpression.Operator op) {
		return (op.equals(InfixExpression.Operator.AND) || op.equals(InfixExpression.Operator.CONDITIONAL_AND)
				|| op.equals(InfixExpression.Operator.CONDITIONAL_OR) || op.equals(InfixExpression.Operator.EQUALS)
				|| op.equals(InfixExpression.Operator.GREATER) || op.equals(InfixExpression.Operator.GREATER_EQUALS)
				|| op.equals(InfixExpression.Operator.LESS) || op.equals(InfixExpression.Operator.LESS_EQUALS)
				|| op.equals(InfixExpression.Operator.NOT_EQUALS) || op.equals(InfixExpression.Operator.OR)
				|| op.equals(InfixExpression.Operator.XOR));
	}

	/**
	 * 夫中缀算符, 有的能返回数, 有的能返回布尔, & | ^ 都能.
	 * 
	 * @return true iff op 能返回数.
	 */
	public static boolean canInfixOpReturnNumber(InfixExpression.Operator op) {
		return (op.equals(InfixExpression.Operator.AND) || op.equals(InfixExpression.Operator.DIVIDE)
				|| op.equals(InfixExpression.Operator.LEFT_SHIFT) || op.equals(InfixExpression.Operator.MINUS)
				|| op.equals(InfixExpression.Operator.OR) || op.equals(InfixExpression.Operator.PLUS)
				|| op.equals(InfixExpression.Operator.REMAINDER)
				|| op.equals(InfixExpression.Operator.RIGHT_SHIFT_SIGNED)
				|| op.equals(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED) || op.equals(InfixExpression.Operator.TIMES)
				|| op.equals(InfixExpression.Operator.XOR));
	}

	/**
	 * 夫前缀算符, ! 只能返回布尔, 其他只能返回数 (~ 是对number进行的按位取反).
	 * 
	 * @return true iff op 能返回布尔.
	 */
	public static boolean canPrefixOpReturnBoolean(PrefixExpression.Operator op) {
		return op.equals(PrefixExpression.Operator.NOT);
	}
}
