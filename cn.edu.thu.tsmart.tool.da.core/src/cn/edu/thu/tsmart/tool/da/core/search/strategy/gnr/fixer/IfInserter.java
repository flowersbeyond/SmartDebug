package cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fixer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.WhileStatement;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.EclipseUtils;
import cn.edu.thu.tsmart.tool.da.core.search.Filter;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.InsertStopFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.StatementFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fixer.Fixer;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSite;
import cn.edu.thu.tsmart.tool.da.core.suggestion.FilterableFix;
import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;

public class IfInserter extends Fixer{
	
	private BugFixSession session;
	public IfInserter(BugFixSession session){
		this.session = session;
	}
	
	/**
	 * insert if(...) {break/continue/return}
	 */
	private Map<Integer, ArrayList<FilterableFix>> insertStop(AbstractFixSite fixSite, int environmentType, String returnTypeName){
		//get expression repository
		//construct ifStatement:
		//a. get a boolean expression that evaluate to true in the current context
		//b. fill in the expression.
		
		Map<Integer, ArrayList<FilterableFix>> results = new HashMap<Integer, ArrayList<FilterableFix>>();
		
		//Map<String, Set<String>> exprRepoMap = exprRepo.getExprRepoMap();
		Set<String> booleanExprs = session.getExpressionGenerator().genIfCondition(fixSite);
		
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
		
		for(String booleanExpr: booleanExprs){
			String breakModifiedString = "if (" + booleanExpr + ") {\n"
					+ "break;\n"
					+ "}\n";
			String continueModifiedString = "if (" + booleanExpr + ") {\n"
					+ "continue;\n"
					+ "}\n";
			
			
			String returnModifiedString = "if (" + booleanExpr + ") {\n"
					+ "return " + returnContentString + ";\n"
					+ "}\n";
			
			
			List<ASTNode> stmts = new ArrayList<ASTNode>();
			if(fixSite instanceof StatementFixSite){
				stmts = (List<ASTNode>)((StatementFixSite)fixSite).getStatements();
			} else if (fixSite instanceof InsertStopFixSite){
				stmts.add(((InsertStopFixSite) fixSite).getStatement());
			} else
				return results;
			
			for(ASTNode stmt: stmts){
				//a loop => break, continue, and return
				//a switch case => break and return
				//a method body => return
				if(environmentType == 1 || environmentType == 2){//loop or switch case
					FilterableFix breakFix = new FilterableFix(fixSite, stmt.getStartPosition(), 0, breakModifiedString, Fix.IF_BREAK, EclipseUtils.getLineNum(stmt), null, "boolean", booleanExpr, "false");
					Filter.addFix(results, breakFix, EclipseUtils.getLineNum(stmt));
				}
				if(environmentType == 1){
					FilterableFix continueFix = new FilterableFix(fixSite, stmt.getStartPosition(), 0, continueModifiedString, Fix.IF_BREAK, EclipseUtils.getLineNum(stmt), null, "boolean", booleanExpr, "false");
					Filter.addFix(results, continueFix, EclipseUtils.getLineNum(stmt));
				}
				
				FilterableFix returnFix = new FilterableFix(fixSite, stmt.getStartPosition(), 0, returnModifiedString, Fix.IF_BREAK, EclipseUtils.getLineNum(stmt), null, "boolean", booleanExpr, "false");
				Filter.addFix(results, returnFix, EclipseUtils.getLineNum(stmt));
			
			
			}
			
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
		}
		
		
		return results;
	}
	
