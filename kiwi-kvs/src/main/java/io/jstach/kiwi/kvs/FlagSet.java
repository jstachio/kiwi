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

/*
 * Sadly there is no ImmutableEnumSet in Java.
 * We don't use <code>Set.of</code> because its order
 * is not predictable.
 */
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

	public void forEach(Consumer<? super E> action) {
		set.forEach(action);
	}

	public boolean equals(Object o) {
		return set.equals(o);
	}

	public Iterator<E> iterator() {
		return set.iterator();
	}

	public int size() {
		return set.size();
	}

	public boolean isEmpty() {
		return set.isEmpty();
	}

	public boolean contains(Object o) {
		return set.contains(o);
	}

	public int hashCode() {
		return set.hashCode();
	}

	public Object[] toArray() {
		return set.toArray();
	}

	public boolean removeAll(Collection<?> c) {
		return set.removeAll(c);
	}

	public <T> T[] toArray(T[] a) {
		return set.toArray(a);
	}

	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean containsAll(Collection<?> c) {
		return set.containsAll(c);
	}

	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> c) {
		return set.retainAll(c);
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public String toString() {
		return set.toString();
	}

	public Set<E> clone() {
		return this;
	}

	public <T> T[] toArray(IntFunction<T[]> generator) {
		return set.toArray(generator);
	}

	public boolean removeIf(Predicate<? super E> filter) {
		throw new UnsupportedOperationException();
	}

	public Spliterator<E> spliterator() {
		return set.spliterator();
	}

	public Stream<E> stream() {
		return set.stream();
	}

	public Stream<E> parallelStream() {
		return set.parallelStream();
	}

}
