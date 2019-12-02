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
import java.util.concurrent.atomic.AtomicBoolean;
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

	private static boolean checkInsn(final AbstractInsnNode p1, final MethodNode u, final ClassNode n,
			final ClassMetadataReader r, final PrintStream out) {
		if (p1 instanceof MethodInsnNode) {
			final MethodInsnNode p = (MethodInsnNode) p1;
			if (p.name.toLowerCase(Locale.US).contains("defineClass".toLowerCase(Locale.US))) {
				System.out.println("Class " + n.name + " may has RCE in method " + u.name);
				out.println(n.name + "#" + u.name);
				return true;
			} else
				return proc(r, out, p);
		} else
			return false;
	}

	public static void main(final String[] args) throws IOException {
		if (args.length < 3) {
			System.out.println("Usage: rce <mods dir> <libraries dir> <log file>");
			System.exit(1);
		}
		IOHelper.walk(Paths.get(args[0]), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				mods.add(new JarFile(file.toFile()));
				return super.visitFile(file, attrs);
			}
		}, true);
		IOHelper.walk(Paths.get(args[1]), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				libraries.add(new JarFile(file.toFile()));
				return super.visitFile(file, attrs);
			}
		}, true);
		conc.addAll(mods);
		conc.addAll(libraries);
		try (PrintStream log = new PrintStream(new FileOutputStream(args[2]), false, "UTF-8");
				ClassMetadataReader cp = new ClassMetadataReader()) {
			cp.getCp().addAll(conc);
			walk(log, cp);
		}
	}

	public static boolean proc(final ClassMetadataReader r, final PrintStream out, final MethodInsnNode m) {
		final AtomicBoolean b = new AtomicBoolean(false);
		try {
			final ClassNode n = new ClassNode();
			new ClassReader(r.getClassData(m.owner)).accept(n, ClassReader.SKIP_FRAMES);
			n.methods.forEach(u -> {
				if (u.name.equals(m.name))
					for (final AbstractInsnNode p1 : u.instructions.toArray())
						if (checkInsn(p1, u, n, r, out)) {
							b.set(true);
							return;
						}
			});
		} catch (final Throwable t) {
			t.printStackTrace(System.err);
		}
		return b.get();
	}

	private static void walk(final PrintStream log, final ClassMetadataReader cp) {
		mods.forEach(e -> {
			try {
				System.out.println("Processing " + e.getName());
				e.stream().filter(a -> a.getName().endsWith(".class")).forEach(a -> {
					try {
						final ClassNode n = new ClassNode();
						new ClassReader(e.getInputStream(a)).accept(n, ClassReader.SKIP_FRAMES);
						n.methods.forEach(u -> {
							for (final AbstractInsnNode p1 : u.instructions.toArray())
								if (checkInsn(p1, u, n, cp, log))
									return;
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
}
