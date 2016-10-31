package cn.edu.thu.tsmart.tool.da.core.validator.cp;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import java.util.Map;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.internal.debug.core.breakpoints.JavaLineBreakpoint;

import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;


public class CheckpointOnNode extends JavaLineBreakpoint{
	
	private static String JAVA_LINE_CHECKPOINT = "cn.thu.edu.thss.tsmart.tool.da.validator.checkpointMarker";
	public static String TEST_CASE_PROPERTY = "cn.thu.edu.thss.tsmart.tool.da.validator.testcaseProperty";
	public static String CONDITIONS = "cn.thu.edu.thss.tsmart.tool.da.validator.conditions";
	public static String CONDITION_SATISFIED = "cn.edu.thu.tsmart.tool.da.validator.conditionSatisfied";
	
	public static int HIT_ALWAYS = 0;
	public static int HIT_NOT_SET = -1;
	
	private static String DEFAULT_CONDITION_STRING="true";
	
	private TestCase ownerTC;
//	private ArrayList<ConditionItem> conditions;
	private String conditionString;
	/**
	 * 	 是个实然. 目前通过call hierarchy看到它与CP对话框和CPview有关(以后都没了.) 
	 (感悟: 一个属性默认应该为 "实然" 属性; "应然" 属性应当用变量名或 final 修饰符 来表示一下... 
	 */
	private boolean conditionSatisfied;
	/**
	 本checkpoint存一个IMarker对象里;<br>
	 我们告诉Eclipse该IMarker和某IFile(事实上就是所分析的Java文件!)关联;<br>
	 Eclipse自动把IMarker内容 以及它与IFile的关联关系 serialize起来; <br>
	 我们在插件启动时用 ResourcesPlugin.getWorkspace().getRoot().findMarkers 来 deserialize.<br>
	 <br>
	 我们确实也将用这个IMarker来在代码行上提示Checkpoint的存在.
	 */
	private IFile file; 
	private String typeName;
	private int lineNumber;
		
	public void setOwnerTestCase(TestCase tc){
		this.ownerTC = tc;
		try {
			this.getMarker().setAttribute(CheckpointOnNode.TEST_CASE_PROPERTY, getTestCaseString());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	public TestCase getOwnerTestCase(){
		return ownerTC;
	}
	
	public void setConditionSatisfied(boolean conditionSatisfied){
		try {
			this.getMarker().setAttribute(CheckpointOnNode.CONDITION_SATISFIED, conditionSatisfied);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean getConsitionSatisfied(){
		return this.conditionSatisfied;
	}
	
	public CheckpointOnNode(IFile file){
		this.conditionString=DEFAULT_CONDITION_STRING;
		this.file = file;
	}
	
	@SuppressWarnings("restriction")
	public CheckpointOnNode(boolean satisfied, IResource resource, String typeName,
			int lineNumber, int charStart, int charEnd, int hitCount,
			boolean add, Map<String, Object> attributes) throws DebugException {
		super(resource, typeName, lineNumber, charStart, charEnd, hitCount, add,
				attributes, JAVA_LINE_CHECKPOINT);
//		this.conditions = new ArrayList<ConditionItem>();
		this.conditionString=DEFAULT_CONDITION_STRING;
		this.file = (IFile)resource;
		this.conditionSatisfied = satisfied;
		this.lineNumber = lineNumber;
		this.typeName = typeName;
	}
//	public ArrayList<ConditionItem> getConditions() {
//		return conditions;
//	}
//	public void setConditions(ArrayList<ConditionItem> conditions) {
//		this.conditions = conditions;
//		if(conditions.size() >= 1){
//			ConditionItem item = conditions.get(0);
//			if(item.getConditionExpr().equals("true"))
//				this.conditionSatisfied = true;
//		}
//		try {
//			this.getMarker().setAttribute(CheckpointOnNode.CONDITIONS, getConditionString());
//		} catch (CoreException e) {
//			e.printStackTrace();
//		}
//		
//	}
	public String getConditionString(){
//		String conditionString = "";
//		for(ConditionItem item: this.conditions){
//			if(item.getHitCount() != -1){
//				conditionString += item.getHitCount() + ";" + item.getConditionExpr() + ";";
//			}
//		}
		return conditionString;
	}
	
	
	public void setConditionString(String conditionString) {
		this.conditionString = conditionString;
	}
	private String getTestCaseString(){
		String testCaseString = ownerTC.getClassName() + ";" + ownerTC.getMethodName();
		return testCaseString;
	}

	/**
	 * 
	 * @return The IFile at which this checkpoint is planned to store.  
	 */
	public IFile getFile(){
		return this.file;
	}
	
	public void ensureCPMarker(){
		if(this.getMarker() == null || !this.getMarker().exists()){
			try {
				IMarker marker = file.createMarker(JAVA_LINE_CHECKPOINT);
				marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
				marker.setAttribute(CheckpointOnNode.TEST_CASE_PROPERTY, getTestCaseString());
				marker.setAttribute(TYPE_NAME, typeName);
				this.setMarker(marker);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} 
	}
}
