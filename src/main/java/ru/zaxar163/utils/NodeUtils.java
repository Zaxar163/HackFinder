package ru.zaxar163.utils;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.D2L;
import static org.objectweb.asm.Opcodes.DALOAD;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DNEG;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.DUP2_X1;
import static org.objectweb.asm.Opcodes.DUP2_X2;
import static org.objectweb.asm.Opcodes.DUP_X1;
import static org.objectweb.asm.Opcodes.DUP_X2;
import static org.objectweb.asm.Opcodes.F2D;
import static org.objectweb.asm.Opcodes.F2I;
import static org.objectweb.asm.Opcodes.F2L;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FCONST_1;
import static org.objectweb.asm.Opcodes.FCONST_2;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FNEG;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.I2B;
import static org.objectweb.asm.Opcodes.I2C;
import static org.objectweb.asm.Opcodes.I2D;
import static org.objectweb.asm.Opcodes.I2F;
import static org.objectweb.asm.Opcodes.I2L;
import static org.objectweb.asm.Opcodes.I2S;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.IINC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INEG;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKEDYNAMIC;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.JSR;
import static org.objectweb.asm.Opcodes.L2D;
import static org.objectweb.asm.Opcodes.LALOAD;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LCONST_1;
import static org.objectweb.asm.Opcodes.LDC;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LNEG;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.NEWARRAY;
import static org.objectweb.asm.Opcodes.NOP;
import static org.objectweb.asm.Opcodes.RET;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SIPUSH;
import static org.objectweb.asm.Opcodes.SWAP;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

public final class NodeUtils {
	public static final int MAX_SAFE_BYTE_COUNT = 65535 - Byte.MAX_VALUE;

	public static List<AnnotationNode> annots(String clazz, final String method, final ClassMetadataReader r) {
		if (clazz.startsWith("L"))
			clazz = Type.getType(clazz).getInternalName();
		try {
			final List<AnnotationNode> ret = new ArrayList<>();
			final ClassNode n = forClass(clazz, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG, r);
			if (n.visibleAnnotations != null)
				ret.addAll(n.visibleAnnotations);
			if (n.invisibleAnnotations != null)
				ret.addAll(n.invisibleAnnotations);
			for (final MethodNode m : n.methods)
				if (method.equals(m.name)) {
					if (m.visibleAnnotations != null)
						ret.addAll(m.visibleAnnotations);
					if (m.invisibleAnnotations != null)
						ret.addAll(m.invisibleAnnotations);
				}
			return ret;
		} catch (final Throwable e) {
			return Collections.emptyList();
		}
	}

	private static int doMethodEmulation(final String desc) {
		int result = 0;
		final Type returnType = Type.getReturnType(desc);

		if (returnType.getSort() == Type.LONG || returnType.getSort() == Type.DOUBLE)
			result++;
		if (returnType.getSort() != Type.VOID)
			result++;

		return result;
	}

	public static ClassNode forClass(final Class<?> cls, final int flags) {
		try (InputStream in = cls.getClassLoader().getResourceAsStream(cls.getName().replace('.', '/') + ".class")) {
			final ClassNode ret = new ClassNode();
			new ClassReader(IOHelper.read(in)).accept(ret, flags);
			return ret;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static ClassNode forClass(final String clazz, final int flags, final ClassMetadataReader r) {
		try {
			final ClassNode ret = new ClassNode();
			r.acceptVisitor(clazz, ret, flags);
			return ret;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static InsnList getSafeStringInsnList(final String string) {
		final InsnList insnList = new InsnList();
		if (string.length() * 3 < MAX_SAFE_BYTE_COUNT) { // faster check
			insnList.add(new LdcInsnNode(string));
			return insnList;
		}

		insnList.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
		insnList.add(new InsnNode(DUP));
		insnList.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));

		final String[] chunks = splitUtf8ToChunks(string, MAX_SAFE_BYTE_COUNT);
		for (final String chunk : chunks) {
			insnList.add(new LdcInsnNode(chunk));
			insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
					"(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
		}
		insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;",
				false));

		return insnList;
	}

