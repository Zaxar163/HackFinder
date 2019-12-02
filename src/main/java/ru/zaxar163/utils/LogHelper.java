package ru.zaxar163.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class LogHelper {

	public enum Level {
		DEV("DEV"), DEBUG("DEBUG"), INFO("INFO"), WARNING("WARN"), ERROR("ERROR");

		public final String name;

		Level(final String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	@FunctionalInterface
	public interface Output {
		void println(String message);
	}

	public static class OutputEnity {
		public Output output;
		public OutputTypes type;

		public OutputEnity(final Output output, final OutputTypes type) {
			this.output = output;
			this.type = type;
		}
	}

	public enum OutputTypes {
		PLAIN, HTML
	}

	private static class WriterOutput implements Output, AutoCloseable {
		private final Writer writer;

		private WriterOutput(final Writer writer) {
			this.writer = writer;
		}

		@Override
		public void close() throws IOException {
			writer.close();
		}

		@Override
		public void println(final String message) {
			try {
				writer.write(message + System.lineSeparator());
				writer.flush();
			} catch (final IOException ignored) {
				// Do nothing?
			}
		}
	}

	public static final String DEBUG_PROPERTY = "launcher.debug";
	public static final String DEV_PROPERTY = "launcher.dev";
	public static final String STACKTRACE_PROPERTY = "launcher.stacktrace";

	public static final String NO_JANSI_PROPERTY = "launcher.noJAnsi";

	// Output settings
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss",
			Locale.US);

	private static final AtomicBoolean DEBUG_ENABLED = new AtomicBoolean(Boolean.getBoolean(DEBUG_PROPERTY));
	private static final AtomicBoolean STACKTRACE_ENABLED = new AtomicBoolean(Boolean.getBoolean(STACKTRACE_PROPERTY));
	private static final AtomicBoolean DEV_ENABLED = new AtomicBoolean(Boolean.getBoolean(DEV_PROPERTY));

	private static final Set<OutputEnity> OUTPUTS = Collections.newSetFromMap(new ConcurrentHashMap<>(2));

	private static final Set<Consumer<Throwable>> EXCEPTIONS_CALLBACKS = Collections
			.newSetFromMap(new ConcurrentHashMap<>(2));

	private static final OutputEnity STD_OUTPUT;

	static {
		// Add std writer
		STD_OUTPUT = new OutputEnity(System.out::println, OutputTypes.PLAIN);
		addOutput(STD_OUTPUT);

		// Add file log writer
		final String logFile = System.getProperty("launcher.logFile");
		if (logFile != null)
			try {
				addOutput(IOHelper.toPath(logFile));
			} catch (final IOException e) {
				error(e);
			}
	}

	public static void addExcCallback(final Consumer<Throwable> output) {
		EXCEPTIONS_CALLBACKS.add(Objects.requireNonNull(output, "output"));
	}

	public static void addOutput(final Output output, final OutputTypes type) {
		OUTPUTS.add(new OutputEnity(Objects.requireNonNull(output, "output"), type));
	}

	public static void addOutput(final OutputEnity output) {
		OUTPUTS.add(Objects.requireNonNull(output, "output"));
	}

	public static void addOutput(final Path file) throws IOException {
		addOutput(IOHelper.newWriter(file, true));
	}

	public static void addOutput(final Writer writer) {
		addOutput(new WriterOutput(writer), OutputTypes.PLAIN);
	}

	public static void debug(final String message) {
		if (isDebugEnabled())
			log(Level.DEBUG, message, false);
	}

	public static void debug(final String format, final Object... args) {
		debug(String.format(format, args));
	}

	public static void dev(final String message) {
		if (isDevEnabled())
			log(Level.DEV, message, false);
	}

	public static void dev(final String format, final Object... args) {
		if (isDevEnabled())
			dev(String.format(format, args));
	}

	public static void error(final String message) {
		log(Level.ERROR, message, false);
	}

	public static void error(final String format, final Object... args) {
		error(String.format(format, args));
	}

	public static void error(final Throwable exc) {
		EXCEPTIONS_CALLBACKS.forEach(e -> e.accept(exc));
		error(isStacktraceEnabled() ? toString(exc) : exc.toString());
	}

	private static String formatLog(final Level level, final String message, final String dateTime, final boolean sub) {
		return rawFormat(level, dateTime, sub) + message;
	}

	public static String getDataTime() {
		return DATE_TIME_FORMATTER.format(LocalDateTime.now());
	}

	public static String htmlFormatLog(final Level level, final String dateTime, final String message,
			final boolean sub) {
		String levelColor;
		switch (level) {
		case WARNING:
			levelColor = "gravitlauncher-log-warning";
			break;
		case ERROR:
			levelColor = "gravitlauncher-log-error";
			break;
		case INFO:
			levelColor = "gravitlauncher-log-info";
			break;
		case DEBUG:
			levelColor = "gravitlauncher-log-debug";
			break;
		case DEV:
			levelColor = "gravitlauncher-log-dev";
			break;
		default:
			levelColor = "gravitlauncher-log-unknown";
			break;
		}
		if (sub)
			levelColor += " gravitlauncher-log-sub";
		return String.format("%s <span class=\"gravitlauncher-log %s\">[%s] %s</span>", dateTime, levelColor,
				level.toString(), sub ? ' ' + message : message);
	}

	public static void info(final String message) {
		log(Level.INFO, message, false);
	}

	public static void info(final String format, final Object... args) {
		info(String.format(format, args));
	}

	public static boolean isDebugEnabled() {
		return DEBUG_ENABLED.get();
	}

	public static boolean isDevEnabled() {
		return DEV_ENABLED.get();
	}

	public static boolean isStacktraceEnabled() {
		return STACKTRACE_ENABLED.get();
	}

	public static void log(final Level level, final String message, final boolean sub) {
		final String dateTime = DATE_TIME_FORMATTER.format(LocalDateTime.now());
		String plainString = null, htmlString = null;
		for (final OutputEnity output : OUTPUTS)
			if (output.type == OutputTypes.HTML) {
				if (htmlString != null) {
					output.output.println(htmlString);
					continue;
				}

				htmlString = htmlFormatLog(level, dateTime, message, sub);
				output.output.println(htmlString);
			} else {
				if (plainString != null) {
					output.output.println(plainString);
					continue;
				}

				plainString = formatLog(level, message, dateTime, sub);
				output.output.println(plainString);
			}
	}

	public static String rawFormat(final LogHelper.Level level, final String dateTime, final boolean sub) {
		return dateTime + " [" + level.name + (sub ? "]  " : "] ");
	}

	public static void rawLog(final Supplier<String> plainStr, final Supplier<String> jansiStr) {
		rawLog(plainStr, jansiStr, null);
	}

	public static void rawLog(final Supplier<String> plainStr, final Supplier<String> jansiStr,
			final Supplier<String> htmlStr) {
		String plainString = null, htmlString = null;
		for (final OutputEnity output : OUTPUTS)
			if (output.type == OutputTypes.HTML) {
				if (htmlString != null) {
					output.output.println(htmlString);
					continue;
				}

				htmlString = htmlStr.get();
				output.output.println(htmlString);
			} else {
				if (plainString != null) {
					output.output.println(plainString);
					continue;
				}

				plainString = plainStr.get();
				output.output.println(plainString);
			}
	}

	public static boolean removeOutput(final OutputEnity output) {
		return OUTPUTS.remove(output);
	}

	public static boolean removeStdOutput() {
		return removeOutput(STD_OUTPUT);
	}

	public static void setDebugEnabled(final boolean debugEnabled) {
		DEBUG_ENABLED.set(debugEnabled);
	}

	public static void setDevEnabled(final boolean stacktraceEnabled) {
		DEV_ENABLED.set(stacktraceEnabled);
	}

	public static void setStacktraceEnabled(final boolean stacktraceEnabled) {
		STACKTRACE_ENABLED.set(stacktraceEnabled);
	}

	public static void subDebug(final String message) {
		if (isDebugEnabled())
			log(Level.DEBUG, message, true);
	}

	public static void subDebug(final String format, final Object... args) {
		subDebug(String.format(format, args));
	}

	public static void subInfo(final String message) {
		log(Level.INFO, message, true);
	}

	public static void subInfo(final String format, final Object... args) {
		subInfo(String.format(format, args));
	}

	public static void subWarning(final String message) {
		log(Level.WARNING, message, true);
	}

	public static void subWarning(final String format, final Object... args) {
		subWarning(String.format(format, args));
	}

	public static String toString(final Throwable exc) {
		final StringWriter sw = new StringWriter();
		exc.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	public static void warning(final String message) {
		log(Level.WARNING, message, false);
	}

	public static void warning(final String format, final Object... args) {
		warning(String.format(format, args));
	}

	private LogHelper() {
	}
}