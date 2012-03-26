package nz.ac.massey.cs.care.ast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.DescendingVisitor;
import org.apache.bcel.classfile.EmptyVisitor;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.Type;
import org.apache.bcel.util.ClassPath;

public class JasminVisitor extends EmptyVisitor {
	private JavaClass clazz;
	private String class_name;
	private ConstantPoolGen cpool;
	private int numOfCI = 0;//this is not the exact no. of invocations. every constructor invokes
							//a new command to initialize the respective field. the total no. is 
							//the number of actual constructor invocations on target class + no. of 
							//constructors in the source class.
	private int numOfVD = 0;
	private int numOfMPT = 0;
	private int numOfMET = 0;
	private int numOfMRT = 0;
	private int numOfSMI = 0;
	private JavaClass targetClass;

	public JasminVisitor(JavaClass clazz, JavaClass target, OutputStream out) {
		this.clazz = clazz;
		this.targetClass = target;
		class_name = clazz.getClassName();
		cpool = new ConstantPoolGen(clazz.getConstantPool());
	}

	/**
	 * Start traversal using DefaultVisitor pattern.
	 */
	public void disassemble() {
		DescendingVisitor v = new DescendingVisitor(clazz, this);
		v.visit();
	}

	public void visitField(Field field) {
		Type t = field.getType();
		String classname = Utility.signatureToString(t.getSignature(), false);
		if (classname.equals(targetClass.getClassName())) {
			numOfVD++;
		}
	}

	public void visitMethod(Method method) {
		String returnType = Utility.signatureToString(method.getReturnType()
				.getSignature(), false);
		if (returnType.equals(targetClass.getClassName())) {
			numOfMRT++;
		}
		int currentMPT = 0; //this is because we want to keep mpts separate from local variables. 
		for (Type t : method.getArgumentTypes()) {
			if (t.toString().equals(targetClass.getClassName())) {
				numOfMPT++;
				currentMPT++;
			}
		}
		if (method.getExceptionTable() != null) {
			String[] names = method.getExceptionTable().getExceptionNames();
			for (int i = 0; i < names.length; i++) {
				String exceptionType = names[i];
				if (exceptionType.equals(targetClass.getClassName())) {
					numOfMET++;
				}
			}
		}
		final MethodGen mg = new MethodGen(method, class_name, cpool);
		InstructionList il = mg.getInstructionList();
		if (il != null) { // is null for abstract methods!
			for (Instruction instr : mg.getInstructionList().getInstructions()) {
				if (instr instanceof NEW) {
					NEW clazz = (NEW) instr;
					String type = clazz.getLoadClassType(cpool).toString();
					if (type.equals(targetClass.getClassName())) {
						numOfCI++;
					}
				} else if (instr instanceof INVOKESTATIC) {
					INVOKESTATIC staticCall = (INVOKESTATIC) instr;
					String type = staticCall.getClassName(cpool);
					if (type.equals(targetClass.getClassName())) {
						numOfSMI++;
					}
				}
			}
		}

		LocalVariableGen[] lvs = mg.getLocalVariables();
		int currentVD = 0;
		for (int i = 0; i < lvs.length; i++) {
			LocalVariableGen l = lvs[i];
			String type = l.getType().toString();
			if (type.equals(targetClass.getClassName())) {
				numOfVD++;
				currentVD++;
			}
		}
		if (currentMPT > 0 && currentVD > 0) {
			numOfVD = numOfVD - currentMPT;
			currentMPT = 0;
			currentVD = 0;
		}
	}

	public static void main(String[] argv) {
		ClassParser parser = null;
		JavaClass java_class;
		ClassPath class_path = new ClassPath();

		try {
			if (argv.length == 0) {
				System.err.println("disassemble: No input files specified");
			} else {
				for (int i = 0; i < argv.length; i++) {
					if (argv[i].endsWith(".class"))
						parser = new ClassParser(argv[i]); // Create parser
															// object
					else {
						InputStream is = class_path.getInputStream(argv[i]);
						String name = argv[i].replace('.', '/') + ".class";

						parser = new ClassParser(is, name);
					}

					java_class = parser.parse();

					String class_name = java_class.getClassName();
					int index = class_name.lastIndexOf('.');
					String path = class_name.substring(0, index + 1).replace(
							'.', File.separatorChar);
					class_name = class_name.substring(index + 1);

					if (!path.equals("")) {
						File f = new File(path);
						f.mkdirs();
					}

					FileOutputStream out = new FileOutputStream(path
							+ class_name + ".j");
					JavaClass target = Repository
							.lookupClass("nz.ac.massey.cs.care.ast.B");
					JasminVisitor v = new JasminVisitor(java_class, target, out);
					v.disassemble();
					int ci = v.getNumOfCI();
					int vd = v.getNumOfVD();
					int met = v.getNumOfMET();
					int mrt = v.getNumOfMRT();
					int mpt = v.getNumOfMPT();
					int smi = v.getNumOfSMI();
					System.out.println("ci=" + ci);
					System.out.println("vd=" + vd);
					System.out.println("met=" + met);
					System.out.println("mrt=" + mrt);
					System.out.println("mpt=" + mpt);
					System.out.println("smi=" + smi);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getNumOfCI() {
		return numOfCI;
	}

	public int getNumOfVD() {
		return numOfVD;
	}

	public int getNumOfMPT() {
		return numOfMPT;
	}

	public int getNumOfMET() {
		return numOfMET;
	}

	public int getNumOfMRT() {
		return numOfMRT;
	}

	public int getNumOfSMI() {
		return numOfSMI;
	}

}