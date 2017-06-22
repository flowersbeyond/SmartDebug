package cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSiteManager;

public class GnrFixSiteManager extends AbstractFixSiteManager {
	
	private HashMap<String, ArrayList<AbstractFixSite>> fixSiteCache = new HashMap<String, ArrayList<AbstractFixSite>>();
	private HashMap<String, TypeDeclaration> classDeclASTCache = new HashMap<String, TypeDeclaration>();
	
	private BugFixSession session;
	public GnrFixSiteManager(BugFixSession session){
		this.session = session;
	}
	
	public TypeDeclaration getTypeDeclarationFromClassName(String className){
		TypeDeclaration td = classDeclASTCache.get(className);
		if(td == null){
			try{
				IJavaProject project = session.getProject();
				IType type = project.findType(className);
				ICompilationUnit icu = type.getCompilationUnit();
		
				td = getTypeDeclaration(icu, className);
			} catch(JavaModelException e){
				e.printStackTrace();
			}
		}
		return td;
	}
	
	public ArrayList<AbstractFixSite> getFixSitesFromLocation(String methodKey,
			int startLineNum, int endLineNum) {
		ArrayList<AbstractFixSite> fixSites = getFixSitesForMethod(methodKey, startLineNum, endLineNum);
		ArrayList<AbstractFixSite> coveringSites = new ArrayList<AbstractFixSite>();
		//TODO:try this out?
		if(fixSites != null){
			for(AbstractFixSite fxst:fixSites){
				if(fxst == null){
					continue;
				}
				if(fxst.coverLineNum(startLineNum, endLineNum)){
					coveringSites.add(fxst);
				}
			}
		}
		return coveringSites;
	}
	
	private ArrayList<AbstractFixSite> getFixSitesForMethod(String methodKey, int startLineNum, int endLineNum){
		ArrayList<AbstractFixSite> fixSites = fixSiteCache.get(methodKey);
		if(fixSites == null){
			fixSites = resolveFixSites(methodKey, startLineNum, endLineNum);
			fixSiteCache.put(methodKey, fixSites);
		}
		return fixSites;
	}
	
