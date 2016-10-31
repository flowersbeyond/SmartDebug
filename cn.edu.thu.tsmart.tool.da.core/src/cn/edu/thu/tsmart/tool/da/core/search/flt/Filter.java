package cn.edu.thu.tsmart.tool.da.core.search.flt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.debug.core.IJavaArray;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaBreakpointListener;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.debug.eval.EvaluationManager;
import org.eclipse.jdt.debug.eval.IAstEvaluationEngine;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
import org.eclipse.jdt.debug.eval.IEvaluationResult;
import org.eclipse.jdt.internal.debug.core.breakpoints.ValidBreakpointLocationLocator;

import cn.edu.thu.tsmart.tool.da.core.BugFixSession;
import cn.edu.thu.tsmart.tool.da.core.Logger;
import cn.edu.thu.tsmart.tool.da.core.suggestion.FilterableFix;
import cn.edu.thu.tsmart.tool.da.core.suggestion.FilterableSetFix;
import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;
import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;


public class Filter {
	
	private BugFixSession session;
	
	public Filter(BugFixSession session){
		this.session = session;
	}
	
	public static ArrayList<String> preciseEqualTypes = new ArrayList<String>();
	static {
		preciseEqualTypes.add("byte");
		preciseEqualTypes.add("short");
		preciseEqualTypes.add("int");
		preciseEqualTypes.add("long");
		
		preciseEqualTypes.add("java.lang.Byte");
		preciseEqualTypes.add("java.lang.Short");
		preciseEqualTypes.add("java.lang.Integer");
		preciseEqualTypes.add("java.lang.Long");
		
		preciseEqualTypes.add("char");
		preciseEqualTypes.add("java.lang.Character");
		
		preciseEqualTypes.add("boolean");
		preciseEqualTypes.add("java.lang.Boolean");
			
		preciseEqualTypes.add("java.lang.String");
	}
	
	public static ArrayList<String> approxEqualTypes = new ArrayList<String>();
	static {
		approxEqualTypes.add("float");
		approxEqualTypes.add("java.lang.Float");
		approxEqualTypes.add("double");
		approxEqualTypes.add("java.lang.Double");
	}
	
	public ArrayList<Fix> filter(Map<Integer, ArrayList<FilterableFix>> fixes, IFile file, String typeName, ArrayList<TestCase> passTCs, TestCase failTC){
		ArrayList<Fix> results = new ArrayList<Fix>();
		for(Integer lineNum:  fixes.keySet()){
			ArrayList<FilterableFix> fixesEntry = fixes.get(lineNum);
			if(fixesEntry.size() == 0)
				continue;
			else{
				FilterableFix fix = fixesEntry.get(0);
				CompilationUnit cu = fix.getfixSite().getCompilationUnit();
				ValidBreakpointLocationLocator locator = new ValidBreakpointLocationLocator(
						cu, lineNum, false, true);
				cu.accept(locator);
				if(locator.getLineLocation() != lineNum)
					continue;
				String realtypeName = locator.getFullyQualifiedTypeName();
				ArrayList<? extends Fix> filtered = filter(fixes.get(lineNum), file, realtypeName, lineNum, passTCs, failTC);
				results.addAll(filtered);
			}
			
		}
		return results;
	}

