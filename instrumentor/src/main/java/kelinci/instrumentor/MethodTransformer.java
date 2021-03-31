package kelinci.instrumentor;

import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.Label;

import kelinci.Mem;

import java.util.HashSet;
import java.util.Random;

/**
 * Adds AFL-like instrumentation to branches.
 * 
 * Uses the ASM MethodVisitor to instrument the start of methods,
 * the location immediately after a branch (else case), as well as
 * all labels. 
 * 
 * There are also methods in MethodVisitor that we could override
 * to instrument tableswitch, lookupswitch and try-catch. But as those
 * jump to labels in any case (including default), instrumenting the
 * labels only is enough.
 */
public class MethodTransformer extends MethodVisitor {
	
	private HashSet<Integer> ids;
	Random r;

	public static int mark = 0;
	public static Label lab = new Label();

	public static int lab_switch_length;
	public static Label[] lab_switch = new Label[100];


	public MethodTransformer(MethodVisitor mv) {
		super(ASM5, mv);

		ids = new HashSet<>();
		r = new Random();
	}
	
	/**
	 * Best effort to generate a random id that is not already in use.
	 */
	private int getNewLocationId() {
		int id;
		int tries = 0;
		do {
			id = r.nextInt(Mem.SIZE);
			tries++;
		} while (tries <= 10 && ids.contains(id));
		ids.add(id);
		return id;
	}

	private void instrumentLocation() {
		
		/**
		 * 
		 * Code instrumented
		 * 
		 * * * * * * * * * * * * * * * * * * *
		 * Mem.coverage(id);
		 * * * * * * * * * * * * * * * * * * * 
		 */
		Integer id = getNewLocationId();
		mv.visitLdcInsn(id);
		mv.visitMethodInsn(INVOKESTATIC, "kelinci/Mem", "coverage", "(I)V", false);
	}

	@Override
	public void visitCode() {
		super.visitCode();
		instrumentLocation();
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		
		if(opcode >= IFEQ && opcode <= IF_ACMPNE){
			super.visitJumpInsn(opcode, label);
			lab = label;
		}else if(opcode == GOTO){
			instrumentLocation();
			super.visitJumpInsn(opcode, label);
		}else{
			super.visitJumpInsn(opcode, label);
		}
	}

	@Override
	public void visitFrame(
		final int type,
		final int numLocal,
		final Object[] local,
		final int numStack,
		final Object[] stack){
			
		if(mark != 0){
			super.visitFrame(type, numLocal, local, numStack, stack);
			instrumentLocation();
			mark = 0;
		}else {
			super.visitFrame(type, numLocal, local, numStack, stack);
		}
	}

	@Override
	public void visitTableSwitchInsn(
		final int min,
		final int max,
		final Label dflt,
		final Label... labels) {
	    	
		lab_switch = labels;
	    lab_switch_length = labels.length;
	    super.visitTableSwitchInsn(min, max, dflt, labels);
	}
	    
	@Override
	public void visitLookupSwitchInsn(
		final Label dflt, 
		final int[] keys, 
		final Label[] labels) {

	    lab_switch = labels;
	    lab_switch_length = labels.length;
	    //System.out.println(labels.length);
	    super.visitLookupSwitchInsn(dflt, keys, labels);
	}
	    
	@Override
	public void visitLabel(final Label label) {

	    if (label == lab) {
			mark = 1;
		}

		for (int i = 0; i < lab_switch_length; i++) {
			if (label == lab_switch[i] ) {
		   		mark = 2;
		   		break;
	    	}	
		}	

		super.visitLabel(label);
	}
}