package cn.edu.thu.tsmart.tool.da.tracer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jdt.core.Signature;

import cn.edu.thu.tsmart.tool.da.tracer.action.BranchAction;
import cn.edu.thu.tsmart.tool.da.tracer.action.EnterAction;
import cn.edu.thu.tsmart.tool.da.tracer.action.ExitAction;
import cn.edu.thu.tsmart.tool.da.tracer.action.InvokeAction;
import cn.edu.thu.tsmart.tool.da.tracer.action.TraceAction;
import cn.edu.thu.tsmart.tool.da.tracer.action.TraceActionFactory;
import cn.edu.thu.tsmart.tool.da.tracer.trace.InvokeTraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.trace.TraceNode;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.ImmutableByteArray;
import com.ibm.wala.util.strings.UTF8Convert;

public class TraceTranslator {

	
	private AnalysisScope scope;
	private ClassHierarchy cha;
	
	private HashMap<String, SSACFG> cfgCache = new HashMap<String, SSACFG>();
	
	private ArrayList<TraceAction> traceActions;
	public TraceTranslator(){
		
	}
	
	public TraceTranslator(String tracefile, AnalysisScope scope, ClassHierarchy cha){
		this.scope = scope;
		this.cha = cha;
		traceActions = new ArrayList<TraceAction>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(tracefile)));
			String s = reader.readLine();
			while(s != null){
				TraceAction action = TraceActionFactory.parse(s);
				traceActions.add(action);
				s = reader.readLine();
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<TraceNode> translate(){
		ArrayList<TraceNode> trace = new ArrayList<TraceNode>();
		int start = 0;
		for(start = 0; start < traceActions.size(); start++){
			if(traceActions.get(start) instanceof EnterAction)
				break;
		}
		
		if(start == traceActions.size()){
			return trace;
		} else{
			while(start < traceActions.size()){
				ArrayList<TraceNode> tracefrag = new ArrayList<TraceNode>();
				start = translate(tracefrag, start);
				trace.addAll(tracefrag);
			}
		}	
		return trace;
	}
	
	/**
	 * recover trace blocks for a single method
	 * Note: if traceActions.get(start) is not an EnterAction, this means that 
	 * the method were not traced, probably some system lib or jars 
	 * that we don't care. so we simply return "start" as the "ending" position.
	 * @param trace the trace we want to generate for this method call
	 * @param start the starting position in traceActions for this method call
	 * @return the (ending position + 1) for this method call
	 */
	private int translate(ArrayList<TraceNode> trace, int start){
		if(!(traceActions.get(start) instanceof EnterAction)){
			return start;
		}
		int i = start;
		SSACFG.BasicBlock currentBlock = null;
		SSACFG cfg = null;
		while(i < traceActions.size()){
			TraceAction action = traceActions.get(i);
			if(action instanceof EnterAction){
				//get the cfg from cfg cache or generate a new one.
				String methodKey = action.getClassName() + ":" + action.getMethodName() + ":" + action.getMethodSignature();
				cfg = cfgCache.get(methodKey);
				if(cfg == null){
					String classSig = Signature.createTypeSignature(action.getClassName(), true);
					if(classSig.endsWith(";"))
						classSig = classSig.substring(0,classSig.length() - 1);
					cfg = generateCFG(classSig, action.getMethodName(), action.getMethodSignature());
					cfgCache.put(methodKey, cfg);
				}
				//record the first basicblock here.
				SSACFG.BasicBlock entryBlock= findEntryBlock(cfg);
				// entryblock is empty!! So no traceblocks are added to the first trace node.
				TraceNode node = new TraceNode(methodKey, null, action.getInstructionIndex(), -1);
				trace.add(node);
				currentBlock = entryBlock;
				i ++;
			}
			else if (action instanceof ExitAction){
				return i + 1;
			} else if (action instanceof BranchAction){
				//find next block
				ArrayList<SSACFG.BasicBlock> coveredBlocks = travelToNext(cfg, currentBlock, action.getInstructionIndex());
				
				String methodKey = action.getClassName() + ":" + action.getMethodName() + ":" + action.getMethodSignature();
				TraceNode node = new TraceNode(methodKey, coveredBlocks, action.getInstructionIndex(), -1);
				trace.add(node);
				currentBlock = coveredBlocks.get(coveredBlocks.size() - 1);
				i ++;
			} else if (action instanceof InvokeAction){
				InvokeAction ivkaction = (InvokeAction)action;
				InvokeTraceNode invokeNode = new InvokeTraceNode(ivkaction.getCalleeClassName(),
						ivkaction.getCalleeMethodName(), ivkaction.getCalleeMethodSignature());
				
				TraceNode currentNode = trace.get(trace.size() - 1);
				currentNode.addCalleeTraceNode(invokeNode);
				
				ArrayList<TraceNode> calleeTrace = new ArrayList<TraceNode>();
				i = translate(calleeTrace, i + 1);
				invokeNode.setCalleeTrace(calleeTrace);
			}
		}
		return i;
	}
	
	private static SSACFG.BasicBlock findEntryBlock(SSACFG cfg){
		return cfg.entry();
	}
	
	private static ArrayList<SSACFG.BasicBlock> travelToNext(SSACFG cfg, SSACFG.BasicBlock currentBlock, int instructionIndex){
		
		ArrayList<SSACFG.BasicBlock>traceblock = new ArrayList<SSACFG.BasicBlock>();
		SSACFG.BasicBlock index = currentBlock;
		
		while(!index.isExitBlock()){
			Iterator<ISSABasicBlock>iter = cfg.getSuccNodes(index);
			boolean found = false;
			SSACFG.BasicBlock nonExitBlock = null;
			SSACFG.BasicBlock targetBlock = null;
			while(iter.hasNext()){
				ISSABasicBlock block = iter.next();
				if(!block.isExitBlock()){
					nonExitBlock = (SSACFG.BasicBlock)block;
					if(block.getFirstInstructionIndex() == instructionIndex){
						found = true;
						targetBlock = (SSACFG.BasicBlock)block;
						break;
					}
				}					
			}
			if(found){
				traceblock.add(targetBlock);
				return traceblock;
			}
			traceblock.add(nonExitBlock);
			index = nonExitBlock;
		}
		//this should never happen. we'll check it out
		return traceblock;
	}
	
	private SSACFG generateCFG(String classSignature, String methodName, String methodSignature){
		MethodReference method = scope.findMethod(AnalysisScope.APPLICATION, classSignature, Atom.findOrCreateUnicodeAtom(methodName),
		        new ImmutableByteArray(UTF8Convert.toUTF8(methodSignature)));
		IMethod imethod = cha.resolveMethod(method);
		AnalysisOptions options = new AnalysisOptions(scope, null);
		IR ir = new AnalysisCache().getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
		return ir.getControlFlowGraph();
	}
	
}


