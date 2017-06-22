package cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fixer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.EclipseUtils;
import cn.edu.thu.tsmart.tool.da.core.search.ExpressionVisitor;
import cn.edu.thu.tsmart.tool.da.core.search.Filter;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.StatementFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fixer.Fixer;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSite;
import cn.edu.thu.tsmart.tool.da.core.suggestion.FilterableFix;
import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;

/*
 * now we only fix the "smallest" expressions, which means:
 * a. no method invocations (these are handled in the method-fix part)
 * b. infix operations are handled just like methods, for example: + -> - | * | /
 */
public class ExpressionFixer extends Fixer{
	
	private BugFixSession session;
	public ExpressionFixer(BugFixSession session){
		this.session = session;
	}

	
	
	public Map<Integer, ArrayList<FilterableFix>> generateFixForStatementBlocks(StatementFixSite fixSite){
		
		Map<String, Set<String>> localVars = session.getExpressionGenerator().genLocalVariableReplacements(fixSite);
		Map<String, Set<String>> fieldVars = session.getExpressionGenerator().genFieldReplacements(fixSite);
			
		Map<Integer, ArrayList<FilterableFix>> results = new HashMap<Integer, ArrayList<FilterableFix>>();
		List<? extends ASTNode> stmts = fixSite.getStatements();
		if(stmts.size() == 0)
			return results;		
			
		for(int i = 0; i < stmts.size(); i ++){
			Filter.merge(results, generateFix(fixSite, stmts.get(i), localVars, fieldVars));
		}

		//ArrayList<Expression> specialExpressions = findSpecialExpressions(stmts);
		//prioritizeSpecialExpressions(specialExpressions);
		//prioritizeAllFixes();
		
		return results;
		
	}

