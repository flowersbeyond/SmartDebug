package cn.edu.thu.tsmart.tool.da.tracer;

import java.util.ArrayList;

public class TraceRecordContainer {
	
	public static ArrayList<String>traceMessageRecord = new ArrayList<String>();
	
	private static boolean flushable = false;
	public static void setFlushable(){
		flushable = true;
	}
	public static void appendTraceMessage(String traceMessage){
		traceMessageRecord.add(traceMessage);
	}
	
	public static ArrayList<String> flush(){
		int currentMessageSize = traceMessageRecord.size();
		while(true){
			try {
				Thread.sleep(1000L);
				if(traceMessageRecord.size() == currentMessageSize)
					break;
				else
					currentMessageSize = traceMessageRecord.size();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		ArrayList<String> currentRecord = traceMessageRecord;
		traceMessageRecord = new ArrayList<String>();
		return currentRecord;
	}

}
