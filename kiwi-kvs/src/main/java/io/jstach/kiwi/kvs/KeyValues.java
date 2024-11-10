package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SequencedCollection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.interpolate.Interpolator;
import io.jstach.kiwi.kvs.interpolate.Interpolator.InterpolationException;
import io.jstach.kiwi.kvs.interpolate.Interpolator.MissingVariableInterpolationException;

public interface KeyValues extends Iterable<KeyValue> {

	public Stream<KeyValue> stream();

	public static Builder builder(KeyValuesResource resource) {
		return new Builder(resource.uri(), resource.reference());
	}

	public static Builder builder() {
		return new Builder(KeyValue.Source.NULL_URI, null);
	}

	public static KeyValues empty() {
		return KeyValuesEmpty.KeyValuesEmpty;
	}

	public static KeyValues copyOf(SequencedCollection<KeyValue> kvs) {
		return new ListKeyValues(List.copyOf(kvs));
	}

	public class Builder {

		private final URI source;

		private final @Nullable KeyValue reference;

		private final AtomicInteger index = new AtomicInteger();

		private final List<KeyValue> keyValues = new ArrayList<>();

		private final EnumSet<KeyValue.Flag> flags = EnumSet.noneOf(KeyValue.Flag.class);

		private final boolean noSource;

		private Builder(URI source, @Nullable KeyValue reference) {
			super();
			this.source = source;
			this.reference = reference;
			this.noSource = source.equals(KeyValue.Source.NULL_URI);
		}

		public Builder flag(KeyValue.Flag flag) {
			flags.add(flag);
			return this;
		}

		public Builder add(String key, String value) {
			keyValues.add(build(key, value));
			return this;
		}

		public Builder add(Entry<String, String> e) {
			return add(e.getKey(), e.getValue());
		}

		private static final Comparator<Entry<String, String>> entryComparator = Map.Entry
			.<String, String>comparingByKey()
			.thenComparing(Entry::getValue);

		public Builder add(SequencedCollection<Entry<String, String>> entries) {
			for (var e : entries) {
				add(e);
			}
			return this;
		}

		public Builder add(Collection<Entry<String, String>> entries) {
			switch (entries) {
				case SequencedCollection<Entry<String, String>> sc -> add(sc);
				default -> entries.stream().sorted(entryComparator).forEach(this::add);
			}
			;
			return this;
		}

		public KeyValue build(String key, String value) {
			var s = noSource ? KeyValue.Source.EMPTY : new KeyValue.Source(source, reference, index.incrementAndGet());
			var m = KeyValue.Meta.of(value, s, flags);
			return new KeyValue(key, value, m);
		}

		public KeyValues build() {
			return KeyValues.copyOf(keyValues);
		}

	}

	public static Collector<KeyValue, ?, KeyValues> collector() {
		return Collectors.collectingAndThen(Collectors.toList(), KeyValues::copyOf);
	}

	public static Collector<Map.Entry<String, String>, ?, KeyValues> collector(Builder builder) {
		return Collector.of(() -> builder,
				// Accumulator: add each entry to the builder
				(b, entry) -> b.add(entry),
				// Combiner: combine two builders (can be ignored if the stream is
				// sequential)
				(b1, b2) -> {
					throw new UnsupportedOperationException("Parallel streams not supported");
				},
				// Finisher: call build on the final builder
				Builder::build);
	}

	@Override
	default Iterator<KeyValue> iterator() {
		return stream().iterator();
	}

	default KeyValues map(UnaryOperator<KeyValue> kv) {
		return ToStringableKeyValues.of(() -> stream().map(kv));
	}

	default KeyValues filter(Predicate<KeyValue> predicate) {
		return ToStringableKeyValues.of(() -> stream().filter(predicate));
	}

	default KeyValues flatMap(Function<KeyValue, KeyValues> func) {
		var f = func.andThen(KeyValues::stream);
		return ToStringableKeyValues.of(() -> stream().flatMap(f));
	}

	default Map<String, String> interpolate(Variables variables) {
		return KeyValuesInterpolator.interpolateKeyValues(this, variables);
	}

	default KeyValues expand(Variables variables) {
		Map<String, String> interpolated = interpolate(variables);
		return map(kv -> kv.withExpanded(interpolated::get));
	}

	default KeyValues memoize() {
		if (this instanceof MemoizedKeyValues mkvs) {
			return mkvs;
		}
		return copyOf(stream().toList());
	}

	default KeyValues redact() {
		return this.map(KeyValue::redact);
	}

	default Map<String, String> toMap() {
		Map<String, String> m = new LinkedHashMap<>();
		stream().forEach(kv -> m.put(kv.key(), kv.expanded()));
		return m;
	}

}

interface ToStringableKeyValues extends KeyValues {

	static KeyValues of(KeyValues kvs) {
		if (kvs instanceof ToStringableKeyValues t) {
			return t;
		}
		return new PrintableKeyValues(kvs);

	}

	static String toString(KeyValues keyValues) {
		StringBuilder sb = new StringBuilder();
		var kvs = keyValues.redact();
		sb.append("KeyValues[").append("\n");
		KeyValuesMedia.ofProperties().formatter().format(sb, kvs);
		sb.append("]").append("\n");
		return sb.toString();
	}

}

interface MemoizedKeyValues extends KeyValues {

}

record PrintableKeyValues(KeyValues values) implements ToStringableKeyValues {
	@Override
	public Stream<KeyValue> stream() {
		return values.stream();
	}

	@Override
	public String toString() {
		return ToStringableKeyValues.toString(this);
	}
}

record ListKeyValues(List<KeyValue> keyValues) implements ToStringableKeyValues, MemoizedKeyValues {

	ListKeyValues {
		keyValues = List.copyOf(keyValues);
	}

	@Override
	public Stream<KeyValue> stream() {
		return keyValues.stream();
	}

	public KeyValues memoize() {
		return this;
	}

	@Override
	public final String toString() {
		return ToStringableKeyValues.toString(this);
	}
}

class KeyValuesInterpolator {

	static Map<String, String> interpolateKeyValues(final KeyValues keyValues, final Variables variables) {
		List<KeyValue> kvs = keyValues.stream().toList();

		final Map<String, KeyValue> flat = new HashMap<>(kvs.size());
		kvs.forEach(kv -> flat.put(kv.key(), kv));

		final Map<String, String> resolved = new LinkedHashMap<>(kvs.size());

		Variables combined = Variables.builder().add(k -> {
			var f = flat.get(k);
			if (f != null) {
				return f.raw();
			}
			return null;

		}).add(resolved).add(variables).build();
		Interpolator sub = Interpolator.create((key) -> {
			return combined.getValue(key);
		});
		for (KeyValue kv : kvs) {
			KeyValue last = flat.remove(kv.key());
			String v = kv.raw();
			String value;
			if (kv.isNoInterpolation()) {
				value = v;
			}
			else {
				try {
					value = (v.indexOf('$') != -1) ? sub.interpolate(kv.key(), v) : v;
				}
				catch (MissingVariableInterpolationException e) {
					throw e;
				}
				catch (InterpolationException e) {
					String r = resolved.get(kv.key());
					if (r == null) {
						r = kv.expanded();
					}
					value = r;
				}
			}

			resolved.put(kv.key(), value);
			if (last != null) {
				flat.put(kv.key(), last);
			}

		}
		return resolved;
	}

}

enum KeyValuesEmpty implements KeyValues {

	KeyValuesEmpty;

	@Override
	public Stream<KeyValue> stream() {
		return Stream.empty();
	}

}