	private ArrayList<? extends Fix> filter(ArrayList<FilterableFix> fixes, IFile file, String typeName, int lineNum, ArrayList<TestCase> passTCs, TestCase failTC){
		
		Object lock = new Object();
		ArrayList<FilterableFix> survivedFixes = fixes;
		ArrayList<FilterableSetFix> fixsets = new ArrayList<FilterableSetFix>();
		try{
			//install breakpoint
			IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
			IBreakpoint[] breakpoints = manager.getBreakpoints();
			for(int i = 0; i < breakpoints.length; i ++){
				breakpoints[i].setEnabled(false);
			}
			
			IJavaLineBreakpoint bp = JDIDebugModel.createLineBreakpoint(file, typeName, lineNum, -1, -1, 0, false, null);
			bp.setEnabled(true);
			manager.addBreakpoint(bp);
			
			// install breakpoint listener
			CorrectTCFilterBPListener correctTCListener = new CorrectTCFilterBPListener(fixes, session.getProject(),session.getLogger(), lock);
			JDIDebugModel.addJavaBreakpointListener(correctTCListener);
			
			session.getLogger().log(Logger.DATA_MODE, Logger.CORRECT_TC_FILTER_START, Logger.generateFilterableSummary(fixes));
			correctTCListener.initHitCount();
			for(TestCase passTC: passTCs){
				// launch the correct test
				ILaunchConfiguration config = session.findLaunchConfiguration(passTC);
				LaunchTerminatedListener launchListener = new LaunchTerminatedListener(config, lock);
				DebugPlugin.getDefault().getLaunchManager().addLaunchListener(launchListener);
				Timer timer = new Timer();
				EvaluationTimeoutTask task = new EvaluationTimeoutTask(lock);
				ILaunch launch = null;
				synchronized(lock){
					timer.schedule(task, 300000);
					launch = config.launch("debug", new NullProgressMonitor());
					lock.wait();
				}
				
				if(task.evaluationTimeOut()){
					DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(launchListener);
					launch.terminate();
					continue;
				}
				DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(launchListener);
				
				if(correctTCListener.noFixSurvived())
					break;
				if(correctTCListener.meetMaxHitCount())
					break;
			}
			JDIDebugModel.removeJavaBreakpointListener(correctTCListener);
			session.getLogger().log(Logger.DATA_MODE, Logger.CORRECT_TC_FILTER_FINISHED, Logger.generateFilterableSummary(correctTCListener.getSurvivedFixes()));
			survivedFixes = correctTCListener.getSurvivedFixes();
			
			if(!correctTCListener.noFixSurvived()){
				session.getLogger().log(Logger.DATA_MODE, Logger.FAIL_TC_FILTER_START, Logger.generateFilterableSummary(correctTCListener.getSurvivedFixes()));
				
				FailTCFilterBPListener failTCListener = new FailTCFilterBPListener(survivedFixes, session.getProject(), session.getLogger(), lock);
				JDIDebugModel.addJavaBreakpointListener(failTCListener);
				
				failTCListener.initHitCount();
				ILaunchConfiguration config = session.findLaunchConfiguration(failTC);
				LaunchTerminatedListener launchListener = new LaunchTerminatedListener(config, lock);
				DebugPlugin.getDefault().getLaunchManager().addLaunchListener(launchListener);
				ILaunch launch = null;
				Timer timer = new Timer();
				EvaluationTimeoutTask task = new EvaluationTimeoutTask(lock);
				synchronized(lock){
					launch = config.launch("debug", new NullProgressMonitor());
					timer.schedule(task, 300000);
					lock.wait();
				}
				if(task.evaluationTimeOut()){
					DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(launchListener);
					launch.terminate();
				}
				DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(launchListener);
				JDIDebugModel.removeJavaBreakpointListener(failTCListener);
				fixsets = failTCListener.getSurvivedFixes();
				session.getLogger().log(Logger.DATA_MODE, Logger.FAIL_TC_FILTER_FINISHED, Logger.generateFilterableSetSummary(failTCListener.getSurvivedFixes()));
			}
			
			//clean up
			bp.setEnabled(false);
			manager.removeBreakpoint(bp, true);
			for(int i = 0; i < breakpoints.length; i ++){
				breakpoints[i].setEnabled(true);
			}
			
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return fixsets;
		
	}
}

class CorrectTCFilterBPListener extends FilterBPListener{
	
	public CorrectTCFilterBPListener(ArrayList<FilterableFix> allfixes,
			IJavaProject project, Logger logger, Object lock) {
		super(allfixes, project, logger, lock);
		this.maxHitCount = 6;
	}
	
	
	public ArrayList<FilterableFix> getSurvivedFixes(){
		return this.allfixes;
	}

