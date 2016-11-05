package cn.edu.thu.tsmart.tool.da.tracer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import cn.edu.thu.tsmart.tool.da.tracer.action.BranchAction;
import cn.edu.thu.tsmart.tool.da.tracer.action.EnterAction;
import cn.edu.thu.tsmart.tool.da.tracer.action.ExitAction;
import cn.edu.thu.tsmart.tool.da.tracer.action.InvokeAction;
import cn.edu.thu.tsmart.tool.da.tracer.action.SuspendAction;
import cn.edu.thu.tsmart.tool.da.tracer.action.TraceAction;
import cn.edu.thu.tsmart.tool.da.tracer.action.TraceActionFactory;
import cn.edu.thu.tsmart.tool.da.tracer.trace.InvokeTraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.TraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.util.EclipseUtils;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.ImmutableByteArray;
import com.ibm.wala.util.strings.UTF8Convert;

public class DynamicTranslator {
	
	private static AnalysisScope scope;
	private static ClassHierarchy cha;

	private ArrayList<InvokeTraceNode> rootInvocationTrace = new ArrayList<InvokeTraceNode>();
	private Stack<TraceStackFrame> traceStack = new Stack<TraceStackFrame>();
	private boolean includeNewBlocks;
	private int timeStamp = 0;
	private boolean failCPFound = false;
	
	public static ArrayList<ITraceEventListener> listeners = new ArrayList<ITraceEventListener>();
	
