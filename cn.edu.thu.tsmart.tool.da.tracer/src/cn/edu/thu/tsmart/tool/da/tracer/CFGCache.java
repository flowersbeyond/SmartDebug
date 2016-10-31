package cn.edu.thu.tsmart.tool.da.tracer;

import java.util.HashMap;

import com.ibm.wala.ssa.SSACFG;

public class CFGCache {

	private static HashMap<String, SSACFG> cfgCache = new HashMap<String, SSACFG>();
	
	public static SSACFG get(String methodKey){
		if(cfgCache.containsKey(methodKey))
			return cfgCache.get(methodKey);
		return null;
	}
	
	public static void put(String methodKey, SSACFG cfg){
		cfgCache.put(methodKey, cfg);
	}
	
	public static void clearCache(){
		cfgCache.clear();
	}
}
