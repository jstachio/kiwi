package io.jstach.kiwi.kvs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/**
 * Sadly there is no ImmutableEnumSet in Java. We don't use <code>Set.of</code> because
 * its order is not predictable.
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
	@SuppressWarnings({ "override.return", "override.param", "toarray.nullable.elements.not.newarray" })
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
	@SuppressWarnings({ "override.return", "override.param" })
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

record FlagNames(List<String> names, List<String> reverseNames) {

	private static final List<String> NEGATION_PREFIXES = List.of("NO_", "NOT_");

	FlagNames {
		List<String> normalizedNames = new ArrayList<>();
		List<String> normalizedReverseNames = new ArrayList<>();

		// Process both lists
		processList(names, normalizedNames, normalizedReverseNames);
		processList(reverseNames, normalizedReverseNames, normalizedNames);

		// Remove duplicates using stream().distinct() and return the filled FlagNames
		// record
		names = normalizedNames.stream().distinct().toList();
		reverseNames = normalizedReverseNames.stream().distinct().toList();
	}

	/**
	 * Processes a list of names, normalizing and generating their counterparts in the
	 * reverse list.
	 * @param inputList the input list to process
	 * @param normalizedList the list where normalized input is added
	 * @param counterpartList the list where the counterparts are added
	 */
	private static void processList(List<String> inputList, List<String> normalizedList, List<String> counterpartList) {
		for (String originalName : inputList) {
			String normalized = originalName.toUpperCase();
			normalizedList.add(normalized);

			boolean isNegated = false;
			String baseName = normalized;

			for (String prefix : NEGATION_PREFIXES) {
				String stripped = removePrefix(normalized, prefix);
				if (stripped != null) {
					isNegated = true;
					baseName = stripped;
					break;
				}
			}

			var negatedList = isNegated ? normalizedList : counterpartList;
			var nonNegatedList = isNegated ? counterpartList : normalizedList;

			for (String prefix : NEGATION_PREFIXES) {
				negatedList.add(prefix + baseName);
			}
			nonNegatedList.add(baseName);
		}
	}

	/**
	 * Removes the specified prefix from a string if it exists.
	 * @param key the string to process
	 * @param prefix the prefix to remove
	 * @return the string without the prefix, or {@code null} if the prefix is not present
	 */
	private static String removePrefix(String key, String prefix) {
		if (!key.startsWith(prefix)) {
			return null;
		}
		return key.substring(prefix.length());
	}
}