	public boolean contains(String methodKey){
		return !(fixSiteCache.get(methodKey) == null);
	}
	
	
	private TypeDeclaration getTypeDeclaration(ICompilationUnit icu, String className){
		TypeDeclaration td = classDeclASTCache.get(className);
		if(td == null){
			ASTParser parser = ASTParser.newParser(AST.JLS8);  // handles JDK 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6
			//TODO: check this out
			parser.setSource(icu);
			parser.setResolveBindings(true);
			CompilationUnit cu = (CompilationUnit) parser.createAST(null);
			TypeDeclarationCollector collector = new TypeDeclarationCollector();
			cu.accept(collector);
			HashMap<String, TypeDeclaration> typeDeclMap = collector.getTypeDeclarations();
			classDeclASTCache.putAll(typeDeclMap);
			for(String typeName: typeDeclMap.keySet() ){
				TypeDeclaration typeDecl = typeDeclMap.get(typeName);
				if(className.equals(typeName)){
					td = typeDecl;
					break;
				}
			}
		}
		return td;
	}
	private ArrayList<AbstractFixSite> resolveFixSites(String methodKey, int startLineNum, int endLineNum){
		
		try {
			String[] methodInfo = methodKey.split(":");
			String className = methodInfo[0].replaceAll("/", ".");
			className = className.replaceAll("\\$", ".");
			String methodName = methodInfo[1];
			String methodSignature = methodInfo[2];
			
			IJavaProject project = session.getProject();
			IType type = project.findType(className);
			ICompilationUnit icu = type.getCompilationUnit();

			TypeDeclaration td = getTypeDeclaration(icu, className);
			if(td == null){
				return null;
			}
			
			MethodDeclaration mds[] = td.getMethods();
			MethodDeclaration targetMethod = null;
			for(MethodDeclaration md: mds){
				IMethodBinding mb = md.resolveBinding();
				String mName = mb.getName();
				if(!(methodName.equals(mName) || (methodName.equals("<init>") && mName.equals(td.getName()))))
					continue;
				if(methodName.equals("<init>") && mName.equals(td.getName())){
					targetMethod = md;
					break;
				}
				CompilationUnit cu = (CompilationUnit)(md.getRoot());
				int methodStartLine = cu.getLineNumber(md.getStartPosition());
				int methodEndLine = cu.getLineNumber(md.getStartPosition() + md.getLength());
				if(methodStartLine <= startLineNum && methodEndLine >= endLineNum){
					targetMethod = md;
					break;
				}
//				ITypeBinding[] paramTbs = mb.getParameterTypes();
//				ITypeBinding returnTb = mb.getReturnType();
//				String[] paramTypeStrs = new String[paramTbs.length];
//				for(int i = 0; i < paramTbs.length; i ++){
//					paramTypeStrs[i] = Signature.createTypeSignature(paramTbs[i].getQualifiedName(), true);
//				}
//				String sig = Signature.createMethodSignature(paramTypeStrs, Signature.createTypeSignature(returnTb.getQualifiedName(), true));
//				sig = sig.replaceAll("\\.", "/");
//				if(sig.equals(methodSignature)){
//					targetMethod = md;
//					break;
//				}
			}
			if(targetMethod == null){
				//something must have been wrong here.
				return null;
			}
			IFile file = (IFile) icu.getUnderlyingResource();
			Block methodBody = targetMethod.getBody();
			FixSiteCollector collector = new FixSiteCollector(file, className);
			methodBody.accept(collector);
			ArrayList<AbstractFixSite> fixSites = collector.getFixSites();
					
			//for constructors we need to add dynamic field declaration & dynamic initializers			
			if(methodName.equals("<init>")){
				List<BodyDeclaration> bodyDecls = td.bodyDeclarations();
				for(BodyDeclaration bd: bodyDecls){
					if(bd instanceof Initializer){
						FixSiteCollector initCol = new FixSiteCollector(file, className);
						((Initializer) bd).getBody().accept(initCol);
						fixSites.addAll(initCol.getFixSites());
					}
					else if(bd instanceof FieldDeclaration){
						//TODO: temporarily we believe that field declarations are always correct.
					}
				}
			}
			
			return fixSites;
			
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	class TypeDeclarationCollector extends ASTVisitor{
		private HashMap<String, TypeDeclaration> typeDeclMap = new HashMap<String, TypeDeclaration>();
		
		public HashMap<String, TypeDeclaration> getTypeDeclarations(){
			return typeDeclMap;
		}
		
		public boolean visit(TypeDeclaration node){
			ITypeBinding typeBinding = node.resolveBinding();
			String qualifiedName = typeBinding.getQualifiedName();
			typeDeclMap.put(qualifiedName, node);
			return true;
		}
	}

	@Override
	public void clearCache() {
		this.classDeclASTCache.clear();
		this.fixSiteCache.clear();		
	}
}

class FixSiteCollector extends ASTVisitor{
	private ArrayList<AbstractFixSite> fixSites = new ArrayList<AbstractFixSite>();
	private IFile file;
	private String qualifiedTypeName;

	public FixSiteCollector(IFile file, String qualifiedTypeName){
		this.file = file;
		this.qualifiedTypeName = qualifiedTypeName;
	}
	
	public ArrayList<AbstractFixSite> getFixSites(){
		return fixSites;
	}
	
	private InsertStopFixSite generateInsertStopFixSite(Statement stmt){
		
		ASTNode root = stmt.getRoot();
		if(root instanceof CompilationUnit){
			int lineStartNum = ((CompilationUnit) root).getLineNumber(stmt.getStartPosition());
			int lineEndNum = ((CompilationUnit) root).getLineNumber(stmt.getStartPosition() + stmt.getLength());
			InsertStopFixSite isFixSite = new InsertStopFixSite(file, qualifiedTypeName, lineStartNum, lineEndNum, stmt);
			return isFixSite;
		}
		
		return null;
	}
	private StatementFixSite generateStatementFixSite(
			ArrayList<Statement> collection) {
		if(collection.isEmpty())
			return null;
		Statement stmtfirst = collection.get(0);
		ASTNode root = stmtfirst.getRoot();
		if(root instanceof CompilationUnit){
			int lineStartNum = ((CompilationUnit) root).getLineNumber(stmtfirst.getStartPosition());
			Statement stmtend = collection.get(collection.size() - 1);
			int lineEndNum = ((CompilationUnit) root).getLineNumber(stmtend.getStartPosition() + stmtend.getLength());
			StatementFixSite sFixSite = new StatementFixSite(file, qualifiedTypeName, lineStartNum, lineEndNum, collection);
			return sFixSite;
		}
		return null;
	}
	
	private ArrayList<AbstractFixSite> generateSubFixSite(ArrayList<Statement> collection){
		if(collection.isEmpty())
			return new ArrayList<AbstractFixSite>();
		ArrayList<AbstractFixSite> results = new ArrayList<AbstractFixSite>();
		for(Statement stmt:collection){
			ArrayList<AbstractFixSite> fixsites = visitGeneralStatement(stmt);
			results.addAll(fixsites);
		}
		return results;
	}
	private ConditionFixSite generateConditionFixSite(Expression condExpr, Statement wrappingStmt, boolean switchCondition){
		
		ASTNode root = wrappingStmt.getRoot();
		if(root instanceof CompilationUnit){
			Expression realCondExpr = condExpr;
			if(switchCondition){
				if(!(condExpr instanceof InfixExpression)){
					// this is impossible since switchconditions are manually constructed
					// to be infix expressions
					return null; 
				}
				realCondExpr = ((InfixExpression)condExpr).getRightOperand();
			}
			int lineStartNum = ((CompilationUnit) root).getLineNumber(realCondExpr.getStartPosition());
			int lineEndNum = ((CompilationUnit) root).getLineNumber(realCondExpr.getStartPosition() + realCondExpr.getLength());
			
			ConditionFixSite condFixSite = new ConditionFixSite(file, qualifiedTypeName, lineStartNum, lineEndNum, condExpr, wrappingStmt, switchCondition);
			return condFixSite;
		}
		return null;
	}

	private boolean isSimpleStatement(Statement stmt){
		int stmtType = stmt.getNodeType();
		switch(stmtType){
		case ASTNode.BLOCK:
		case ASTNode.DO_STATEMENT:
		case ASTNode.ENHANCED_FOR_STATEMENT:
		case ASTNode.FOR_STATEMENT:
		case ASTNode.IF_STATEMENT:
		case ASTNode.SWITCH_CASE:
		case ASTNode.SWITCH_STATEMENT:
		case ASTNode.SYNCHRONIZED_STATEMENT:
		case ASTNode.TRY_STATEMENT:
		case ASTNode.TYPE_DECLARATION_STATEMENT:
		case ASTNode.WHILE_STATEMENT:
		case ASTNode.LABELED_STATEMENT:
			return false;
			default:
				return true;
		}
	}
	private ArrayList<AbstractFixSite> visitGeneralStatement(Statement stmt) {
		
		ArrayList<AbstractFixSite> stmtFixSites = new ArrayList<AbstractFixSite>();
		if(isSimpleStatement(stmt)){
			ArrayList<Statement> stmtList = new ArrayList<Statement>();
			stmtList.add(stmt);
			StatementFixSite sfixSite = generateStatementFixSite(stmtList);
			stmtFixSites.add(sfixSite);
			return stmtFixSites;
		} else {
			return visitCompoundStatement(stmt);
		}
	}
	private ArrayList<AbstractFixSite> visitCompoundStatement(Statement stmt) {
		if(stmt instanceof LabeledStatement){
			Statement innerStmt = ((LabeledStatement) stmt).getBody();
			FixSiteCollector collector = new FixSiteCollector(file, qualifiedTypeName);
			innerStmt.accept(collector);
			return collector.getFixSites();
		}
		ArrayList<AbstractFixSite> fixSites = new ArrayList<AbstractFixSite>();
		int stmtType = stmt.getNodeType();
		switch(stmtType){
			case ASTNode.DO_STATEMENT:
			case ASTNode.ENHANCED_FOR_STATEMENT:
			case ASTNode.FOR_STATEMENT:
			case ASTNode.IF_STATEMENT:	
			case ASTNode.SWITCH_STATEMENT:	
			case ASTNode.WHILE_STATEMENT:{
				InsertStopFixSite isfixsite = generateInsertStopFixSite(stmt);
				if(isfixsite != null)
					fixSites.add(isfixsite);
			}
		}
		
		
		FixSiteCollector collector = new FixSiteCollector(file, qualifiedTypeName);
		stmt.accept(collector);
		fixSites.addAll(collector.getFixSites());
		return fixSites;
	}
	@Override
	public boolean visit(Block node) {
		List<Statement> stmts = node.statements();
		for(int i = 0; i < stmts.size(); i ++){
			Statement stmt = stmts.get(i);
			if(isSimpleStatement(stmt)){
				ArrayList<Statement> collection = new ArrayList<Statement>();
				for(; i < stmts.size(); i ++){
					Statement currentstmt = stmts.get(i);
					if(isSimpleStatement(currentstmt))
						collection.add(currentstmt);
					else
						break;
				}
				i --; // i should point to the last statement we visited.
				
				StatementFixSite sFixSite = generateStatementFixSite(collection);
				fixSites.add(sFixSite);
				
			} else {
				ArrayList<AbstractFixSite> fss = visitCompoundStatement(stmt);
				fixSites.addAll(fss);
			}
			
		}
		return false;
	}
	@Override
	public boolean visit(DoStatement node) {
		Expression expr = node.getExpression();
		ConditionFixSite cFixSite = generateConditionFixSite(expr, node, false);
		fixSites.add(cFixSite);
		
		Statement body = node.getBody();
		ArrayList<AbstractFixSite> sfixSites = visitGeneralStatement(body);
		fixSites.addAll(sfixSites);
		
		return false;
	}
	
	@Override
	public boolean visit(EnhancedForStatement node) {
		Expression expr = node.getExpression();
		ConditionFixSite cFixSite = generateConditionFixSite(expr, node, false);
		fixSites.add(cFixSite);
		
		Statement body = node.getBody();
		ArrayList<AbstractFixSite> sfixSites = visitGeneralStatement(body);
		fixSites.addAll(sfixSites);
		
		return false;
	}
	
	@Override
	public boolean visit(ForStatement node) {
		Expression expr = node.getExpression();
		ConditionFixSite cFixSite = generateConditionFixSite(expr, node, false);
		fixSites.add(cFixSite);
		
		Statement body = node.getBody();
		ArrayList<AbstractFixSite> sfixSites = visitGeneralStatement(body);
		fixSites.addAll(sfixSites);
		
		return false;
	}
	
	@Override
	public boolean visit(IfStatement node) {
		Expression expr = node.getExpression();
		ConditionFixSite cFixSite = generateConditionFixSite(expr, node, false);
		fixSites.add(cFixSite);
		
		Statement thenbody = node.getThenStatement();
		ArrayList<AbstractFixSite> thenfixSites = visitGeneralStatement(thenbody);
		fixSites.addAll(thenfixSites);
		
		Statement elsebody = node.getElseStatement();
		if(elsebody != null){
			ArrayList<AbstractFixSite> elsefixSites = visitGeneralStatement(elsebody);
			fixSites.addAll(elsefixSites);
		}
		return false;
	}
	
	/**
	 * switch cases are handled inside switchstatements
	 * @param node
	 * @return
	 */
	@Override
	public boolean visit(SwitchCase node) {
		return false;
	}
	
	@Override
	public boolean visit(SwitchStatement node) {
		
		List<Statement> stmts= node.statements(); 
		
		// SwitchCase nodes mark the start of the switch groups.
		for(int i = 0; i < stmts.size(); i ++){
			Statement switchstart = stmts.get(i);
			if(switchstart instanceof SwitchCase){
				ArrayList<Statement> group = new ArrayList<Statement>();
				for(i = i + 1 ; i < stmts.size(); i ++){
					Statement currentstmt = stmts.get(i);
					if(currentstmt instanceof SwitchCase)
						break;
					else
						group.add(currentstmt);
				}
				i --; //i should always point to the last statement we visited.
				
				ArrayList<AbstractFixSite> switchCaseFixSites
					= visitSwitchCaseGroup(node, (SwitchCase) switchstart, group);
				fixSites.addAll(switchCaseFixSites);
			}
		}
		
		return true;
	}
	
	private ArrayList<AbstractFixSite> visitSwitchCaseGroup(SwitchStatement node, SwitchCase switchstart,
			ArrayList<Statement> group) {
		
		ArrayList<AbstractFixSite> switchfixSites = new ArrayList<AbstractFixSite>();
		Expression switchexpr = node.getExpression();
		Expression caseexpr = switchstart.getExpression();
		if(caseexpr != null){		
			AST ast = node.getAST();
			InfixExpression condExpr = ast.newInfixExpression();
			condExpr.setOperator(InfixExpression.Operator.EQUALS);
			Expression switchexprCopy = (Expression)ASTNode.copySubtree(switchexpr.getAST(), switchexpr);
			Expression caseexprCopy = (Expression)ASTNode.copySubtree(caseexpr.getAST(), caseexpr);
			condExpr.setLeftOperand(switchexprCopy);
			condExpr.setRightOperand(caseexprCopy);
			AbstractFixSite condFixSite = generateConditionFixSite(condExpr, switchstart, true);
			switchfixSites.add(condFixSite);
		}
		
		ArrayList<AbstractFixSite> fixSites = generateSubFixSite(group);
		switchfixSites.addAll(fixSites);
		
		
		return switchfixSites;
	}
	@Override
	public boolean visit(SynchronizedStatement node) {
		ArrayList<AbstractFixSite> sfixSites = visitGeneralStatement(node.getBody());
		fixSites.addAll(sfixSites);
		return false;
	}
	
	@Override
	public boolean visit(ThrowStatement node){
		return false;
	}
	
	@Override
	public boolean visit(TryStatement node) {
		ArrayList<AbstractFixSite> tryfixSites = visitGeneralStatement(node.getBody());
		fixSites.addAll(tryfixSites);
		
		if(node.getFinally() != null){
			ArrayList<AbstractFixSite> finallyfixSites = visitGeneralStatement(node.getFinally());
			fixSites.addAll(finallyfixSites);
		}
		return false;
	}
	
	
	/**
	 * We temporarily ignore inner classes
	 * @param node
	 * @return
	 */
	@Override
	public boolean visit(TypeDeclarationStatement node) {
		return false;
	}
	
	@Override
	public boolean visit(WhileStatement node) {
		Expression expr = node.getExpression();
		ConditionFixSite cFixSite = generateConditionFixSite(expr, node, false);
		fixSites.add(cFixSite);
		
		Statement body = node.getBody();
		ArrayList<AbstractFixSite> sfixSites = visitGeneralStatement(body);
		fixSites.addAll(sfixSites);
		return false;
	}
}