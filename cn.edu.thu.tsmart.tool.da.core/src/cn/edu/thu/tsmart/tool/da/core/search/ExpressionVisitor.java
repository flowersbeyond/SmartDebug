package cn.edu.thu.tsmart.tool.da.core.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
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

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSite;

public class ExpressionVisitor extends ASTVisitor{
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

