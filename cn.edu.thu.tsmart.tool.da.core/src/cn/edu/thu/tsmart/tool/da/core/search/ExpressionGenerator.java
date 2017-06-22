package cn.edu.thu.tsmart.tool.da.core.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.EclipseUtils;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.ConditionFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.InsertStopFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.StatementFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSite;

public class ExpressionGenerator {
	
	private BugFixSession session;
	
	public ExpressionGenerator(BugFixSession session){
		this.session = session;
	}
	
	private String recoverQualifiedName(IImportDeclaration[] imports,
			IPackageDeclaration[] packageDeclarations, String qName) {
		if(qName == null)
			return null;
		if(EclipseUtils.isPrimitive(qName))
			return qName;
		if(qName.equals("Object"))
			return "java.lang.Object";
		if(qName.indexOf('.') != -1)
			return qName;
		if(imports != null){
			for(int i = 0; i < imports.length; i ++){
				String name = imports[i].getElementName();
				if(name.endsWith(qName))
					return name;
				else if (name.endsWith("*")){
					name = name.substring(0, name.indexOf('*'));
					String possibleTypeName = name + qName;
					try {
						IType possibleType = session.getProject().findType(possibleTypeName);
						if(possibleType != null)
							return possibleTypeName;
					} catch (JavaModelException e) {
						e.printStackTrace();
						continue;
					}
				}
			}
		}
		
		if(packageDeclarations != null && packageDeclarations.length >= 1){
			IPackageDeclaration packDecl = packageDeclarations[0];
			String possibleTypeName = packDecl.getElementName() + "." + qName;
			try {
				IType possibleType = session.getProject().findType(possibleTypeName);
				if(possibleType != null)
					return possibleTypeName;
			} catch (JavaModelException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		return null;
	}

	private static String getQName(String typeSignature){
		String qualifier = Signature.getSignatureQualifier(typeSignature);
		String simpleName = Signature.getSignatureSimpleName(typeSignature);
		
		if(qualifier.length() == 0)
			return simpleName;
		else
			return qualifier + "." + simpleName;
		
	}
	
	public Set<String> genIfCondition(AbstractFixSite fixsite) {
		try{
			Statement stmt = null;
			if (fixsite instanceof StatementFixSite){
				StatementFixSite sfs = (StatementFixSite)fixsite;
				if(sfs.getStatements().size() == 0)
					return new HashSet<String>();
				stmt = (Statement)((StatementFixSite) fixsite).getStatements().get(0);
			} else if (fixsite instanceof InsertStopFixSite){
				stmt = ((InsertStopFixSite) fixsite).getStatement();
			}
			if(stmt == null)
				return new HashSet<String>();
			
			String qualifiedType = getDefinedType(stmt);
			// get boolean constant defined within this class
			Map<String, Set<String>> fields = getThisFields(stmt);
			Set<String> booleanFields = new HashSet<String>();
			if(fields.containsKey("boolean")){
				booleanFields.addAll(fields.get("boolean"));
			}
			if(fields.containsKey("java.lang.Boolean")){
				booleanFields.addAll(fields.get("java.lang.Boolean"));
			}
			
			Map<String, Set<String>> localVariables = getLocalVariables(stmt);
			// get methodcall on local variable with no parameters
			ArrayList<String> localVariableBooleanMethodInvoks = getVariableBooleanMethodInvoks(qualifiedType, localVariables, null);
			// get method call on this method with no parameters
			ArrayList<String> thisBooleanMethodInvoks = getThisBooleanMethodInvoks(qualifiedType, null);
			
			Set<String> ifConditions = new HashSet<String>();
			ifConditions.addAll(localVariableBooleanMethodInvoks);
			ifConditions.addAll(thisBooleanMethodInvoks);
			for(String boolField: booleanFields){
				ifConditions.add(boolField);
				ifConditions.add("!" + boolField);
			}
			return ifConditions;
		}
		catch (JavaModelException e) {
			return new HashSet<String>();
		}
	}
	
	public ArrayList<String> genBooleanAppendPart(ConditionFixSite fixsite){
		ASTNode node = fixsite.getConditionExpression();
		if(node instanceof Expression){
			Expression expr = (Expression)node;
			return genBooleanAppendPart(expr);
		}	
		return new ArrayList<String>();
	}
	
	public ArrayList<String> genBooleanAppendPart(Expression booleanExpr){
		try{
			// get boolean constant defined within this class
			String qualifiedType = getDefinedType(booleanExpr);
			Map<String, Set<String>> exprVariables = getLocalVariables(booleanExpr);
			Set<String> mustContain = new HashSet<String>();
			for(String type: exprVariables.keySet()){
				mustContain.addAll(exprVariables.get(type));
			}
			// generate method call for variables
			ArrayList<String> localVariableBooleanMethodInvoks = getVariableBooleanMethodInvoks(qualifiedType, exprVariables, mustContain);
			
			// boolean constant
			Map<String, Set<String>> thisFields = getThisFields(booleanExpr);
			ArrayList<String> booleanFields = new ArrayList<String>();
			if(thisFields.containsKey("boolean")){
				booleanFields.addAll(thisFields.get("boolean"));
			}
			if(thisFields.containsKey("java.lang.Boolean")){
				booleanFields.addAll(thisFields.get("java.lang.Boolean"));
			}
			
			ASTNode node = booleanExpr;
			while(node instanceof Expression)
				node = node.getParent();
			Map<String, Set<String>> localVals = getLocalVariables(node);
			if(localVals.containsKey("boolean")){
				booleanFields.addAll(localVals.get("boolean"));
			}
			if(localVals.containsKey("java.lang.Boolean")){
				booleanFields.addAll(localVals.get("java.lang.Boolean"));
			}
			// generate method call from this object with a variable parameter
			ArrayList<String> thisBooleanMethodInvoks = getThisBooleanMethodInvoks(qualifiedType, exprVariables);
			
			// generate value comparison on expression variables
			Set<String> allLocalValueVariables = getLocalValueVariables(booleanExpr);
			Set<String> valueCompareExpressions = getValueCompareExpressions(allLocalValueVariables, mustContain);
		
			// sum everything up
			ArrayList<String> booleanAppendPart = new ArrayList<String>();
			booleanAppendPart.addAll(localVariableBooleanMethodInvoks);
			booleanAppendPart.addAll(thisBooleanMethodInvoks);
			for(String str : booleanFields){
				booleanAppendPart.add(str);
				booleanAppendPart.add("!" + str);
			}
			booleanAppendPart.addAll(valueCompareExpressions);
			return booleanAppendPart;
		}
		catch (JavaModelException e){
			return new ArrayList<String>();
		}
	}
	
	public Map<String, Set<String>> genLocalVariableReplacements(StatementFixSite fixSite){
		if(fixSite.getStatements().size() <= 0)
			return new HashMap<String, Set<String>>();
		ASTNode node = fixSite.getStatements().get(0);
		//generate local Expressions, type maps
		Map<String, Set<String>> localVariables = getLocalVariables(node);
		
		return localVariables;
	}
	
	public Map<String, Set<String>> genFieldReplacements(StatementFixSite fixSite){
		try{
			if(fixSite.getStatements().size() <= 0)
				return new HashMap<String, Set<String>>();
			ASTNode node = fixSite.getStatements().get(0);
			//generate field Expression, type maps
			Map<String, Set<String>> thisFields = getThisFields(node);
			
			return thisFields;
		} catch (JavaModelException e){
			return new HashMap<String, Set<String>>();
		}
		
	}
	
	
	private Map<String, Set<String>> getLocalVariables(ASTNode node){
		if(node instanceof Expression){
			VariableCollector collector = new VariableCollector();
			node.accept(collector);
			return collector.getVariables();
		} else {
			ASTNode parent = node.getParent();
			while(!(parent instanceof MethodDeclaration)){
				parent = parent.getParent();
			}
			VariableCollector collector = new VariableCollector();
			parent.accept(collector);
			return collector.getVariables();
		}
	}
	
	private Map<String, Set<String>> getThisFields(ASTNode node) throws JavaModelException{
		String qualifiedType = getDefinedType(node);
		IType declareType = session.getProject().findType(qualifiedType);
		if(declareType == null)
			return new HashMap<String, Set<String>>();
		IField[] fields = declareType.getFields();
		
		Map<String, Set<String>> fieldMap = new HashMap<String, Set<String>>();
		for(int i = 0; i < fields.length; i ++){
			String qualifier = Signature.getSignatureQualifier(fields[i].getTypeSignature());
			String simpleName = Signature.getSignatureSimpleName(fields[i].getTypeSignature());
			String typeName = simpleName;
			if(qualifier != null && qualifier.length() > 0){
				typeName = qualifier + "." + simpleName;
			}
			
			if(!fieldMap.containsKey(typeName))
				fieldMap.put(typeName, new HashSet<String>());
			fieldMap.get(typeName).add(fields[i].getElementName());
		}
		
		return fieldMap;
		
	}
	
	private Set<String> getLocalValueVariables(Expression booleanExpr) {
		ValueVariableCollector collector = new ValueVariableCollector();
		ASTNode parent = booleanExpr.getParent();
		while(!(parent instanceof MethodDeclaration)){
			parent = parent.getParent();
		}
		parent.accept(collector);
		
		return collector.getVariables();
	}
	
	private ArrayList<String> getThisBooleanMethodInvoks(String fullyQualifiedName, Map<String, Set<String>> validParams) throws JavaModelException{
		IType declareType = session.getProject().findType(fullyQualifiedName);
		if(declareType == null)
			return new ArrayList<String>();
		ArrayList<String> booleanInvoks = new ArrayList<String>();
		if(declareType != null){
			IMethod[] methods = declareType.getMethods();
			for(int i = 0; i < methods.length; i ++){
				IMethod method = methods[i];
				String[] paramTypes = method.getParameterTypes();
				if(paramTypes.length >= 2)
					continue;
				
				String returnType = getQName(method.getReturnType());
				if(returnType.equals("boolean") || returnType.equals("java.lang.Boolean")){
					
					switch(paramTypes.length){
					case 0:
						booleanInvoks.add(method.getElementName() + "()");
						booleanInvoks.add("!" + method.getElementName() + "()");
						break;
					case 1:
						if(validParams == null)
							break;
						String paramType = getQName(paramTypes[0]);
						if(paramType == null)
							break;
						Set<String> pars = validParams.get(paramType);
						if(pars != null){
							for(String par: pars){
								booleanInvoks.add(method.getElementName() + "(" + par + ")");
								booleanInvoks.add("!" + method.getElementName() + "(" + par + ")");
							}
						}					
						break;
					}
				}
				
			}
		}
				
		return booleanInvoks;
	}
	
	
	private ArrayList<String> getVariableBooleanMethodInvoks(String fullyQualifiedName, Map<String, Set<String>> repo, Set<String> mustContain) throws JavaModelException{
		ArrayList<String> booleanMethodInvoks = new ArrayList<String>();
		IJavaProject proj = session.getProject();
		for(String type: repo.keySet()){
			IType t = proj.findType(type);
			if(t != null){
				IMethod[] methods = t.getMethods();
				for(int i = 0; i < methods.length; i ++){
					IMethod method = methods[i];
					String[] paramTypes = method.getParameterTypes();
					if(paramTypes.length >= 2)
						continue;
					
					if((method.getFlags() & Flags.AccStatic) != 0){
						continue;
					}
					
					
					
					
					IType declType = method.getDeclaringType();
					ICompilationUnit declUnit = declType.getCompilationUnit();
					
					String returnTypeName = "";
					if(method.getReturnType() == null)
						continue;
					if(declUnit == null){
						returnTypeName = getQName(method.getReturnType());
					} else {
						returnTypeName = recoverQualifiedName(declUnit.getImports(), declUnit.getPackageDeclarations(),getQName(method.getReturnType()));
					}
					if(returnTypeName == null){
						System.out.println("retType:" +  method.getElementName() + ":" + method.getSignature() + "not found");
						continue;
					}
					if(!returnTypeName.equals("boolean") && !returnTypeName.equals("java.lang.Boolean")){
						continue;
					}
					String paramType = "";
					if(paramTypes != null && paramTypes.length == 1){
						if(declUnit == null){
							paramType = getQName(paramTypes[0]);
						} else {
							paramType = recoverQualifiedName(declUnit.getImports(), declUnit.getPackageDeclarations(), getQName(paramTypes[0]));
						}
						if(paramType == null){
							System.out.println("paramType:" + method.getElementName() + ":" + method.getSignature() + "not found");
							paramType = getQName(paramTypes[0]);
							continue;
						}
					}
					
					
					Set<String> recs = repo.get(t.getFullyQualifiedName());
					
					if(recs != null){
						
						for(String rec :recs){
							if(rec.equals("this"))
								continue;
							switch(paramTypes.length){
								case 0:
									if(mustContain == null || mustContain.contains(rec)){
										booleanMethodInvoks.add(rec + "." + method.getElementName() + "()");
										booleanMethodInvoks.add("!" + rec + "." + method.getElementName() + "()");
									}
									break;
								case 1:
									if(mustContain == null)
										break;
									Set<String> pars = repo.get(paramType);
									if(pars != null){
										for (String par : pars) {
											if(mustContain.contains(rec) || mustContain.contains(par)){
												booleanMethodInvoks.add(rec + "." + method.getElementName()+ "(" + par + ")");
												booleanMethodInvoks.add("!" + rec + "." + method.getElementName()+ "(" + par + ")");
											}
										}
									}
									break;
							}
						}
					}
				}
			}
		}
		
		return booleanMethodInvoks;
		
	}
	
	private Set<String> getValueCompareExpressions(Set<String> allLocalValueVariables, Set<String> mustContain){
		Set<String> compExprs = new HashSet<String>();
		for(String lhs: allLocalValueVariables){
			if(! mustContain.contains(lhs))
				continue;
		
			for(String rhs: allLocalValueVariables){
				if (lhs != rhs) {
					compExprs.add(lhs + "<" + rhs);
					compExprs.add(lhs + "<=" + rhs);
					compExprs.add(lhs + "!=" + rhs);
				}
			}
		}
		
		return compExprs;
	}
	
	private static String getDefinedType(ASTNode node){
		ASTNode parent = node;
		while(!(parent instanceof TypeDeclaration && ((TypeDeclaration)parent).isPackageMemberTypeDeclaration())){
			parent = parent.getParent();
			if(parent instanceof CompilationUnit)
				return "";
		}
		
		TypeDeclaration td = (TypeDeclaration)parent;
		return td.resolveBinding().getQualifiedName();
	}

	public Map<String, Set<String>> genFieldReplacements(
			ConditionFixSite fixSite) {
		Map<String, Set<String>> results = new HashMap<String, Set<String>>();
		ASTNode node = fixSite.getWrappingStatement();
		try {
			if(node != null)
				return getThisFields(node);
			else
				return results;
		} catch (JavaModelException e) {
			return results;
		}
	}
	
	public Map<String, Set<String>> genLocalVariableReplacements(
			ConditionFixSite fixSite) {
		Map<String, Set<String>> results = new HashMap<String, Set<String>>();
		ASTNode node = fixSite.getWrappingStatement();
	
		if(node != null)
			return getLocalVariables(node);
		else
			return results;
		
	}
	
	
}

class VariableCollector extends ASTVisitor {
	Map<String, Set<String>> variables = new HashMap<String, Set<String>>();
	public Map<String, Set<String>> getVariables(){
		return variables;
	}
	
	@Override
	public boolean visit (SimpleName node) {
		ITypeBinding binding = node.resolveTypeBinding();
		if(binding != null){
			String typeName = binding.getQualifiedName();
			if(!variables.containsKey(typeName)) {
				variables.put(typeName, new HashSet<String> ());
			}
			variables.get(typeName).add(node.toString());
		}
		return false;
	}
}

class ValueVariableCollector extends ASTVisitor {
	Set<String>  variables = new HashSet<String>();
	public Set<String> getVariables(){
		return variables;
	}
	
	@Override
	public boolean visit (SimpleName node) {
		ITypeBinding binding = node.resolveTypeBinding();
		if(binding != null){
			String typeName = binding.getQualifiedName();
			if(EclipseUtils.isPrimitive(typeName) && !typeName.equals("boolean") && !typeName.equals("java.lang.Boolean")){				
				variables.add(node.toString());
			}
		}
		return false;
	}
}
