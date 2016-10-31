package cn.edu.thu.tsmart.tool.da.core.fl;

import cn.edu.thu.tsmart.tool.da.tracer.CFGCache;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSACFG;

public class BasicBlock {

	String methodKey;
	SSACFG.BasicBlock block;
	int startLineNum;
	int endLineNum;
	//TODO: position
	public BasicBlock(String methodKey, SSACFG.BasicBlock block){
		
		this.methodKey = methodKey;
		this.block = block;
		startLineNum = getFirstLineNum();
		endLineNum = getLastLineNum();
	}
	
	public BasicBlock(String methodKey, int startLineNum, int endLineNum){
		this.methodKey = methodKey;
		this.startLineNum = startLineNum;
		this.endLineNum = endLineNum;
	}
	
	public String getClassName() {
		String[] keyparts = methodKey.split(":");
		return keyparts[0];
	}
	
	public String getMethodKey(){
		return methodKey;
	}
	
	public SSACFG.BasicBlock getSSABasicBlock(){
		return block;
	}
	
	private int getFirstLineNum(){
		SSACFG cfg = CFGCache.get(methodKey);
		IMethod method = cfg.getMethod();
		if(method instanceof ShrikeCTMethod){
			ShrikeCTMethod shrikeMethod = (ShrikeCTMethod)method;
			try {
				SourcePosition srcpos = shrikeMethod.getSourcePosition(shrikeMethod.getBytecodeIndex(block.getFirstInstructionIndex()));
				
				return srcpos.getFirstLine();
			} catch (InvalidClassFileException e) {
				e.printStackTrace();
				return -1;
			}
		}
		return -1;
	}
	
	private int getLastLineNum(){
		SSACFG cfg = CFGCache.get(methodKey);
		IMethod method = cfg.getMethod();
		if(method instanceof ShrikeCTMethod){
			ShrikeCTMethod shrikeMethod = (ShrikeCTMethod)method;
			try {
				SourcePosition srcpos = shrikeMethod.getSourcePosition(shrikeMethod.getBytecodeIndex(block.getLastInstructionIndex()));
				
				return srcpos.getLastLine();
			} catch (InvalidClassFileException e) {
				e.printStackTrace();
				return -1;
			}
		}
		return -1;
	}
	
	@Override
	public String toString(){
		String str = methodKey + "line " + startLineNum + " - " + endLineNum;
		return str;
	}

	public static BasicBlock parseString(String string) {
		String[] parts = string.split("#");
		String methodKey = parts[0];
		String startLineNumStr = parts[1];
		String endLineNumStr = parts[2];
		return new BasicBlock(methodKey, Integer.parseInt(startLineNumStr), Integer.parseInt(endLineNumStr));
	}

	public String toDumpString() {
		StringBuffer buf = new StringBuffer(this.methodKey);
		buf.append('#');
		buf.append(startLineNum);
		buf.append('#');
		buf.append(endLineNum);
		return buf.toString();
	}

	public int getStartLineNum() {
		return this.startLineNum;
	}
	
	public int getEndLineNum() {
		return this.endLineNum;
	}

}
