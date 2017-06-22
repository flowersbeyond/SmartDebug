package cn.edu.thu.tsmart.tool.da.core.search.strategy.npe;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.Signature;

import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.ImmutableByteArray;
import com.ibm.wala.util.strings.UTF8Convert;

public class NPESlicer {
	 public static Collection<Statement> doSlicing(TestCase tc, AnalysisScope scope, ClassHierarchy cha, NPEInfo npeInfo) {
		 HashSet<Entrypoint> entrypoints = new HashSet<Entrypoint>();
		 String classSignature = Signature.createTypeSignature(tc.getClassName(), true);
		 classSignature = classSignature.substring(0, classSignature.lastIndexOf(";"));
		 classSignature = classSignature.replaceAll("\\.", "/");
		 MethodReference method = scope.findMethod(AnalysisScope.APPLICATION, classSignature, Atom.findOrCreateUnicodeAtom(tc.getMethodName()),
			        new ImmutableByteArray(UTF8Convert.toUTF8("()V")));
			IMethod testMethod = cha.resolveMethod(method);
		 DefaultEntrypoint testEntrypoint = new DefaultEntrypoint(testMethod, cha);
		 entrypoints.add(testEntrypoint);
	     AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

        // build the call graph
        com.ibm.wala.ipa.callgraph.CallGraphBuilder cgb = Util.makeZeroCFABuilder(options, new AnalysisCache(),cha, scope, null, null);
        CallGraph cg = null;
		try {
			cg = cgb.makeCallGraph(options,null);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CallGraphBuilderCancelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        PointerAnalysis pa = cgb.getPointerAnalysis();

        // find seed statement
        /*String methodName = parseMethodName(npeInfo);
        String methodSignature = parseMethodSignature(npeInfo);
        String callName = parseCallName(npeInfo);
        int lineNum = parseLineNum(npeInfo);
        */
        String methodName = npeInfo.getMethodName();
        String methodSignature = npeInfo.getMethodSignature();
        String callName = npeInfo.getCallName();
        int lineNum = npeInfo.getLineNum();
        
        Statement statement = findCallTo(findMethod(cg, methodName, methodSignature), callName, lineNum);
        
        // context-sensitive traditional slice
        Collection<Statement> slice = null;
		try {
			slice = Slicer.computeBackwardSlice (statement, cg, pa);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CancelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        dumpSlice(slice);
        return slice;
    }

    

	public static CGNode findMethod(CallGraph cg, String methodName, String methodSignature) {
	    	//TODO: which method?
	      Descriptor d = Descriptor.findOrCreateUTF8(methodSignature);
	      Atom name = Atom.findOrCreateUnicodeAtom(methodName);
//	      Collection<CGNode> entrypoints = cg.getEntrypointNodes();
//	      for(CGNode entry: entrypoints){
//	    	  Iterator<? extends CGNode> it = cg.iterator();
//	    	  
//	    	  for (Iterator<? extends CGNode> it = cg.; it.hasNext()
//	      }
	      for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
	        CGNode n = it.next();
	        System.out.println(n.getGraphNodeId() + ":" + n.getMethod().getName().toString() + n.getMethod().getDescriptor().toString());
	        
	        if (n.getMethod().getName().toString().equals(methodName)){
	        	
	        	if(n.getMethod().getDescriptor().toString().equals(methodSignature)) {
	        		return n;
	        	}
	        }
	      }
	      Assertions.UNREACHABLE("failed to find main() method");
	      return null;
	    }

	public static Statement findCallTo(CGNode n, String callName, int lineNum) {
	      IR ir = n.getIR();
	      for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
	        SSAInstruction s = it.next();
	        if (s instanceof com.ibm.wala.ssa.SSAAbstractInvokeInstruction) {
	          com.ibm.wala.ssa.SSAAbstractInvokeInstruction call = (com.ibm.wala.ssa.SSAAbstractInvokeInstruction) s;
	          if (call.getCallSite().getDeclaredTarget().getName().toString().equals(callName)) {
	        	  SourcePosition sp = null;
				try {
					sp = n.getMethod().getSourcePosition(s.iindex);
				} catch (InvalidClassFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	  if(sp.getFirstLine() <= lineNum && lineNum <= sp.getLastLine()){
		            com.ibm.wala.util.intset.IntSet indices = ir.getCallInstructionIndices(call.getCallSite());
		            //com.ibm.wala.util.debug.Assertions.productionAssertion(indices.size() == 1, "expected 1 but got " + indices.size());
		            return new com.ibm.wala.ipa.slicer.NormalStatement(n, indices.intIterator().next());
	        	  }
	          }
	        }
	      }
	      //Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + n);
	      return null;
	    }

	public static void dumpSlice(Collection<Statement> slice) {
	    for (Statement s : slice) {
	       System.out.println(s);
	    }
	}
}



