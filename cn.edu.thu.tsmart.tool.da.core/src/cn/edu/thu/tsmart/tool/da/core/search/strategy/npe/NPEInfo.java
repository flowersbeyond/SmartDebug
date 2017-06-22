package cn.edu.thu.tsmart.tool.da.core.search.strategy.npe;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class NPEInfo {

	// find seed statement
    String methodName;
    String methodSignature;
    String callName;
    int lineNum;
    
    public NPEInfo(String npeInfo, IJavaProject proj){
 		String[] npeStacks = npeInfo.split("\n");
		String srcStackString = npeStacks[1];
		
		//	at testcases.CWE476_NULL_Pointer_Dereference.CWE476_NULL_Pointer_Dereference__binary_if_01.bad(CWE476_NULL_Pointer_Dereference__binary_if_01.java:30)
		String methodKey = srcStackString.substring(srcStackString.indexOf("at") + 3, srcStackString.indexOf("("));
		String className = methodKey.substring(0, methodKey.lastIndexOf('.'));
		methodName = methodKey.substring(methodKey.lastIndexOf('.') + 1);
		
		String locString = srcStackString.substring(srcStackString.indexOf('(') + 1, srcStackString.indexOf(')'));
		lineNum = Integer.parseInt(locString.substring(locString.indexOf(':') + 1));
		
		IFile file = null;
		try {
			file = (IFile)proj.findType(className).getUnderlyingResource();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ICompilationUnit compilationUnit = (ICompilationUnit)JavaCore.create(file);
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setResolveBindings(true);
		parser.setSource(compilationUnit);
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		MethodFinder finder = new MethodFinder(methodName, lineNum);
		cu.accept(finder);
		MethodDeclaration md = finder.getMethodDeclaration();
		
		ITypeBinding[] paramType = md.resolveBinding().getParameterTypes();
		String[] paramTypeNames = new String[paramType.length];
		for(int i = 0; i < paramType.length; i ++){
			paramTypeNames[i] = Signature.createTypeSignature(paramType[i].getQualifiedName(),true);
		}
		String returnTypeName = Signature.createTypeSignature(md.resolveBinding().getReturnType().getQualifiedName(),true);
		methodSignature = Signature.createMethodSignature(paramTypeNames, returnTypeName);
		
		callName = finder.getCallName();
    }
    
    public String getMethodName(){
    	return methodName;
    }
    
    public String getMethodSignature(){
    	return methodSignature;
    }
    
    public String getCallName(){
    	return callName;
    }
    
    public int getLineNum(){
    	return this.lineNum;
    }
	   
    
    
}

class MethodFinder extends ASTVisitor{
	
	private int lineNum;
	private String methodName;
	public MethodFinder(String methodName, int lineNum){
		this.methodName = methodName;
		this.lineNum = lineNum;
	}
	private MethodDeclaration md;
	public MethodDeclaration getMethodDeclaration(){
		return md;
	}
	
	private String callName;
	public String getCallName(){
		return callName;
	}
	
	@Override
	public boolean visit(MethodDeclaration node){
		CompilationUnit cu = (CompilationUnit)node.getRoot();
		int startLineNum = cu.getLineNumber(node.getStartPosition());
		int endLineNum = cu.getLineNumber(node.getStartPosition() + node.getLength());
		if(node.getName().toString().equals(methodName) && startLineNum <= lineNum && endLineNum >= lineNum){
			this.md = node;
			CallFinder cf = new CallFinder(lineNum);
			md.accept(cf);
			this.callName = cf.getCallName();
		}
			
		return true;
	}
	
}

class CallFinder extends ASTVisitor{
	private int lineNum;
	public CallFinder(int lineNum){
		this.lineNum = lineNum;
	}
	
	private String callName;
	
	@Override
	public boolean visit(MethodInvocation node){
		CompilationUnit cu = (CompilationUnit)node.getRoot();
		int startLineNum = cu.getLineNumber(node.getStartPosition());
		int endLineNum = cu.getLineNumber(node.getStartPosition() + node.getLength());
		if(startLineNum <= lineNum && endLineNum >= lineNum){
			this.callName = node.getName().toString();
			return false;
		}
		return true;
	}
	
	public String getCallName(){
		return this.callName;
	}
}