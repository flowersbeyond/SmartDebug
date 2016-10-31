package cn.edu.thu.tsmart.tool.da.core.search.strategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.search.fixSite.StatementFixSite;
import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;
import cn.edu.thu.tsmart.tool.da.core.suggestion.MethodFix;

public class MethodFixer extends Fixer{

	/**
	 * overloaded methods: method with the same name but different signature
	 */
	BugFixSession session;
	public MethodFixer(BugFixSession session){
		this.session = session;
	}
	private ArrayList<Fix> changeOverloaded(StatementFixSite fixSite, MethodInvocation mi, Map<String, Set<String>> vars){
		
		ArrayList<Fix> fixes = new ArrayList<Fix>();
		ArrayList<String> reps = genOverloadedReplaces(mi, vars);
		for(String rep: reps){		
			MethodFix fix = new MethodFix(fixSite, mi.getStartPosition(), mi.getLength(), rep, Fix.METHOD_GENERAL_OVERLOAD, FixerUtil.getLineNum(mi));
			fixes.add(fix);
		}
		
		return fixes;
		
	}

	
	public static ArrayList<String> genOverloadedReplaces(MethodInvocation mi, Map<String, Set<String>> vars){
		ArrayList<String> reps = new ArrayList<String>();
		
		ArrayList<IMethodBinding> overloadedMethods = new ArrayList<IMethodBinding>();
		IMethodBinding targetMB = mi.resolveMethodBinding();
		ITypeBinding declaringClass = targetMB.getDeclaringClass();
		IMethodBinding[] allMethodBindings = declaringClass.getDeclaredMethods();
		
		for(int i = 0; i < allMethodBindings.length; i ++){
			IMethodBinding mb = allMethodBindings[i];
			if(mb.getName().equals(targetMB.getName()) && !mb.toString().equals(targetMB.toString())){
				overloadedMethods.add(mb);
			}
		}
		
		
		List<Expression> oldParams = mi.arguments();
		ITypeBinding[] oldParamBindings = targetMB.getParameterTypes();
		int oldParamNum = oldParamBindings == null ? 0 : oldParamBindings.length;
		for(IMethodBinding olmb: overloadedMethods){
			ITypeBinding[] paramBindings = olmb.getParameterTypes();
			int newParamNum = paramBindings == null ? 0 : paramBindings.length;
			if(Math.abs(oldParamNum - newParamNum) >= 2)
				continue;
			ArrayList<String> newParams = new ArrayList<String>();
			int i = 0;//flag of mi params
			int j = 0;//flag of olmb params
			boolean fillable = true;
			while(j < paramBindings.length){
				boolean compatibleExprExist = false;
				for(int k = i; k < oldParamBindings.length; k ++){
					ITypeBinding oldtypeBinding = oldParamBindings[k];
					if(oldtypeBinding.getQualifiedName().equals(paramBindings[j].getQualifiedName())){
						compatibleExprExist = true;
						i = k;
						break;
					}
				}
				if(compatibleExprExist){
					if(newParams.size() <= j)
						newParams.add(oldParams.get(i).toString());
					else
						newParams.set(j, oldParams.get(i).toString());
					i ++;
					j ++;
				} else {
					//fill in the blank of a new expression
					String paramTypeName = paramBindings[j].getQualifiedName();
					Set<String> possibleExprs = new HashSet<String>();
					if(vars.containsKey(paramTypeName)){
						possibleExprs.addAll(vars.get(paramTypeName));
					}
					String simpleParamTypeName = paramTypeName.substring(paramTypeName.lastIndexOf('.') + 1);
					if(vars.containsKey(simpleParamTypeName)){
						possibleExprs.addAll(vars.get(simpleParamTypeName));
					}
					
					if(possibleExprs.size() > 0){
						if(newParams.size() <= j)
							newParams.add(possibleExprs.iterator().next());
						else
							newParams.set(j, possibleExprs.iterator().next());
					} else{
						fillable = false;
						break;
					}
					j ++;
				}
				if(!fillable){
					continue;
				}				
			}
			
			//newParams are successfully filled here, we generate the new block fix:
			String newParamListString = "";
			if(newParams.size() > 0){
				for(int p = 0; p < newParams.size() - 1; p ++){
					newParamListString += newParams.get(p) + ", ";
				}
				newParamListString += newParams.get(newParams.size() - 1);
			}
			String newMethodInvocString = "";
			if(mi.getExpression() != null)
				newMethodInvocString = mi.getExpression().toString() + ".";
			newMethodInvocString += mi.getName().toString() + "(" + newParamListString + ")";
			reps.add(newMethodInvocString);
		}
		return reps;
			
	}
	
