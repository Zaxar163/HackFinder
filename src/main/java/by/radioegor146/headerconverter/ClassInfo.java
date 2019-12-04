package by.radioegor146.headerconverter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ClassInfo {
	public String name;
	public String superClass;
	public String[] interfaces;
	private String[] cpStrings;
	private short[] cpClasses;

	public ClassInfo(final byte[] classData) {
		parse(ByteBuffer.wrap(classData));
	}

	private String decodeString(final ByteBuffer buf) {
		final int size = buf.getChar();
		final int oldLimit = buf.limit();
		buf.limit(buf.position() + size);
		final StringBuilder sb = new StringBuilder(size + (size >> 1) + 16);
		while (buf.hasRemaining()) {
			final byte b = buf.get();
			if (b > 0)
				sb.append((char) b);
			else {
				final int b2 = buf.get();
				if ((b & 0xF0) != 0xE0)
					sb.append((char) ((b & 0x1F) << 6 | b2 & 0x3F));
				else {
					final int b3 = buf.get();
					sb.append((char) ((b & 0xF) << 12 | (b2 & 0x3F) << 6 | b3 & 0x3F));
				}
			}
		}
		buf.limit(oldLimit);
		return sb.toString();
	}

	private void parse(final ByteBuffer buf) {
		if (buf.order(ByteOrder.BIG_ENDIAN).getInt() != -889275714)
			return;
		buf.getChar();
		buf.getChar();
		final int num = buf.getChar();
		cpStrings = new String[num];
		cpClasses = new short[num];
		for (int ix = 1; ix < num; ++ix) {
			final byte tag = buf.get();
			switch (tag) {
			default: {
				return;
			}
			case 1: {
				cpStrings[ix] = decodeString(buf);
				break;
			}
			case 7: {
				cpClasses[ix] = buf.getShort();
				break;
			}
			case 8:
			case 16: {
				buf.getChar();
				break;
			}
			case 9:
			case 10:
			case 11:
			case 12: {
				buf.getChar();
				buf.getChar();
				break;
			}
			case 3: {
				buf.getInt();
				break;
			}
			case 4: {
				buf.getFloat();
				break;
			}
			case 6: {
				buf.getDouble();
				++ix;
				break;
			}
			case 5: {
				buf.getLong();
				++ix;
				break;
			}
			case 15: {
				buf.get();
				buf.getChar();
				break;
			}
			case 18: {
				buf.getChar();
				buf.getChar();
				break;
			}
			}
		}
		buf.getChar();
		name = cpStrings[cpClasses[buf.getChar()]].replace('/', '.');
		superClass = cpStrings[cpClasses[buf.getChar()]].replace('/', '.');
		interfaces = new String[buf.getChar()];
		for (int i = 0; i < interfaces.length; ++i)
			interfaces[i] = cpStrings[cpClasses[buf.getChar()]].replace('/', '.');
	}
}