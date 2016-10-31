package cn.edu.thu.tsmart.tool.da.core.validator.cp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.debug.core.breakpoints.ValidBreakpointLocationLocator;

import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;

public class CheckpointUtils {
	public static Checkpoint createCheckpoint(boolean conditionSatisfied, IResource resource,
			String typeName, int lineNumber, int charStart, int charEnd,
			int hitCount, boolean add, Map<String, Object> attributes)
			throws CoreException {

		if (attributes == null) {
			attributes = new HashMap<String, Object>(10);
		}
		return new Checkpoint(conditionSatisfied, resource, typeName, lineNumber, charStart,
				charEnd, hitCount, add, attributes);

	}
	
	public static Checkpoint createCheckpoint(IMarker marker){
		IResource resource = marker.getResource();
		if(resource instanceof IFile){
			
			IJavaElement element = JavaCore.create((IFile)resource);
			if(element instanceof ICompilationUnit){
				//get the real line number
				ICompilationUnit unit = (ICompilationUnit)element;
				ASTParser parser = ASTParser.newParser(AST.JLS4);
		        parser.setKind(ASTParser.K_COMPILATION_UNIT);
		        parser.setSource(unit);
		        parser.setResolveBindings(true);
		        parser.setEnvironment(null, null, null, true);
		        parser.setBindingsRecovery(true);
		        final CompilationUnit ast = (CompilationUnit) parser.createAST(null);

		        int lineNum = marker.getAttribute(IMarker.LINE_NUMBER, -1);
				ValidBreakpointLocationLocator locator = new ValidBreakpointLocationLocator(
						ast, lineNum, false, true);
				ast.accept(locator);
				int realLineNum = locator.getLineLocation();
				String typeName = locator.getFullyQualifiedTypeName();
				
				try {
					boolean conditionSatisfied = (boolean)marker.getAttribute(Checkpoint.CONDITION_SATISFIED, false);
					Checkpoint cp = createCheckpoint(conditionSatisfied, resource, typeName, realLineNum,
							marker.getAttribute(IMarker.CHAR_START, -1), marker.getAttribute(IMarker.CHAR_END, -1),
							0, false, null);
					String conditionString = (String)marker.getAttribute(Checkpoint.CONDITIONS);
					String testCaseString = (String)marker.getAttribute(Checkpoint.TEST_CASE_PROPERTY);
					
					ArrayList<ConditionItem> conditions = parseConditionString(conditionString);
					TestCase testcase = parseTestCaseString(testCaseString);
					cp.setConditions(conditions);
					cp.setOwnerTestCase(testcase);
					return cp;
				} catch (CoreException e) {
					e.printStackTrace();
				}
				
			}
		}
		return null;
	}
	
	private static ArrayList<ConditionItem> parseConditionString(String conditionString){
		ArrayList<ConditionItem> items = new ArrayList<ConditionItem>();
		String[] strings = conditionString.split(";");
		for(int i = 0; i < strings.length;){
			if(strings[i].equals(""))
				break;
			items.add(new ConditionItem(Integer.parseInt(strings[i]), strings[i + 1]));
			i = i + 2;
		}
		return items;
	}
	private static TestCase parseTestCaseString(String testCaseString){
		String[] strings = testCaseString.split(";");
		return new TestCase(strings[0],strings[1]);
	}
}
