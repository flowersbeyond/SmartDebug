package cn.edu.thu.tsmart.tool.da.core.search.strategy.npe.fixer;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.EclipseUtils;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.npe.fs.NPEFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fixer.Fixer;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSite;
import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;
import cn.edu.thu.tsmart.tool.da.core.suggestion.NPEFix;

public class NPEFixer extends Fixer{
	/**
	 * insert if(...!= null){...}
	 */
	private BugFixSession session;
	
	public NPEFixer(BugFixSession session){
		this.session = session;
	}
	
	
	/**
	 * insert if(...) {break/continue/return}
	 */
	private ArrayList<NPEFix> insertStop(NPEFixSite fixSite, int environmentType, String returnTypeName){
		//get expression repository
		//construct ifStatement:
		//a. get a boolean expression that evaluate to true in the current context
		//b. fill in the expression.
		
		ArrayList<NPEFix> results = new ArrayList<NPEFix>();
		
		//Map<String, Set<String>> exprRepoMap = exprRepo.getExprRepoMap();
		String booleanExpr = fixSite.getExpr().toString() + " != null";
		String returnContentString = "";
		if(returnTypeName.equals("void")){
			returnContentString = "";
		} else if(returnTypeName.equals("byte")
				|| returnTypeName.equals("short")
				|| returnTypeName.equals("int")
				|| returnTypeName.equals("long")
				|| returnTypeName.equals("double")
				|| returnTypeName.equals("float")
				|| returnTypeName.equals("java.lang.Byte")
				|| returnTypeName.equals("java.lang.Short")
				|| returnTypeName.equals("java.lang.Integer")
				|| returnTypeName.equals("java.lang.Long")
				|| returnTypeName.equals("java.lang.Long")
				|| returnTypeName.equals("java.lang.Double")
				|| returnTypeName.equals("java.lang.Float")){
			returnContentString = "0";
		} else if(returnTypeName.equals("char")
				|| returnTypeName.equals("java.lang.Character")){
				returnContentString = "\' \'";
		} else if(returnTypeName.equals("java.lang.String")){
			returnContentString = "\"\"";
		} else{
			returnContentString = "null";
		}
		
			String breakModifiedString = "if (" + booleanExpr + ") {\n"
					+ "break;\n"
					+ "}\n";
			String continueModifiedString = "if (" + booleanExpr + ") {\n"
					+ "continue;\n"
					+ "}\n";
			
			
			String returnModifiedString = "if (" + booleanExpr + ") {\n"
					+ "return " + returnContentString + ";\n"
					+ "}\n";
			
			
			ASTNode wrappingStmt = fixSite.getExpr();
			while(!(wrappingStmt instanceof Statement)){
				wrappingStmt = wrappingStmt.getParent();
			}
			
			
			ASTNode stmt = wrappingStmt;
				//a loop => break, continue, and return
				//a switch case => break and return
				//a method body => return
				if(environmentType == 1 || environmentType == 2){//loop or switch case
					NPEFix breakFix = new NPEFix(fixSite, stmt.getStartPosition(), 0, breakModifiedString, Fix.IF_BREAK, EclipseUtils.getLineNum(stmt), null, "boolean", booleanExpr, "false");
					results.add(breakFix);
				}
				if(environmentType == 1){
					NPEFix continueFix = new NPEFix(fixSite, stmt.getStartPosition(), 0, continueModifiedString, Fix.IF_BREAK, EclipseUtils.getLineNum(stmt), null, "boolean", booleanExpr, "false");
					results.add(continueFix);
				}
				
				NPEFix returnFix = new NPEFix(fixSite, stmt.getStartPosition(), 0, returnModifiedString, Fix.IF_BREAK, EclipseUtils.getLineNum(stmt), null, "boolean", booleanExpr, "false");
				results.add(returnFix);
			
			
			
			
			//TODO:: do we need to insert these stop-statements after the last statement of a fixsite?
			/*
			ASTNode stmtN = stmts.get(stmts.size() - 1);
			if(environmentType == 1 || environmentType == 2){//loop or switch case
				Fix breakFix = new BlockFix(fixSite, stmtN.getStartPosition() + stmtN.getLength(), 0, breakModifiedString);
				fixes.add(breakFix);
			}
			if(environmentType == 1){
				Fix continueFix = new BlockFix(fixSite, stmtN.getStartPosition() + stmtN.getLength(), 0, continueModifiedString);
				fixes.add(continueFix);
			}
			for(String returnModifiedString: returnModifiedStrings){
				Fix returnFix = new BlockFix(fixSite, stmtN.getStartPosition() + stmtN.getLength(), 0, returnModifiedString);
				fixes.add(returnFix);
			}*/
		
		
		
		return results;
	}
	
