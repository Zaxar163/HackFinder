package ru.zaxar163.hackfinder;

import java.io.IOException;
import java.util.Arrays;

import ru.zaxar163.helper.ConvertMe;

public class MainReal {
	private static void help() {
		System.out.println("Modes: rce, packethack, packethacksrc, classConverter, jarConverter");
		System.out.println("Usage: <mode>");
		System.exit(1);
	}

	public static void main(final String[] args) throws IOException {
		if (args.length < 1)
			help();
		final String[] realArgs = Arrays.copyOfRange(args, 1, args.length);
		switch (args[0]) {
		case "packethack":
			ModScan.main(realArgs);
			break;
		case "rce":
			RCEFinder.main(realArgs);
			break;
		case "packethacksrc":
			ModSrcScan.main(realArgs);
			break;
		case "jarConverter":
			ConvertMe.mainJar(realArgs);
		case "classConverter":
			ConvertMe.mainClass(realArgs);
			break;
		default:
			help();
			break;
		}
	}
}
