package cn.edu.thu.tsmart.tool.da.expr4j.oracle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OracleManager {
	
	private static final String oracleFileName = "D:/Documents/Research/defects4j/experiments/oracle.txt";
	private static Map<String, Oracle> oracles = new HashMap<String, Oracle>();
	
	public static void readInOracle(){
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(oracleFileName)));
			String s = reader.readLine();
			while(s != null){
				String[] strs = s.split("\t");
				String versionName = strs[0];
				String qualifiedTestName = strs[1];
				String modifiedFileName = strs[2];
				modifiedFileName = modifiedFileName.replaceAll("/", ".");
				int lineNum = Integer.parseInt(strs[3]);
				String fixType = strs[4];
				Oracle oracle = new Oracle(versionName, qualifiedTestName, modifiedFileName, lineNum, fixType);
				oracles.put(versionName, oracle);
				s = reader.readLine();				
			}			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Oracle getOracle(String projName){
		return oracles.get(projName);
	}

}