	public static void clearAnalysisScope(){
		scope = null;
		cha = null;
	}
	public DynamicTranslator(IJavaProject project, boolean includeNewBlocks) {
		this.includeNewBlocks = includeNewBlocks;
		if(scope == null || cha == null){
			String projectClassPath;
			try {
				String projectPath = EclipseUtils.getProjectDir(project);
				File scopeFile = new File(projectPath + "bin-scope.txt");
				if(scopeFile.exists()){
					scope = AnalysisScopeReader.readJavaScope(projectPath + "bin-scope.txt", null, DynamicTranslator.class.getClassLoader());
				} else {
					projectClassPath = EclipseUtils.getProjectDir(project)
							+ "/bin/";
					scope = AnalysisScopeReader
							.makeJavaBinaryAnalysisScope(projectClassPath, null);
				}
				cha = ClassHierarchy.make(scope);
			} catch (JavaModelException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassHierarchyException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void handleNewActions(){
		ArrayList<String> actionMessages = TraceRecordContainer.flush();
		System.out.println("MESSAGE SIZE:" + actionMessages.size());
		ArrayList<TraceAction>actions = new ArrayList<TraceAction>();
		for(int i = 0; i < actionMessages.size(); i ++){
			actions.add(TraceActionFactory.parse(actionMessages.get(i)));
		}
		for(int i = 0; i < actions.size(); i ++){
			handleNewAction(actions.get(i));
		}
		fireTraceEvent("");

	}
	
	private void handleNewAction(TraceAction action){
		if(action instanceof EnterAction){
			
			//get the cfg from cfg cache or generate a new one.
			String methodKey = action.getClassName() + ":" + action.getMethodName() + ":" + action.getMethodSignature();
			SSACFG cfg = CFGCache.get(methodKey);
			if(cfg == null){
				if(includeNewBlocks){
					String classSig = Signature.createTypeSignature(action.getClassName(), true);
					if(classSig.endsWith(";"))
						classSig = classSig.substring(0,classSig.length() - 1);
					//if(classSig.indexOf('$') != -1)
						//classSig = classSig.replace('$', '/');
					String methodSig = action.getMethodSignature();
					//if(methodSig.indexOf('$') != -1)
						//methodSig = methodSig.replace('$', '/');
					cfg = generateCFG(classSig, action.getMethodName(), methodSig);				
					
					if(cfg != null)
						CFGCache.put(methodKey, cfg);
					else return;
				}
				else
					return;
			}
			
			//record the first basicblock here.
			SSACFG.BasicBlock entryBlock= findEntryBlock(cfg);
			
			
			ArrayList<TraceNode> trace = null;
			if(!traceStack.isEmpty()){
				// if this is a callee of another caller, then the trace (ArrayList<TraceNode>)
				// is already initialized in the caller's "invoke action".
				// we only need to get it from the caller's stack frame.
				TraceStackFrame frame = traceStack.peek();
				trace = frame.getTrace();
				TraceNode lastNode = trace.get(trace.size() - 1);
				ArrayList<InvokeTraceNode> invokeList = lastNode.getCalleeList();
				if(invokeList.size() == 0){
					//This must come from the "<clinit>" function. We add a new invoketracenode in the caller site:
					InvokeTraceNode node = new InvokeTraceNode(action.getClassName(),
							action.getMethodName(), action.getMethodSignature());
					ArrayList<TraceNode> calleeTrace = new ArrayList<TraceNode>();
					node.setCalleeTrace(calleeTrace);
					invokeList.add(node);
				} else {
					InvokeTraceNode lastInvokeNode = invokeList.get(invokeList.size() - 1);
					if((!lastInvokeNode.correspondsToCallee(action.getClassName(), action.getMethodName(), action.getMethodSignature()))){
						// in some rare cases, an enter action may not be leaded by an invoke action.
						// so we need to build a new invoketraceaction in the caller site.
						InvokeTraceNode node = new InvokeTraceNode(action.getClassName(),
								action.getMethodName(), action.getMethodSignature());
						ArrayList<TraceNode> calleeTrace = new ArrayList<TraceNode>();
						node.setCalleeTrace(calleeTrace);
						invokeList.add(node);
					}
				}
				InvokeTraceNode lastInvokeNode = invokeList.get(invokeList.size() - 1);
				trace = lastInvokeNode.getCalleeTrace();
			} else {
				// this is a "root caller"
				trace = new ArrayList<TraceNode>();
				InvokeTraceNode root = new InvokeTraceNode(action.getClassName(),
						action.getMethodName(), action.getMethodSignature());
				root.setCalleeTrace(trace);
				rootInvocationTrace.add(root);
			}
			
			// entryblock is empty!! So no traceblocks are added to the first trace node.
			TraceNode node = new TraceNode(methodKey, null, action.getInstructionIndex(), timeStamp);
			trace.add(node);
			
			TraceStackFrame newFrame = new TraceStackFrame(trace, cfg, entryBlock);
			traceStack.push(newFrame);
		}
		else if (action instanceof ExitAction){
			String methodKey = action.getClassName() + ":" + action.getMethodName() + ":" + action.getMethodSignature();
			SSACFG cfg = CFGCache.get(methodKey);
			if(cfg == null){
				return;
			}
			TraceStackFrame frame = traceStack.peek();
			cfg = frame.getCfg();
			SSACFG.BasicBlock currentBlock = frame.getCurrentBlock();
			ArrayList<TraceNode> trace = frame.getTrace();
			
			//find exit block
			ArrayList<SSACFG.BasicBlock> coveredBlocks = travelToExit(cfg, currentBlock, action.getInstructionIndex());
			if(!coveredBlocks.isEmpty()){
				methodKey = action.getClassName() + ":" + action.getMethodName() + ":" + action.getMethodSignature();
				TraceNode node = new TraceNode(methodKey, coveredBlocks, action.getInstructionIndex(), timeStamp);
				trace.add(node);
			}
			traceStack.pop();
		} else if (action instanceof BranchAction){
			String methodKey = action.getClassName() + ":" + action.getMethodName() + ":" + action.getMethodSignature();
			SSACFG cfg = CFGCache.get(methodKey);
			if(cfg == null){
				return;
			}
			
			TraceStackFrame frame = traceStack.peek();
			
			cfg = frame.getCfg();
			SSACFG.BasicBlock currentBlock = frame.getCurrentBlock();
			ArrayList<TraceNode> trace = frame.getTrace();
			
			//find next block
			ArrayList<SSACFG.BasicBlock> coveredBlocks = travelToNext(cfg, currentBlock, action.getInstructionIndex());
			if(!coveredBlocks.isEmpty()){
				methodKey = action.getClassName() + ":" + action.getMethodName() + ":" + action.getMethodSignature();
				TraceNode node = new TraceNode(methodKey, coveredBlocks, action.getInstructionIndex(), timeStamp);
				trace.add(node);
				frame.setCurrentBlock(coveredBlocks.get(coveredBlocks.size() - 1));
			}
		} else if (action instanceof InvokeAction){
			String methodKey = action.getClassName() + ":" + action.getMethodName() + ":" + action.getMethodSignature();
			SSACFG cfg = CFGCache.get(methodKey);
			if(cfg == null){
				return;
			}
			TraceStackFrame frame = traceStack.peek();
			cfg = frame.getCfg();
			SSACFG.BasicBlock currentBlock = frame.getCurrentBlock();
			ArrayList<TraceNode> trace = frame.getTrace();
			
			//find blocks to the method invocation
			ArrayList<SSACFG.BasicBlock> coveredBlocks = travelToMethodInvok(cfg, currentBlock, action.getInstructionIndex());
			if(!coveredBlocks.isEmpty()){
				methodKey = action.getClassName() + ":" + action.getMethodName() + ":" + action.getMethodSignature();
				TraceNode node = new TraceNode(methodKey, coveredBlocks, action.getInstructionIndex(), timeStamp);
				trace.add(node);
				frame.setCurrentBlock(coveredBlocks.get(coveredBlocks.size() - 1));
			}
			
			InvokeAction ivkaction = (InvokeAction)action;
			InvokeTraceNode invokeNode = new InvokeTraceNode(ivkaction.getCalleeClassName(),
					ivkaction.getCalleeMethodName(), ivkaction.getCalleeMethodSignature());
			TraceNode currentNode = trace.get(trace.size() - 1);
			currentNode.addCalleeTraceNode(invokeNode);
			
			ArrayList<TraceNode> calleeTrace = new ArrayList<TraceNode>();
			invokeNode.setCalleeTrace(calleeTrace);
		}
	}
	
	private SSACFG generateCFG(String classSignature, String methodName, String methodSignature){
		MethodReference method = scope.findMethod(AnalysisScope.APPLICATION, classSignature, Atom.findOrCreateUnicodeAtom(methodName),
		        new ImmutableByteArray(UTF8Convert.toUTF8(methodSignature)));
		IMethod imethod = cha.resolveMethod(method);
		if(imethod == null){
			return null;
			//System.out.println("Cannot resolve method: " + classSignature + ":" + methodName + ":" + methodSignature);
		}
		AnalysisOptions options = new AnalysisOptions(scope, null);
		IR ir = new AnalysisCache().getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
		return ir.getControlFlowGraph();
	}
	
	private static SSACFG.BasicBlock findEntryBlock(SSACFG cfg){
		return cfg.entry();
	}
	
	private static ArrayList<SSACFG.BasicBlock> travelToNext(SSACFG cfg, SSACFG.BasicBlock currentBlock, int instructionIndex){
		
		ArrayList<SSACFG.BasicBlock>traceblock = new ArrayList<SSACFG.BasicBlock>();
		
		Map<Integer, Integer> traceBackRecord = new HashMap<Integer, Integer>();
		ArrayList<SSACFG.BasicBlock> searchQueue = new ArrayList<SSACFG.BasicBlock>();
		
		Iterator<ISSABasicBlock>iter = cfg.getSuccNodes(currentBlock);
		while(iter.hasNext()){
			searchQueue.add((SSACFG.BasicBlock)iter.next());
		}
		int index = 0;
		while(index < searchQueue.size() && index < cfg.getNumberOfNodes()){
			SSACFG.BasicBlock block = searchQueue.get(index);
			if(block.getFirstInstructionIndex() == instructionIndex){
				int currentIndex = index;
				while(traceBackRecord.containsKey(currentIndex)){
					traceblock.add(0, searchQueue.get(currentIndex));
					currentIndex = traceBackRecord.get(currentIndex);
				}
				traceblock.add(0, searchQueue.get(currentIndex));
				return traceblock;
			} else{
				iter = cfg.getSuccNodes(block);
				while(iter.hasNext()){
					// the index number of the newly added block is searchQueue.size()
					traceBackRecord.put(searchQueue.size(), index);
					searchQueue.add((SSACFG.BasicBlock)iter.next());
				}
			}
			index ++;
		}
		
		//this should never happen. we'll check it out
		return traceblock;
	}
	
private static ArrayList<SSACFG.BasicBlock> travelToMethodInvok(SSACFG cfg, SSACFG.BasicBlock currentBlock, int methodInvokIndex){
		
		ArrayList<SSACFG.BasicBlock>traceblock = new ArrayList<SSACFG.BasicBlock>();
		
		if(currentBlock.getLastInstructionIndex() == methodInvokIndex)
			return traceblock;
		
		Map<Integer, Integer> traceBackRecord = new HashMap<Integer, Integer>();
		ArrayList<SSACFG.BasicBlock> searchQueue = new ArrayList<SSACFG.BasicBlock>();
		
		Iterator<ISSABasicBlock>iter = cfg.getSuccNodes(currentBlock);
		while(iter.hasNext()){
			searchQueue.add((SSACFG.BasicBlock)iter.next());
		}
		int index = 0;
		while(index < searchQueue.size() && index < cfg.getNumberOfNodes()){
			SSACFG.BasicBlock block = searchQueue.get(index);
			if(block.getLastInstructionIndex() == methodInvokIndex){
				int currentIndex = index;
				while(traceBackRecord.containsKey(currentIndex)){
					traceblock.add(0, searchQueue.get(currentIndex));
					currentIndex = traceBackRecord.get(currentIndex);
				}
				traceblock.add(0, searchQueue.get(currentIndex));
				return traceblock;
			} else{
				iter = cfg.getSuccNodes(block);
				while(iter.hasNext()){
					// the index number of the newly added block is searchQueue.size()
					traceBackRecord.put(searchQueue.size(), index);
					searchQueue.add((SSACFG.BasicBlock)iter.next());
				}
			}
			index ++;
		}
		
		//this should never happen. we'll check it out
		return traceblock;
	}
	
	private static ArrayList<SSACFG.BasicBlock> travelToExit(SSACFG cfg, SSACFG.BasicBlock currentBlock, int exitInstructionIndex){
		
		ArrayList<SSACFG.BasicBlock>traceblock = new ArrayList<SSACFG.BasicBlock>();
		
		if(currentBlock.getLastInstructionIndex() == exitInstructionIndex){
			// we are at the exit block already, so we just return an empty block list
			return traceblock;
		}
		
		Map<Integer, Integer> traceBackRecord = new HashMap<Integer, Integer>();
		ArrayList<SSACFG.BasicBlock> searchQueue = new ArrayList<SSACFG.BasicBlock>();
		
		Iterator<ISSABasicBlock>iter = cfg.getSuccNodes(currentBlock);
		while(iter.hasNext()){
			searchQueue.add((SSACFG.BasicBlock)iter.next());
		}
		int index = 0;
		while(index < searchQueue.size() && index < cfg.getNumberOfNodes()){
			SSACFG.BasicBlock block = searchQueue.get(index);
			if(block.getLastInstructionIndex() == exitInstructionIndex){
				int currentIndex = index;
				while(traceBackRecord.containsKey(currentIndex)){
					traceblock.add(0, searchQueue.get(currentIndex));
					currentIndex = traceBackRecord.get(currentIndex);
				}
				traceblock.add(0, searchQueue.get(currentIndex));
				return traceblock;
			} else{
				iter = cfg.getSuccNodes(block);
				while(iter.hasNext()){
					// the index number of the newly added block is searchQueue.size()
					traceBackRecord.put(searchQueue.size(), index);
					searchQueue.add((SSACFG.BasicBlock)iter.next());
				}
			}
			index ++;
		}
		
		//this should never happen. we'll check it out
		return traceblock;
	}
	
	public void addSuspendAction(String className, String methodName, String methodSignature, int hitLineNum){
		
	}

	/**
	 * 
	 * @param launchTerminated deprecated
	 */
	public void fireTraceEvent(String launchTerminated) {
		for(ITraceEventListener l: listeners){
			l.handleEvent(ITraceEventListener.INTRACE, ITraceEventListener.HIT_BREAKPOINT, this);
		}
		
	}

	public static void registerTraceEventListener(ITraceEventListener listener) {
		listeners.add(listener);
		
	}

	public ArrayList<InvokeTraceNode> getCurrentTrace() {
		return this.rootInvocationTrace;
	}
	public void increaseTimeStamp() {
		if(!this.failCPFound)
			this.timeStamp ++;		
	}
	public void setFailCPFound() {
		this.failCPFound = true;		
	}

}

class TraceStackFrame{
	private ArrayList<TraceNode> trace;
	private SSACFG.BasicBlock currentBlock;
	private SSACFG cfg;
	
	public TraceStackFrame(ArrayList<TraceNode> trace, SSACFG cfg, SSACFG.BasicBlock currentBlock){
		this.trace = trace;
		this.currentBlock = currentBlock;
		this.cfg = cfg;
	}
	
	public ArrayList<TraceNode> getTrace() {
		return trace;
	}

	public SSACFG.BasicBlock getCurrentBlock() {
		return currentBlock;
	}
	public void setCurrentBlock(SSACFG.BasicBlock block){
		this.currentBlock = block;
	}
	public SSACFG getCfg() {
		return cfg;
	}
}