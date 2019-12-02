package ru.zaxar163.utils;

import java.util.Map;
import java.util.Objects;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

public final class VerifyHelper {

	public static final IntPredicate POSITIVE = i -> i > 0;

	public static final IntPredicate NOT_NEGATIVE = i -> i >= 0;

	public static final LongPredicate L_POSITIVE = l -> l > 0;

	public static final LongPredicate L_NOT_NEGATIVE = l -> l >= 0;

	public static final Predicate<String> NOT_EMPTY = s -> !s.isEmpty();

	public static <K, V> V getMapValue(final Map<K, V> map, final K key, final String error) {
		return verify(map.get(key), Objects::nonNull, error);
	}

	public static <K, V> void putIfAbsent(final Map<K, V> map, final K key, final V value, final String error) {
		verify(map.putIfAbsent(key, value), Objects::isNull, error);
	}

	public static IntPredicate range(final int min, final int max) {
		return i -> i >= min && i <= max;
	}

	public static <T> T verify(final T object, final Predicate<T> predicate, final String error) {
		if (predicate.test(object))
			return object;
		throw new IllegalArgumentException(error);
	}

	public static double verifyDouble(final double d, final DoublePredicate predicate, final String error) {
		if (predicate.test(d))
			return d;
		throw new IllegalArgumentException(error);
	}

	public static int verifyInt(final int i, final IntPredicate predicate, final String error) {
		if (predicate.test(i))
			return i;
		throw new IllegalArgumentException(error);
	}

	public static long verifyLong(final long l, final LongPredicate predicate, final String error) {
		if (predicate.test(l))
			return l;
		throw new IllegalArgumentException(error);
	}

	private VerifyHelper() {
	}
}