	@Override
	public int breakpointHit(IJavaThread thread, IJavaBreakpoint breakpoint) {
		hitcount ++;
		Map<Expression, String> oldValueMap = new HashMap<Expression, String>();
		
		if(hitcount >= maxHitCount){
			try {
				thread.getLaunch().terminate();
			} catch (DebugException e) {
				e.printStackTrace();
			}
			synchronized(lock){
				lock.notifyAll();
			}
		}
		
		
		IJavaStackFrame stackframe = null;
		try {
			stackframe = (IJavaStackFrame) thread.getTopStackFrame();
		} catch (DebugException e1) {
			e1.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
		
		ArrayList<FilterableFix> survivedFixes = new ArrayList<FilterableFix>();
		if(stackframe == null){
			survivedFixes.addAll(allfixes);
		} else {
			//SideEffectHandler sideEffectHandler = new SideEffectHandler(stackframe, project);
			//sideEffectHandler.enable(true);
			IProgressMonitor pm = new NullProgressMonitor();
			//sideEffectHandler.start(pm);
			int compilationErrorCount = 0;
			logger.log(Logger.DEBUG_MODE, Logger.CORRECT_TC_FILTER_START, "Iterate " + hitcount);
			for(FilterableFix fix : allfixes){
				String newExprString = fix.getNewExprString();
				
				//handle side effects:
				//sideEffectHandler.startHandlingSideEffects();
				IJavaValue newExprValue = evaluateExpr(newExprString, thread, stackframe);
				String actualValue = getValueSchetch(newExprValue, fix.getTargetExprType());
				//sideEffectHandler.stopHandlingSideEffects();
				if(actualValue == null){
					compilationErrorCount ++;
					continue; //compilation error occurs, the expression is illegal here
				}
				// we know it should be true or false
				if(fix.hasExpectedValue()){
					if(actualValue.equals(fix.getExpectedValue())){
						survivedFixes.add(fix);
						logger.log(Logger.DEBUG_MODE, Logger.FILTER_SURVIVE, newExprString + ":" + actualValue + ";" + "expected:" + fix.getExpectedValue());
					} else{
						logger.log(Logger.DEBUG_MODE, Logger.FILTER_DEAD, newExprString + ":" + actualValue + ";" + "expected:" + fix.getExpectedValue());
					}
					
				}
				else {
					// we need to evaluate the targeted expression first:
					if(actualValue != null){
						Expression oldExpression = fix.getTargetExpression();
						String oldValue = "";
						if(!oldValueMap.containsKey(oldExpression)){
							//sideEffectHandler.startHandlingSideEffects();
							IJavaValue oldExprValue = evaluateExpr(oldExpression.toString(), thread, stackframe);
							oldValue = getValueSchetch(oldExprValue, fix.getTargetExprType());
							oldValueMap.put(oldExpression, oldValue);
							//sideEffectHandler.stopHandlingSideEffects();
						} else
							oldValue = oldValueMap.get(oldExpression);
						
						if(oldValue == null || actualValue.equals(oldValue)){
							survivedFixes.add(fix);
							logger.log(Logger.DEBUG_MODE, Logger.FILTER_SURVIVE, newExprString + ":" + actualValue + ";" + oldExpression.toString() + ":" + oldValue);
						} else {
							logger.log(Logger.DEBUG_MODE, Logger.FILTER_DEAD, newExprString + ":" + actualValue + ";" + oldExpression.toString() + ":" + oldValue);
						}
					} 
				}
			}
			logger.log(Logger.DEBUG_MODE, Logger.CORRECT_TC_FILTER_FINISHED, "Iterate " + hitcount);
			
			if(compilationErrorCount != 0){
				logger.log(Logger.DATA_MODE, Logger.FILTER_COMPILATION_ERROR, compilationErrorCount + "");
				logger.log(Logger.EXPR_MODE, Logger.FILTER_COMPILATION_ERROR, compilationErrorCount + "");
			}
			
			//sideEffectHandler.stop(pm);
		}
		
		allfixes = survivedFixes;
		logger.log(Logger.DATA_MODE, "FILTER_ITER", this.hitcount + ":\t" + Logger.generateFilterableSummary(allfixes));
		
		if(survivedFixes.size() == 0){
			try {
				thread.getLaunch().terminate();
			} catch (DebugException e) {
				e.printStackTrace();
			}
			synchronized(lock){
				lock.notifyAll();
			}
		}		
		
		return IJavaBreakpointListener.DONT_SUSPEND;
	}
}

class FailTCFilterBPListener extends FilterBPListener{
	
	private ArrayList<FilterableFix> survivedFixes = new ArrayList<FilterableFix>();
	
	private Map<String, List<FilterableFix>> valueFixMap = new HashMap<String, List<FilterableFix>>();
	
