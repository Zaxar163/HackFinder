package by.radioegor146.headerconverter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ConverterJar {

	public static void convert(final File in, final File out, final String prefix) throws IOException {
		try (final ZipInputStream zipFile = new ZipInputStream(new FileInputStream(in));
				final PrintStream outPs = new PrintStream(new FileOutputStream(out), false, "UTF-8")) {
			final List<ClassPair> classes = new ArrayList<>();
			final HashMap<String, ClassPair> classMap = new HashMap<>();
			ZipEntry entry;
			while ((entry = zipFile.getNextEntry()) != null)
				if (entry.getName().endsWith(".class")) {
					final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					final byte[] data = new byte[16384];
					int nRead;
					while ((nRead = zipFile.read(data, 0, data.length)) != -1)
						buffer.write(data, 0, nRead);
					final byte[] classData = buffer.toByteArray();
					classes.add(new ClassPair(classData));
					final ClassInfo cinfo = new ClassInfo(classes.get(classes.size() - 1).classData);
					classes.get(classes.size() - 1).classInfo = cinfo;
					classMap.put(cinfo.name, classes.get(classes.size() - 1));
				}
			classes.forEach(_item -> classes.forEach(cpair -> {
				ClassInfo cinfo2;
				String[] interfaces;
				int length;
				int k = 0;
				String iface;
				cinfo2 = cpair.classInfo;
				if (classMap.containsKey(cinfo2.superClass))
					classMap.get(cinfo2.superClass).priority = Math.max(classMap.get(cinfo2.superClass).priority,
							cpair.priority + 1);
				interfaces = cinfo2.interfaces;
				for (length = interfaces.length; k < length; ++k) {
					iface = interfaces[k];
					if (classMap.containsKey(iface))
						classMap.get(iface).priority = Math.max(classMap.get(iface).priority, cpair.priority + 1);
				}
			}));
			Collections.sort(classes);
			classes.forEach(cls -> System.out.println(cls.classInfo.name));
			int tcnt = 0;
			outPs.println("#include \"jni.h\"");
			outPs.print("unsigned char ");
			outPs.print(prefix);
			outPs.println("Datas[]={");
			for (int i = 0; i < classes.size(); ++i)
				for (int j = 0; j < classes.get(i).classData.length; ++j) {
					outPs.print("0x" + String.format("%X", classes.get(i).classData[j]));
					++tcnt;
					if (i != classes.size() - 1 || j != classes.get(i).classData.length - 1)
						outPs.print(",");
					if (tcnt % 32 == 0)
						outPs.println();
				}
			outPs.println("};");
			outPs.print("jsize ");
			outPs.print(prefix);
			outPs.println("Sizes[]={");
			for (int i = 0; i < classes.size(); ++i) {
				outPs.print(classes.get(i).classData.length);
				if (i != classes.size() - 1)
					outPs.print(",");
			}
			outPs.println("};");
			outPs.print("jsize ");
			outPs.print(prefix);
			outPs.print("Count=");
			outPs.print(classes.size());
			outPs.println(";");
			outPs.flush();
		}
	}
}