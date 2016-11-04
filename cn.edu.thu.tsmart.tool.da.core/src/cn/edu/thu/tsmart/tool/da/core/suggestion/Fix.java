package cn.edu.thu.tsmart.tool.da.core.suggestion;

import org.eclipse.core.resources.IFile;

public abstract class Fix {
	
	public abstract void doFix();
	public abstract void undoFix();
	
	public abstract IFile[] getModifiedFiles();
	
	//fix types
	public static final String EXPR_CHANGE = "expr_change";
	public static final String COND_EXPR_CHANGE = "cond_expr_change";
	public static final String IF_CONTINUE = "if_continue";
	public static final String IF_BREAK = "if_break";
	public static final String IF_RETURN = "if_return";
	public static final String IF_NULL_CHECK = "if_null_check";
	public static final String IF_ARRAY_RANGE_CHECK = "if_array_range_check";
	public static final String IF_LIST_RANGE_CHECK = "if_list_range_check";
	public static final String IF_CAST_CHECK = "if_cast_check";
	public static final String METHOD_GENERAL_OVERLOAD = "method_general_overload";
	public static final String METHOD_CNSTR_OVERLOAD = "method_constructor_overload";
	public static final String METHOD_SAME_SIG = "method_same_signature";
	
	public abstract String getFixType();
	public abstract int getFixLineNum();
	public abstract String getFileName();
	
	private int score;
	public void setScore(int score){
		this.score = score;
	}
	
	public int getScore(){
		return score;
	}
	
}
