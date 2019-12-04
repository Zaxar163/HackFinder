package by.radioegor146.headerconverter;

public class ClassPair implements Comparable<ClassPair> {
	public byte[] classData;
	public int priority;
	public ClassInfo classInfo;

	public ClassPair(final byte[] classData) {
		this.classData = classData;
		priority = 0;
	}

	@Override
	public int compareTo(final ClassPair o) {
		return o.priority - priority;
	}
}