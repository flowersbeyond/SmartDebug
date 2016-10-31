package cn.edu.thu.tsmart.tool.da.core.search.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaBreakpointListener;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.core.breakpoints.ValidBreakpointLocationLocator;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.Logger;
import cn.edu.thu.tsmart.tool.da.core.search.fixSite.ConditionFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.fixSite.FixSite;
import cn.edu.thu.tsmart.tool.da.core.search.fixSite.InsertStopFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.fixSite.StatementFixSite;
import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;

public class ExpressionRepositoryManager {
	
	private Map<FixSite, ExpressionRepository> exprRepoCache = new HashMap<FixSite, ExpressionRepository>();
	private BugFixSession session;
	//private MyExpressionGenerator exprGenerator;
	
	public ExpressionRepositoryManager(BugFixSession session){
		this.session = session;
	}
	
	public ExpressionRepository getExpressionRepository(FixSite fixSite, TestCase coveringTestCase){
		if(exprRepoCache.containsKey(fixSite))
			return exprRepoCache.get(fixSite);
		else{
			session.getLogger().log(Logger.DATA_MODE, Logger.GEN_EXPR_REPO_START, "");
			ExpressionRepository exprRepo = genExpressionRepository(fixSite, coveringTestCase);
			if(exprRepo != null && exprRepo.getExprRepoMap() != null){
				session.getLogger().log(Logger.DATA_MODE, Logger.GEN_EXPR_REPO_FINISHED, exprRepo.getRepoSize() + "");
				Map<String, Set<String>> repo = exprRepo.getExprRepoMap();
				if(repo.containsKey("boolean")){
					Set<String> boolExprs = repo.get("boolean");
					if(boolExprs != null){
						List<String> notExprs = new ArrayList<String>();
						for(String expr: boolExprs){
							if(!expr.startsWith("!")){
								String notExpr1 = "!" + expr;
								String notExpr2 = "! " + expr;
								if(!boolExprs.contains(notExpr1) && !boolExprs.contains(notExpr2))
									notExprs.add(notExpr1);
							}
						}
						boolExprs.addAll(notExprs);
					}
				}
				exprRepoCache.put(fixSite, exprRepo);
				return exprRepo;
			} else {
				exprRepoCache.put(fixSite, null);
				session.getLogger().log(Logger.DATA_MODE, Logger.GEN_EXPR_REPO_FINISHED, "NULL");
				return null;
			}
		}
	}
	
