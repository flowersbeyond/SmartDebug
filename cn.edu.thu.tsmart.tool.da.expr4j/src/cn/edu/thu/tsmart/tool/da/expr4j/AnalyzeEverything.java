package cn.edu.thu.tsmart.tool.da.expr4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class AnalyzeEverything {
	
	static final String datadir = "D:/Documents/Research/defects4j/experiments/data/";
	static BufferedWriter writer;
	public static void main(String args[]) throws IOException{
		writer = new BufferedWriter(new FileWriter(new File(datadir + "SUM.txt")));
		File dir = new File(datadir);
		File[] files = dir.listFiles();
		for(int i = 0; i < files.length; i ++){
			File file = files[i];
			if(!file.isDirectory()){
				String fileName = file.getName();
				String[] parts = fileName.split("\\.");
				if(parts[0].endsWith("expr")){
					String versionName = parts[0].substring(0, parts[0].lastIndexOf('-'));
					int[] data = analyzeData(file);
					writeData(versionName, data);
				}
			}
		}
		writer.close();
	}
	
	static int[] analyzeData(File file) throws IOException{
		int[] data = new int[10];
		//0:expr count, 1: bool count, 2: compilation error, 3: expr filter, 4: bool filter, 5: exprSet, 6: boolSet, 
		//7: methodCount, 8: final fix count, 9: final validate count
		BufferedReader reader = new BufferedReader(new FileReader(file));
		boolean shouldstop = false;
		while(true){
			
			ArrayList<String> group = new ArrayList<String>();
			while(true){
				String s = reader.readLine();
				if(s == null){
					shouldstop = true;
					break;
				}
				group.add(s);
				if(s.contains("END_SEARCH_FIX")){
					break;
				}
			}
			if(shouldstop)
				break;
			int[] groupdata = analyzeData(group);
			for(int i = 0; i < data.length; i ++){
				data[i] += groupdata[i];
			}
		
		}
		reader.close();
		return data;
		
	}
	
	static int[] analyzeData(ArrayList<String> group){
		//0:expr count, 1: bool count, 2: compilation error, 3: expr filter, 4: bool filter, 5: exprSet, 6: boolSet, 
		//7: methodCount, 8: final fix count, 9: final validate count
		int[] groupData = new int[10];
		for(String log: group){
			if(log.contains("GENERATE_METHOD_FIX")){
				String numStr = log.substring(log.lastIndexOf('\t') + 1);
				groupData[7] = Integer.parseInt(numStr);
			}
			if(log.contains("COMPILATION_ERROR")){
				String numStr = log.substring(log.lastIndexOf('\t') + 1);
				int count = Integer.parseInt(numStr);
				groupData[2] = Math.max(count, groupData[2]);
			}
			
			if(log.contains("PRE_FILTER_STATUS")){
				String info = log.substring(log.lastIndexOf('\t') + 1);
				String[] parts = info.split(";");
				int exprCount = Integer.parseInt(parts[0].substring(parts[0].lastIndexOf(':') + 1));
				int boolCount = Integer.parseInt(parts[1].substring(parts[1].lastIndexOf(':') + 1));
				groupData[0] = exprCount;
				groupData[1] = boolCount;
			}
			if(log.contains("POST_FILTER_STATUS")){
				String info = log.substring(log.lastIndexOf('\t') + 1);
				String[] parts = info.split(";");
				int exprCount = Integer.parseInt(parts[0].substring(parts[0].lastIndexOf(':') + 1));
				int boolCount = Integer.parseInt(parts[1].substring(parts[1].lastIndexOf(':') + 1));
				int exprSetCount = Integer.parseInt(parts[2].substring(parts[2].lastIndexOf(':') + 1));
				int boolSetCount = Integer.parseInt(parts[3].substring(parts[3].lastIndexOf(':') + 1));
				groupData[3] = exprCount;
				groupData[4] = boolCount;
				groupData[5] = exprSetCount;
				groupData[6] = boolSetCount;
			}
			
		}
		
		groupData[8] = groupData[7] + groupData[3] + groupData[4];
		groupData[9] = groupData[7] + groupData[5] + groupData[6];
		
		return groupData;
	}
	
	static void writeData(String versionName, int[] data) throws IOException{
		StringBuffer buf = new StringBuffer(versionName);
		for(int i = 0; i < data.length; i ++){
			buf.append("\t" + data[i]);
		}
		
		writer.write(buf.toString());
		writer.newLine();
	}
	
	

}
