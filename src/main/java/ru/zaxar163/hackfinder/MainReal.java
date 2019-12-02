package ru.zaxar163.hackfinder;

import java.io.IOException;
import java.util.Arrays;

public class MainReal {
	private static void help() {
		System.out.println("Modes: rce, packethack, packethacksrc");
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
		default:
			help();
			break;
		}
	}
}
