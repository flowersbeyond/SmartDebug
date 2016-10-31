package cn.edu.thu.tsmart.tool.da.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import cn.edu.thu.tsmart.tool.da.core.suggestion.FilterableFix;
import cn.edu.thu.tsmart.tool.da.core.suggestion.FilterableSetFix;
import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;
import cn.edu.thu.tsmart.tool.da.core.suggestion.MethodFix;

public class Logger {

	public static final int EXPR_MODE = 0;
	public static final int DEBUG_MODE = 1;
	public static final int DATA_MODE = 2;

	public static final String INIT = "INIT";
	public static final String INIT_FINISHED = "INIT_FINISHED";
	public static final String FL_TOTAL = "FL_TOTAL_SUSPICIOUS_COUNT";
	public static final String BEGIN_SEARCH = "BEGIN_SEARCH";
	public static final String SEARCH_FIX = "SEARCH_FIX";
	public static final String END_SEARCH_FIX = "END_SEARCH_FIX";
	public static final String IGNORE_BB = "IGNORE_BB";
	
	public static final String GEN_EXPR_REPO_START = "GENERATE_EXPR_REPO_START";
	public static final String GEN_EXPR_REPO_FINISHED = "GENERATE_EXPR_REPO_FINISHED";
	
	public static final String GEN_METHOD_FIX = "GENERATE_METHOD_FIX";
	public static final String METHOD_FIX_DETAIL = "METHOD_FIX_DETAIL";
	
	public static final String CORRECT_TC_FILTER_START = "CORRECT_TC_FILTER_START";
	public static final String CORRECT_TC_FILTER_FINISHED = "CORRECT_TC_FILTER_FINISHED";
	public static final String FAIL_TC_FILTER_START = "FAIL_TC_FILTER_START";
	public static final String FAIL_TC_FILTER_FINISHED = "FAIL_TC_FILTER_FINISHED";
	public static final String FAIL_TC_FILTER_SPLIT_FINISHED = "FAIL_TC_FILTER_SPLIT_FINISHED";
	public static final String FILTER_COMPILATION_ERROR = "FILTER_COMPILATION_ERROR_COUNT";
	public static final String FILTER_SURVIVE = "FILTER_SURVIVE";
	public static final String FILTER_DEAD = "FILTER_DEAD";
	
	
	public static final String FIX_PLAUSIBLE = "FIX_PLAUSIBLE";
	public static final String FIX_SUCCESS = "FIX_SUCCESS";
	
	public static final String FIX_DONE = "FIX_DONE";
	
	
	
	
	private static String logRootDir = "data/";
	private String debugLogFile;
	private String dataLogFile;
	private String exprLogFile;
	
	private String projName;
	
	public static void setLogRootDir(String rootdir){
		logRootDir = rootdir;
		File dir = new File(logRootDir);
		if(!dir.exists())
			dir.mkdirs();
		
	}
	public Logger(String projName){
		this.projName = projName;
		this.debugLogFile = logRootDir + projName + "-debug.txt";
		this.dataLogFile = logRootDir + projName + "-data.txt";
		this.exprLogFile = logRootDir + projName + "-expr.txt";
		
	}
	
	public void startLog(){
		try {
			File dataFile = new File(dataLogFile);
			if(!dataFile.exists()){
				dataFile.createNewFile();
			}
			BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile));
			writer.write("");
			writer.close();
			
			File debugFile = new File(debugLogFile);
			if(!debugFile.exists())
				debugFile.createNewFile();			
			writer = new BufferedWriter(new FileWriter(debugFile));
			writer.write("");
			writer.close();
			
