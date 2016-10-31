package cn.edu.thu.tsmart.tool.da.instr;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.ConstantInstruction;
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
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeCT.ClassWriter;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

public class SDClassTransformer implements ClassFileTransformer {

	// Keep these commonly used instructions around
	//static final Instruction getSysErr = Util.makeGet(System.class, "err");
	static final Instruction callSendMessage = Util.makeInvoke(Messenger.class, "sendMessage");
	static final Instruction callCloseSocket = Util.makeInvoke(Messenger.class, "closeSocket");
	//static final Instruction callPrintln = Util.makeInvoke(PrintStream.class,
	//		"println", new Class[] { String.class });

	public static final String traceprefix = "[TRACE:]";
	public static final String ENTER = "{";
	public static final String EXIT = "}";
	public static final String IF_BRANCH_FALL_THROUGH = "/|";
	public static final String IF_BRANCH_TO = "/\\";
	public static final String GOTO = "/";
	public static final String INVOKE = "()";
	public static final String SWITCH = "//";

	private List<String> classNames;
	private static String testClassName;
	private static List<String> testMethodName;

	public SDClassTransformer(List<String> classNames, String testClassName, List<String> testMethodName) {
		this.classNames = classNames;
		SDClassTransformer.testClassName = testClassName;
		SDClassTransformer.testMethodName = testMethodName;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		
		String normalizedClassName = className.replaceAll("/", ".");
		if (!classNames.contains(normalizedClassName))
			return classfileBuffer;
		
		
		try {
			ClassInstrumenter ci = new ClassInstrumenter(normalizedClassName,
					classfileBuffer, null, false);
			byte[] finalBytes = doClass(ci);
			if(finalBytes != null)
				return finalBytes;
			else 
				return classfileBuffer;
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return classfileBuffer;
		
	}

	private static byte[] doClass(final ClassInstrumenter ci)
			throws Exception {
		final String className = ci.getReader().getName();
		String normalizedClassName = className.replaceAll("/", ".");
		for (int m = 0; m < ci.getReader().getMethodCount(); m++) {
			MethodData d = ci.visitMethod(m);
			if(d == null){
				System.out.println(className + " :" + m + " null2");
				continue;
			}

			String methodSignature = d.getSignature();
			String methodName = d.getName();
			String methodTracePrefix = traceprefix + className + ":"
					+ methodName + ":" + methodSignature + ":";

			// d could be null, e.g., if the method is abstract or native
			if (d != null) {
				MethodEditor me = new MethodEditor(d);
				me.beginPass();

				IInstruction[] instr = me.getInstructions();
				// trace entry
				final String msgentry = methodTracePrefix + ENTER + ":" + 0;
				/*
				 * just back up the code here: + "Entering call to " +
				 * Util.makeClass("L" + ci.getReader().getName() + ";") + "." +
				 * ci.getReader().getMethodName(m);
				 */
				me.insertAtStart(new MethodEditor.Patch() {
					@Override
					public void emitTo(MethodEditor.Output w) {
						//w.emit(getSysErr);
						w.emit(ConstantInstruction.makeString(msgentry));
						w.emit(callSendMessage);
					}
				});

				// trace exit
				for (int i = 0; i < instr.length; i++) {
					if (instr[i] instanceof ReturnInstruction) {
						final String msgexit = methodTracePrefix + EXIT + ":"
								+ i;
						
						me.insertBefore(i, new MethodEditor.Patch() {
							@Override
							public void emitTo(MethodEditor.Output w) {
								//w.emit(getSysErr);
								w.emit(ConstantInstruction.makeString(msgexit));
								w.emit(callSendMessage);
								if(normalizedClassName.equals(testClassName) && testMethodName.contains(methodName)){
									w.emit(callCloseSocket);
								}
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
				for (int i = 0; i < instr.length; i++) {
					if (instr[i] instanceof ConditionalBranchInstruction) {
						ConditionalBranchInstruction cbr = (ConditionalBranchInstruction) instr[i];
						int target = cbr.getTarget();
						int next = i + 1;
						final String msg0 = methodTracePrefix
								+ IF_BRANCH_FALL_THROUGH + ":" + next;
						final String msg1 = methodTracePrefix + IF_BRANCH_TO
								+ ":" + target;

						if (!branchTargetLoc.contains(next)) {
							me.insertBefore(next, new MethodEditor.Patch() {

								@Override
								public void emitTo(Output w) {
									//w.emit(getSysErr);
									w.emit(ConstantInstruction.makeString(msg0));
									w.emit(callSendMessage);

								}
							});
							branchTargetLoc.add(next);
						}
						if (!branchTargetLoc.contains(target)) {
							me.insertBefore(target, new MethodEditor.Patch() {

								@Override
								public void emitTo(Output w) {
									//w.emit(getSysErr);
									w.emit(ConstantInstruction.makeString(msg1));
									w.emit(callSendMessage);

								}
							});
							branchTargetLoc.add(target);
						}
					} else if (instr[i] instanceof GotoInstruction) {

						GotoInstruction gotoInstr = (GotoInstruction) instr[i];
						int label = gotoInstr.getLabel();
						final String msg0 = methodTracePrefix + GOTO + ":"
								+ label;
						if (!branchTargetLoc.contains(label)) {
							me.insertBefore(label, new MethodEditor.Patch() {

								@Override
								public void emitTo(Output w) {
									//w.emit(getSysErr);
									w.emit(ConstantInstruction.makeString(msg0));
									w.emit(callSendMessage);

								}
							});
							branchTargetLoc.add(label);
						}

					} else if (instr[i] instanceof InvokeDynamicInstruction) {
						InvokeDynamicInstruction invokeInstr = (InvokeDynamicInstruction) instr[i];
						final String msg0 = methodTracePrefix + INVOKE + ":"
								+ i + ":" + invokeInstr.getClassType() + ":"
								+ invokeInstr.getMethodName() + ":"
								+ invokeInstr.getMethodSignature();
						me.insertBefore(i, new MethodEditor.Patch() {

							@Override
							public void emitTo(Output w) {
								//w.emit(getSysErr);
								w.emit(ConstantInstruction.makeString(msg0));
								w.emit(callSendMessage);

							}
						});
					} else if (instr[i] instanceof InvokeInstruction) {
						InvokeInstruction invokeInstr = (InvokeInstruction) instr[i];
						final String msg0 = methodTracePrefix + INVOKE + ":"
								+ i + ":" + invokeInstr.getClassType() + ":"
								+ invokeInstr.getMethodName() + ":"
								+ invokeInstr.getMethodSignature();
						me.insertBefore(i, new MethodEditor.Patch() {

							@Override
							public void emitTo(Output w) {
								//w.emit(getSysErr);
								w.emit(ConstantInstruction.makeString(msg0));
								w.emit(callSendMessage);

							}
						});
					} else if (instr[i] instanceof SwitchInstruction) {
						SwitchInstruction switchInstr = (SwitchInstruction) instr[i];
						int[] casesAndLabels = switchInstr.getCasesAndLabels();
						int defaultLabel = switchInstr.getDefaultLabel();
						for (int j = 1; j < casesAndLabels.length; j += 2) {
							int switchTarget = casesAndLabels[j];
							final String msg0 = methodTracePrefix + SWITCH
									+ ":" + switchTarget;
							if (!branchTargetLoc.contains(switchTarget)) {
								me.insertBefore(switchTarget,
										new MethodEditor.Patch() {

											@Override
											public void emitTo(Output w) {
												//w.emit(getSysErr);
												w.emit(ConstantInstruction
														.makeString(msg0));
												w.emit(callSendMessage);

											}
										});
								branchTargetLoc.add(switchTarget);
							}
						}

						final String msg1 = methodTracePrefix + SWITCH + ":"
								+ defaultLabel;
						if (!branchTargetLoc.contains(defaultLabel)) {
							me.insertBefore(defaultLabel,
									new MethodEditor.Patch() {

										@Override
										public void emitTo(Output w) {
											//w.emit(getSysErr);
											w.emit(ConstantInstruction
													.makeString(msg1));
											w.emit(callSendMessage);

										}
									});
							branchTargetLoc.add(defaultLabel);
						}
					}
				}

				// this updates the data d
				me.applyPatches();

			}
		}
		if (ci.isChanged()) {
			ClassWriter cw = ci.emitClass();
			byte[] finalbytes = cw.makeBytes();
			return finalbytes;
		}
		return null;
	}
}