package cn.edu.thu.tsmart.tool.da.core.search.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class ExpressionRepository {
	
	Map<String, Set<String>> exprRepoMap = new HashMap<String, Set<String>>();
	public ExpressionRepository(Map<String, Set<String>> exprRepoMap){
		this.exprRepoMap = exprRepoMap;
	}
	
	public Map<String, Set<String>> getExprRepoMap(){
		return exprRepoMap;
	}

	public int getRepoSize() {
		int size = 0;
		if(exprRepoMap != null){
			for(String key: exprRepoMap.keySet()){
				size += exprRepoMap.get(key).size();
			}
		}
		return size;
	}

	public int countExprs() {
		int count = 0;
		for(String key: exprRepoMap.keySet()){
			Set<String> exprs = exprRepoMap.get(key);
			if(exprs != null){
				count += exprs.size();
			}
		}
		return count;
	}
	

}
