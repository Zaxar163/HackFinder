package ru.zaxar163.hackfinder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

public class SimpleTransform {
	public static void main(String[] args) throws IOException {
		File in = new File("in");
		File out = new File("out");
		out.mkdir();
		for (File f : in.listFiles(e -> e.getName().endsWith(".class"))) {
			File fout = new File(out, f.getName().concat(".java"));
			try (PrintWriter w = new PrintWriter(fout, "UTF-8")) {
				ClassReader cr = new ClassReader(Files.readAllBytes(f.toPath()));
				cr.accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(w)), 2);
			}
		}
	}
}
