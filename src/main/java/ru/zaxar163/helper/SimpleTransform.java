package ru.zaxar163.helper;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

public class SimpleTransform {
	public static void main(final String[] args) throws IOException {
		final File in = new File("in");
		final File out = new File("out");
		out.mkdir();
		for (final File f : in.listFiles(e -> e.getName().endsWith(".class"))) {
			final File fout = new File(out, f.getName().concat(".java"));
			try (PrintWriter w = new PrintWriter(fout, "UTF-8")) {
				final ClassReader cr = new ClassReader(Files.readAllBytes(f.toPath()));
				cr.accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(w)), 2);
			}
		}
	}
}
