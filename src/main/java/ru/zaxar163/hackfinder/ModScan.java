package ru.zaxar163.hackfinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class ModScan {
	public static void main(final String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		if (args.length < 2) {
			System.out.println("Usage: packethack <mods dir> <log file>");
			System.exit(1);
		}
		final File mods = new File(args[0]);
		try (PrintStream log = new PrintStream(new FileOutputStream(args[1]), false, "UTF-8")) {
			Arrays.stream(mods.listFiles()).filter(e -> e.getName().endsWith(".jar") || e.getName().endsWith(".zip"))
					.map(e -> {
						try {
							return new ZipFile(e);
						} catch (final Exception e1) {
							throw new RuntimeException(e1);
						}
					}).forEach(e -> {
						try {
							System.out.println("Processing " + e.getName());
							e.stream().filter(a -> a.getName().endsWith(".class")).forEach(a -> {
								try {
									final ClassNode n = new ClassNode();
									new ClassReader(e.getInputStream(a)).accept(n, ClassReader.SKIP_FRAMES);
									n.methods.forEach(u -> {
										for (final AbstractInsnNode p1 : u.instructions.toArray())
											if (p1 instanceof MethodInsnNode) {
												final MethodInsnNode p = (MethodInsnNode) p1;
												if (p.name.toLowerCase(Locale.US)
														.contains("readItemStack".toLowerCase(Locale.US))
														|| p.name.toLowerCase(Locale.US).contains("func_150791_c")) {
													System.out.println("Class " + n.name
															+ " may has packethack in method " + u.name);
													log.println(new File(e.getName()).getName() + "=" + n.name + "#"
															+ u.name);
												}
												return;
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
	}
}