	/**
	 * insert if(...!= null){...}
	 */
	private Map<Integer, ArrayList<FilterableFix>> insertNullChecker(StatementFixSite fixSite, Map<ASTNode, ArrayList<? extends Expression>> exprsMap){
		Map<Integer, ArrayList<FilterableFix>> results = new HashMap<Integer, ArrayList<FilterableFix>>();
		
		for(ASTNode wrappingStmt: exprsMap.keySet()){
		
			// if (VARIABLENAME != null) { following context}
			ArrayList<? extends Expression> exprs = exprsMap.get(wrappingStmt);
			for(Expression expr: exprs){
				
				//create nullcondition string
				String nullCondStr = expr.toString() + " != null";
				if(expr instanceof MethodInvocation){
					MethodInvocation mi = (MethodInvocation)expr;
					if(mi.getExpression() == null){
						continue;
					}
					nullCondStr = mi.getExpression().toString() + " != null";
				} else if (expr instanceof CastExpression){
					CastExpression ce = (CastExpression)expr;
					nullCondStr = ce.getExpression().toString() + " != null";
				}
				
				if(wrappingStmt instanceof ExpressionStatement){
				
					if(((ExpressionStatement)wrappingStmt).getExpression() instanceof Assignment){
						//wrappingstmt: XX = XXXX
						Assignment assign = (Assignment)((ExpressionStatement)wrappingStmt).getExpression();
						Expression assignee = assign.getLeftHandSide();
						if(assignee.getStartPosition() < expr.getStartPosition() 
								&& (assignee.getStartPosition() + assignee.getLength())
									> (expr.getStartPosition() + expr.getLength())){
							//the expr is on the lhs
							continue;
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
						FilterableFix fix = new FilterableFix(fixSite, wrappingStmt.getStartPosition(), wrappingStmt.getLength(), modifiedString, Fix.IF_NULL_CHECK, EclipseUtils.getLineNum(wrappingStmt), null, nullCondStr, "boolean", "true");
						Filter.addFix(results, fix, EclipseUtils.getLineNum(wrappingStmt));
						continue;
					}
				} 
				
				// we assume it is just common statement
				// if anything is wrong syntactically, we leave it to the compiler
				String modifiedString = "if (" + nullCondStr + ") {\n" 
							+ wrappingStmt.toString() + "\n"
							+ "}\n";
				FilterableFix fix = new FilterableFix(fixSite, wrappingStmt.getStartPosition(), wrappingStmt.getLength(), modifiedString, Fix.IF_NULL_CHECK, EclipseUtils.getLineNum(wrappingStmt), null, nullCondStr, "boolean", "true");
				Filter.addFix(results, fix, EclipseUtils.getLineNum(wrappingStmt));				
			}
		}
		return results;
	}
	
	/**
	 * insert if(... instanceof...){...}
	 */
	private Map<Integer, ArrayList<FilterableFix>> insertCastChecker(StatementFixSite fixSite, Map<ASTNode, ArrayList<? extends Expression>> castExprsMap){
		
		// if(CastExpressionVariable instance of CastExpressionType) {CastExpressionStatement}
		Map<Integer, ArrayList<FilterableFix>> results = new HashMap<Integer, ArrayList<FilterableFix>>();
		
		for(ASTNode wrappingStmt: castExprsMap.keySet()){
			ArrayList<Expression> castExprs = (ArrayList<Expression>) castExprsMap.get(wrappingStmt);
		
			for(Expression expr: castExprs){
				CastExpression castExpr = (CastExpression)expr;
				Type type = castExpr.getType();
				Expression targetExpr = castExpr.getExpression();
				//create nullcondition string
				String instanceOfCond = targetExpr.toString() + " instanceof " + type.toString();
				
				if(wrappingStmt instanceof ExpressionStatement){
				
					if(((ExpressionStatement)wrappingStmt).getExpression() instanceof Assignment){
						//wrappingstmt: XX = XXXX
						Assignment assign = (Assignment)((ExpressionStatement)wrappingStmt).getExpression();
						Expression assignee = assign.getLeftHandSide();
						if(assignee.getStartPosition() < castExpr.getStartPosition() 
								&& (assignee.getStartPosition() + assignee.getLength())
									> (castExpr.getStartPosition() + castExpr.getLength())){
							//the expr is on the lhs
							continue;
						}				
					}
				} else if (wrappingStmt instanceof SingleVariableDeclaration){
					SingleVariableDeclaration varDecl = (SingleVariableDeclaration)wrappingStmt;
					Expression initExpr = varDecl.getInitializer();
					if(initExpr.getStartPosition() < castExpr.getStartPosition() 
							&& initExpr.getStartPosition() + initExpr.getLength() > castExpr.getStartPosition() + castExpr.getLength()){
						// Type varName = initializer
						int eqIndex = varDecl.toString().indexOf("=");
						String varDeclLHSStr = varDecl.toString().substring(0, eqIndex);
						String modifiedString = varDeclLHSStr + ";\n"
								+ "if (" + instanceOfCond + ") {\n"
								+ varDecl.getName() + " = " + initExpr.toString() + "\n"
								+ "}\n";
						FilterableFix fix = new FilterableFix(fixSite, wrappingStmt.getStartPosition(), wrappingStmt.getLength(), modifiedString, Fix.IF_CAST_CHECK, EclipseUtils.getLineNum(wrappingStmt), null, instanceOfCond, "boolean", "true");
						Filter.addFix(results, fix, EclipseUtils.getLineNum(wrappingStmt));
					}
				} else {
					// we assume it is just common statement
					// if anything is wrong syntactically, we leave it to the compiler
					String modifiedString = "if (" + instanceOfCond + ") {\n" 
								+ wrappingStmt.toString() + "\n"
								+ "}\n";
					FilterableFix fix = new FilterableFix(fixSite, wrappingStmt.getStartPosition(), wrappingStmt.getLength(), modifiedString, Fix.IF_CAST_CHECK, EclipseUtils.getLineNum(wrappingStmt), null, instanceOfCond, "boolean", "true");
					Filter.addFix(results, fix, EclipseUtils.getLineNum(wrappingStmt));
				}
				
			}
		}
		return results;
	}
	
	/**
	 * insert if(i > 0 && i < array.length){...}
	 */
	private Map<Integer, ArrayList<FilterableFix>> insertRangeChecker(StatementFixSite fixSite, Map<ASTNode, ArrayList<? extends Expression>> arrayAccessesMap){
		//pre-cond
		// a. ArrayAccess Expression
		// get ArrayAccess Array & ArrayAccess Index
		//if(index > 0 && index < array.length){ArrayAcess statement}
		Map<Integer, ArrayList<FilterableFix>> results = new HashMap<Integer, ArrayList<FilterableFix>>();
		for(ASTNode wrappingStmt: arrayAccessesMap.keySet()){
			ArrayList<Expression> arrayAccesses = (ArrayList<Expression>) arrayAccessesMap.get(wrappingStmt);
		
			for(Expression expr: arrayAccesses){
				ArrayAccess aaExpr = (ArrayAccess)expr;
				Expression indexExpr = aaExpr.getIndex();
				Expression arrayExpr = aaExpr.getArray();
				//create range check condition string
				String rangeCheckCond = indexExpr.toString() + " > 0 && " + indexExpr + " < " + arrayExpr.toString() + ".length";
				
				if (wrappingStmt instanceof SingleVariableDeclaration){
					SingleVariableDeclaration varDecl = (SingleVariableDeclaration)wrappingStmt;
					Expression initExpr = varDecl.getInitializer();
					if(initExpr.getStartPosition() < aaExpr.getStartPosition() 
							&& initExpr.getStartPosition() + initExpr.getLength() > aaExpr.getStartPosition() + aaExpr.getLength()){
						// Type varName = initializer
						int eqIndex = varDecl.toString().indexOf("=");
						String varDeclLHSStr = varDecl.toString().substring(0, eqIndex);
						String modifiedString = varDeclLHSStr + ";\n"
								+ "if (" + rangeCheckCond + ") {\n"
								+ varDecl.getName() + " = " + initExpr.toString() + "\n"
								+ "}\n";
						FilterableFix fix = new FilterableFix(fixSite, wrappingStmt.getStartPosition(), wrappingStmt.getLength(), modifiedString, Fix.IF_ARRAY_RANGE_CHECK, EclipseUtils.getLineNum(wrappingStmt), null, rangeCheckCond, "boolean", "true");
						Filter.addFix(results, fix, EclipseUtils.getLineNum(wrappingStmt));
					}
				} else {
					// we assume it is just common statement
					// if anything is wrong syntactically, we leave it to the compiler
					String modifiedString = "if (" + rangeCheckCond + ") {\n" 
								+ wrappingStmt.toString() + "\n"
								+ "}\n";
					FilterableFix fix = new FilterableFix(fixSite, wrappingStmt.getStartPosition(), wrappingStmt.getLength(), modifiedString, Fix.IF_ARRAY_RANGE_CHECK, EclipseUtils.getLineNum(wrappingStmt), null, rangeCheckCond, "boolean", "true");
					Filter.addFix(results, fix, EclipseUtils.getLineNum(wrappingStmt));
				}
				
			}
		}
		return results;
	}

	/**
	 * insert if(index < arrayList.size())
	 */
	private Map<Integer, ArrayList<FilterableFix>> insertCollectionBoundChecker(StatementFixSite fixSite, Map<ASTNode, ArrayList<? extends Expression>> methodInvocsMap){
		//pre-cond
		// a. Collection.get(i)?
		// get Collection Variable & Collection IndexVariable
		// fill in: if(i > 0 & i < collection.size()){CollectionBoundChecker}
		Map<Integer, ArrayList<FilterableFix>> results = new HashMap<Integer, ArrayList<FilterableFix>>();
		for(ASTNode wrappingStmt: methodInvocsMap.keySet()){
			ArrayList<Expression> methodInvocs= (ArrayList<Expression>) methodInvocsMap.get(wrappingStmt);
			for(Expression expr: methodInvocs){
				MethodInvocation miExpr = (MethodInvocation)expr;
				Expression miRecExpr = miExpr.getExpression();
				SimpleName miName = miExpr.getName();
				if(!miName.toString().equals("get"))
					continue;
				ITypeBinding tb = miRecExpr.resolveTypeBinding();
				
				ITypeHierarchy th = null;
				try {
					IType type = session.getProject().findType(tb.getQualifiedName());
					if(type == null)
						continue;
					th = type.newSupertypeHierarchy(new NullProgressMonitor());
				} catch (JavaModelException e) {
					e.printStackTrace();
					continue;
				}
				IType[] interfaces = th.getAllInterfaces();
				boolean implementedList = false;
				for(IType iftype: interfaces){
					if(iftype.getFullyQualifiedName().equals("java.util.list")){
						implementedList = true;
						break;
					}
				}
				
				if(implementedList){
					List<Expression> args = miExpr.arguments();
					if(args.size() == 1){
						Expression arg = args.get(0);
						ITypeBinding argTB = arg.resolveTypeBinding();
						if(argTB.getQualifiedName().equals("int") || argTB.getQualifiedName().equals("java.lang.Integer")){
							//create range check condition
							String rangeCheckCond = arg.toString() + " > 0 && " 
									+ arg.toString() + " < " + miRecExpr.toString() + ".size()";
													
							if (wrappingStmt instanceof SingleVariableDeclaration){
								SingleVariableDeclaration varDecl = (SingleVariableDeclaration)wrappingStmt;
								Expression initExpr = varDecl.getInitializer();
								if(initExpr.getStartPosition() < miExpr.getStartPosition() 
										&& initExpr.getStartPosition() + initExpr.getLength() > miExpr.getStartPosition() + miExpr.getLength()){
									// Type varName = initializer
									int eqIndex = varDecl.toString().indexOf("=");
									String varDeclLHSStr = varDecl.toString().substring(0, eqIndex);
									String modifiedString = varDeclLHSStr + ";\n"
											+ "if (" + rangeCheckCond + ") {\n"
											+ varDecl.getName() + " = " + initExpr.toString() + "\n"
											+ "}\n";
									FilterableFix fix = new FilterableFix(fixSite, wrappingStmt.getStartPosition(), wrappingStmt.getLength(), modifiedString, Fix.IF_LIST_RANGE_CHECK, EclipseUtils.getLineNum(wrappingStmt), null, rangeCheckCond, "boolean", "true");
									Filter.addFix(results, fix, EclipseUtils.getLineNum(wrappingStmt));
								}
							} else {
								// we assume it is just common statement
								// if anything is wrong syntactically, we leave it to the compiler
								String modifiedString = "if (" + rangeCheckCond + ") {\n" 
											+ wrappingStmt.toString() + "\n"
											+ "}\n";
								FilterableFix fix = new FilterableFix(fixSite, wrappingStmt.getStartPosition(), wrappingStmt.getLength(), modifiedString, Fix.IF_LIST_RANGE_CHECK, EclipseUtils.getLineNum(wrappingStmt), null, rangeCheckCond, "boolean", "true");
								Filter.addFix(results, fix, EclipseUtils.getLineNum(wrappingStmt));
							}
							
						}
					}
				}
			}
		}
		return results;
	}

	public Map<Integer, ArrayList<FilterableFix>> generateFix(StatementFixSite fixSite) {
		Map<Integer, ArrayList<FilterableFix>> results = new HashMap<Integer, ArrayList<FilterableFix>>();
		
		List<ASTNode> stmts = (List<ASTNode>) fixSite.getStatements();
		
		ASTNode stmt0 = stmts.get(0);
		ASTNode environment = stmt0.getParent();
		int environmentType = 0; //1: loop, 2: switchCase, 3: methodBody
		boolean found = false;
		while(!found){
			if(environment instanceof DoStatement || environment instanceof ForStatement
					|| environment instanceof WhileStatement || environment instanceof EnhancedForStatement){
				found = true;
				environmentType = 1;
				break;
			} else if(environment instanceof SwitchCase){
				found = true;
				environmentType = 2;
				break;
			} else if(environment instanceof MethodDeclaration){
				found = true;
				environmentType = 3;
				break;
			}
			environment = environment.getParent();
		}
		
		MethodDeclaration md = null;
		ASTNode parent = stmt0.getParent();
		while(!(parent instanceof MethodDeclaration)){
			parent = parent.getParent();
		}
		md = (MethodDeclaration)parent;
		Type returnType = md.getReturnType2();
		String returnTypeName = returnType.resolveBinding().getQualifiedName();
		
		Map<ASTNode, ArrayList<? extends Expression>> castExprsMap = new HashMap<ASTNode, ArrayList<? extends Expression>>();
		Map<ASTNode, ArrayList<? extends Expression>> arrayAccessesMap = new HashMap<ASTNode, ArrayList<? extends Expression>>();
		Map<ASTNode, ArrayList<? extends Expression>> fieldAccessesMap = new HashMap<ASTNode, ArrayList<? extends Expression>>();
		Map<ASTNode, ArrayList<? extends Expression>> methodInvocsMap = new HashMap<ASTNode, ArrayList<? extends Expression>>();
		
		
		for(ASTNode stmt: stmts){
			SpecialExpressionCollector collector = new SpecialExpressionCollector();
			stmt.accept(collector);
			castExprsMap.put(stmt, collector.castExpressions);
			arrayAccessesMap.put(stmt, collector.arrayAccesses);
			fieldAccessesMap.put(stmt, collector.fieldAccesses);
			methodInvocsMap.put(stmt, collector.methodInvocations);
		}
		
		
		
		Filter.merge(results, insertStop(fixSite, environmentType, returnTypeName));
		Filter.merge(results, insertNullChecker(fixSite, castExprsMap));
		Filter.merge(results, insertNullChecker(fixSite, arrayAccessesMap));
		Filter.merge(results, insertNullChecker(fixSite, fieldAccessesMap));
		Filter.merge(results, insertNullChecker(fixSite, methodInvocsMap));
		Filter.merge(results, insertCastChecker(fixSite, castExprsMap));
		Filter.merge(results, insertRangeChecker(fixSite, arrayAccessesMap));
		Filter.merge(results, insertCollectionBoundChecker(fixSite, methodInvocsMap));
			
		return results;
	}
	
	public Map<Integer, ArrayList<FilterableFix>> generateFix(InsertStopFixSite fixSite) {
		Map<Integer, ArrayList<FilterableFix>> results = new HashMap<Integer, ArrayList<FilterableFix>>();
		
		
		ASTNode environment = fixSite.getStatement().getParent();
		int environmentType = 0; //1: loop, 2: switchCase, 3: methodBody
		boolean found = false;
		while(!found){
			if(environment instanceof DoStatement || environment instanceof ForStatement
					|| environment instanceof WhileStatement || environment instanceof EnhancedForStatement){
				found = true;
				environmentType = 1;
				break;
			} else if(environment instanceof SwitchCase){
				found = true;
				environmentType = 2;
				break;
			} else if(environment instanceof MethodDeclaration){
				found = true;
				environmentType = 3;
				break;
			}
			environment = environment.getParent();
		}
		
		MethodDeclaration md = null;
		ASTNode parent = fixSite.getStatement().getParent();
		while(!(parent instanceof MethodDeclaration)){
			parent = parent.getParent();
		}
		md = (MethodDeclaration)parent;
		Type returnType = md.getReturnType2();
		String returnTypeName = returnType.resolveBinding().getQualifiedName();
		
		Map<ASTNode, ArrayList<? extends Expression>> castExprsMap = new HashMap<ASTNode, ArrayList<? extends Expression>>();
		Map<ASTNode, ArrayList<? extends Expression>> arrayAccessesMap = new HashMap<ASTNode, ArrayList<? extends Expression>>();
		Map<ASTNode, ArrayList<? extends Expression>> fieldAccessesMap = new HashMap<ASTNode, ArrayList<? extends Expression>>();
		Map<ASTNode, ArrayList<? extends Expression>> methodInvocsMap = new HashMap<ASTNode, ArrayList<? extends Expression>>();
		
		
		
		Filter.merge(results, insertStop(fixSite, environmentType, returnTypeName));
		
			
		return results;
		
	}
	
	
	private class SpecialExpressionCollector extends ASTVisitor{
		ArrayList<CastExpression> castExpressions = new ArrayList<CastExpression>();
		ArrayList<ArrayAccess> arrayAccesses = new ArrayList<ArrayAccess>();
		ArrayList<FieldAccess> fieldAccesses = new ArrayList<FieldAccess>();
		ArrayList<MethodInvocation> methodInvocations = new ArrayList<MethodInvocation>();
		
		@Override
		public boolean visit(CastExpression node){
			castExpressions.add(node);
			return true;
		}
		
		@Override
		public boolean visit(ArrayAccess node){
			arrayAccesses.add(node);
			return true;
		}
		
		@Override
		public boolean visit(FieldAccess node){
			fieldAccesses.add(node);
			return true;
		}
		
		@Override
		public boolean visit(MethodInvocation node){
			methodInvocations.add(node);
			return true;
		}
	}


	
}