	/*
	private ArrayList<Expression> findSpecialExpressions(List<? extends ASTNode> statements){
		ArrayList<Expression> exprList = new ArrayList<Expression>();
		
		if(statements.size() == 0)
			return exprList;
		
		ASTNode parent = statements.get(0).getParent();
		Statement loopStatement = null;
		Statement brchStatement = null;
		while(!(parent instanceof MethodDeclaration)){
			if(parent instanceof ForStatement){
				loopStatement = (ForStatement)parent;
				break;
			}
			if(parent instanceof WhileStatement){
				loopStatement = (WhileStatement)parent;
				break;
			}
			if(parent instanceof IfStatement && brchStatement == null){
				brchStatement = (IfStatement)parent;
			}
			parent = parent.getParent();
		}
		
		//block in for loop, we recognize the loop variants in the "Updater & loop condition"
		if(loopStatement != null && loopStatement instanceof ForStatement){
			ForStatement forstmt = (ForStatement)loopStatement;
			List<Expression> updaters = forstmt.updaters();
			Expression loopCondition = forstmt.getExpression();
			
			//1. get a list of concerned IVariableBindings
			ArrayList<IVariableBinding> bindingList = new ArrayList<IVariableBinding>();
			SpecialExpressionFinder bindingFinder = 
					new SpecialExpressionFinder(bindingList, 
							SpecialExpressionFinder.IVARIABLEBINDING_COLLECTING_MODE);
			loopCondition.accept(bindingFinder);
			for(Expression expr: updaters){
				expr.accept(bindingFinder);
			}
			
			//2. get all concerned expressions in the current actionblock
			SpecialExpressionFinder expressionCollector = new SpecialExpressionFinder(bindingFinder.getVariableBindings(),
					SpecialExpressionFinder.EXPRESSION_FINDING_MODE);
			for(ASTNode stmt: statements){
				stmt.accept(expressionCollector);
			}
			exprList.addAll(expressionCollector.getSpecialExpressions());
			
			
			
		}
		//block in while loop, we recognize the loop variants in the "loop condition"
		if(loopStatement != null && loopStatement instanceof WhileStatement){
			WhileStatement whilestmt = (WhileStatement)loopStatement;
			Expression loopCondition = whilestmt.getExpression();
			
			//1. get a list of concerned IVariableBindings
			ArrayList<IVariableBinding> bindingList = new ArrayList<IVariableBinding>();
			SpecialExpressionFinder bindingFinder = 
					new SpecialExpressionFinder(bindingList, 
							SpecialExpressionFinder.IVARIABLEBINDING_COLLECTING_MODE);
			loopCondition.accept(bindingFinder);
			
			//2. get all concerned expressions in the current actionblock
			SpecialExpressionFinder expressionCollector = new SpecialExpressionFinder(bindingFinder.getVariableBindings(),
					SpecialExpressionFinder.EXPRESSION_FINDING_MODE);
			for(ASTNode stmt: statements){
				stmt.accept(expressionCollector);
			}
			exprList.addAll(expressionCollector.getSpecialExpressions());
			
		}
		//block in if statement, we recognize the if condition
		if(brchStatement != null){
			IfStatement ifstmt = (IfStatement)brchStatement;
			Expression ifCondition = ifstmt.getExpression();
			
			//1. get a list of concerned IVariableBindings
			ArrayList<IVariableBinding> bindingList = new ArrayList<IVariableBinding>();
			SpecialExpressionFinder bindingFinder = 
					new SpecialExpressionFinder(bindingList, 
							SpecialExpressionFinder.IVARIABLEBINDING_COLLECTING_MODE);
			ifCondition.accept(bindingFinder);
			
			//2. get all concerned expressions in the current actionblock
			SpecialExpressionFinder expressionCollector = new SpecialExpressionFinder(bindingFinder.getVariableBindings(),
					SpecialExpressionFinder.EXPRESSION_FINDING_MODE);
			for(ASTNode stmt: statements){
				stmt.accept(expressionCollector);
			}
			exprList.addAll(expressionCollector.getSpecialExpressions());
		}
		return exprList;
	}
	private ArrayList<Expression> findSpecialExpressions(ASTNode conditionExpr, ASTNode stmt){
		
		ArrayList<Expression> exprList = new ArrayList<Expression>();
		
		if(stmt == null)
			return exprList;
		
		ASTNode parent = stmt;
		Statement loopStatement = null;
		Statement brchStatement = null;
		while(!(parent instanceof MethodDeclaration)){
			if(parent instanceof ForStatement){
				loopStatement = (ForStatement)parent;
				break;
			}
			if(parent instanceof WhileStatement){
				loopStatement = (WhileStatement)parent;
				break;
			}
			if(parent instanceof IfStatement && brchStatement == null && parent != stmt){
				brchStatement = (IfStatement)parent;
			}
			parent = parent.getParent();
		}
		
		//block in for loop, we recognize the loop variants in the "Updater & loop condition"
		if(loopStatement != null && loopStatement instanceof ForStatement){
			ForStatement forstmt = (ForStatement)loopStatement;
			List<Expression> updaters = forstmt.updaters();
			Expression loopCondition = forstmt.getExpression();
			
			//1. get a list of concerned IVariableBindings
			ArrayList<IVariableBinding> bindingList = new ArrayList<IVariableBinding>();
			SpecialExpressionFinder bindingFinder = 
					new SpecialExpressionFinder(bindingList, 
							SpecialExpressionFinder.IVARIABLEBINDING_COLLECTING_MODE);
			loopCondition.accept(bindingFinder);
			for(Expression expr: updaters){
				expr.accept(bindingFinder);
			}
			
			//2. get all concerned expressions in the current actionblock
			SpecialExpressionFinder expressionCollector = new SpecialExpressionFinder(bindingFinder.getVariableBindings(),
					SpecialExpressionFinder.EXPRESSION_FINDING_MODE);
			conditionExpr.accept(expressionCollector);
			exprList.addAll(expressionCollector.getSpecialExpressions());
			
			
			
		}
		//block in while loop, we recognize the loop variants in the "loop condition"
		if(loopStatement != null && loopStatement instanceof WhileStatement){
			WhileStatement whilestmt = (WhileStatement)loopStatement;
			Expression loopCondition = whilestmt.getExpression();
			
			//1. get a list of concerned IVariableBindings
			ArrayList<IVariableBinding> bindingList = new ArrayList<IVariableBinding>();
			SpecialExpressionFinder bindingFinder = 
					new SpecialExpressionFinder(bindingList, 
							SpecialExpressionFinder.IVARIABLEBINDING_COLLECTING_MODE);
			loopCondition.accept(bindingFinder);
			
			//2. get all concerned expressions in the current actionblock
			SpecialExpressionFinder expressionCollector = new SpecialExpressionFinder(bindingFinder.getVariableBindings(),
					SpecialExpressionFinder.EXPRESSION_FINDING_MODE);
			conditionExpr.accept(expressionCollector);
			exprList.addAll(expressionCollector.getSpecialExpressions());
			
		}
		//block in if statement, we recognize the if condition
		if(brchStatement != null){
			IfStatement ifstmt = (IfStatement)brchStatement;
			Expression ifCondition = ifstmt.getExpression();
			
			//1. get a list of concerned IVariableBindings
			ArrayList<IVariableBinding> bindingList = new ArrayList<IVariableBinding>();
			SpecialExpressionFinder bindingFinder = 
					new SpecialExpressionFinder(bindingList, 
							SpecialExpressionFinder.IVARIABLEBINDING_COLLECTING_MODE);
			ifCondition.accept(bindingFinder);
			
			//2. get all concerned expressions in the current actionblock
			SpecialExpressionFinder expressionCollector = new SpecialExpressionFinder(bindingFinder.getVariableBindings(),
					SpecialExpressionFinder.EXPRESSION_FINDING_MODE);
			conditionExpr.accept(expressionCollector);
			exprList.addAll(expressionCollector.getSpecialExpressions());
		}
		return exprList;
	}
	
	class SpecialExpressionFinder extends ASTVisitor{
		private ArrayList<Expression> specialExpressionList = new ArrayList<Expression>();
		private ArrayList<IVariableBinding> coveredVariableBindings;
		
		public static final int EXPRESSION_FINDING_MODE = 0;
		public static final int IVARIABLEBINDING_COLLECTING_MODE = 1;
		private int mode;
		public SpecialExpressionFinder(ArrayList<IVariableBinding> existingBindings, int mode){
			this.coveredVariableBindings = existingBindings;
			this.mode = mode;
		}
		
		public ArrayList<Expression> getSpecialExpressions() {
			return specialExpressionList;
		}
		
		public ArrayList<IVariableBinding> getVariableBindings(){
			return coveredVariableBindings;
		}
					
		private void recordSpecialExpression(Expression node, IVariableBinding variableBinding) {
			if(mode == EXPRESSION_FINDING_MODE){
				boolean existingBinding = false;
				for(IVariableBinding binding: coveredVariableBindings){
					if(binding.equals(variableBinding)){
						existingBinding = true;
						break;
					}
				}
				if(existingBinding){
					specialExpressionList.add(node);
				}
			}
			if(mode == IVARIABLEBINDING_COLLECTING_MODE){
				boolean newbinding = true;
				for(IVariableBinding binding: coveredVariableBindings){
					if(binding.equals(variableBinding)){
						newbinding = false;
						break;
					}
				}
				if(newbinding){
					coveredVariableBindings.add(variableBinding);
				}
			}
		}
		@Override
		public void endVisit(FieldAccess node){
			IVariableBinding binding = node.resolveFieldBinding();
			recordSpecialExpression(node, binding);
		}
		
			
		@Override
		public void endVisit(SuperFieldAccess node){
			IVariableBinding binding = node.resolveFieldBinding();
			recordSpecialExpression(node, binding);
		}
		
		@Override
		public void endVisit(SimpleName node){
			IBinding binding = node.resolveBinding();
			if(binding instanceof IVariableBinding){
				recordSpecialExpression(node, (IVariableBinding)binding);
			}
		}
		@Override
		public void endVisit(QualifiedName node){
			IBinding binding = node.resolveBinding();
			if(binding instanceof IVariableBinding){
				recordSpecialExpression(node, (IVariableBinding)binding);
			}
		}
		
	}
	
	//TODO: prioritize special expression with special variables:
	private void prioritizeSpecialExpressions(ArrayList<Expression> specialExpressionList){
		
		ArrayList<Fix> fixList = this.results;
		for(Fix fix : fixList){
			ExpressionFix exprFix = (ExpressionFix)fix;
			Expression expr = exprFix.getExpression();
			
			int coveredLength = 0;
			for(Expression specialExpr: specialExpressionList){
				if(expr.getStartPosition() <= specialExpr.getStartPosition()
						&& expr.getStartPosition() + expr.getLength() >= specialExpr.getStartPosition() + specialExpr.getLength()){
					coveredLength += specialExpr.getLength();
				}
			}
			double coveredRatio = (double)coveredLength/(double)(expr.getLength());
			exprFix.setExprPriority(coveredRatio);
		}
		
		ArrayList<ExpressionFix> sortedFixes = new ArrayList<ExpressionFix>();
		
		for(Fix fix: fixList){
			ExpressionFix exprFix = (ExpressionFix)fix;
			int i = 0;
			for(; i < sortedFixes.size(); i ++){
				if(exprFix.getExprPriority() > sortedFixes.get(i).getExprPriority()){
					sortedFixes.add(i, exprFix);
					break;
				}
			}
			if(i == sortedFixes.size())
				sortedFixes.add(exprFix);
		}
		this.results.clear();
		this.results.addAll(sortedFixes);
	}
	
	private void prioritizeAllFixes(){
		ArrayList<Fix> fixList = this.results;
		for(Fix fix : fixList){
			ExpressionFix exprFix = (ExpressionFix)fix;
			Expression expr = exprFix.getExpression();
			String fixString = exprFix.getModifiedString();
			double similarity = U.stringSim(expr.toString(), fixString);
			exprFix.setFixSimilarity(similarity);
		}
		
		ArrayList<ExpressionFix> sortedFixes = new ArrayList<ExpressionFix>();
		
		for(Fix fix: fixList){
			ExpressionFix exprFix = (ExpressionFix)fix;
			int i = 0;
			if(exprFix.getFixSimilarity() < 1.3)
				continue;
			for( ; i < sortedFixes.size(); i ++){
				if(exprFix.getFixSimilarity() > sortedFixes.get(i).getFixSimilarity()){
					sortedFixes.add(i, exprFix);
					break;
				}
			}
			if(i == sortedFixes.size())
				sortedFixes.add(exprFix);
		}
		this.results.clear();
		this.results.addAll(sortedFixes);
		
		
	}
	*/
	
