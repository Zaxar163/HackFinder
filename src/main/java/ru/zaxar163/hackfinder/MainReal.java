package ru.zaxar163.hackfinder;

import java.io.IOException;
import java.util.Arrays;

public class MainReal {
	public static void main(String[] args) throws IOException {
		if (args.length < 1) 
			help();
		String[] realArgs = Arrays.copyOfRange(args, 1, args.length);
		switch(args[0]) {
		case "packethack":
			ModScan.main(realArgs);
			break;
		case "rce":
			RCEFinder.main(realArgs);
			break;
		default:
			help();
			break;
		}
	}

	private static void help() {
		System.out.println("Modes: rce, packethack");
		System.out.println("Usage: <mode>");
		System.exit(1);
	}
}