	/**
	 * overloaded constructors: constructor with different signature
	 * @param cic
	 * @param exprRepo
	 * @return
	 */
	private ArrayList<Fix> changeOverloaded(StatementFixSite fixSite, ClassInstanceCreation cic, Map<String, Set<String>> vars){
		

		ArrayList<Fix> fixes = new ArrayList<Fix>();
		ArrayList<String> reps = genOverloadedReplaces(cic, vars);
		for(String rep: reps){
			MethodFix fix = new MethodFix(fixSite, cic.getStartPosition(), cic.getLength(), rep, Fix.METHOD_CNSTR_OVERLOAD, FixerUtil.getLineNum(cic));
			fixes.add(fix);
		}
		
		return fixes;
	}
	
	public static ArrayList<String> genOverloadedReplaces(ClassInstanceCreation cic, Map<String, Set<String>> vars){
		ArrayList<String> reps = new ArrayList<String>();
		
		ArrayList<IMethodBinding> constructorMBs = new ArrayList<IMethodBinding>();
		IMethodBinding targetMB = cic.resolveConstructorBinding();
		ITypeBinding declaringClass = targetMB.getDeclaringClass();
		IMethodBinding[] allMethodBindings = declaringClass.getDeclaredMethods();
		
		for(int i = 0; i < allMethodBindings.length; i ++){
			IMethodBinding mb = allMethodBindings[i];
			if(mb.isConstructor()){
				constructorMBs.add(mb);
			}
		}
		
		
		List<Expression> oldParams = cic.arguments();
		ITypeBinding[] oldParamBindings = targetMB.getParameterTypes();
		int oldParamNum = oldParamBindings == null ? 0 : oldParamBindings.length;
		for(IMethodBinding cntrmb: constructorMBs){
			ITypeBinding[] paramBindings = cntrmb.getParameterTypes();
			int paramNum = paramBindings == null ? 0 : paramBindings.length;
			if(Math.abs(oldParamNum - paramNum) >= 2)
				continue;
			ArrayList<String> newParams = new ArrayList<String>();
			int i = 0;//flag of mi params
			int j = 0;//flag of olmb params
			boolean fillable = true;
			while(j < paramBindings.length){
				boolean compatibleExprExist = false;
				for(int k = i; k < oldParamBindings.length; k ++){
					ITypeBinding oldtypeBinding = oldParamBindings[k];
					if(oldtypeBinding.getQualifiedName().equals(paramBindings[j].getQualifiedName())){
						compatibleExprExist = true;
						i = k;
						break;
					}
				}
				if(compatibleExprExist){
					if(newParams.size() <= j)
						newParams.add(oldParams.get(i).toString());
					else
						newParams.set(j, oldParams.get(i).toString());
					i ++;
					j ++;
				} else {
					//fill in the blank of a new expression
					String paramTypeName = paramBindings[j].getQualifiedName();
					Set<String> possibleExprs = new HashSet<String>();
					if(vars.containsKey(paramTypeName)){
						possibleExprs.addAll(vars.get(paramTypeName));
					}
					String simpleParamTypeName = paramTypeName.substring(paramTypeName.lastIndexOf('.') + 1);
					if(vars.containsKey(simpleParamTypeName)){
						possibleExprs.addAll(vars.get(simpleParamTypeName));
					}
					if(possibleExprs.size() > 0){
						if(newParams.size() <= j)
							newParams.add(possibleExprs.iterator().next());
						else
							newParams.set(j, possibleExprs.iterator().next());
					} else{
						fillable = false;
						break;
					}
					j ++;
				}
				if(!fillable){
					continue;
				}				
			}
			
			//newParams are successfully filled here, we generate the new block fix:
			String newParamListString = "";
			if(newParams.size() > 0){
				for(int p = 0; p < newParams.size() - 1; p ++){
					newParamListString += newParams.get(p) + ", ";
				}
				newParamListString += newParams.get(newParams.size() - 1);
			}
			String newConstructorParamString = "";
			
			newConstructorParamString += "new " + cic.getType().toString() + "(" + newParamListString + ")";
			reps.add(newConstructorParamString);
		}
		return reps;
	}
	
