package ru.zaxar163.utils;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

public final class IOHelper {
	private static final class DeleteDirVisitor extends SimpleFileVisitor<Path> {
		private final Path dir;
		private final boolean self;

		private DeleteDirVisitor(final Path dir, final boolean self) {
			this.dir = dir;
			this.self = self;
		}

		@Override
		public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
			final FileVisitResult result = super.postVisitDirectory(dir, exc);
			if (self || !this.dir.equals(dir))
				Files.delete(dir);
			return result;
		}

		@Override
		public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
			Files.delete(file);
			return super.visitFile(file, attrs);
		}
	}

	private static final class SkipHiddenVisitor implements FileVisitor<Path> {
		private final FileVisitor<Path> visitor;

		private SkipHiddenVisitor(final FileVisitor<Path> visitor) {
			this.visitor = visitor;
		}

		@Override
		public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
			return Files.isHidden(dir) ? FileVisitResult.CONTINUE : visitor.postVisitDirectory(dir, exc);
		}

		@Override
		public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
			return Files.isHidden(dir) ? FileVisitResult.SKIP_SUBTREE : visitor.preVisitDirectory(dir, attrs);
		}

		@Override
		public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
			return Files.isHidden(file) ? FileVisitResult.CONTINUE : visitor.visitFile(file, attrs);
		}

		@Override
		public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
			return visitor.visitFileFailed(file, exc);
		}
	}

	public static final long MB32 = 1 << 25;

	// Charset

	public static final Charset UNICODE_CHARSET = StandardCharsets.UTF_8;

	public static final Charset ASCII_CHARSET = StandardCharsets.US_ASCII;
	// Constants

	public static final int SOCKET_TIMEOUT = VerifyHelper.verifyInt(
			Integer.parseUnsignedInt(System.getProperty("launcher.socketTimeout", Integer.toString(30000))),
			VerifyHelper.POSITIVE, "launcher.socketTimeout can't be <= 0");

	public static final int HTTP_TIMEOUT = VerifyHelper.verifyInt(
			Integer.parseUnsignedInt(System.getProperty("launcher.httpTimeout", Integer.toString(5000))),
			VerifyHelper.POSITIVE, "launcher.httpTimeout can't be <= 0");

	public static final int BUFFER_SIZE = VerifyHelper.verifyInt(
			Integer.parseUnsignedInt(System.getProperty("launcher.bufferSize", Integer.toString(4096))),
			VerifyHelper.POSITIVE, "launcher.bufferSize can't be <= 0");
	// Platform-dependent

	public static final String CROSS_SEPARATOR = "/";

	public static final FileSystem FS = FileSystems.getDefault();

	public static final String PLATFORM_SEPARATOR = FS.getSeparator();
	// Увидел исключение на NetBSD beta добавил

	public static final boolean POSIX = FS.supportedFileAttributeViews().contains("posix")
			|| FS.supportedFileAttributeViews().contains("Posix");
	// Paths

	public static final Path JVM_DIR = Paths.get(System.getProperty("java.home"));

	public static final Path HOME_DIR = Paths.get(System.getProperty("user.home"));

	public static final Path WORKING_DIR = Paths.get(System.getProperty("user.dir"));
	// Open options - as arrays
	private static final OpenOption[] READ_OPTIONS = { StandardOpenOption.READ };

	private static final OpenOption[] WRITE_OPTIONS = { StandardOpenOption.CREATE, StandardOpenOption.WRITE,
			StandardOpenOption.TRUNCATE_EXISTING };
	private static final OpenOption[] APPEND_OPTIONS = { StandardOpenOption.CREATE, StandardOpenOption.WRITE,
			StandardOpenOption.APPEND };
	// Other options
	private static final LinkOption[] LINK_OPTIONS = {};

	private static final CopyOption[] COPY_OPTIONS = { StandardCopyOption.REPLACE_EXISTING };
	private static final Set<FileVisitOption> WALK_OPTIONS = Collections.singleton(FileVisitOption.FOLLOW_LINKS);

	// Other constants
	private static final Pattern CROSS_SEPARATOR_PATTERN = Pattern.compile(CROSS_SEPARATOR, Pattern.LITERAL);

	private static final Pattern PLATFORM_SEPARATOR_PATTERN = Pattern.compile(PLATFORM_SEPARATOR, Pattern.LITERAL);
	public static final String USER_AGENT = System.getProperty("launcher.userAgentDefault",
			"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");

	public static void close(final AutoCloseable closeable) {
		try {
			closeable.close();
		} catch (final Exception exc) {
			LogHelper.error(exc);
		}
	}

	public static void close(final InputStream in) {
		try {
			in.close();
		} catch (final Exception ignored) {
		}
	}

	public static void close(final OutputStream out) {
		try {
			out.flush();
			out.close();
		} catch (final Exception ignored) {
		}
	}

	public static URL convertToURL(final String url) {
		try {
			return new URL(url);
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException("Invalid URL", e);
		}
	}

	public static void copy(final Path source, final Path target) throws IOException {
		createParentDirs(target);
		Files.copy(source, target, COPY_OPTIONS);
	}

	public static void createParentDirs(final Path path) throws IOException {
		final Path parent = path.getParent();
		if (parent != null && !isDir(parent))
			Files.createDirectories(parent);
	}

	public static String decode(final byte[] bytes) {
		return new String(bytes, UNICODE_CHARSET);
	}

	public static String decodeASCII(final byte[] bytes) {
		return new String(bytes, ASCII_CHARSET);
	}

	public static void deleteDir(final Path dir, final boolean self) throws IOException {
		walk(dir, new DeleteDirVisitor(dir, self), true);
	}

	public static byte[] encode(final String s) {
		return s.getBytes(UNICODE_CHARSET);
	}

	public static byte[] encodeASCII(final String s) {
		return s.getBytes(ASCII_CHARSET);
	}

	public static boolean exists(final Path path) {
		return Files.exists(path, LINK_OPTIONS);
	}

	public static Path getCodeSource(final Class<?> clazz) {
		return Paths.get(toURI(clazz.getProtectionDomain().getCodeSource().getLocation()));
	}

	public static String getFileName(final Path path) {
		return path.getFileName().toString();
	}

	public static String getIP(final SocketAddress address) {
		return ((InetSocketAddress) address).getAddress().getHostAddress();
	}

	public static byte[] getResourceBytes(final String name) throws IOException {
		return read(getResourceURL(name));
	}

	public static URL getResourceURL(final String name) throws NoSuchFileException {
		final URL url = IOHelper.class.getResource('/' + name);
		if (url == null)
			throw new NoSuchFileException(name);
		return url;
	}

	public static boolean hasExtension(final Path file, final String extension) {
		return getFileName(file).endsWith('.' + extension);
	}

	public static boolean isDir(final Path path) {
		return Files.isDirectory(path, LINK_OPTIONS);
	}

	public static boolean isEmpty(final Path dir) throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			return !stream.iterator().hasNext();
		}
	}

	public static boolean isFile(final Path path) {
		return Files.isRegularFile(path, LINK_OPTIONS);
	}

	public static boolean isValidFileName(final String fileName) {
		return !fileName.equals(".") && !fileName.equals("..")
				&& fileName.chars().noneMatch(ch -> ch == '/' || ch == '\\') && isValidPath(fileName);
	}

	public static boolean isValidPath(final String path) {
		try {
			toPath(path);
			return true;
		} catch (final InvalidPathException ignored) {
			return false;
		}
	}

	public static boolean isValidTextureBounds(final int width, final int height, final boolean cloak) {
		return width % 64 == 0 && (height << 1 == width || !cloak && height == width) && width <= 1024
				|| cloak && width % 22 == 0 && height % 17 == 0 && width / 22 == height / 17;
	}

	public static void move(final Path source, final Path target) throws IOException {
		createParentDirs(target);
		Files.move(source, target, COPY_OPTIONS);
	}

	public static byte[] newBuffer() {
		return new byte[BUFFER_SIZE];
	}

	public static InputStream newBufferedInput(final Path file) throws IOException {
		return new BufferedInputStream(Files.newInputStream(file, READ_OPTIONS));
	}

	public static BufferedInputStream newBufferedInput(final URL url) throws IOException {
		return new BufferedInputStream(newConnection(url).getInputStream());
	}

	public static OutputStream newBufferedOutput(final Path file) throws IOException {
		return newBufferedOutput(file, false);
	}

	public static OutputStream newBufferedOutput(final Path file, final boolean append) throws IOException {
		createParentDirs(file);
		return new BufferedOutputStream(Files.newOutputStream(file, append ? APPEND_OPTIONS : WRITE_OPTIONS));
	}

	public static ByteArrayOutputStream newByteArrayOutput() {
		return new ByteArrayOutputStream();
	}

	public static char[] newCharBuffer() {
		return new char[BUFFER_SIZE];
	}

	public static URLConnection newConnection(final URL url) throws IOException {
		final URLConnection connection = url.openConnection();
		if (connection instanceof HttpURLConnection) {
			connection.setReadTimeout(HTTP_TIMEOUT);
			connection.setConnectTimeout(HTTP_TIMEOUT);
			connection.addRequestProperty("User-Agent", USER_AGENT); // Fix for stupid servers
		} else
			connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(false);
		return connection;
	}

	public static HttpURLConnection newConnectionPost(final URL url) throws IOException {
		final HttpURLConnection connection = (HttpURLConnection) newConnection(url);
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		return connection;
	}

	public static Deflater newDeflater() {
		final Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
		deflater.setStrategy(Deflater.DEFAULT_STRATEGY);
		return deflater;
	}

	public static Inflater newInflater() {
		return new Inflater(true);
	}

	public static InputStream newInput(final Path file) throws IOException {
		return Files.newInputStream(file, READ_OPTIONS);
	}

	public static InputStream newInput(final URL url) throws IOException {
		return newConnection(url).getInputStream();
	}

	public static OutputStream newOutput(final Path file) throws IOException {
		return newOutput(file, false);
	}

	public static OutputStream newOutput(final Path file, final boolean append) throws IOException {
		createParentDirs(file);
		return Files.newOutputStream(file, append ? APPEND_OPTIONS : WRITE_OPTIONS);
	}

	public static BufferedReader newReader(final InputStream input) {
		return newReader(input, UNICODE_CHARSET);
	}

	public static BufferedReader newReader(final InputStream input, final Charset charset) {
		return new BufferedReader(new InputStreamReader(input, charset));
	}

	public static BufferedReader newReader(final Path file) throws IOException {
		return Files.newBufferedReader(file, UNICODE_CHARSET);
	}

	public static BufferedReader newReader(final URL url) throws IOException {
		final URLConnection connection = newConnection(url);
		final String charset = connection.getContentEncoding();
		return newReader(connection.getInputStream(), charset == null ? UNICODE_CHARSET : Charset.forName(charset));
	}

	public static Socket newSocket() throws SocketException {
		final Socket socket = new Socket();
		setSocketFlags(socket);
		return socket;
	}

	public static BufferedWriter newWriter(final FileDescriptor fd) {
		return newWriter(new FileOutputStream(fd));
	}

	public static BufferedWriter newWriter(final OutputStream output) {
		return new BufferedWriter(new OutputStreamWriter(output, UNICODE_CHARSET));
	}

	public static BufferedWriter newWriter(final Path file) throws IOException {
		return newWriter(file, false);
	}

	public static BufferedWriter newWriter(final Path file, final boolean append) throws IOException {
		createParentDirs(file);
		return Files.newBufferedWriter(file, UNICODE_CHARSET, append ? APPEND_OPTIONS : WRITE_OPTIONS);
	}

	public static ZipEntry newZipEntry(final String name) {
		final ZipEntry entry = new ZipEntry(name);
		entry.setTime(0);
		return entry;
	}

	public static ZipEntry newZipEntry(final ZipEntry entry) {
		return newZipEntry(entry.getName());
	}

	public static ZipInputStream newZipInput(final InputStream input) {
		return new ZipInputStream(input, UNICODE_CHARSET);
	}

	public static ZipInputStream newZipInput(final Path file) throws IOException {
		return newZipInput(newInput(file));
	}

	public static ZipInputStream newZipInput(final URL url) throws IOException {
		return newZipInput(newInput(url));
	}

	public static byte[] read(final DataInput in) throws IOException {
		final byte[] ret = new byte[in.readInt()];
		in.readFully(ret);
		return ret;
	}

	public static byte[] read(final InputStream input) throws IOException {
		try (ByteArrayOutputStream output = newByteArrayOutput()) {
			transfer(input, output);
			return output.toByteArray();
		}
	}

	public static void read(final InputStream input, final byte[] bytes) throws IOException {
		int offset = 0;
		while (offset < bytes.length) {
			final int length = input.read(bytes, offset, bytes.length - offset);
			if (length < 0)
				throw new EOFException(String.format("%d bytes remaining", bytes.length - offset));
			offset += length;
		}
	}

	public static byte[] read(final Path file) throws IOException {
		final long size = readAttributes(file).size();
		if (size > Integer.MAX_VALUE)
			throw new IOException("File too big");

		// Read bytes from file
		final byte[] bytes = new byte[(int) size];
		try (InputStream input = newInput(file)) {
			read(input, bytes);
		}

		// Return result
		return bytes;
	}

	public static byte[] read(final URL url) throws IOException {
		try (InputStream input = newInput(url)) {
			return read(input);
		}
	}

	public static BasicFileAttributes readAttributes(final Path path) throws IOException {
		return Files.readAttributes(path, BasicFileAttributes.class, LINK_OPTIONS);
	}

	public static BufferedImage readTexture(final Object input, final boolean cloak) throws IOException {
		final ImageReader reader = ImageIO.getImageReadersByMIMEType("image/png").next();
		try {
			reader.setInput(ImageIO.createImageInputStream(input), false, false);

			// Verify texture bounds
			final int width = reader.getWidth(0);
			final int height = reader.getHeight(0);
			if (!isValidTextureBounds(width, height, cloak))
				throw new IOException(String.format("Invalid texture bounds: %dx%d", width, height));

			// Read image
			return reader.read(0);
		} finally {
			reader.dispose();
		}
	}

	public static String request(final URL url) throws IOException {
		return decode(read(url)).trim();
	}

	public static InetSocketAddress resolve(final InetSocketAddress address) {
		if (address.isUnresolved())
			return new InetSocketAddress(address.getHostString(), address.getPort());
		return address;
	}

	public static Path resolveIncremental(final Path dir, final String name, final String extension) {
		final Path original = dir.resolve(name + '.' + extension);
		if (!exists(original))
			return original;

		// Incremental resolve
		int counter = 1;
		while (true) {
			final Path path = dir.resolve(String.format("%s (%d).%s", name, counter, extension));
			if (exists(path)) {
				counter++;
				continue;
			}
			return path;
		}
	}

	public static Path resolveJavaBin(final Path javaDir) {
		// Get Java binaries path
		final Path javaBinDir = (javaDir == null ? JVM_DIR : javaDir).resolve("bin");

		// Verify has "javaw.exe" file
		if (!LogHelper.isDebugEnabled()) {
			final Path javawExe = javaBinDir.resolve("javaw.exe");
			if (isFile(javawExe))
				return javawExe;
		}

		// Verify has "java.exe" file
		final Path javaExe = javaBinDir.resolve("java.exe");
		if (isFile(javaExe))
			return javaExe;

		// Verify has "java" file
		final Path java = javaBinDir.resolve("java");
		if (isFile(java))
			return java;

		// Throw exception as no runnable found
		throw new RuntimeException("Java binary wasn't found");
	}

	public static void setSocketFlags(final Socket socket) throws SocketException {
		// Set socket flags
		socket.setKeepAlive(false);
		socket.setTcpNoDelay(false);
		socket.setReuseAddress(true);

		// Set socket options
		socket.setSoTimeout(SOCKET_TIMEOUT);
		try {
			socket.setTrafficClass(0b11100);
		} catch (final SocketException ignored) {
			// Windows XP has no support for that
		}
		socket.setPerformancePreferences(1, 0, 2);
	}

	public static Path toAbsPath(final Path path) {
		return path.normalize().toAbsolutePath();
	}

	public static String toAbsPathString(final Path path) {
		return toAbsPath(path).toFile().getAbsolutePath();
	}

	public static byte[] toByteArray(final InputStream in) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());
		IOHelper.transfer(in, out);
		return out.toByteArray();
	}

	public static Path toPath(final String path) {
		return Paths
				.get(CROSS_SEPARATOR_PATTERN.matcher(path).replaceAll(Matcher.quoteReplacement(PLATFORM_SEPARATOR)));
	}

	public static String toString(final Path path) {
		return PLATFORM_SEPARATOR_PATTERN.matcher(path.toString())
				.replaceAll(Matcher.quoteReplacement(CROSS_SEPARATOR));
	}

	public static URI toURI(final URL url) {
		try {
			return url.toURI();
		} catch (final URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static URL toURL(final Path path) {
		try {
			return path.toUri().toURL();
		} catch (final MalformedURLException e) {
			throw new InternalError(e);
		}
	}

	public static void transfer(final byte[] write, final Path file, final boolean append) throws IOException {
		try (OutputStream out = newOutput(file, append)) {
			out.write(write);
		}
	}

	public static long transfer(final InputStream input, final OutputStream output) throws IOException {
		long transferred = 0;
		final byte[] buffer = newBuffer();
		for (int length = input.read(buffer); length >= 0; length = input.read(buffer)) {
			output.write(buffer, 0, length);
			transferred += length;
		}
		return transferred;
	}

	public static long transfer(final InputStream input, final Path file) throws IOException {
		return transfer(input, file, false);
	}

	public static long transfer(final InputStream input, final Path file, final boolean append) throws IOException {
		try (OutputStream output = newOutput(file, append)) {
			return transfer(input, output);
		}
	}

	public static void transfer(final Path file, final OutputStream output) throws IOException {
		try (InputStream input = newInput(file)) {
			transfer(input, output);
		}
	}

	public static String urlDecode(final String s) {
		try {
			return URLDecoder.decode(s, UNICODE_CHARSET.name());
		} catch (final UnsupportedEncodingException e) {
			throw new InternalError(e);
		}
	}

	public static String urlEncode(final String s) {
		try {
			return URLEncoder.encode(s, UNICODE_CHARSET.name());
		} catch (final UnsupportedEncodingException e) {
			throw new InternalError(e);
		}
	}

	public static String verifyFileName(final String fileName) {
		return VerifyHelper.verify(fileName, IOHelper::isValidFileName,
				String.format("Invalid file name: '%s'", fileName));
	}

	public static int verifyLength(final int length, final int max) throws IOException {
		if (length < 0 || max < 0 && length != -max || max > 0 && length > max)
			throw new IOException("Illegal length: " + length);
		return length;
	}

	public static BufferedImage verifyTexture(final BufferedImage skin, final boolean cloak) {
		return VerifyHelper.verify(skin, i -> isValidTextureBounds(i.getWidth(), i.getHeight(), cloak),
				String.format("Invalid texture bounds: %dx%d", skin.getWidth(), skin.getHeight()));
	}

	public static String verifyURL(final String url) {
		try {
			new URL(url).toURI();
			return url;
		} catch (MalformedURLException | URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URL", e);
		}
	}

	public static void walk(final Path dir, final FileVisitor<Path> visitor, final boolean hidden) throws IOException {
		Files.walkFileTree(dir, WALK_OPTIONS, Integer.MAX_VALUE, hidden ? visitor : new SkipHiddenVisitor(visitor));
	}

	public static void write(final DataOutput out, final byte[] read) throws IOException {
		out.writeInt(read.length);
		out.write(read);
	}

	public static void write(final Path file, final byte[] bytes) throws IOException {
		createParentDirs(file);
		Files.write(file, bytes, WRITE_OPTIONS);
	}

	private IOHelper() {
	}
}