	public static int getUtf8CharSize(final char c) {
		if (c >= 0x0001 && c <= 0x007F)
			return 1;
		else if (c <= 0x07FF)
			return 2;
		return 3;
	}

	public static int opcodeEmulation(final AbstractInsnNode e) {
		int stackSize = 0;
		switch (e.getOpcode()) {
		case NOP:
		case LALOAD: // (index, arrayref) -> (long, long_top)
		case DALOAD: // (index, arrayref) -> (double, double_top)
		case SWAP: // (value1, value2) -> (value2, value1)
		case INEG:
		case LNEG:
		case FNEG:
		case DNEG:
		case IINC:
		case I2F:
		case L2D:
		case F2I:
		case D2L:
		case I2B:
		case I2C:
		case I2S:
		case GOTO:
		case RETURN:
		case NEWARRAY:
		case ANEWARRAY:
		case ARRAYLENGTH:
		case CHECKCAST:
		case INSTANCEOF:
			// Does nothing
			break;
		case ACONST_NULL:
		case ICONST_M1:
		case ICONST_0:
		case ICONST_1:
		case ICONST_2:
		case ICONST_3:
		case ICONST_4:
		case ICONST_5:
		case FCONST_0:
		case FCONST_1:
		case FCONST_2:
		case BIPUSH:
		case SIPUSH:
		case ILOAD:
		case FLOAD:
		case ALOAD:
		case DUP:
		case DUP_X1:
		case DUP_X2:
		case I2L:
		case I2D:
		case F2L:
		case F2D:
		case NEW:
			// Pushes one-word constant to stack
			stackSize++;
			break;
		case LDC:
			final LdcInsnNode ldc = (LdcInsnNode) e;
			if (ldc.cst instanceof Long || ldc.cst instanceof Double)
				stackSize++;

			stackSize++;
			break;
		case LCONST_0:
		case LCONST_1:
		case DCONST_0:
		case DCONST_1:
		case LLOAD:
		case DLOAD:
		case DUP2:
		case DUP2_X1:
		case DUP2_X2:
			// Pushes two-word constant or two one-word constants to stack
			stackSize++;
			stackSize++;
			break;
		case INVOKEVIRTUAL:
		case INVOKESPECIAL:
		case INVOKEINTERFACE:
			stackSize += doMethodEmulation(((MethodInsnNode) e).desc);
			break;
		case INVOKESTATIC:
			stackSize += doMethodEmulation(((MethodInsnNode) e).desc);
			break;
		case INVOKEDYNAMIC:
			stackSize += doMethodEmulation(((InvokeDynamicInsnNode) e).desc);
			break;
		case JSR:
		case RET:
			throw new RuntimeException("Did not expect JSR/RET instructions");
		default:
			break;
		}
		return stackSize;
	}

	public static InsnList push(final int value) {
		final InsnList ret = new InsnList();
		if (value >= -1 && value <= 5)
			ret.add(new InsnNode(Opcodes.ICONST_0 + value));
		else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE)
			ret.add(new IntInsnNode(Opcodes.BIPUSH, value));
		else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE)
			ret.add(new IntInsnNode(Opcodes.SIPUSH, value));
		else
			ret.add(new LdcInsnNode(value));
		return ret;
	}

	public static String[] splitUtf8ToChunks(final String text, final int maxBytes) {
		final List<String> parts = new ArrayList<>();

		final char[] chars = text.toCharArray();

		int lastCharIndex = 0;
		int currentChunkSize = 0;

		for (int i = 0; i < chars.length; i++) {
			final char c = chars[i];
			final int charSize = getUtf8CharSize(c);
			if (currentChunkSize + charSize < maxBytes)
				currentChunkSize += charSize;
			else {
				parts.add(text.substring(lastCharIndex, i));
				currentChunkSize = 0;
				lastCharIndex = i;
			}
		}

		if (currentChunkSize != 0)
			parts.add(text.substring(lastCharIndex));

		return parts.toArray(new String[0]);
	}

	private NodeUtils() {
	}
}
