package kelinci.instrumentor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Does nothing more than call the MethodTransformer.
 */
public class ClassTransformer extends ClassVisitor {
	
	public ClassTransformer(ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
	}

	@Override
	public MethodVisitor visitMethod(
		int access, 
		String name,
		String desc, 
		String signature, 
		String[] exceptions) {
		
		MethodVisitor mv;
		mv = cv.visitMethod(access, name, desc, signature, exceptions);

		if (mv != null) {
			System.out.println("visit method: " + name);
			mv = new MethodTransformer(mv);
		}

		return mv;
	}
}
