package cn.edu.thu.tsmart.tool.da.core.validator.cp;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.internal.debug.core.breakpoints.JavaLineBreakpoint;

import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;


public class Checkpoint extends JavaLineBreakpoint{
	
	private static String JAVA_LINE_CHECKPOINT = "cn.thu.edu.thss.tsmart.tool.da.validator.checkpointMarker";
	public static String TEST_CASE_PROPERTY = "cn.thu.edu.thss.tsmart.tool.da.validator.testcaseProperty";
	public static String CONDITIONS = "cn.thu.edu.thss.tsmart.tool.da.validator.conditions";
	public static String CONDITION_SATISFIED = "cn.edu.thu.tsmart.tool.da.validator.conditionSatisfied";
	
	
	private TestCase ownerTC;
	private ArrayList<ConditionItem> conditions;
	
	public static final String HIT_ALWAYS = "always";

	private boolean conditionSatisfied;
	
	private IFile file; 
	private String typeName;
	private int lineNumber;
		
	public void setOwnerTestCase(TestCase tc){
		this.ownerTC = tc;
		try {
			this.getMarker().setAttribute(Checkpoint.TEST_CASE_PROPERTY, getTestCaseString());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	public TestCase getOwnerTestCase(){
		return ownerTC;
	}
	
	public void setConditionSatisfied(boolean conditionSatisfied){
		try {
			this.getMarker().setAttribute(Checkpoint.CONDITION_SATISFIED, conditionSatisfied);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	public boolean getConsitionSatisfied(){
		return this.conditionSatisfied;
	}
	
	public Checkpoint(){
		
	}
	
	@SuppressWarnings("restriction")
	public Checkpoint(boolean satisfied, IResource resource, String typeName,
			int lineNumber, int charStart, int charEnd, int hitCount,
			boolean add, Map<String, Object> attributes) throws DebugException {
		super(resource, typeName, lineNumber, charStart, charEnd, hitCount, add,
				attributes, JAVA_LINE_CHECKPOINT);
		this.conditions = new ArrayList<ConditionItem>();
		this.file = (IFile)resource;
		this.conditionSatisfied = satisfied;
		this.lineNumber = lineNumber;
		this.typeName = typeName;
	}
	public ArrayList<ConditionItem> getConditions() {
		return conditions;
	}
	public void setConditions(ArrayList<ConditionItem> conditions) {
		this.conditions = conditions;
		if(conditions.size() >= 1){
			ConditionItem item = conditions.get(0);
			if(item.getConditionExpr().equals("true"))
				this.conditionSatisfied = true;
		}
		try {
			this.getMarker().setAttribute(Checkpoint.CONDITIONS, getConditionString());
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
	}
	private String getConditionString(){
		String conditionString = "";
		for(ConditionItem item: this.conditions){
			if(!item.getHitCount().equals("")){
				conditionString += item.getHitCount() + ";" + item.getConditionExpr() + ";";
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
		if(this.getMarker() == null || !this.getMarker().exists()){
			try {
				IMarker marker = file.createMarker(JAVA_LINE_CHECKPOINT);
				marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
				marker.setAttribute(Checkpoint.TEST_CASE_PROPERTY, getTestCaseString());
				marker.setAttribute(TYPE_NAME, typeName);
				this.setMarker(marker);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} 
	}
}