	private ExpressionRepository genExpressionRepository(FixSite fixSite, TestCase coveringTestCase){
		ASTNode stmt = null;
		ArrayList<String> typeNames = new ArrayList<String>();
		if(fixSite instanceof StatementFixSite){
			List<ASTNode> stmts = (List<ASTNode>) ((StatementFixSite)fixSite).getStatements();
			
			// Get the expression repository at the last statement for this block. Here we have the 
			// biggest expression repository available. Some of them may not be applicable
			// for prior statements, since some of the local variables may not have been defined yet.
			// However we pass this validating work to the validating process.
			stmt = stmts.get(stmts.size() - 1);
			
			//get all types needed in one operation here:
			for(ASTNode node: stmts){
				ExpressionVisitor visitor = new ExpressionVisitor();
				node.accept(visitor);
				for(String typeName: visitor.getAllTypes()){
					if(!typeNames.contains(typeName)){
						typeNames.add(typeName);
					}
				}
			}

		} else if(fixSite instanceof ConditionFixSite){
			stmt = ((ConditionFixSite) fixSite).getWrappingStatement();
			ASTNode condExpr = ((ConditionFixSite) fixSite).getConditionExpression();
			ExpressionVisitor visitor = new ExpressionVisitor();
			condExpr.accept(visitor);
			for(String typeName: visitor.getAllTypes()){
				typeNames.add(typeName);
			}
			
		} else if(fixSite instanceof InsertStopFixSite){
			stmt = ((InsertStopFixSite)fixSite).getStatement();
			ASTNode node = stmt;
			while(!(node instanceof MethodDeclaration)){
				node = node.getParent();
				if(node instanceof CompilationUnit)
					break;
			}
			if(node instanceof MethodDeclaration){
				ITypeBinding tb = ((MethodDeclaration) node).resolveBinding().getReturnType();
				if(tb!= null && !tb.getQualifiedName().equals("void")){
					typeNames.add(tb.getQualifiedName());
				}
			}
			
			//This is used for synthesizing boolean expressions,
			//for example if(boolean){...}
			if(!typeNames.contains("boolean"))
				typeNames.add("boolean");
			
		}
		
		if(stmt == null)
			return null;
		
		if(!typeNames.contains("boolean"))
			typeNames.add("boolean");
		
		ASTNode node = stmt.getRoot();
		if(node instanceof CompilationUnit){
			CompilationUnit cu = (CompilationUnit)node;
			
			ValidBreakpointLocationLocator locator = new ValidBreakpointLocationLocator(
					cu, cu.getLineNumber(stmt.getStartPosition()), false, true);
			cu.accept(locator);
			int realLineNum = locator.getLineLocation();
			String typeName = locator.getFullyQualifiedTypeName();
			if(realLineNum == 0 || typeName == null)
				return null;

			ICompilationUnit unit = (ICompilationUnit)cu.getJavaElement();
			try {
				Object breakpointMutualLock = session.getBreakpointMutualLock();
				synchronized(breakpointMutualLock){
					
					IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
					IBreakpoint[] bps = manager.getBreakpoints();
					
					ArrayList<IBreakpoint> enabledbps = new ArrayList<IBreakpoint>();
					for(IBreakpoint bp0: bps){
						if(bp0.isEnabled()){
							enabledbps.add(bp0);
							bp0.setEnabled(false);
						}
					}
					
					IJavaLineBreakpoint bp = JDIDebugModel.createLineBreakpoint(
							unit.getResource(), typeName, cu.getLineNumber(stmt.getStartPosition()), 
							stmt.getStartPosition(), stmt.getStartPosition() + stmt.getLength(),
							0, false, null);
					
					manager.addBreakpoint(bp);
					bp.setEnabled(true);
					
					Object lock = new Object();
					GenRepoBreakpointListener genRepoListener = new GenRepoBreakpointListener(lock, session);
					JDIDebugModel.addJavaBreakpointListener(genRepoListener);
					ILaunchConfiguration config = session.findLaunchConfiguration(coveringTestCase);
					
					Timer timer = new Timer();
					ExpressionGeneratorTimeoutTask timeoutTask = new ExpressionGeneratorTimeoutTask(lock);
					ILaunch launch = null;
					synchronized(lock){
						System.out.println("start launching");
						launch = config.launch(ILaunchManager.DEBUG_MODE, new NullProgressMonitor());
						//timer.schedule(timeoutTask , 10000);			
						System.out.println("launch started");
						lock.wait();
					}
					
					if(!timeoutTask.validateTimeOut()){
						timeoutTask.cancel();
					} else if(launch != null) {
						launch.terminate();
					}
					
					bp.setEnabled(false);
					manager.removeBreakpoint(bp, true);
					/*IMarker marker = bp.getMarker();
					marker.delete();*/
					
					for(IBreakpoint bp0: enabledbps){
						bp0.setEnabled(true);
					}
					//manager.addBreakpoints(bps);
					JDIDebugModel.removeJavaBreakpointListener(genRepoListener);
					
					Map<String, Set<String>> exprRepoMap = genRepoListener.getExpressionRepository();
					ExpressionRepository exprRepo = new ExpressionRepository(exprRepoMap);
					return exprRepo;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();				
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return null;
		
	}
}

class GenRepoBreakpointListener implements IJavaBreakpointListener{
	
	private Map<String, Set<String>> exprRepository;
	private Object lock;
	private BugFixSession session;
	
	public GenRepoBreakpointListener(Object lock, BugFixSession session){
		this.lock = lock;
		this.session = session;
	}
	
	public Map<String, Set<String>> getExpressionRepository(){
		return this.exprRepository;
	}
	
	@Override
	public void addingBreakpoint(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint) {							
	}

	@Override
	public int installingBreakpoint(
			IJavaDebugTarget target,
			IJavaBreakpoint breakpoint, IJavaType type) {
		return 0;
	}

	@Override
	public void breakpointInstalled(
			IJavaDebugTarget target,
			IJavaBreakpoint breakpoint) {							
	}

	@Override
	public int breakpointHit(IJavaThread thread,
			IJavaBreakpoint breakpoint) {
		/*try {
			IJavaStackFrame frame = (IJavaStackFrame) thread.getTopStackFrame();
			MyExpressionGenerator generator = session.getExpressionGenerator();
			
			// TODO: the return type of this result?
			this.exprRepository = generator.genExprRepo(frame);
			
						
			thread.getLaunch().terminate();
			synchronized(lock){
				lock.notifyAll();
			}
		} catch (DebugException e) {
			e.printStackTrace();
		}*/
		return 0;
	}

	@Override
	public void breakpointRemoved(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint) {							
	}

	@Override
	public void breakpointHasRuntimeException(
			IJavaLineBreakpoint breakpoint,
			DebugException exception) {							
	}

	@Override
	public void breakpointHasCompilationErrors(
			IJavaLineBreakpoint breakpoint, Message[] errors) {
	}

}

class ExpressionGeneratorTimeoutTask extends TimerTask{
	Object lock;
	boolean timeout = false;
	public ExpressionGeneratorTimeoutTask(Object lock){
		this.lock = lock;
	}

	public boolean validateTimeOut(){
		return timeout;
	}
	@Override
	public void run() {
		timeout = true;
		synchronized(lock){
			lock.notifyAll();
		}
	}
	
}

class ExpressionVisitor extends ASTVisitor{
	//private Map<Expression, String> nodeTypeMap = new HashMap<Expression, String>();
	
	private Map<Expression, String> simpleNodeTypeMap = new HashMap<Expression, String>();
	private Map<Expression, String> prefixNodeTypeMap = new HashMap<Expression, String>();
	private Map<Expression, String> infixNodeTypeMap = new HashMap<Expression, String>();
	private Map<Expression, String> postfixNodeTypeMap = new HashMap<Expression, String>();
	private Map<Expression, String> methodInvocNodeTypeMap = new HashMap<Expression, String>();
	
	public Map<Expression, String> getSimpleNodeTypeMap() {
		return simpleNodeTypeMap;
	}

	public Map<Expression, String> getPrefixNodeTypeMap() {
		return prefixNodeTypeMap;
	}

	public Map<Expression, String> getInfixNodeTypeMap() {
		return infixNodeTypeMap;
	}

	public Map<Expression, String> getPostfixNodeTypeMap() {
		return postfixNodeTypeMap;
	}

	public Map<Expression, String> getMethodInvocNodeTypeMap() {
		return methodInvocNodeTypeMap;
	}
	
	
	protected void merge(ExpressionVisitor other){
		mergeNodeTypeMap(simpleNodeTypeMap, other.simpleNodeTypeMap);
		mergeNodeTypeMap(prefixNodeTypeMap, other.prefixNodeTypeMap);
		mergeNodeTypeMap(infixNodeTypeMap, other.infixNodeTypeMap);
		mergeNodeTypeMap(postfixNodeTypeMap, other.postfixNodeTypeMap);
		mergeNodeTypeMap(methodInvocNodeTypeMap, other.methodInvocNodeTypeMap);
		
	}
	
	protected void mergeNodeTypeMap (Map<Expression, String> map1, Map<Expression, String> map2){
		for(Expression key: map2.keySet()){
			map1.put(key, map2.get(key));
		}
	}
	
	
	/*public Map<Expression, String> getNodeTypeMap() {
		return nodeTypeMap;
	}*/
	

	
	public ArrayList<String> getAllTypes(){
		ArrayList<String> allTypes = new ArrayList<String>();
		for(String type: simpleNodeTypeMap.values())
			if(!allTypes.contains(type))
				allTypes.add(type);
		for(String type: prefixNodeTypeMap.values())
			if(!allTypes.contains(type))
				allTypes.add(type);
		for(String type: infixNodeTypeMap.values())
			if(!allTypes.contains(type))
				allTypes.add(type);
		for(String type: postfixNodeTypeMap.values())
			if(!allTypes.contains(type))
				allTypes.add(type);
		for(String type: methodInvocNodeTypeMap.values())
			if(!allTypes.contains(type))
				allTypes.add(type);
		
		return allTypes;
	}
	
	
	@Override
	public boolean visit(ArrayAccess node){
		Expression array = node.getArray();
		Expression dimension = node.getIndex();
		if(array != null){
			ExpressionVisitor visitor = new ExpressionVisitor();
			array.accept(visitor);
			this.merge(visitor);
		}
		if(dimension != null){
			ExpressionVisitor visitor = new ExpressionVisitor();
			dimension.accept(visitor);
			this.merge(visitor);
		}
		
		return false;
	}
	
	private String getExpressionTypeName(Expression node){
		ITypeBinding tb = node.resolveTypeBinding();
		if(tb == null)
			return null;
		String qualifiedTypeName = tb.getQualifiedName();
		if(qualifiedTypeName.indexOf("<") != -1){ //generics are not considered, since code hint does not support them....
			return null;
		}
		if(qualifiedTypeName == null || qualifiedTypeName.equals("") || qualifiedTypeName.equals("null")){
			System.out.println(node.toString());
			return null;
		}
		if(!qualifiedTypeName.equals("void"))
			return qualifiedTypeName;
		return null;
	}

	@Override
	public boolean visit(ArrayCreation node){
		
		ArrayInitializer init = node.getInitializer();
		if(init != null){
			ExpressionVisitor visitor = new ExpressionVisitor();
			init.accept(visitor);
			this.merge(visitor);
		}
		
		return false;
	}

	

	@Override
	public boolean visit(ArrayInitializer node){
		List<Expression> exprs = node.expressions();
		if(exprs != null){
			for(Expression expr: exprs){
				ExpressionVisitor visitor = new ExpressionVisitor();
				expr.accept(visitor);
				this.merge(visitor);
			}
		}
		return false;
	}
	
	
	@Override
	public boolean visit(Assignment node){
		Expression rhs = node.getRightHandSide();
		if(rhs != null){
			ExpressionVisitor visitor = new ExpressionVisitor();
			rhs.accept(visitor);
			this.merge(visitor);
		}
		return false;
	}
	
	@Override
	public boolean visit(BooleanLiteral node){
		String typeName = getExpressionTypeName(node);
		if(typeName != null)
		simpleNodeTypeMap.put(node, typeName);
		return true;
	}
	
	@Override
	public boolean visit(CastExpression node){
		Expression expr = node.getExpression();
		if(expr != null){
			ExpressionVisitor visitor = new ExpressionVisitor();
			expr.accept(visitor);
			this.merge(visitor);
		}
		return false;
	}
	
	@Override
	public boolean visit(CharacterLiteral node){
		String typeName = getExpressionTypeName(node);
		if(typeName != null)
		simpleNodeTypeMap.put(node, typeName);
		return true;
	}
	
	@Override
	public boolean visit(ClassInstanceCreation node){
		List<Expression> args = node.arguments();
		if(args != null){
			for(Expression arg: args){
				ExpressionVisitor visitor = new ExpressionVisitor();
				arg.accept(visitor);
				this.merge(visitor);
			}
		}
		
		return false;
	}	
	
	@Override
	public boolean visit(FieldAccess node){
		String typeName = getExpressionTypeName(node);
		if(typeName != null)
		simpleNodeTypeMap.put(node, typeName);
		return true;
	}
	
	@Override
	public boolean visit(InfixExpression node){
		String typeName = getExpressionTypeName(node);
		if(typeName != null)
			infixNodeTypeMap.put(node, typeName);
		return true;
	}
	
	@Override
	public boolean visit(InstanceofExpression node){
		Expression expr = node.getLeftOperand();
		ExpressionVisitor visitor = new ExpressionVisitor();
		expr.accept(visitor);
		this.merge(visitor);
		return false;
	}
	
	@Override
	public boolean visit(MethodInvocation node){
		String typeName = getExpressionTypeName(node);
		if(typeName != null)
		methodInvocNodeTypeMap.put(node, typeName);
		return true;
	}
	
	
	@Override
	public boolean visit(NullLiteral node){
		String typeName = getExpressionTypeName(node);
		if(typeName != null)
			simpleNodeTypeMap.put(node, typeName);
		return true;
	}
	
	@Override
	public boolean visit(NumberLiteral node){
		String typeName = getExpressionTypeName(node);
		if(typeName != null)
			simpleNodeTypeMap.put(node, typeName);
		return true;
	}
	
	@Override
	public boolean visit(ParenthesizedExpression node){
		Expression expr = node.getExpression();
		ExpressionVisitor visitor = new ExpressionVisitor();
		expr.accept(visitor);
		this.merge(visitor);
		return false;
	}
	
	@Override
	public boolean visit(PostfixExpression node){
		String typeName = getExpressionTypeName(node);
		if(typeName != null)
			postfixNodeTypeMap.put(node, typeName);
		return true;
	}
	
	@Override
	public boolean visit(PrefixExpression node){
		String typeName = getExpressionTypeName(node);
		if(typeName != null)
			prefixNodeTypeMap.put(node, typeName);
		return true;
	}
	
	@Override
	public boolean visit(StringLiteral node){
		String typeName = getExpressionTypeName(node);
		if(typeName != null)
			simpleNodeTypeMap.put(node, typeName);
		return true;
	}
	
	@Override
	public boolean visit(SuperFieldAccess node){
		String typeName = getExpressionTypeName(node);
		if(typeName != null)
			simpleNodeTypeMap.put(node, typeName);
		return true;
	}
	
	@Override
	public boolean visit(SimpleName node){
		if(node.getParent() instanceof MethodInvocation){
			MethodInvocation mi = (MethodInvocation)(node.getParent());
			if(mi.getName().equals(node))
				return false;
		}
		
		String typeName = getExpressionTypeName(node);
		if(typeName != null)
			simpleNodeTypeMap.put(node, typeName);
		return false;
	}
	public boolean visit(QualifiedName node){
		String typeName = getExpressionTypeName(node);
		if(typeName != null)
			simpleNodeTypeMap.put(node, typeName);
		return false;
	}
	
	
	@Override
	public boolean visit(SuperMethodInvocation node){
		List<Expression> args = node.arguments();
		if(args != null){
			for(Expression arg: args){
				ExpressionVisitor visitor = new ExpressionVisitor();
				arg.accept(visitor);
				this.merge(visitor);
			}
		}
		return false;
	}
	
	
	@Override
	public boolean visit(ThisExpression node){
		return false;
	}
	
	@Override
	public boolean visit(VariableDeclarationExpression node){
		List<VariableDeclarationFragment> frags = node.fragments();
		if(frags != null){
			for(VariableDeclarationFragment frag: frags){
				Expression expr = frag.getInitializer();
				if(expr != null){
					ExpressionVisitor visitor = new ExpressionVisitor();
					expr.accept(visitor);
				}
			}
		}
		
		return false;
	}
}

