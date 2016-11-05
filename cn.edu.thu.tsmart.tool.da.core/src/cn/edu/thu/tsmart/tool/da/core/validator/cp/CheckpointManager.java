package cn.edu.thu.tsmart.tool.da.core.validator.cp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;

import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;

public class CheckpointManager {

	private Map<TestCase, ArrayList<Checkpoint>> conditions;
	private static CheckpointManager cpmanager = new CheckpointManager();
	
	private boolean inSync = false;
	
	public void setSync(){
		inSync = true;
	}
	
	public void setOutOfSync(){
		inSync = false;
	}
	
	public boolean isInSync(){
		return inSync;
	}

	private CheckpointManager() {
		this.conditions = new HashMap<TestCase, ArrayList<Checkpoint>>();
	}

	public static CheckpointManager getInstance() {
		if (cpmanager != null)
			return cpmanager;
		return new CheckpointManager();
	}

	public Map<TestCase, ArrayList<Checkpoint>> getConditions(){
		return this.conditions;
	}
	public void addCheckpoint(TestCase testcase, Checkpoint cp) {
		ArrayList<Checkpoint> cplist = null;		
		for(TestCase tc: conditions.keySet()){
			if(tc.equals(testcase))
				cplist = conditions.get(tc);
				
		}
		if (cplist == null) {
			ArrayList<Checkpoint> newList = new ArrayList<Checkpoint>();
			conditions.put(testcase, newList);
			cplist = newList;
		}
		cplist.add(cp);
	}

	public void removeCheckpoint(Checkpoint cp) {
		ArrayList<Checkpoint> cplist = conditions.get(cp.getOwnerTestCase());
		cplist.remove(cp);
		if(cplist.size() == 0){
			conditions.remove(cp.getOwnerTestCase());
		}
	}

	public ArrayList<TestCase> getHandledTestCases() {
		ArrayList<TestCase> testcases = new ArrayList<TestCase>();
		for (TestCase tc : this.conditions.keySet()) {
			testcases.add(tc);
		}
		return testcases;
	}
	public ArrayList<Checkpoint> getConditionForTestCase(TestCase tc) {
		for(TestCase testcase: conditions.keySet()){
			if(testcase.equals(tc)){
				return conditions.get(testcase);
			}
		}
		return null;
	}

	public Checkpoint findExistingCheckpoint(IFile file,
			TestCase currentTestCase, int lineNumber) {
		for (TestCase tc : conditions.keySet()) {
			if (tc.getClassName().equals(currentTestCase.getClassName())
					&& tc.getMethodName().equals(currentTestCase.getMethodName())) {
				ArrayList<Checkpoint> checkpoints = conditions.get(tc);
				for (Checkpoint cp : checkpoints) {
					if (cp.getFile().equals(file)
							&& cp.getLineNumber() == lineNumber) {
						return cp;
					}
				}
				return null;
			}
		}
		return null;
	}
	
	public void restoreCheckpoints(){
		IMarker[] checkpointMarkers = null;
		int depth = IResource.DEPTH_INFINITE;
		try {
			checkpointMarkers = ResourcesPlugin.getWorkspace().getRoot().findMarkers(
					"cn.thu.edu.thss.tsmart.tool.da.validator.checkpointMarker", true, depth);
			for(int i = 0; i < checkpointMarkers.length; i ++){
				IMarker marker = checkpointMarkers[i];
				Checkpoint cp = CheckpointUtils.createCheckpoint(marker);
				for(TestCase tc:conditions.keySet()){
					TestCase newTC = cp.getOwnerTestCase();
					if(newTC.getClassName().equals(tc.getClassName()) && newTC.getMethodName().equals(tc.getMethodName())){
						cp.setOwnerTestCase(tc);
						break;
					}
				}
				this.addCheckpoint(cp.getOwnerTestCase(), cp);
			}
			for(int i = 0; i < checkpointMarkers.length; i ++){
				checkpointMarkers[i].delete();
			}
		} catch (CoreException e) {
			
		}
	}

	public TestCase getTestCase(String testClassName, String testMethodName) {
		for(TestCase tc: conditions.keySet()){
			if(tc.getClassName().equals(testClassName) && tc.getMethodName().equals(testMethodName))
				return tc;
		}
		return null;
	}

	public ArrayList<Checkpoint> getConditionForTestCase(IMethod method) {
		String testClassName = method.getDeclaringType().getFullyQualifiedName();
		String testMethodName = method.getElementName();
		return getConditionForTestCase(new TestCase(testClassName, testMethodName));
		
	}

	public void ensureAllCPMarkers() {
		for(TestCase tc: conditions.keySet()){
			ArrayList<Checkpoint> checkpoints = conditions.get(tc);
			for(Checkpoint cp: checkpoints){
				cp.ensureCPMarker();
			}
		}
		
	}

	public void registerTestCase(TestCase testCase) {
		if(conditions.containsKey(testCase)){
			for(TestCase tc: conditions.keySet()){
				if(tc.equals(testCase)){
					tc.setStatus(testCase.getStatus());
					return;
				}
			}
		}
		else
			conditions.put(testCase, new ArrayList<Checkpoint>());
		
	}

}