	public FailTCFilterBPListener(ArrayList<FilterableFix> allfixes,
			IJavaProject project, Logger logger, Object lock) {
		super(allfixes, project, logger, lock);
		valueFixMap.put("", allfixes);
		this.maxHitCount = 8;
	}
	
	public ArrayList<FilterableSetFix> getSurvivedFixes(){
		ArrayList<FilterableSetFix> fixsets = new ArrayList<FilterableSetFix>();
		for(String str: valueFixMap.keySet()){
			List<? extends Fix> fixes = valueFixMap.get(str);
			if(survivedFixes.contains(fixes.get(0))){
				FilterableSetFix fixset = new FilterableSetFix(fixes);
				fixsets.add(fixset);
			}
		}
		
		return fixsets;
	}

	@Override
	public int breakpointHit(IJavaThread thread, IJavaBreakpoint breakpoint) {
		hitcount ++;
		Map<Expression, String> oldValueMap = new HashMap<Expression, String>();
		
		if(hitcount >= maxHitCount){
			try {
				thread.getLaunch().terminate();
			} catch (DebugException e) {
				e.printStackTrace();
			}
			synchronized(lock){
				lock.notifyAll();
			}
		}
		
		
		IJavaStackFrame stackframe = null;
		try {
			stackframe = (IJavaStackFrame) thread.getTopStackFrame();
		} catch (DebugException e1) {
			e1.printStackTrace();
		} catch (Exception e2){
			e2.printStackTrace();
		}
		
		if(stackframe == null){
			return IJavaBreakpointListener.DONT_SUSPEND;
		} else {
			//SideEffectHandler sideEffectHandler = new SideEffectHandler(stackframe, project);
			//sideEffectHandler.enable(true);
			IProgressMonitor pm = new NullProgressMonitor();
			//sideEffectHandler.start(pm);
			int compilationErrorCount = 0;
			logger.log(Logger.DEBUG_MODE, Logger.FAIL_TC_FILTER_START, "survive count:" + survivedFixes.size());
			Map<String, List<FilterableFix>> map = new HashMap<String, List<FilterableFix>>();
			for(String valueString: valueFixMap.keySet()){
				List<? extends Fix> fixes = valueFixMap.get(valueString);
				String keyPrefix = valueString;
				for(Fix f : fixes){
					FilterableFix fix = (FilterableFix)f;
					String newExprString = fix.getNewExprString();
					//handle side effects:
					//sideEffectHandler.startHandlingSideEffects();
					IJavaValue newExprValue = evaluateExpr(newExprString, thread, stackframe);
					String actualValue = getValueSchetch(newExprValue, fix.getTargetExprType());
					//sideEffectHandler.stopHandlingSideEffects();
					if(actualValue == null){
						compilationErrorCount ++;
						continue; //compilation error occurs, the expression is illegal here
					}
					
					//split the valueFixMap
					
					String key = keyPrefix + '#' + actualValue;
					if(!map.containsKey(key)){
						map.put(key, new ArrayList<FilterableFix>());
					}
					map.get(key).add(fix);
					
					// we know it should be true or false
					if(fix.hasExpectedValue()){
						if(!actualValue.equals(fix.getExpectedValue())){
							logger.log(Logger.DEBUG_MODE, Logger.FILTER_SURVIVE, newExprString + ":" + actualValue + ";" + "not expected:" + fix.getExpectedValue());
							if(!survivedFixes.contains(fix))
								survivedFixes.add(fix);
						} else {
							logger.log(Logger.DEBUG_MODE, Logger.FILTER_DEAD, newExprString + ":" + actualValue + ";" + "not expected:" + fix.getExpectedValue());
						}
						
					}
					else {
						// we need to evaluate the targeted expression first:
						if(actualValue != null){
							Expression oldExpression = fix.getTargetExpression();
							String oldValue = "";
							if(!oldValueMap.containsKey(oldExpression)){
								//sideEffectHandler.startHandlingSideEffects();
								IJavaValue oldExprValue = evaluateExpr(oldExpression.toString(), thread, stackframe);
								oldValue = getValueSchetch(oldExprValue, fix.getTargetExprType());
								oldValueMap.put(oldExpression, oldValue);
								//sideEffectHandler.stopHandlingSideEffects();
							} else
								oldValue = oldValueMap.get(oldExpression);
							
							if(!(oldValue == null) && !actualValue.equals(oldValue)){
								logger.log(Logger.DEBUG_MODE, Logger.FILTER_SURVIVE, newExprString + ":" + actualValue + ";" + oldExpression.toString() + ":" + oldValue);
								if(!(survivedFixes.contains(fix)))
									survivedFixes.add(fix);
							} else {
								logger.log(Logger.DEBUG_MODE, Logger.FILTER_DEAD, newExprString + ":" + actualValue + ";" + oldExpression.toString() + ":" + oldValue);
							}
						} 
					}
				}
				logger.log(Logger.DEBUG_MODE, Logger.FAIL_TC_FILTER_FINISHED, "survive count:" + survivedFixes.size());
				if(compilationErrorCount != 0){
					logger.log(Logger.DATA_MODE, Logger.FILTER_COMPILATION_ERROR, compilationErrorCount + "");
					logger.log(Logger.EXPR_MODE, Logger.FILTER_COMPILATION_ERROR, compilationErrorCount + "");
				}
				//sideEffectHandler.stop(pm);
			}
			
			valueFixMap = map;
			logger.log(Logger.DEBUG_MODE, Logger.FAIL_TC_FILTER_SPLIT_FINISHED, Logger.generateFilterableMapSummary(valueFixMap));
		}	
		
		return IJavaBreakpointListener.DONT_SUSPEND;
	}
}

abstract class FilterBPListener implements IJavaBreakpointListener{
	
