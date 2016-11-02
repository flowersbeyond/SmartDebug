package cn.edu.thu.tsmart.tool.da.core.validator.cp;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;

import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;


public class Checkpoint {
	
	private static String JAVA_LINE_CHECKPOINT = "cn.thu.edu.thss.tsmart.tool.da.validator.checkpointMarker";
	public static String TEST_CASE_PROPERTY = "cn.thu.edu.thss.tsmart.tool.da.validator.testcaseProperty";
	public static String CONDITIONS = "cn.thu.edu.thss.tsmart.tool.da.validator.conditions";
	public static String CONDITION_SATISFIED = "cn.edu.thu.tsmart.tool.da.validator.conditionSatisfied";
	public static String TYPE_NAME = "cn.thu.edu.thss.tsmart.tool.da.validator.typeName";
	
	private TestCase ownerTC;
	private ArrayList<ConditionItem> conditions;
	
	public static final String HIT_ALWAYS = "always";
	public static final String EXPECTATION_PASSED = "true";
	
	

	private IFile file;
	private IMarker marker;
	private String typeName;
	private int lineNumber;
	private StatusCode checkpointStatus = StatusCode.UNKNOWN;
	
	public void setOwnerTestCase(TestCase tc){
		this.ownerTC = tc;
	}
	public TestCase getOwnerTestCase(){
		return ownerTC;
	}
	
	public void setConditionSatisfied(boolean conditionSatisfied){
		try {
			this.marker.setAttribute(Checkpoint.CONDITION_SATISFIED, conditionSatisfied);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public Checkpoint(){
		
	}

	public Checkpoint(TestCase testcase, IFile resource, String typeName, int lineNumber) throws DebugException {
		
		this.conditions = new ArrayList<ConditionItem>();
		this.ownerTC = testcase;
		this.file = (IFile)resource;
		this.lineNumber = lineNumber;
		this.typeName = typeName;
		try {
			IMarker marker = file.createMarker(JAVA_LINE_CHECKPOINT);
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
			marker.setAttribute(Checkpoint.TEST_CASE_PROPERTY, getTestCaseString());
			marker.setAttribute(TYPE_NAME, typeName);
			this.marker = marker;
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	public ArrayList<ConditionItem> getConditions() {
		return conditions;
	}
	public void setConditions(ArrayList<ConditionItem> conditions) {
		this.conditions = conditions;
		try {
			this.marker.setAttribute(Checkpoint.CONDITIONS, getConditionString());
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
	}
	private String getConditionString(){
		String conditionString = "";
		for(ConditionItem item: this.conditions){
			if(!item.getHitCondition().equals("")){
				conditionString += item.getHitCondition() + ";" + item.getExpectation() + ";";
			}
		}
		return conditionString;
	}
	private String getTestCaseString(){
		String testCaseString = ownerTC.getClassName() + ";" + ownerTC.getMethodName();
		return testCaseString;
	}

	public IFile getFile(){
		return this.file;
	}
	
	public void ensureCPMarker(){
		if(this.marker == null || !this.marker.exists()){
			try {
				IMarker marker = file.createMarker(JAVA_LINE_CHECKPOINT);
				marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
				marker.setAttribute(Checkpoint.TEST_CASE_PROPERTY, getTestCaseString());
				marker.setAttribute(TYPE_NAME, typeName);
				this.marker = marker;
			} catch (CoreException e) {
				e.printStackTrace();
			}
			
		} 
	}
	public int getLineNumber() {
		return this.lineNumber;
	}
	public String getTypeName() {
		return typeName;
	}
	public StatusCode getStatus() {
		return this.checkpointStatus;
	}
	public IMarker getMarker() {
		return this.marker;
	}
	public void setStatus(StatusCode status) {
		this.checkpointStatus = status;		
	}
}