	/**
	 * change to another method with the same signature but different name
	 */
	private ArrayList<Fix> changeSameSig(StatementFixSite fixSite, MethodInvocation mi, Map<String, Set<String>> vars){
		
		ArrayList<Fix> fixes = new ArrayList<Fix>();
		ArrayList<String> reps = genSameSigReplacers(mi, vars);
		for(String rep: reps){	
			MethodFix fix = new MethodFix(fixSite, mi.getStartPosition(), mi.getLength(), rep, Fix.METHOD_SAME_SIG, FixerUtil.getLineNum(mi));
			fixes.add(fix);			
		}
		
		return fixes;
	}
	
	
	public static ArrayList<String> genSameSigReplacers(MethodInvocation mi, Map<String, Set<String>> vars){
		ArrayList<String> reps = new ArrayList<String>();
		if(mi.arguments().size() == 0)
			return reps;
		
		ArrayList<IMethodBinding> methodMBs = new ArrayList<IMethodBinding>();
		IMethodBinding targetMB = mi.resolveMethodBinding();
		ITypeBinding declaringClass = targetMB.getDeclaringClass();
		IMethodBinding[] allMethodBindings = declaringClass.getDeclaredMethods();
		
		List<Expression> oldParams = mi.arguments();
		
		for(int i = 0; i < allMethodBindings.length; i ++){
			IMethodBinding mb = allMethodBindings[i];
			if(!mb.getName().equals(targetMB.getName())){
				ITypeBinding[] oldParamTypes = targetMB.getParameterTypes();
				ITypeBinding[] newParamTypes = mb.getParameterTypes();
				if(oldParamTypes.length != newParamTypes.length)
					continue;
				
				boolean qualified = true;
				for(int j = 0; j < oldParamTypes.length; j ++){
					if(!(oldParamTypes[j].toString().equals(newParamTypes[j].toString()))){
						qualified = false;
						break;
					}
				}
				if(!qualified)
					continue;
				
				String paramListString = "";
				if(oldParams!= null && oldParams.size() > 0){
					for(int p = 0; p < oldParams.size() - 1; p ++){
						paramListString += oldParams.get(p).toString() + ", ";
					}
					paramListString += oldParams.get(oldParams.size() - 1);
				}
				String newMethodInvocString = "";
				if(mi.getExpression() != null)
					newMethodInvocString += mi.getExpression().toString() + ".";
				newMethodInvocString += mb.getName() + "(" + paramListString + ")";
				reps.add(newMethodInvocString);
			}
		}
		return reps;
	}

	public ArrayList<Fix> generateFix(StatementFixSite fixSite) {
		ArrayList<MethodInvocation> methodInvocs = new ArrayList<MethodInvocation>();
		ArrayList<ClassInstanceCreation> classInstanceCreations = new ArrayList<ClassInstanceCreation>();
		Map<String, Set<String>> localVars = session.getExpressionGenerator().genLocalVariableReplacements(fixSite);
		Map<String, Set<String>> fieldVars = session.getExpressionGenerator().genFieldReplacements(fixSite);
		for(String type: fieldVars.keySet()){
			if(!localVars.containsKey(type)){
				localVars.put(type, fieldVars.get(type));
			} else {
				localVars.get(type).addAll(fieldVars.get(type));
			}
		}
		
		
		for(ASTNode stmt: fixSite.getStatements()){
			MethodInvocCollector collector = new MethodInvocCollector();
			stmt.accept(collector);
			methodInvocs.addAll(collector.methodInvocs);
			classInstanceCreations.addAll(collector.classInstanceCreations);
		}
		
		ArrayList<Fix> fixes = new ArrayList<Fix>();
		for(MethodInvocation mi: methodInvocs){
			fixes.addAll(changeOverloaded(fixSite, mi, localVars));
			fixes.addAll(changeSameSig(fixSite, mi, localVars));
		}
		
		for(ClassInstanceCreation cic: classInstanceCreations){
			fixes.addAll(changeOverloaded(fixSite, cic, localVars));
		}
		return fixes;
	}
	
	private class MethodInvocCollector extends ASTVisitor{
		public ArrayList<MethodInvocation> methodInvocs = new ArrayList<MethodInvocation>();
		public ArrayList<ClassInstanceCreation> classInstanceCreations = new ArrayList<ClassInstanceCreation>();
		
		@Override
		public boolean visit(MethodInvocation node){
			methodInvocs.add(node);
			return true;
		}
		
		@Override
		public boolean visit(ClassInstanceCreation node){
			classInstanceCreations.add(node);
			return true;
		}
		
	}
}