	protected ArrayList<FilterableFix> allfixes;
	protected IJavaProject project;
	protected Logger logger;
	protected int maxHitCount = 6;
	protected int hitcount = 0;
	
	protected Object lock;
	
	public FilterBPListener(ArrayList<FilterableFix> allfixes, IJavaProject project, Logger logger, Object lock){
		this.allfixes = allfixes;
		this.project = project;
		this.logger = logger;
		this.lock = lock;
	}

	public boolean meetMaxHitCount() {
		if(hitcount >= maxHitCount)
			return true;
		return false;
	}

	public boolean noFixSurvived() {
		return allfixes.isEmpty();
	}

	public void initHitCount(){
		this.hitcount = 0;
	}

	
	
	protected String getValueSchetch(IJavaValue exprValue, String targetExprType) {
		try{
			if(exprValue == null)
				return null;
			String valueSchetch = getPrimValueSchetch(exprValue);
			
			if(valueSchetch != null && valueSchetch.equals("ERROR_SMART_DEBUG")){
				return null;
			}
			else if (valueSchetch != null)
				return valueSchetch;
			else {
				valueSchetch = "";
				
				LinkedList<IJavaVariable> varqueue = new LinkedList<IJavaVariable>();
				IVariable[] vars = exprValue.getVariables();
				for(int i = 0; i < vars.length; i ++){
					varqueue.add((IJavaVariable)vars[i]);
				}
				
				int count = 0;

				while(!varqueue.isEmpty() && count <=10){
					IJavaVariable var = varqueue.poll();
					String newVarSchetch = getPrimValueSchetch((IJavaValue)var.getValue());
					if(newVarSchetch != null && newVarSchetch.equals("ERROR_SMART_DEBUG"))
						return null;
					else if (newVarSchetch != null){
						valueSchetch += newVarSchetch + ";";
						count ++;
					} else {
						IJavaValue value = (IJavaValue)var.getValue();
						IVariable[] newvars = value.getVariables();
						for(int i = 0; i < newvars.length; i ++){
							varqueue.add((IJavaVariable)newvars[i]);
						}
					}
				}
			}
			return valueSchetch;
		} catch(DebugException e){
			e.printStackTrace();
			return null;
		}
	}
	
	protected String getPrimValueSchetch(IJavaValue value){
		try{
			String schetch = getPrimValueInner(value);
			if(schetch != null)
				return schetch;
			StringBuffer buf = new StringBuffer("");
			IJavaType exprType = value.getJavaType();
			if(exprType instanceof IJavaArray){
				IJavaArray arrayValue = (IJavaArray) exprType;
				IJavaValue[] values = arrayValue.getValues();
				if(values!= null && values.length >= 1){
					for(int i = 0; i < values.length && i <= 10; i ++){
						String s = getPrimValueInner(values[i]);
						if(s == null)
							return null;
						else
							buf.append(s);
					}
				}
				return buf.toString();
				
			}
				return null;
		} catch(DebugException e){
			e.printStackTrace();
			return "ERROR_SMART_DEBUG";
		}
		
	}
	
