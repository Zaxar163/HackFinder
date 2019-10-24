package ru.zaxar163.hackfinder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ru.zaxar163.utils.ClassMetadataReader;
import ru.zaxar163.utils.IOHelper;

public class RCEFinder {
	public static final List<JarFile> libraries, mods, conc;
	static {
		libraries = new ArrayList<>();
		mods = new ArrayList<>();
		conc = new ArrayList<>();
	}
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("Usage: rce <mods dir> <libraries dir> <log file>");
			System.exit(1);
		}
		IOHelper.walk(Paths.get(args[0]), new SimpleFileVisitor<Path>() {
			@Override
	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				mods.add(new JarFile(file.toFile()));
	            return super.visitFile(file, attrs);
	        }
		}, true);
		IOHelper.walk(Paths.get(args[1]), new SimpleFileVisitor<Path>() {
			@Override
	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				libraries.add(new JarFile(file.toFile()));
	            return super.visitFile(file, attrs);
	        }
		}, true);
		conc.addAll(mods);
		conc.addAll(libraries);
		try (PrintStream log = new PrintStream(new FileOutputStream(args[1]), false, "UTF-8"); ClassMetadataReader cp = new ClassMetadataReader()) {
			cp.getCp().addAll(conc);
			walk(log, cp);
		}
	}
	private static void walk(PrintStream log, ClassMetadataReader cp) {
		mods.forEach(e -> {
			try {
				System.out.println("Processing " + e.getName());
				e.stream().filter(a -> a.getName().endsWith(".class")).forEach(a -> {
					try {
						final ClassNode n = new ClassNode();
						new ClassReader(e.getInputStream(a)).accept(n, ClassReader.SKIP_FRAMES);
						n.methods.forEach(u -> {
							for (final AbstractInsnNode p1 : u.instructions.toArray()) {
								if (checkInsn(p1, u, n, cp, log)) return;
							}
						});
					} catch (final Throwable t) {
						t.printStackTrace(System.err);
					}
				});
			} catch (final Throwable t) {
				throw new RuntimeException(t);
			}
		});
		System.gc();
	}

	public static void proc(ClassMetadataReader r, PrintStream out, MethodInsnNode m) {
		try {
			final ClassNode n = new ClassNode();
			new ClassReader(r.getClassData(m.owner)).accept(n, ClassReader.SKIP_FRAMES);
			n.methods.forEach(u -> {
				if (u.name.equals(m.name))
					for (final AbstractInsnNode p1 : u.instructions.toArray())
						if (checkInsn(p1, u, n, r, out)) return;
			});
		} catch (final Throwable t) {
			t.printStackTrace(System.err);
		}
	}
	private static boolean checkInsn(AbstractInsnNode p1, MethodNode u, ClassNode n, ClassMetadataReader r, PrintStream out) {
		if (p1 instanceof MethodInsnNode) {
			final MethodInsnNode p = (MethodInsnNode) p1;
			if (p.name.toLowerCase(Locale.US)
					.contains("defineClass".toLowerCase(Locale.US))) {
				System.out.println("Class " + n.name
						+ " may has RCE in method " + u.name);
				out.println(n.name + "#"
						+ u.name);
				return true;
			} else proc(r, out, p);
		}
		return false;
	}
}
