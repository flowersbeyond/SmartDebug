package cn.edu.thu.tsmart.tool.da.core.search.sc.calculator;

import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;


/**
 * Created by SnowOnion
 * on 2015-12-11 18:00.
 * To change this template, use Preferences | Editor | File and Code Templates | Includes | File Header.
 */
public class ParenthesizedExpressionCalculator {

    static double sim(ParenthesizedExpression e1, Expression e2){
        if(e2 instanceof ArrayInitializer){
            return 0;
        }
        else{ // 剥掉括号
            return SimilarityCalculator.sim(e1.getExpression(),e2);
        }
    }
}
