package ru.zaxar163.hackfinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class ModSrcScan {
	public static void check(final PrintStream log, final Path pa) throws IOException {
		Files.lines(pa, StandardCharsets.UTF_8).forEach(p -> {
			if (p.toLowerCase(Locale.US).contains("readItemStack".toLowerCase(Locale.US))
					|| p.toLowerCase(Locale.US).contains("func_150791_c")) {
				System.out.println("Class " + pa.toFile().getName() + " may has packethack");
				log.println(pa.toFile().getName());
			}
		});
	}

	public static void main(final String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		if (args.length < 2) {
			System.out.println("Usage: packethack <mods src dir> <log file>");
			System.exit(1);
		}
		final File mods = new File(args[0]);
		try (PrintStream log = new PrintStream(new FileOutputStream(args[1]), false, "UTF-8")) {
			Files.walk(mods.toPath()).filter(e -> Files.isReadable(e) && e.toFile().getName().endsWith(".java"))
					.forEach(e -> {
						try {
							check(log, e);
						} catch (final IOException e1) {
							throw new RuntimeException(e1);
						}
					});
			System.gc();
		} catch (final Throwable e) {
			e.printStackTrace(System.err);
		}
	}
}
