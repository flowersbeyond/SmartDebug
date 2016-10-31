/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cn.edu.thu.tsmart.tool.da.tracer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import cn.edu.thu.tsmart.tool.da.tracer.util.EclipseUtils;

import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.Disassembler;
import com.ibm.wala.shrikeBT.GotoInstruction;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.Instruction;
import com.ibm.wala.shrikeBT.InvokeDynamicInstruction;
import com.ibm.wala.shrikeBT.InvokeInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.MethodEditor;
import com.ibm.wala.shrikeBT.MethodEditor.Output;
import com.ibm.wala.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrikeBT.SwitchInstruction;
import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.shrikeBT.analysis.Verifier;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.ClassWriter;

/**
 * This is the main instrumentor. We instrument a "trace writing method" at
 * each: 
 * A: ConditionalBranchInstruction 
 * B: GotoInstruction 
 * C: InvokeDynamicInstruction 
 * D: InvodeInstruction 
 * E: ReturnInstruction 
 * F: SwitchInstruction 
 * G: ThrowInstruction
 * 
 * @author evelyn
 * 
 */
public class Instrumentor {
	private final static boolean disasm = true;

	private final static boolean verify = true;

	private static OfflineInstrumenter instrumenter = new OfflineInstrumenter(false);
	
	// Keep these commonly used instructions around
	static final Instruction getSysErr = Util.makeGet(System.class, "err");

	static final Instruction callPrintln = Util.makeInvoke(PrintStream.class,
			"println", new Class[] { String.class });
	
	public static final String traceprefix = "[TRACE:]";
	public static final String ENTER = "{";
	public static final String EXIT = "}";
	public static final String IF_BRANCH_FALL_THROUGH = "/|";
	public static final String IF_BRANCH_TO = "/\\";
	public static final String GOTO = "/";
	public static final String INVOKE = "()";
	public static final String SWITCH = "//";
	
	

	private IJavaProject project;
	private List<String> classNames;

	public Instrumentor(IJavaProject project, List<String> classNames) {
		this.project = project;
		this.classNames = classNames;
	}

