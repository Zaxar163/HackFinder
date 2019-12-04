package by.radioegor146.headerconverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

public class ConverterClass {
	public static void convert(final File in, final File out, final String nameStart) throws IOException {
		try (final PrintStream outPs = new PrintStream(new FileOutputStream(out), false, "UTF-8")) {
			final byte[] bytes = Files.readAllBytes(in.toPath());
			int tcnt = 0;
			outPs.println("#include \"jni.h\"");
			outPs.println("unsigned char ");
			outPs.println(nameStart);
			outPs.println("[]={");
			for (int j = 0; j < bytes.length; ++j) {
				outPs.print("0x" + String.format("%X", bytes[j]));
				++tcnt;
				if (j != bytes.length - 1)
					outPs.print(",");
				if (tcnt % 32 == 0)
					outPs.println();
			}
			outPs.println("};");
			outPs.print("jsize ");
			outPs.print(nameStart);
			outPs.print("Size");
			outPs.print("=");
			outPs.print(bytes.length);
			outPs.println(";");
		}
	}
}