	private ArrayList<NPEFix> insertNullChecker(NPEFixSite fixSite){
		// if (VARIABLENAME != null) { following context}
		ArrayList<NPEFix> fixes = new ArrayList<NPEFix>();
		//create nullcondition string
		ASTNode expr = fixSite.getExpr();
		ASTNode wrappingStmt = expr;
		while(!(wrappingStmt instanceof Statement)){
			wrappingStmt = wrappingStmt.getParent();
		}
		String nullCondStr = expr.toString() + " != null";
		
		if(wrappingStmt instanceof ExpressionStatement){
		
			if(((ExpressionStatement)wrappingStmt).getExpression() instanceof Assignment){
				//wrappingstmt: XX = XXXX
				Assignment assign = (Assignment)((ExpressionStatement)wrappingStmt).getExpression();
				Expression assignee = assign.getLeftHandSide();
				if(assignee.getStartPosition() < expr.getStartPosition() 
						&& (assignee.getStartPosition() + assignee.getLength())
							> (expr.getStartPosition() + expr.getLength())){
					//the expr is on the lhs
					return fixes;
				}			
			}
		} else if (wrappingStmt instanceof SingleVariableDeclaration){
			SingleVariableDeclaration varDecl = (SingleVariableDeclaration)wrappingStmt;
			Expression initExpr = varDecl.getInitializer();
			if(initExpr.getStartPosition() < expr.getStartPosition() 
					&& initExpr.getStartPosition() + initExpr.getLength() > expr.getStartPosition() + expr.getLength()){
				// Type varName = initializer
				int eqIndex = varDecl.toString().indexOf("=");
				String varDeclLHSStr = varDecl.toString().substring(0, eqIndex);
				String modifiedString = varDeclLHSStr + ";\n"
						+ "if (" + nullCondStr + ") {\n"
						+ varDecl.getName().toString() + " = " + initExpr.toString() + "\n"
						+ "}\n";
				NPEFix fix = new NPEFix(fixSite, wrappingStmt.getStartPosition(), wrappingStmt.getLength(), modifiedString, Fix.IF_NULL_CHECK, EclipseUtils.getLineNum(wrappingStmt), null, nullCondStr, "boolean", "true");
				fixes.add(fix);
				return fixes;
			}
		} 
		
		// we assume it is just common statement
		// if anything is wrong syntactically, we leave it to the compiler
		String modifiedString = "if (" + nullCondStr + ") {\n" 
					+ wrappingStmt.toString() + "\n"
					+ "}\n";
		NPEFix fix = new NPEFix(fixSite, wrappingStmt.getStartPosition(), wrappingStmt.getLength(), modifiedString, Fix.IF_NULL_CHECK, EclipseUtils.getLineNum(wrappingStmt), null, nullCondStr, "boolean", "true");
		fixes.add(fix);		
			
		
		return fixes;
	}
	
	
	public ArrayList<NPEFix> generateFix(NPEFixSite fixSite){
		ArrayList<NPEFix> fixes = new ArrayList<NPEFix>();
		fixes.addAll(insertNullChecker(fixSite));
		return fixes;
		//fixes.addAll(createNewInstance(fixSite, exprRepo));
	}
	
}
