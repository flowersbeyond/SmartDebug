package cn.edu.thu.tsmart.tool.da.instr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;

public class SDAgent {
	private static ArrayList<String> classNames = new ArrayList<String>();
	private static String testClassName = "";
	private static ArrayList<String> testMethodNames = new ArrayList<String>();
	public static void premain(String agentArguments,
			Instrumentation instrumentation) {
		parseArguments(agentArguments);
		SDClassTransformer transformer = new SDClassTransformer(classNames, testClassName, testMethodNames);
		instrumentation.addTransformer(transformer);
	}
	
	private static void parseArguments(String agentArguments){
		try {
			String configDir = agentArguments;
			String classesConfigDir = configDir + "/classes-config";
			String testCaseConfigDir = configDir + "/testcase-config";
			BufferedReader reader = new BufferedReader(new FileReader(new File(classesConfigDir)));
			
			// first line is the server port
			String s = reader.readLine();
			int port = Integer.parseInt(s);
			Messenger.init(port);
			//now we parse classNames
			s = reader.readLine();
			while(s != null){
				classNames.add(s);
				s = reader.readLine();
			}
			reader.close();
			
			reader = new BufferedReader(new FileReader(new File(testCaseConfigDir)));
			// now testClassName
			testClassName = reader.readLine();
			
			//now testMethod count
			int testMethodCount = Integer.parseInt(reader.readLine());
			for(int i = 0; i < testMethodCount; i ++){
				String testMethodName = reader.readLine();
				testMethodNames.add(testMethodName);
			}
			reader.close();
						
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return;
	}
	
	public static void main(String args[]){
		
	}
}