	public void instrument() {
		Writer w;
		try {
			
			String projectdir = EclipseUtils.getProjectDir(project);
			w = new BufferedWriter(new FileWriter(projectdir + "/bin/report", false));
		
			for(String className: classNames){
				File classFile = getClassFile(className);
				instrumenter.addInputClass(classFile.getParentFile(), classFile);
			}
						
			String outputJarName = projectdir + "/bin/output.jar";			
			instrumenter.setOutputJar(new File(outputJarName));
			
			instrumenter.setPassUnmodifiedClasses(true);
			instrumenter.beginTraversal();
			ClassInstrumenter ci;
			while ((ci = instrumenter.nextClass()) != null) {
				doClass(ci, w);
			}
			instrumenter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private File getClassFile(String className){
		try {
			String projectdir = EclipseUtils.getProjectDir(project);
			String classDir = className.replaceAll("\\.", "/");
			String classFileDir = projectdir + "/bin/" + classDir + ".class";
			File file = new File(classFileDir);
			return file;
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}



	private static void doClass(final ClassInstrumenter ci, Writer w)
			throws Exception {
		final String className = ci.getReader().getName();
		w.write("Class: " + className + "\n");
		w.flush();

		for (int m = 0; m < ci.getReader().getMethodCount(); m++) {
			MethodData d = ci.visitMethod(m);
			
			String methodSignature = d.getSignature();
			String methodName = d.getName();
			String methodTracePrefix = traceprefix + className + ":" + methodName + ":"  + methodSignature + ":";
			
			// d could be null, e.g., if the method is abstract or native
			if (d != null) {
				w.write("Instrumenting " + ci.getReader().getMethodName(m)
						+ " " + ci.getReader().getMethodType(m) + ":\n");
				w.flush();

				if (disasm) {
					w.write("Initial ShrikeBT code:\n");
					(new Disassembler(d)).disassembleTo(w);
					w.flush();
				}

				if (verify) {
					Verifier v = new Verifier(d);
					v.verify();
				}

				MethodEditor me = new MethodEditor(d);
				me.beginPass();
				
				IInstruction[] instr = me.getInstructions();
				// trace entry
				final String msgentry = methodTracePrefix + ENTER + ":" + 0;
						/*
						 * just back up the code here:
							+ "Entering call to "
							+ Util.makeClass("L" + ci.getReader().getName()
									+ ";") + "."
							+ ci.getReader().getMethodName(m);
						*/
				me.insertAtStart(new MethodEditor.Patch() {
					@Override
					public void emitTo(MethodEditor.Output w) {
						w.emit(getSysErr);
						w.emit(ConstantInstruction.makeString(msgentry));
						w.emit(callPrintln);
					}
				});
				
				
				//trace exit
				for (int i = 0; i < instr.length; i++) {
					if (instr[i] instanceof ReturnInstruction) {
						final String msgexit = methodTracePrefix + EXIT + ":" + i;
						me.insertBefore(i, new MethodEditor.Patch() {
							@Override
							public void emitTo(MethodEditor.Output w) {
								w.emit(getSysErr);
								w.emit(ConstantInstruction.makeString(msgexit));
								w.emit(callPrintln);
							}
						});
					}
				}

				
				// * A: ConditionalBranchInstruction 
				// * B: GotoInstruction 
				// * C: InvokeDynamicInstruction 
				// * D: InvokeInstruction 
				// * E: ReturnInstruction 
				// * F: SwitchInstruction 

				ArrayList<Integer> branchTargetLoc = new ArrayList<Integer>();
				for(int i = 0; i < instr.length; i ++){
					if (instr[i] instanceof ConditionalBranchInstruction){
						ConditionalBranchInstruction cbr = (ConditionalBranchInstruction)instr[i];
						int target = cbr.getTarget();
						int next = i + 1;
						final String msg0 = methodTracePrefix + IF_BRANCH_FALL_THROUGH + ":" + next;
						final String msg1 = methodTracePrefix + IF_BRANCH_TO + ":" + target;
						
						if(!branchTargetLoc.contains(target)){
							me.insertBefore(next, new MethodEditor.Patch() {
								
								@Override
								public void emitTo(Output w) {
									w.emit(getSysErr);
									w.emit(ConstantInstruction.makeString(msg0));
									w.emit(callPrintln);
									
								}
							});
							branchTargetLoc.add(target);
						}
						if(!branchTargetLoc.contains(next)){
							me.insertBefore(target, new MethodEditor.Patch() {
								
								@Override
								public void emitTo(Output w) {
									w.emit(getSysErr);
									w.emit(ConstantInstruction.makeString(msg1));
									w.emit(callPrintln);
									
								}
							});
							branchTargetLoc.add(next);
						}
					}
					else if(instr[i] instanceof GotoInstruction){
						
						GotoInstruction gotoInstr = (GotoInstruction)instr[i];
						int label = gotoInstr.getLabel();
						final String msg0 = methodTracePrefix + GOTO + ":" + label;
						if(!branchTargetLoc.contains(label)){
							me.insertBefore(label, new MethodEditor.Patch() {
								
								@Override
								public void emitTo(Output w) {
									w.emit(getSysErr);
									w.emit(ConstantInstruction.makeString(msg0));
									w.emit(callPrintln);
									
								}
							});
							branchTargetLoc.add(label);
						}
						
					}
					else if(instr[i] instanceof InvokeDynamicInstruction){
						InvokeDynamicInstruction invokeInstr = (InvokeDynamicInstruction)instr[i];
						final String msg0 = methodTracePrefix + INVOKE + ":" + i + ":" + invokeInstr.getClassType() + ":" + invokeInstr.getMethodName() + ":" + invokeInstr.getMethodSignature();
						me.insertBefore(i, new MethodEditor.Patch() {
							
							@Override
							public void emitTo(Output w) {
								w.emit(getSysErr);
								w.emit(ConstantInstruction.makeString(msg0));
								w.emit(callPrintln);
								
							}
						});
					}
					else if (instr[i] instanceof InvokeInstruction){
						InvokeInstruction invokeInstr = (InvokeInstruction)instr[i];
						final String msg0 = methodTracePrefix + INVOKE + ":" + i + ":" + invokeInstr.getClassType() + ":" + invokeInstr.getMethodName() + ":" + invokeInstr.getMethodSignature();
						me.insertBefore(i, new MethodEditor.Patch() {
							
							@Override
							public void emitTo(Output w) {
								w.emit(getSysErr);
								w.emit(ConstantInstruction.makeString(msg0));
								w.emit(callPrintln);
								
							}
						});
					}
					else if (instr[i] instanceof SwitchInstruction){
						SwitchInstruction switchInstr = (SwitchInstruction)instr[i];
						int[] casesAndLabels = switchInstr.getCasesAndLabels();
						int defaultLabel = switchInstr.getDefaultLabel();
						for(int j = 1; j < casesAndLabels.length; j += 2){
							int switchTarget = casesAndLabels[j];
							final String msg0 = methodTracePrefix + SWITCH + ":" + switchTarget;
							if(!branchTargetLoc.contains(switchTarget)){
								me.insertBefore(switchTarget, new MethodEditor.Patch() {
									
									@Override
									public void emitTo(Output w) {
										w.emit(getSysErr);
										w.emit(ConstantInstruction.makeString(msg0));
										w.emit(callPrintln);
										
									}
								});
								branchTargetLoc.add(switchTarget);
							}
						}
						
						final String msg1 = methodTracePrefix + SWITCH + ":" + defaultLabel;
						if(!branchTargetLoc.contains(defaultLabel)){
							me.insertBefore(defaultLabel, new MethodEditor.Patch() {
								
								@Override
								public void emitTo(Output w) {
									w.emit(getSysErr);
									w.emit(ConstantInstruction.makeString(msg1));
									w.emit(callPrintln);
									
								}
							});
							branchTargetLoc.add(defaultLabel);
						}
					}
				}
				
				// this updates the data d
				me.applyPatches();

				if (disasm) {
					w.write("Final ShrikeBT code:\n");
					(new Disassembler(d)).disassembleTo(w);
					w.flush();
				}
			}
		}
		if (ci.isChanged()) {
			ClassWriter cw = ci.emitClass();
			instrumenter.outputModifiedClass(ci, cw);
		}
	}
}