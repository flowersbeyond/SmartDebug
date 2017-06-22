package cn.edu.thu.tsmart.tool.da.core.search.strategy.npe.fs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.EclipseUtils;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.ConditionFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.GnrFixSiteManager;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.StatementFixSite;
import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSite;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

public class NPEFixSiteManager extends GnrFixSiteManager{

	private HashMap<String, Set<Integer>> sliceMap;
	public NPEFixSiteManager(BugFixSession session) {
		super(session);
		// TODO Auto-generated constructor stub
	}
	
	public void setSlice(Collection<Statement> slice) {
		this.sliceMap = new HashMap<String, Set<Integer>>();
		for(Statement stmt: slice){
			if(stmt instanceof StatementWithInstructionIndex){
				StatementWithInstructionIndex indexedStmt = (StatementWithInstructionIndex)stmt;
				int instructionIndex = indexedStmt.getInstructionIndex();
				IMethod method = stmt.getNode().getMethod();
				String methodKey = getMethodKey(method);
				if(!sliceMap.containsKey(methodKey)){
					sliceMap.put(methodKey, new HashSet<Integer>());
				}
				try {
					SourcePosition sp = method.getSourcePosition(instructionIndex);
					if(sp != null){
						sliceMap.get(methodKey).add(sp.getFirstLine());
					}
				} catch (InvalidClassFileException e) {
					e.printStackTrace();
				}
			}
		}
	}
		
	private String getMethodKey(IMethod method){
		//TODO:
		String classKey = method.getDeclaringClass().getName().toString();
		if(classKey.startsWith("L")){
			classKey = classKey.substring(1);
		}
		String key = classKey + ":" + method.getName().toString() + ":" + method.getDescriptor().toString();
		return key;
	}
	
	public ArrayList<AbstractFixSite> getFixSitesFromLocation(String methodKey,
			int startLineNum, int endLineNum){
		ArrayList<AbstractFixSite> npeSites = new ArrayList<AbstractFixSite>();
		
		if(!sliceMap.containsKey(methodKey))
			return npeSites;
		Set<Integer> coveredLines = sliceMap.get(methodKey);
		Set<Integer> willCoverLines = new HashSet<Integer>();
		ArrayList<AbstractFixSite> fixSites = super.getFixSitesFromLocation(methodKey, startLineNum, endLineNum);
		for(AbstractFixSite fs: fixSites){
			if(fs instanceof ConditionFixSite){
				ConditionFixSite condfs = (ConditionFixSite)fs;
				ASTNode expr = condfs.getConditionExpression();
				MethodInvocationCollector collector = new MethodInvocationCollector();
				expr.accept(collector);
				ArrayList<MethodInvocation> invoks = collector.getMethodInvoks();
				
				for(MethodInvocation invok: invoks){
				
					int lineNum = EclipseUtils.getLineNum(invok);
					if(coveredLines.contains(lineNum)){
						Expression caller = invok.getExpression();
						if(caller != null){
							NPEFixSite npefs = new NPEFixSite(fs.getFile(), methodKey, lineNum, lineNum, caller);
							willCoverLines.add(lineNum);
							npeSites.add(npefs);
						}
					}
				
				}
			}
			if(fs instanceof StatementFixSite){
				StatementFixSite stmtfs = (StatementFixSite)fs;
				List<? extends ASTNode> stmts = stmtfs.getStatements();
				for(ASTNode stmt: stmts){
					
					MethodInvocationCollector collector = new MethodInvocationCollector();
					stmt.accept(collector);
					ArrayList<MethodInvocation> invoks = collector.getMethodInvoks();
					
					for(MethodInvocation invok: invoks){
					
						int lineNum = EclipseUtils.getLineNum(invok);
						if(coveredLines.contains(lineNum)){
							Expression caller = invok.getExpression();
							if(caller != null){
								NPEFixSite npefs = new NPEFixSite(fs.getFile(), methodKey, lineNum, lineNum, caller);
								willCoverLines.add(lineNum);
								npeSites.add(npefs);
							}
						}
					
					}
				}
			}
		}
		
		coveredLines.removeAll(willCoverLines);
		if(coveredLines.isEmpty()){
			sliceMap.remove(methodKey);
		}
		return npeSites;
		
	}

	public void clearCache(){
		super.clearCache();
	}

}


class MethodInvocationCollector extends ASTVisitor{
	ArrayList<MethodInvocation> invoks = new ArrayList<MethodInvocation>();
	
	public ArrayList<MethodInvocation> getMethodInvoks(){
		return invoks;
	}
	
	@Override
	public boolean visit(MethodInvocation node){
		invoks.add(node);
		return true;
	}
}