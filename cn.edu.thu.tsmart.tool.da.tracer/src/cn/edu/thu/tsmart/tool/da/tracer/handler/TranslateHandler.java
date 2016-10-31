package cn.edu.thu.tsmart.tool.da.tracer.handler;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import cn.edu.thu.tsmart.tool.da.tracer.TraceTranslator;
import cn.edu.thu.tsmart.tool.da.tracer.Tracer;
import cn.edu.thu.tsmart.tool.da.tracer.trace.TraceNode;
import cn.edu.thu.tsmart.tool.da.tracer.util.EclipseUtils;
import cn.edu.thu.tsmart.tool.da.tracer.util.PrettyPrinter;

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.config.AnalysisScopeReader;

public class TranslateHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			System.out.println("start translating");
			
			IJavaProject project = Tracer.getCurrentProject();
			String projectClassPath = EclipseUtils.getProjectDir(project)
					+ "/bin/";
			AnalysisScope scope = AnalysisScopeReader
					.makeJavaBinaryAnalysisScope(projectClassPath, null);
			ClassHierarchy cha = ClassHierarchy.make(scope);
			String traceFileDir = projectClassPath + "/trace";
			TraceTranslator translator = new TraceTranslator(traceFileDir,
					scope, cha);
			ArrayList<TraceNode> trace = translator.translate();
			PrettyPrinter.prettyPrint(trace);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JavaModelException e1) {
			e1.printStackTrace();
		} catch (ClassHierarchyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	

}