	protected String getPrimValueInner(IJavaValue value) throws DebugException{
		IJavaType exprType = value.getJavaType();
		if(value.getValueString() == "null")
			return "null";
		if(Filter.preciseEqualTypes.contains(exprType.getName()))
			return value.getValueString();
		else if(Filter.approxEqualTypes.contains(exprType.getName())){
			String valueString = value.getValueString();
			if(exprType.getName().equals("double") || exprType.getName().equals("java.lang.Double")){
				if(valueString.length() >= 8)
					valueString = valueString.substring(0, 8);
			} else {
				if(valueString.length() >= 5)
					valueString = valueString.substring(0, 5);
			}
			return valueString;
		}
		
		return null;
	}

	protected IJavaValue evaluateExpr(String newExprString, IJavaThread thread,
			IJavaStackFrame stackframe) {
		
		IAstEvaluationEngine evalEngine = EvaluationManager.newAstEvaluationEngine(project, (IJavaDebugTarget) thread.getDebugTarget());
		Object evalLock = new Object();
		EvaluationListener listener = new EvaluationListener(evalLock);
		
		Timer timer = new Timer();
		EvaluationTimeoutTask timeoutTask = new EvaluationTimeoutTask(evalLock);
		synchronized(evalLock){
			try {
				evalEngine.evaluate(newExprString, stackframe, listener, DebugEvent.EVALUATION, false);
				timer.schedule(timeoutTask, 200);
				evalLock.wait();
			} catch (DebugException e) {
				e.printStackTrace();
				return null;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		IJavaValue value = null;
		if(!timeoutTask.evaluationTimeOut()){
			timeoutTask.cancel();
			IEvaluationResult result = listener.getResult();
			if(result.getErrorMessages().length == 0 && result.getException() == null){
				value = result.getValue();
			}
		}
		evalEngine.dispose();
		return value;
	}
	
	@Override
	public void addingBreakpoint(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint) {
	}
	@Override
	public int installingBreakpoint(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint, IJavaType type) {
		return 0;
	}
	@Override
	public void breakpointInstalled(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint) {
	}
	@Override
	public void breakpointRemoved(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint) {
	}
	@Override
	public void breakpointHasRuntimeException(IJavaLineBreakpoint breakpoint,
			DebugException exception) {
	}
	@Override
	public void breakpointHasCompilationErrors(IJavaLineBreakpoint breakpoint,
			Message[] errors) {
	}
}

class LaunchTerminatedListener implements ILaunchesListener2{

	ILaunchConfiguration config;
	Object lock;
	public LaunchTerminatedListener(ILaunchConfiguration config, Object lock){
		this.config = config;
		this.lock = lock;
	}
	@Override
	public void launchesRemoved(ILaunch[] launches) {
	}
	@Override
	public void launchesAdded(ILaunch[] launches) {
	}
	@Override
	public void launchesChanged(ILaunch[] launches) {
	}
	@Override
	public void launchesTerminated(ILaunch[] launches) {
		for(int i = 0; i < launches.length; i ++){
			if(launches[i].getLaunchConfiguration().equals(this.config)){
				synchronized(lock){
					lock.notifyAll();
				}
				break;
			}
		}
		
	}
	
}

class EvaluationListener implements IEvaluationListener{
	private IEvaluationResult result;
	private Object lock;
	
	public EvaluationListener(Object lock){
		this.lock = lock;
	}
	
	public IEvaluationResult getResult(){
		return result;
	}
	@Override
	public void evaluationComplete(IEvaluationResult result) {
		this.result = result;
		synchronized(lock){
			lock.notifyAll();
		}
	}
	
}

class EvaluationTimeoutTask extends TimerTask{
	Object lock;
	boolean timeout = false;
	public EvaluationTimeoutTask(Object lock){
		this.lock = lock;
	}
	
	public boolean evaluationTimeOut(){
		return timeout;
	}
	
	@Override
	public void run(){
		timeout = true;
		synchronized(lock){
			lock.notifyAll();
		}
	}
}