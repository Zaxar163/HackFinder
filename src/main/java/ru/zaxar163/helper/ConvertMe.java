package ru.zaxar163.helper;

import java.io.File;
import java.io.IOException;

import by.radioegor146.headerconverter.ConverterClass;
import by.radioegor146.headerconverter.ConverterJar;

public class ConvertMe {
	public static void mainClass(final String[] args) throws IOException {
		if (args.length < 3) {
			System.out.println("Usage: classConverter <in class> <out header> <prefix>");
			System.exit(1);
		}
		ConverterClass.convert(new File(args[0]), new File(args[1]), args[2]);
	}

	public static void mainJar(final String[] args) throws IOException {
		if (args.length < 3) {
			System.out.println("Usage: jarConverter <in jar> <out header> <prefix>");
			System.exit(1);
		}
		ConverterJar.convert(new File(args[0]), new File(args[1]), args[2]);
	}
}
