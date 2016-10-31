package cn.edu.thu.tsmart.tool.da.tracer;


public interface ITraceEventListener {
	
	public static final String DA_ANALYSIS = "da_analysis";
	public static final String INTRACE = "intrace";
	
	public static final String HIT_BREAKPOINT = "hit breakpoint";
	public static final String LAUNCH_TERMINATED = "launch terminated";
	
	public void handleEvent(String eventCause, String eventKind, DynamicTranslator dynamicTranslator);

}
