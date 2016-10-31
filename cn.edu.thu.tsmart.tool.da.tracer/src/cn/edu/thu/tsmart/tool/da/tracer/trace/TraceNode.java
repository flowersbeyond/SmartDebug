package cn.edu.thu.tsmart.tool.da.tracer.trace;

import java.util.ArrayList;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSACFG;

public class TraceNode extends AbstractCommonTraceNode {
	private String methodKey;
	private ArrayList<SSACFG.BasicBlock> blocktrace;
	private int instructionIndex;
	private int lineNumStart;
	private int lineNumEnd;

	private InvokeTraceNode callerNode;
	private ArrayList<InvokeTraceNode> calleeList = new ArrayList<InvokeTraceNode>();

	public TraceNode(String methodKey, ArrayList<SSACFG.BasicBlock> coveredBlocks, int instructionIndex) {
		this.methodKey = methodKey;
		this.blocktrace = coveredBlocks;
		this.instructionIndex = instructionIndex;
		if (blocktrace == null) {
			lineNumStart = -1;
			lineNumEnd = -1;
		} else {
			lineNumStart = Integer.MAX_VALUE;
			lineNumEnd = -1;
			for (int i = 0; i < blocktrace.size(); i++) {
				IMethod method = blocktrace.get(i).getMethod();
				int bytecodeInstructionIndex = 0;
				if (method instanceof IBytecodeMethod) {
					try {
						bytecodeInstructionIndex = ((IBytecodeMethod) method).getBytecodeIndex(instructionIndex);
					} catch (InvalidClassFileException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				int lineNum = method.getLineNumber(bytecodeInstructionIndex);
				if (lineNum < lineNumStart)
					lineNumStart = lineNum;
				if (lineNum > lineNumEnd)
					lineNumEnd = lineNum;
			}
		}
	}

	public String getMethodKey() {
		return methodKey;
	}

	public ArrayList<SSACFG.BasicBlock> getBlockTrace() {
		return blocktrace;
	}

	public int getInstructionIndex() {
		return instructionIndex;
	}

	public void addCalleeTraceNode(InvokeTraceNode calleeTraceNode) {
		calleeList.add(calleeTraceNode);
		calleeTraceNode.setCallSiteNode(this);
	}

	public InvokeTraceNode getLastCalleeTraceNode() {
		if (calleeList.size() == 0)
			return null;
		return calleeList.get(calleeList.size() - 1);
	}

	public ArrayList<InvokeTraceNode> getCalleeList() {
		return calleeList;
	}

	public InvokeTraceNode getCallerNode() {
		return callerNode;
	}

	public void setCallerNode(InvokeTraceNode callerNode) {
		this.callerNode = callerNode;
	}

	@Override
	public String toString() {
		String nodeString = "";
		if (blocktrace == null) {
			nodeString += "Enter method: " + methodKey;
		} else {
			nodeString += methodKey + ": line " + lineNumStart + " - " + lineNumEnd;
			/*
			 * for(int i = 0; i < blocktrace.size(); i ++){ IMethod method =
			 * blocktrace.get(i).getMethod(); int bytecodeInstructionIndex = 0;
			 * if(method instanceof IBytecodeMethod){ try {
			 * bytecodeInstructionIndex = ((IBytecodeMethod)
			 * method).getBytecodeIndex(instructionIndex); } catch
			 * (InvalidClassFileException e) { // TODO Auto-generated catch
			 * block e.printStackTrace(); } } nodeString +=
			 * method.getSignature() + ": line:" +
			 * method.getLineNumber(bytecodeInstructionIndex) + ";\n"; }
			 */
		}
		return nodeString;
	}

	/**
	 * e.g. org/eclipse/ast
	 * @return
	 */
	public String getClassName() {
		String[] keys = methodKey.split(":");
		return keys[0];
	}

	public int getStartLineNum() {
		return lineNumStart;
	}

	public int getEndLineNum() {
		return lineNumEnd;
	}

}
