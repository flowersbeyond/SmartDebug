package cn.edu.thu.tsmart.tool.da.core.search.strategy;

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
import cn.edu.thu.tsmart.tool.da.core.search.fixSite.ConditionFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.fixSite.FixSite;
import cn.edu.thu.tsmart.tool.da.core.search.fixSite.InsertStopFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.fixSite.StatementFixSite;

public class MyExpressionGenerator {
	
	private static String[] primitiveNames = new String[]{"boolean", "byte", "short", "int", "long", "float", "double", "char"};
	private BugFixSession session;
	
	public MyExpressionGenerator(BugFixSession session){
		this.session = session;
	}
	
	/*public Map<String, Set<String>> genExprRepo(IJavaStackFrame frame) {
		Map<String, Set<String>> repo = new HashMap<String, Set<String>>();
		try{
			init(repo, frame);
			Map<String, Set<String>> inteExprs = padInteger(repo, frame);
			Set<String> boolExprs = padBoolean(repo, frame);
			Map<String, Set<String>> invocExprs = padMethodInvoc(repo, frame);
			
			for(String key: inteExprs.keySet()){
				if(!repo.containsKey(key)){
					repo.put(key, inteExprs.get(key));
				} else {
					repo.get(key).addAll(inteExprs.get(key));
				}
			}
			
			repo.get("boolean").addAll(boolExprs);
			
			for(String key: invocExprs.keySet()){
				if(!repo.containsKey(key)){
					repo.put(key, invocExprs.get(key));
				} else {
					repo.get(key).addAll(invocExprs.get(key));
				}
			}
			
		} catch (DebugException e){
			e.printStackTrace();
		} catch (JavaModelException e){
			e.printStackTrace();
		}
		return repo;
	}
	
	private void init(Map<String, Set<String>> repo, IJavaStackFrame frame) throws DebugException{
		//local reachable variables:
		IJavaVariable[] vars = frame.getLocalVariables();
		for(int i = 0; i < vars.length; i ++){
			IJavaVariable var = vars[i];
			IJavaType type = null;
			try{
				type = var.getJavaType();
			} catch (DebugException e){
				e.printStackTrace();
			}
			if(type == null)
				continue;
			String typeName = type.getName();//getQName(type.getSignature());
			if(!repo.containsKey(typeName)){
				repo.put(typeName, new HashSet<String>());
			}
			
			repo.get(typeName).add(var.getName());
		}
		
		//this.XXX if exists
		IJavaObject thisObj = frame.getThis();
		if(thisObj != null){
			IVariable[] fields = thisObj.getVariables();
			for(int i = 0; i < fields.length; i ++){
				if(fields[i] instanceof IJavaVariable){
					IJavaVariable field = (IJavaVariable)fields[i];
					IJavaType fieldType = null;
					try{
						fieldType = field.getJavaType();
					} catch (DebugException e){
						e.printStackTrace();
					}
					if(fieldType == null)
						continue;
					String fieldTypeName = fieldType.getName();//getQName(fieldType.getSignature());
					if(!repo.containsKey(fieldTypeName)){
						repo.put(fieldTypeName, new HashSet<String>());
					}
					
					repo.get(fieldTypeName).add(field.getName());
				}
			}
			
			IJavaType thisType = thisObj.getJavaType();
			String thisTypeName = thisType.getName();//getQName(thisType.getSignature());
			if(thisType != null){
				if(!repo.containsKey(thisTypeName)){
					repo.put(thisTypeName, new HashSet<String>());
				}
				
				repo.get(thisTypeName).add("this");
			}
		}
		
		//static fields
		IJavaReferenceType reftype = frame.getReferenceType();
		if(reftype != null){
			String[] fieldNames = reftype.getAllFieldNames();
			for(int i = 0; i < fieldNames.length; i ++){
				IJavaFieldVariable var = reftype.getField(fieldNames[i]);
				if(var != null && var.isStatic()){
					IJavaType type = null;
					try{
						type = var.getJavaType();
					} catch (DebugException e){
						e.printStackTrace();
					}
					if(type == null)
						continue;
					String typeName = type.getName();//getQName(type.getSignature());
					if(!repo.containsKey(typeName)){
						repo.put(typeName, new HashSet<String>());
					}
					repo.get(typeName).add(var.getName());
				}
			}
		}
		
		// constants
			if(!repo.containsKey("boolean")){
				repo.put("boolean", new HashSet<String>());
			}
			repo.get("boolean").add("true");
			repo.get("boolean").add("false");
		
	}
	
	private Map<String, Set<String>> padInteger(Map<String, Set<String>> repo, IJavaStackFrame frame) throws DebugException{
		
		Map<String, Set<String>> valueExprs = new HashMap<String, Set<String>>();
		IJavaDebugTarget target = (IJavaDebugTarget) frame.getDebugTarget();
		genValueExprs(repo, target, "byte", valueExprs);
		genValueExprs(repo, target, "short", valueExprs);
		genValueExprs(repo, target, "int", valueExprs);
		genValueExprs(repo, target, "long", valueExprs);
		genValueExprs(repo, target, "float", valueExprs);
		genValueExprs(repo, target, "double", valueExprs);
		
		return valueExprs;
	}
	
	private void genValueExprs(Map<String, Set<String>> repo, IJavaDebugTarget target, String typeName, Map<String, Set<String>> valueExprs) throws DebugException{
		
		Set<String> genExprs = new HashSet<String>();
		Set<String> exprs = repo.get(typeName);
		if(exprs != null){
			for(String expr: exprs){
				genExprs.add(expr + "+1");
				genExprs.add(expr + "-1");
				for(String expr2: exprs){
					if(expr2 != expr && !genExprs.contains(expr2 + "+" + expr)){
						genExprs.add(expr + "+" + expr2);
					}
					if(expr2 != expr && !genExprs.contains(expr2 + "-" + expr)){
						genExprs.add(expr + "-" + expr2);
					}
				}
			}
			valueExprs.put(typeName, genExprs);
		}
	}
	
	private Set<String> padBoolean(Map<String, Set<String>> repo, IJavaStackFrame frame) throws DebugException{
		IJavaDebugTarget target = (IJavaDebugTarget) frame.getDebugTarget();
		
		Set<String> boolExprs = new HashSet<String>();
		
			genCompExprs(repo, target, "byte", boolExprs);
			genCompExprs(repo, target, "short", boolExprs);
			genCompExprs(repo, target, "int", boolExprs);
			genCompExprs(repo, target, "long", boolExprs);
			genCompExprs(repo, target, "float", boolExprs);
			genCompExprs(repo, target, "double", boolExprs);
		
		
			// method call
			// this would be done in padMethodInvoc
			
			// object != null
			for(String type: repo.keySet()){
				if(!isPrimitive(type)){
					Set<String> objs = repo.get(type);
					if(objs == null) continue;
					for(String obj: objs){
						boolExprs.add(obj + "!=null");
					}
				}
			}
		
		return boolExprs;
	}
	
	
	public void genCompExprs(Map<String, Set<String>> repo, IJavaDebugTarget target, String typeName, Set<String> compExprs) throws DebugException{
		// a > b, a >= b, a < b, a <= b, a != b
		
		Set<String> valueVariables = repo.get(typeName);
		if(valueVariables == null)
			return;
		for(String lhs: valueVariables){
			for(String rhs: valueVariables){
				if(lhs != rhs){
					if(!compExprs.contains(rhs + ">" + lhs)){
						compExprs.add(lhs + "<" + rhs);
					}
					if(!compExprs.contains(rhs + ">=" + lhs)){
						compExprs.add(lhs + "<=" + rhs);
					}
					if(!compExprs.contains(rhs + "!=" + lhs)){
						compExprs.add(lhs + "!=" + rhs);
					}
				}
			}
		}
		
	}
	
	
	
	private Map<String, Set<String>> padMethodInvoc(Map<String, Set<String>> repo, IJavaStackFrame frame) throws JavaModelException, DebugException{
		// a.m(p1);
		// a.m();
		
		Map<String, Set<String>> methodInvocs = new HashMap<String, Set<String>>();
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
						returnTypeName = getQName(method.getReturnType());
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
					
					if(!methodInvocs.containsKey(returnTypeName))
						methodInvocs.put(returnTypeName, new HashSet<String>());
					Set<String> recs = repo.get(t.getFullyQualifiedName());
					
					if(recs != null){
						
						for(String rec :recs){
							if(rec.equals("this"))
								continue;
							switch(paramTypes.length){
								case 0:
									methodInvocs.get(returnTypeName).add(rec + "." + method.getElementName() + "()");
									break;
								case 1:
									Set<String> pars = repo.get(paramType);
									if(pars != null){
										for (String par : pars) {
											methodInvocs.get(returnTypeName).add(rec + "." + method.getElementName()+ "(" + par + ")");
										}
									}
									break;
							}
						}
					}
				}
			}
		}
		
		// m(p1, p2);
		// m(p1);
		// m();
		
		String declareTypeName = frame.getDeclaringTypeName();
		IType declareType = session.getProject().findType(declareTypeName);
		if(declareType != null){
			IMethod[] methods = declareType.getMethods();
			for(int i = 0; i < methods.length; i ++){
				IMethod method = methods[i];
				String[] paramTypes = method.getParameterTypes();
				if(paramTypes.length >= 2)
					continue;
				
				String returnType = getQName(method.getReturnType());
				if(!methodInvocs.containsKey(returnType))
					methodInvocs.put(returnType, new HashSet<String>());
				switch(paramTypes.length){
				case 0:
					methodInvocs.get(returnType).add(method.getElementName() + "()");
					break;
				case 1:
					String paramType = getQName(paramTypes[0]);
					if(paramType == null)
						break;
					Set<String> pars = repo.get(paramType);
					if(pars != null){
						for(String par: pars){
							methodInvocs.get(returnType).add(method.getElementName() + "(" + par + ")");
						}
					}					
					break;
				}
				
			}
		}
				
		return methodInvocs;
		
	}
	
	
	
	
*/
	public static boolean isPrimitive(String typeName){
		for(int i = 0; i < primitiveNames.length; i ++){
			if(typeName.equals(primitiveNames[i])){
				return true;
			}
		}
		return false;
	}
	

	private String recoverQualifiedName(IImportDeclaration[] imports,
			IPackageDeclaration[] packageDeclarations, String qName) {
		if(qName == null)
			return null;
		if(isPrimitive(qName))
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
						// TODO Auto-generated catch block
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
				// TODO Auto-generated catch block
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
	public Set<String> genIfCondition(FixSite fixsite) {
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
			if(MyExpressionGenerator.isPrimitive(typeName) && !typeName.equals("boolean") && !typeName.equals("java.lang.Boolean")){				
				variables.add(node.toString());
			}
		}
		return false;
	}
}
