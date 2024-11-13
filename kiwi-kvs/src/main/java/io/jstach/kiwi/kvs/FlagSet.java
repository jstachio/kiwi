package io.jstach.kiwi.kvs;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/**
 * Sadly there is no ImmutableEnumSet in Java.
 * We don't use <code>Set.of</code> because its order
 * is not predictable.
 */
@SuppressWarnings({})
final class FlagSet<E extends Enum<E>> implements Set<E> {

	private final EnumSet<E> set;

	FlagSet(EnumSet<E> set) {
		super();
		this.set = EnumSet.copyOf(set);
	}

	public static <E extends Enum<E>> FlagSet<E> copyOf(FlagSet<E> fs) {
		return fs;
	}

	public static <E extends Enum<E>> FlagSet<E> copyOf(EnumSet<E> fs) {
		return new FlagSet<>(fs);
	}

	public static <E extends Enum<E>> FlagSet<E> copyOf(Collection<E> col, Class<E> enumClass) {
		if (col instanceof FlagSet<E> fs) {
			return copyOf(fs);
		}
		return new FlagSet<>(enumSetOf(enumClass, col));
	}

	static <E extends Enum<E>> EnumSet<E> enumSetOf(Class<E> enumClass, Collection<E> col) {
		if (col.isEmpty()) {
			return EnumSet.noneOf(enumClass);
		}
		return EnumSet.copyOf(col);
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		set.forEach(action);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		return set.equals(o);
	}

	@Override
	public Iterator<E> iterator() {
		return set.iterator();
	}

	@Override
	public int size() {
		return set.size();
	}

	@Override
	public boolean isEmpty() {
		return set.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return set.contains(o);
	}

	@Override
	public int hashCode() {
		return set.hashCode();
	}

	@Override
	public Object[] toArray() {
		return set.toArray();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return set.removeAll(c);
	}

	@Override
	@SuppressWarnings({"override.return", "override.param", "toarray.nullable.elements.not.newarray"})
	public <T> @Nullable T[] toArray(T[] a) {
		return set.toArray(a);
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return set.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return set.retainAll(c);
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return set.toString();
	}

	@Override
	public Set<E> clone() {
		return this;
	}

	@Override
	// I'm not sure it is possible to placate checker with jspecify for toArray
	@SuppressWarnings({"override.return", "override.param"})
	public <T> @Nullable T[] toArray(IntFunction<T[]> generator) {
		return set.toArray(generator);
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Spliterator<E> spliterator() {
		return set.spliterator();
	}

	@Override
	public Stream<E> stream() {
		return set.stream();
	}

	@Override
	public Stream<E> parallelStream() {
		return set.parallelStream();
	}

}