	private Map<Integer, ArrayList<FilterableFix>> generateFix(AbstractFixSite fixSite, ASTNode stmt, Map<String, Set<String>> localVars, Map<String, Set<String>> fieldVars){
		
		Map<Integer, ArrayList<FilterableFix>> fixes = new HashMap<Integer, ArrayList<FilterableFix>>();
		
		if(stmt instanceof ConstructorInvocation){
			List<?> argList = ((ConstructorInvocation) stmt).arguments();
			for(Object obj: argList){
				Filter.merge(fixes, generateFixInner(fixSite,(Expression)obj, localVars, fieldVars));
			}			
			
		}else if(stmt instanceof ExpressionStatement){
			Expression expr = ((ExpressionStatement) stmt).getExpression();
			Filter.merge(fixes, generateFixInner(fixSite, expr, localVars, fieldVars));
			
		}else if(stmt instanceof ReturnStatement){
			Expression expr = ((ReturnStatement) stmt).getExpression();
			if(expr != null)
				Filter.merge(fixes, generateFixInner(fixSite, expr, localVars, fieldVars));
			
		}else if(stmt instanceof SuperConstructorInvocation){
			Expression expr = ((SuperConstructorInvocation) stmt).getExpression();
			if(expr != null)
				Filter.merge(fixes, generateFixInner(fixSite, expr, localVars, fieldVars));
		}else if(stmt instanceof SwitchCase){
			Expression expr = ((SwitchCase) stmt).getExpression();
			if(expr != null)
				Filter.merge(fixes, generateFixInner(fixSite, expr, localVars, fieldVars));
		}else if(stmt instanceof VariableDeclarationStatement){
			VariableDeclarationStatement vdstmt = (VariableDeclarationStatement)stmt;
			List<?> fragments = vdstmt.fragments();
			for(Object frag: fragments){
				if(frag instanceof VariableDeclarationFragment){
					Expression expr = ((VariableDeclarationFragment) frag).getInitializer();
					if(expr != null)
						Filter.merge(fixes, generateFixInner(fixSite, expr, localVars, fieldVars));
				}
			}
		}else if(stmt instanceof VariableDeclarationExpression){
			VariableDeclarationExpression vdexpr = (VariableDeclarationExpression)stmt;
			List<?> fragments = vdexpr.fragments();
			for(Object frag: fragments){
				if(frag instanceof VariableDeclarationFragment){
					Expression expr = ((VariableDeclarationFragment) frag).getInitializer();
					if(expr != null)
						Filter.merge(fixes, generateFixInner(fixSite, expr, localVars, fieldVars));
				}
			}
		}
		else if(stmt instanceof PostfixExpression){
			PostfixExpression postfix = (PostfixExpression)stmt;
			Expression expr = postfix.getOperand();
			if(expr != null)
				Filter.merge(fixes, generateFixInner(fixSite, expr, localVars, fieldVars));
		} else if(stmt instanceof PrefixExpression){
			Expression expr = ((PrefixExpression) stmt).getOperand();
			if(expr != null)
				Filter.merge(fixes, generateFixInner(fixSite, expr, localVars, fieldVars));
		}
		
		return fixes;
	}
	
