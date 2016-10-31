package cn.edu.thu.tsmart.tool.da.core.search.strategy;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import cn.edu.thu.tsmart.tool.da.core.suggestion.FilterableFix;

public class FixerUtil {

	/**
	 * merge the pairs in newResults into results, and return results
	 * @param results
	 * @param newResults
	 * @return
	 */
	public static void merge(Map<Integer, ArrayList<FilterableFix>> results, Map<Integer, ArrayList<FilterableFix>> newResults){
		for(Integer lineNum: newResults.keySet()){
			if(results.containsKey(lineNum)){
				ArrayList<FilterableFix> fixes = results.get(lineNum);
				fixes.addAll(newResults.get(lineNum));
			} else {
				results.put(lineNum, newResults.get(lineNum));
			}
		}
		
	}
	
	public static void addFix(Map<Integer, ArrayList<FilterableFix>> results, FilterableFix fix, int lineNum){
		if(!results.containsKey(lineNum)){
			ArrayList<FilterableFix> fixes = new ArrayList<FilterableFix>();
			results.put(lineNum, fixes);
		}
		ArrayList<FilterableFix> fixes = results.get(lineNum);
		fixes.add(fix);
	}
	
	public static int getLineNum(ASTNode e){
		CompilationUnit cu = (CompilationUnit)e.getRoot();
		return cu.getLineNumber(e.getStartPosition());
	}
}
