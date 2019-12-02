package ru.zaxar163.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Позволяет искать методы внутри незагруженных классов и общие суперклассы для
 * чего угодно. Работает через поиск class-файлов в classpath.
 */
public class ClassMetadataReader implements Closeable {
	private static class CheckSuperClassVisitor extends ClassVisitor {

		String superClassName;

		public CheckSuperClassVisitor() {
			super(Opcodes.ASM7);
		}

		@Override
		public void visit(final int version, final int access, final String name, final String signature,
				final String superName, final String[] interfaces) {
			superClassName = superName;
		}
	}

	private final List<JarFile> cp;

	public ClassMetadataReader() {
		cp = new ArrayList<>();
	}

	public ClassMetadataReader(final List<JarFile> cp) {
		this.cp = cp;
	}

	public void acceptVisitor(final byte[] classData, final ClassVisitor visitor) {
		new ClassReader(classData).accept(visitor, 0);
	}

	public void acceptVisitor(final byte[] classData, final ClassVisitor visitor, final int flags) {
		new ClassReader(classData).accept(visitor, flags);
	}

	public void acceptVisitor(final String className, final ClassVisitor visitor) throws IOException {
		acceptVisitor(getClassData(className), visitor);
	}

	public void acceptVisitor(final String className, final ClassVisitor visitor, final int flags) throws IOException {
		acceptVisitor(getClassData(className), visitor, flags);
	}

	@Override
	public void close() {
		cp.forEach(IOHelper::close);
		cp.clear();
	}

	public byte[] getClassData(final String className) throws IOException {
		for (final JarFile f : cp)
			if (f.getEntry(className + ".class") != null) {
				byte[] bytes;
				try (InputStream in = f.getInputStream(f.getEntry(className + ".class"))) {
					bytes = IOHelper.read(in);
				}
				return bytes;
			}
		return IOHelper.read(IOHelper.getResourceURL(className + ".class"));
	}

	public List<JarFile> getCp() {
		return cp;
	}

	public String getSuperClass(final String type) {
		if (type.equals("java/lang/Object"))
			return null;
		try {
			return getSuperClassASM(type);
		} catch (final Exception e) {
			return "java/lang/Object";
		}
	}

	protected String getSuperClassASM(final String type) throws IOException {
		final CheckSuperClassVisitor cv = new CheckSuperClassVisitor();
		acceptVisitor(type, cv);
		return cv.superClassName;
	}

	/**
	 * Возвращает суперклассы в порядке возрастающей конкретности (начиная с
	 * java/lang/Object и заканчивая данным типом)
	 */
	public ArrayList<String> getSuperClasses(String type) {
		final ArrayList<String> superclasses = new ArrayList<>(1);
		superclasses.add(type);
		while ((type = getSuperClass(type)) != null)
			superclasses.add(type);
		Collections.reverse(superclasses);
		return superclasses;
	}

}