	private Map<Integer, ArrayList<FilterableFix>> generateFixInner(AbstractFixSite fixSite, Expression expr, Map<String, Set<String>> localVars, Map<String, Set<String>> fieldVars){
		
		// now we do the actual generation
		ExpressionVisitor visitor = new ExpressionVisitor();
		expr.accept(visitor);
		CompilationUnit cu = (CompilationUnit)expr.getRoot();
		
		Map<Integer, ArrayList<FilterableFix>> results = new HashMap<Integer, ArrayList<FilterableFix>>();
		
		Map<Expression, String> simpleNodeMap = visitor.getSimpleNodeTypeMap();
		for(Expression e: simpleNodeMap.keySet()){
			
			String qualifiedTypeName = simpleNodeMap.get(e);
			String simpleTypeName = qualifiedTypeName.substring(qualifiedTypeName.lastIndexOf('.') + 1);
			/*if(e.resolveTypeBinding().isEnum()){
				ITypeBinding tb = e.resolveTypeBinding();
				try {
					IType type = session.getProject().findType(tb.getQualifiedName());
					IField[] fields = type.getFields();
					for(int i = 0; i < fields.length; i ++){
						int fixLineNum = cu.getLineNumber(e.getStartPosition());
						String exprString = fields[i].getDeclaringType().getElementName() + "." + fields[i].getElementName();
						FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), exprString,  Fix.EXPR_CHANGE, fixLineNum, e, typeName, exprString);
						Filter.addFix(results, fix, FixerUtil.getLineNum(e));
					}
				} catch (JavaModelException e1) {
					e1.printStackTrace();
				}
				continue;
			}*/
			
			// see if e is local variable or field variable
			
			Set<String> exprList = new HashSet<String>();
			if(fieldVars.containsKey(qualifiedTypeName))
				exprList.addAll(fieldVars.get(qualifiedTypeName));
			if(fieldVars.containsKey(simpleTypeName))
				exprList.addAll(fieldVars.get(simpleTypeName));
			
			if(!exprList.contains(e.toString())){
				if(localVars.containsKey(qualifiedTypeName))
					exprList.addAll(localVars.get(qualifiedTypeName));
				if(localVars.containsKey(simpleTypeName))
					exprList.addAll(localVars.get(simpleTypeName));
			}
			
			if(exprList.size() == 0)
				continue;
			
			for(String exprString: exprList){
				if(e.toString().equals(exprString))
					continue;
				if(e.getParent() instanceof PrefixExpression || e.getParent() instanceof PostfixExpression 
						|| (e.getParent() instanceof Assignment && ((Assignment)e.getParent()).getLeftHandSide().equals(e))){
					if(!(exprString.contains("+") || exprString.contains("-") || exprString.contains("*")
							|| exprString.contains("/") || exprString.contains("%") || exprString.contains("++")
							|| exprString.contains("--") || exprString.contains("("))){
						int fixLineNum = cu.getLineNumber(e.getStartPosition());
						FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), exprString, Fix.EXPR_CHANGE, fixLineNum, e, qualifiedTypeName, exprString);
						Filter.addFix(results, fix, EclipseUtils.getLineNum(e));
					}
				} else {
					int fixLineNum = cu.getLineNumber(e.getStartPosition());
					FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), exprString,  Fix.EXPR_CHANGE, fixLineNum, e, qualifiedTypeName, exprString);
					Filter.addFix(results, fix, EclipseUtils.getLineNum(e));
				}
			}
		}
		
		
		Map<Expression, String> infixNodeMap = visitor.getInfixNodeTypeMap();
		for(Expression e: infixNodeMap.keySet()){
			InfixExpression infixExpr = (InfixExpression)e;
			String typeName = infixNodeMap.get(e);
			if(typeName.equals("boolean")){
				Expression outermostBooleanExpr = infixExpr;				
				ASTNode parentExpr = infixExpr;
				while(parentExpr instanceof Expression){
					ITypeBinding tb = ((Expression)parentExpr).resolveTypeBinding();
					if(tb.getQualifiedName().equals("boolean")){
						outermostBooleanExpr = (Expression)parentExpr;
					} else 
						break;
					parentExpr = parentExpr.getParent();
				}
				
				Operator op = infixExpr.getOperator();
				Expression left = infixExpr.getLeftOperand();
				Expression right = infixExpr.getRightOperand();
				int leftFracStartPos = outermostBooleanExpr.getStartPosition();
				int leftFracEndPos = left.getStartPosition() + left.getLength();
				int leftFracLength = leftFracEndPos - leftFracStartPos;
				
				int rightFracStartPos = right.getStartPosition();
				int rightFracEndPos = outermostBooleanExpr.getStartPosition() + outermostBooleanExpr.getLength();
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
					
				
				int infixFixLineNum = cu.getLineNumber(infixExpr.getStartPosition());
				if(op.equals(Operator.EQUALS) || op.equals(Operator.GREATER) || op.equals(Operator.GREATER_EQUALS)
						|| op.equals(Operator.LESS) || op.equals(Operator.LESS_EQUALS) ||op.equals(Operator.NOT_EQUALS)){
					
					if(!op.equals(Operator.EQUALS)){
						String modifiedExprString = left.toString() + "==" + right.toString();
						FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedExprString, Fix.EXPR_CHANGE, infixFixLineNum,
								outermostBooleanExpr, typeName, leftFrac + "==" + rightFrac);
						Filter.addFix(results, fix, EclipseUtils.getLineNum(e));
					}
					if(!op.equals(Operator.GREATER)){
						String modifiedExprString = left.toString() + ">" + right.toString();
						FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedExprString, Fix.EXPR_CHANGE, infixFixLineNum,
								outermostBooleanExpr, typeName, leftFrac + ">" + rightFrac);
						Filter.addFix(results, fix, EclipseUtils.getLineNum(e));
					}
					if(!op.equals(Operator.GREATER_EQUALS)){
						String modifiedExprString = left.toString() + ">=" + right.toString();
						FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedExprString, Fix.EXPR_CHANGE, infixFixLineNum,
								outermostBooleanExpr, typeName, leftFrac + ">=" + rightFrac);
						Filter.addFix(results, fix, EclipseUtils.getLineNum(e));
					}
					if(!op.equals(Operator.LESS)){
						String modifiedExprString = left.toString() + "<" + right.toString();
						FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedExprString, Fix.EXPR_CHANGE, infixFixLineNum,
								outermostBooleanExpr, typeName, leftFrac + "<" + rightFrac);
						Filter.addFix(results, fix, EclipseUtils.getLineNum(e));
					}
					if(!op.equals(Operator.LESS_EQUALS)){
						String modifiedExprString = left.toString() + "<=" + right.toString();
						FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedExprString, Fix.EXPR_CHANGE, infixFixLineNum,
								outermostBooleanExpr, typeName, leftFrac + "<=" + rightFrac);
						Filter.addFix(results, fix, EclipseUtils.getLineNum(e));
					}
					if(!op.equals(Operator.NOT_EQUALS)){
						String modifiedExprString = left.toString() + "!=" + right.toString();
						FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedExprString, Fix.EXPR_CHANGE, infixFixLineNum,
								outermostBooleanExpr, typeName, leftFrac + "!=" + rightFrac);
						Filter.addFix(results, fix, EclipseUtils.getLineNum(e));
					}
				}
				if(op.equals(Operator.CONDITIONAL_AND) || op.equals(Operator.CONDITIONAL_OR)){
					if(!op.equals(Operator.CONDITIONAL_AND)){
						String modifiedExprString = left.toString() + "||" + right.toString();
						FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedExprString, Fix.EXPR_CHANGE, infixFixLineNum,
								outermostBooleanExpr, typeName, leftFrac + "||" + rightFrac);
						Filter.addFix(results, fix, EclipseUtils.getLineNum(e));
					}
					if(!op.equals(Operator.CONDITIONAL_OR)){
						String modifiedExprString = left.toString() + "&&" + right.toString();
						FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedExprString, Fix.EXPR_CHANGE, infixFixLineNum,
								outermostBooleanExpr, typeName, leftFrac + "&&" + rightFrac);
						Filter.addFix(results, fix, EclipseUtils.getLineNum(e));
					}
				}
				
				if(outermostBooleanExpr.equals(infixExpr)){
					// this is the outermost boolean expression, so we add ||() and &&()
					
					ArrayList<String> appendPads = session.getExpressionGenerator().genBooleanAppendPart(outermostBooleanExpr);
					for(String boolExpr: appendPads){
						String modifiedString = infixExpr.toString() + "||" + boolExpr;
						FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedString, Fix.EXPR_CHANGE, EclipseUtils.getLineNum(e),
								e, typeName, modifiedString);
						Filter.addFix(results, fix, EclipseUtils.getLineNum(e));
						
						modifiedString = infixExpr.toString() + "&&" + boolExpr;
						fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedString, Fix.EXPR_CHANGE, EclipseUtils.getLineNum(e),
								e, typeName, modifiedString);
						Filter.addFix(results, fix, EclipseUtils.getLineNum(e));
					}
				}
				
			}
		}
	
		/*
		Map<Expression, String> prefixNodeMap = visitor.getPrefixNodeTypeMap();
		for(Expression e: prefixNodeMap.keySet()){
			String typeName = prefixNodeMap.get(e);
			PrefixExpression prefixExpr = (PrefixExpression)e;
			PrefixExpression.Operator op = prefixExpr.getOperator();
			Expression operand = prefixExpr.getOperand();
			if(op.equals(PrefixExpression.Operator.INCREMENT)){
				String modifiedString = "--" + operand.toString();
				FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedString, Fix.EXPR_CHANGE, FixerUtil.getLineNum(e), e, typeName, modifiedString);
				Filter.addFix(results, fix, FixerUtil.getLineNum(e));
			} else if(op.equals(PrefixExpression.Operator.DECREMENT)){
				String modifiedString = "++" + operand.toString();
				FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedString, Fix.EXPR_CHANGE, FixerUtil.getLineNum(e), e, typeName, modifiedString);
				Filter.addFix(results, fix, FixerUtil.getLineNum(e));
			} else if(op.equals(PrefixExpression.Operator.NOT)){
				String modifiedString = operand.toString();
				FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedString, Fix.EXPR_CHANGE, FixerUtil.getLineNum(e), e, typeName, modifiedString);
				Filter.addFix(results, fix, FixerUtil.getLineNum(e));
			} else{
				if(!op.equals(PrefixExpression.Operator.COMPLEMENT)){
					String modifiedString = "~" + operand.toString();
					FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedString, Fix.EXPR_CHANGE, FixerUtil.getLineNum(e), e, typeName, modifiedString);
					Filter.addFix(results, fix, FixerUtil.getLineNum(e));
				}
				if(!op.equals(PrefixExpression.Operator.MINUS)){
					String modifiedString = "-" + operand.toString();
					FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedString, Fix.EXPR_CHANGE, FixerUtil.getLineNum(e), e, typeName, modifiedString);
					Filter.addFix(results, fix, FixerUtil.getLineNum(e));
				}
				if(!op.equals(PrefixExpression.Operator.PLUS)){
					String modifiedString = "+" + operand.toString();
					FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedString, Fix.EXPR_CHANGE, FixerUtil.getLineNum(e), e, typeName, modifiedString);
					Filter.addFix(results, fix, FixerUtil.getLineNum(e));
				}
			}
		}
		
		Map <Expression, String> postfixNodeMap = visitor.getPostfixNodeTypeMap();
		for(Expression e: postfixNodeMap.keySet()){
			String typeName = postfixNodeMap.get(e);
			PostfixExpression postfixExpr = (PostfixExpression)e;
			PostfixExpression.Operator op = postfixExpr.getOperator();
			Expression operand = postfixExpr.getOperand();
			if(op.equals(PostfixExpression.Operator.DECREMENT)){
				String modifiedString = operand.toString() + "++";
				FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedString, Fix.EXPR_CHANGE, FixerUtil.getLineNum(e), e, typeName, modifiedString);
				Filter.addFix(results, fix, FixerUtil.getLineNum(e));
			}
			if(op.equals(PostfixExpression.Operator.INCREMENT)){
				String modifiedString = operand.toString() + "--";
				FilterableFix fix = new FilterableFix(fixSite, e.getStartPosition(), e.getLength(), modifiedString, Fix.EXPR_CHANGE, FixerUtil.getLineNum(e), e, typeName, modifiedString);
				Filter.addFix(results, fix, FixerUtil.getLineNum(e));
			}
		}
		*/
		return results;

	}
	
}