			File exprFile = new File(exprLogFile);
			if(!exprFile.exists())
				exprFile.createNewFile();			
			writer = new BufferedWriter(new FileWriter(exprFile));
			writer.write("");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void log(int mode, String cause, String info){
		try {
			Date date = new Date();
			DateFormat df = DateFormat.getTimeInstance();
			String dateString = df.format(date);
			BufferedWriter writer;
			
			if(mode == DEBUG_MODE)
				writer = new BufferedWriter(new FileWriter(new File(debugLogFile), true));
			else if (mode == DATA_MODE)
				writer = new BufferedWriter(new FileWriter(new File(dataLogFile), true));
			else
				writer = new BufferedWriter(new FileWriter(new File(exprLogFile), true));
			
			writer.write("[" + dateString + "]\t" + "[" + cause + "]:\t" + info + "\n");
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public static String generateMethodFixSummary(ArrayList<Fix> fixes){
		StringBuffer buf = new StringBuffer("");
		for(Fix fix: fixes){
			if(fix instanceof MethodFix){
				buf.append("||||" + ((MethodFix)fix).toString());
			}
		}
		return buf.toString();
	}
	
	public synchronized static String generateFilterableSummary(ArrayList<FilterableFix> fixes){
		int totalFixCount = fixes.size();
		int conditionExprCount = 0;
		int generalExprCount = 0;
		int ifArrayRangeCount = 0;
		int ifBreakCount = 0;
		int ifCastCount = 0;
		int ifContinueCount = 0;
		int ifListRangeCount = 0;
		int ifNullCount = 0;
		int ifReturnCount = 0;
		
		for(FilterableFix fix: fixes){
			String fixType = fix.getFixType();
			if(fixType.equals(Fix.COND_EXPR_CHANGE)){
				conditionExprCount ++;
			} else if(fixType.equals(Fix.EXPR_CHANGE)){
				generalExprCount ++;
			} else if(fixType.equals(Fix.IF_ARRAY_RANGE_CHECK)){
				ifArrayRangeCount ++;
			} else if(fixType.equals(Fix.IF_BREAK)){
				ifBreakCount ++;
			} else if (fixType.equals(Fix.IF_CAST_CHECK)){
				ifCastCount ++;
			} else if (fixType.equals(Fix.IF_CONTINUE)){
				ifContinueCount ++;
			} else if(fixType.equals(Fix.IF_LIST_RANGE_CHECK)){
				ifListRangeCount ++;
			} else if(fixType.equals(Fix.IF_NULL_CHECK)){
				ifNullCount ++;
			} else if(fixType.equals(Fix.IF_RETURN)){
				ifReturnCount ++;
			}		
			
		}
		
		String summary = totalFixCount + "\t"
				+ conditionExprCount + "\t"
				+ generalExprCount + "\t"
				+ ifArrayRangeCount + "\t"
				+ ifBreakCount + "\t"
				+ ifCastCount + "\t"
				+ ifContinueCount + "\t"
				+ ifListRangeCount + "\t"
				+ ifNullCount + "\t"
				+ ifReturnCount;
		return summary;
		
		
	}
	public static String generateFilterableSetSummary(
			ArrayList<FilterableSetFix> fixsets) {
		StringBuffer buf = new StringBuffer("");
		buf.append(fixsets.size() + "\t");
		for(FilterableSetFix fix: fixsets){
			buf.append(fix.size() + "\t");
		}
		return buf.toString();
	}
	public static String generateFilterableMapSummary(
			Map<String, List<FilterableFix>> valueFixMap) {
		StringBuffer buf = new StringBuffer("");
		for(String key: valueFixMap.keySet()){
			buf.append(key + ":" + valueFixMap.get(key).size() + "\t");
		}
		return buf.toString();
	}
	public static String generateFixStatistics(
			Map<Integer, ArrayList<FilterableFix>> fixes) {
		// TODO Auto-generated method stub
		int exprcount = 0;
		int boolcount = 0;
		
		for(Integer key: fixes.keySet()){
			ArrayList<FilterableFix> fs = fixes.get(key);
			for(FilterableFix fix: fs){
				//fix types
				if(fix.getFixType().equals(Fix.EXPR_CHANGE)){
					exprcount ++;
				}else {
					
					boolcount ++;
				}
			}
		}
		return "EXPR:" + exprcount + ";BOOL:" + boolcount;
	}
	public static String generateFixStatistics(ArrayList<Fix> preFilteredFixes) {
		// TODO Auto-generated method stub
		int exprcount = 0;
		int boolcount = 0;
		int exprSetCount = 0;
		int boolSetCount = 0;
		for(Fix fix: preFilteredFixes){
			if(fix instanceof FilterableSetFix){
				FilterableSetFix setFix = (FilterableSetFix)fix;
				List<? extends Fix> fixes = setFix.getFixes();
				for(Fix subfix: fixes){
					if(subfix.getFixType().equals(Fix.EXPR_CHANGE)){
						exprcount ++;
					} else {
						boolcount ++;
					}
				}
			} else if(fix.getFixType().equals(Fix.EXPR_CHANGE)){
				exprcount ++;
			} else {
				boolcount ++;
			}
			
			if(fix.getFixType().equals(Fix.EXPR_CHANGE)){
				exprSetCount ++;
			} else {
				boolSetCount ++;
			}
		}
		return "EXPR:" + exprcount + ";BOOL:" + boolcount + ";EXPRSET:" + exprSetCount + ";BOOLSETCOUNT:" + boolSetCount;
	}
}
