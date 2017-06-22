package cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fixer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.search.ExpressionVisitor;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.ConditionFixSite;
import cn.edu.thu.tsmart.tool.da.core.suggestion.FilterableFix;
import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;

public class ConditionExprFixer {
	
	BugFixSession session;
	public ConditionExprFixer(BugFixSession session){
		this.session = session;
	}
	
	/**
	 * generate fix for if conditions.
	 * currently, we generate the following expressions:
	 * a. expr => expr || ?, expr && ?
	 * b. expr = a>b | a<b | a>=b | a<=b | !a | a == b | a != b
	 * @param fixSite
	 * @param repository
	 * @return
	 */
	public Map<Integer, ArrayList<FilterableFix>> generateFix(ConditionFixSite fixSite) {
		
		Map<Integer, ArrayList<FilterableFix>> results = new HashMap<Integer, ArrayList<FilterableFix>>();
		Expression expr = (Expression) fixSite.getConditionExpression();
		ASTNode node = fixSite.getWrappingStatement();
		/*if(node instanceof ForStatement || node instanceof WhileStatement || node instanceof EnhancedForStatement){
			return results;
		}*/
		
		ExpressionVisitor visitor = new ExpressionVisitor();
		expr.accept(visitor);
		
		if(!(expr.getRoot() instanceof CompilationUnit)){
			return results;
		}
		CompilationUnit cu = (CompilationUnit)expr.getRoot();
		int lineNum = cu.getLineNumber(expr.getStartPosition());
		ArrayList<FilterableFix> fixes = new ArrayList<FilterableFix>();
		
		Map<Expression, String> infixNodeMap = visitor.getInfixNodeTypeMap();
		for(Expression e: infixNodeMap.keySet()){
			InfixExpression infixExpr = (InfixExpression)e;
			Operator op = infixExpr.getOperator();
			
			Expression left = infixExpr.getLeftOperand();
			Expression right = infixExpr.getRightOperand();
			int leftFracStartPos = expr.getStartPosition();
			int leftFracEndPos = left.getStartPosition() + left.getLength();
			int leftFracLength = leftFracEndPos - leftFracStartPos;
			
			int rightFracStartPos = right.getStartPosition();
			int rightFracEndPos = expr.getStartPosition() + expr.getLength();
			int rightFracLength = rightFracEndPos - rightFracStartPos;
			
			ITextFileBuffer fileBuffer = ITextFileBufferManager.DEFAULT.getTextFileBuffer(fixSite.getFile().getFullPath(), LocationKind.IFILE);
			if(fileBuffer == null){
				System.out.println("FileBuffer Not Found: " + fixSite.getFile().getFullPath());
				try {
					ITextFileBufferManager.DEFAULT.connect(fixSite.getFile().getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
					fileBuffer = ITextFileBufferManager.DEFAULT.getTextFileBuffer(fixSite.getFile().getFullPath(), LocationKind.IFILE);
				} catch (CoreException e1) {
					e1.printStackTrace();
				}
			}
			IDocument document = fileBuffer.getDocument();
			String leftFrac = "";
			String rightFrac = "";
			try {
				leftFrac = document.get(leftFracStartPos, leftFracLength);
				rightFrac = document.get(rightFracStartPos, rightFracLength);
			} catch (BadLocationException e1) {
				e1.printStackTrace();
			}
			
			int infixExprLineNum = cu.getLineNumber(infixExpr.getStartPosition());
			if(op.equals(Operator.EQUALS) || op.equals(Operator.GREATER) || op.equals(Operator.GREATER_EQUALS)
					|| op.equals(Operator.LESS) || op.equals(Operator.LESS_EQUALS) ||op.equals(Operator.NOT_EQUALS)){
				
				if(!op.equals(Operator.EQUALS)){
					String modifiedExprString = left.toString() + "==" + right.toString();
					FilterableFix fix = new FilterableFix(fixSite, infixExpr.getStartPosition(), infixExpr.getLength(), modifiedExprString, Fix.COND_EXPR_CHANGE, infixExprLineNum,
							expr, "boolean", leftFrac + "==" + rightFrac);
					fixes.add(fix);
				}
				if(!op.equals(Operator.GREATER)){
					String modifiedExprString = left.toString() + ">" + right.toString();
					FilterableFix fix = new FilterableFix(fixSite, infixExpr.getStartPosition(), infixExpr.getLength(), modifiedExprString, Fix.COND_EXPR_CHANGE, infixExprLineNum,
							expr, "boolean", leftFrac + ">" + rightFrac);
					fixes.add(fix);
				}
				if(!op.equals(Operator.GREATER_EQUALS)){
					String modifiedExprString = left.toString() + ">=" + right.toString();
					FilterableFix fix = new FilterableFix(fixSite, infixExpr.getStartPosition(), infixExpr.getLength(), modifiedExprString, Fix.COND_EXPR_CHANGE, infixExprLineNum,
							expr, "boolean", leftFrac + ">=" + rightFrac);
					fixes.add(fix);
				}
				if(!op.equals(Operator.LESS)){
					String modifiedExprString = left.toString() + "<" + right.toString();
					FilterableFix fix = new FilterableFix(fixSite, infixExpr.getStartPosition(), infixExpr.getLength(), modifiedExprString, Fix.COND_EXPR_CHANGE, infixExprLineNum,
							expr, "boolean", leftFrac + "<" + rightFrac);
					fixes.add(fix);
				}
				if(!op.equals(Operator.LESS_EQUALS)){
					String modifiedExprString = left.toString() + "<=" + right.toString();
					FilterableFix fix = new FilterableFix(fixSite, infixExpr.getStartPosition(), infixExpr.getLength(), modifiedExprString, Fix.COND_EXPR_CHANGE, infixExprLineNum,
							expr, "boolean", leftFrac + "<=" + rightFrac);
					fixes.add(fix);
				}
				if(!op.equals(Operator.NOT_EQUALS)){
					String modifiedExprString = left.toString() + "!=" + right.toString();
					FilterableFix fix = new FilterableFix(fixSite, infixExpr.getStartPosition(), infixExpr.getLength(), modifiedExprString, Fix.COND_EXPR_CHANGE, infixExprLineNum,
							expr, "boolean", leftFrac + "!=" + rightFrac);
					fixes.add(fix);
				}
			}
			if(op.equals(Operator.CONDITIONAL_AND) || op.equals(Operator.CONDITIONAL_OR)){
				
				if(!op.equals(Operator.CONDITIONAL_AND)){
					String modifiedExprString = left.toString() + "||" + right.toString();
					FilterableFix fix = new FilterableFix(fixSite, infixExpr.getStartPosition(), infixExpr.getLength(), modifiedExprString, Fix.COND_EXPR_CHANGE, infixExprLineNum,
							expr, "boolean", leftFrac + "||" + rightFrac);
					fixes.add(fix);
				}
				if(!op.equals(Operator.CONDITIONAL_OR)){
					String modifiedExprString = left.toString() + "&&" + right.toString();
					FilterableFix fix = new FilterableFix(fixSite, infixExpr.getStartPosition(), infixExpr.getLength(), modifiedExprString, Fix.COND_EXPR_CHANGE, infixExprLineNum,
							expr, "boolean", leftFrac + "&&" + rightFrac);
					fixes.add(fix);
				}
			}			
		}
	
		//Map<String, Set<String>> exprRepoMap = repository.exprRepoMap;
		ArrayList<String> booleanExprs = session.getExpressionGenerator().genBooleanAppendPart(fixSite);
		for(String bExpr: booleanExprs){
			String modifiedExprString = expr.toString() + "||" + bExpr;
			FilterableFix fix = new FilterableFix(fixSite, expr.getStartPosition(), expr.getLength(), modifiedExprString, Fix.COND_EXPR_CHANGE, lineNum, expr, "boolean", modifiedExprString);
			fixes.add(fix);
			
			modifiedExprString = expr.toString() + "&&" + bExpr;
			fix = new FilterableFix(fixSite, expr.getStartPosition(), expr.getLength(), modifiedExprString,  Fix.COND_EXPR_CHANGE, lineNum, expr, "boolean", modifiedExprString);
			fixes.add(fix);
			
			/*modifiedExprString = "(" + expr.toString() + ")" + "||" + bExpr;
			fix = new FilterableFix(fixSite, expr.getStartPosition(), expr.getLength(), modifiedExprString,  Fix.COND_EXPR_CHANGE, lineNum, expr, "boolean", modifiedExprString);
			fixes.add(fix);
			
			modifiedExprString = "(" + expr.toString() + ")" + "&&" + bExpr;
			fix = new FilterableFix(fixSite, expr.getStartPosition(), expr.getLength(), modifiedExprString,  Fix.COND_EXPR_CHANGE, lineNum, expr, "boolean", modifiedExprString);
			fixes.add(fix);*/
		}
		
		
		Map<Expression, String> methodInvocs = visitor.getMethodInvocNodeTypeMap();
		Map<String, Set<String>> repExprs = session.getExpressionGenerator().genFieldReplacements(fixSite);
		Map<String, Set<String>> vars = session.getExpressionGenerator().genLocalVariableReplacements(fixSite);
		for(String s: vars.keySet()){
			if(repExprs.containsKey(s)){
				repExprs.get(s).addAll(vars.get(s));
			}
			else{
				repExprs.put(s, vars.get(s));
			}
		}
		
		for(Expression e: methodInvocs.keySet()){
			if(e instanceof MethodInvocation){
				MethodInvocation mi = (MethodInvocation)e;
				ArrayList<String> reps = MethodFixer.genOverloadedReplaces(mi, repExprs);
				ArrayList<String> reps2 = MethodFixer.genSameSigReplacers(mi, repExprs);
				
				reps.addAll(reps2);
				
				for(String rep: reps){
					String newBooleanExpr = "";
					String oldBooleanExpr = expr.toString();
					String oldMethodInvoc = e.toString();
					int leftEnd = oldBooleanExpr.indexOf(oldMethodInvoc);
					int rightStart = leftEnd + oldMethodInvoc.length();
					newBooleanExpr = oldBooleanExpr.substring(0, leftEnd) + rep + oldBooleanExpr.substring(rightStart);
					FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), rep, Fix.COND_EXPR_CHANGE, cu.getLineNumber(e.getStartPosition()),
							expr, "boolean", newBooleanExpr);
					fixes.add(fix);
				}
			}
		}
		
		results.put(lineNum, fixes);
		
		return results;
	}

}
