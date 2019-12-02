package ru.zaxar163.utils;

import java.util.ArrayList;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * ClassWriter с другой реализацией метода getCommonSuperClass: при его
 * использовании не происходит загрузки классов.
 */
public class SafeClassWriter extends ClassWriter {

	private final ClassMetadataReader classMetadataReader;

	public SafeClassWriter(final ClassMetadataReader classMetadataReader, final int flags) {
		super(flags);
		this.classMetadataReader = classMetadataReader;
	}

	public SafeClassWriter(final ClassReader classReader, final ClassMetadataReader classMetadataReader,
			final int flags) {
		super(classReader, flags);
		this.classMetadataReader = classMetadataReader;
	}

	@Override
	protected String getCommonSuperClass(final String type1, final String type2) {
		final ArrayList<String> superClasses1 = classMetadataReader.getSuperClasses(type1);
		final ArrayList<String> superClasses2 = classMetadataReader.getSuperClasses(type2);
		final int size = Math.min(superClasses1.size(), superClasses2.size());
		int i;
		for (i = 0; i < size && superClasses1.get(i).equals(superClasses2.get(i)); i++)
			;
		if (i == 0)
			return "java/lang/Object";
		else
			return superClasses1.get(i - 1);
	}

}