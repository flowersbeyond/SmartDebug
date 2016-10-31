package cn.edu.thu.tsmart.tool.da.core.search.sc.calculator;

import org.eclipse.jdt.core.dom.*;

import cn.edu.thu.tsmart.tool.da.core.search.sc.U;

/**
 * Created by SnowOnion
 * on 2015-12-11 17:57.
 * To change this template, use Preferences | Editor | File and Code Templates | Includes | File Header.
 */
public class StringLiteralCalculator {

    public static double sim(StringLiteral e1, Expression e2){
        if(e2 instanceof StringLiteral){
            return sim(e1,(StringLiteral)e2);
        }
        else{
        	return SimilarityConstant.sim(e1.getNodeType(), e2.getNodeType());
        }
    }
    
    

    public static double sim(StringLiteral e1,StringLiteral e2){
        return U.stringSim(e1.getLiteralValue(),e2.getLiteralValue());
    }

}
