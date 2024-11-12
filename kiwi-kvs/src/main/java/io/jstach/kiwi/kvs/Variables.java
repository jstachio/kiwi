package io.jstach.kiwi.kvs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.Variables.Parameters;

/**
 * Represents a mapping function that associates a key to a value, typically used for
 * variable resolution during interpolation.
 *
 * <p>
 * The {@code Variables} interface allows lookup of values based on keys and is used in
 * contexts where key-to-value mappings are required for interpolation. Unlike
 * {@link KeyValues}, there are no duplicate keys in {@code Variables}.
 */
@FunctionalInterface
public interface Variables extends Function<String, @Nullable String> {

	/**
	 * Retrieves the value mapped to the specified key.
	 * @param key the key whose value is to be retrieved
	 * @return the value associated with the key, or {@code null} if not found
	 */
	public @Nullable String getValue(String key);

	@Override
	default @Nullable String apply(String t) {
		return getValue(t);
	}

	/**
	 * Represents a specialized {@code Variables} interface that can list all keys it
	 * knows about. The data in {@code Parameters} is guaranteed to be immutable and
	 * copied when created.
	 *
	 * <p>
	 * Unlike {@link KeyValues}, there are no duplicate keys in {@code Parameters}.
	 */
	public sealed interface Parameters extends Variables {

		/**
		 * Returns an iterable over all keys that this {@code Parameters} instance knows
		 * about.
		 * @return an iterable of keys
		 */
		public Iterable<String> keys();

		/**
		 * Iterates over all key-value pairs in this {@code Parameters} instance and
		 * applies them to a provided consumer.
		 * @param consumer the consumer to apply each key-value pair
		 */
		default void forKeyValues(BiConsumer<String, String> consumer) {
			for (var k : keys()) {
				String value = getValue(k);
				if (value == null) {
					continue;
				}
				consumer.accept(k, value);
			}
		}

		/**
		 * Creates a {@code Parameters} instance from a provided map.
		 * @param map the map to create the parameters from
		 * @return a new {@code Parameters} instance
		 */
		public static Parameters of(Map<String, String> map) {
			return new MapParameters(map);
		}

	}

	/**
	 * Returns an empty {@code Parameters} instance.
	 * @return an empty {@code Parameters} instance
	 */
	public static Parameters empty() {
		return EmptyVariables.EMPTY_VARIABLES;
	}

	/**
	 * Creates a new {@code Variables} builder for constructing a composite
	 * {@code Variables} instance.
	 * @return a new {@link Builder} instance
	 */
	public static Variables.Builder builder() {
		return new Builder();
	}

	/**
	 * A builder class for creating composite {@link Variables} instances by combining
	 * multiple sources of key-to-value mappings.
	 */
	public final static class Builder {

		@Nullable
		private Map<String, String> properties = null;

		private List<Variables> suppliers = new ArrayList<>();

		private Builder() {
		}

		/**
		 * Adds a key-value mapping to this builder.
		 * @param key the key to add
		 * @param value the value associated with the key
		 * @return this builder instance
		 */
		public Builder add(String key, String value) {
			Map<String, String> _properties = properties;
			if (_properties == null) {
				_properties = properties = new LinkedHashMap<>();
			}
			_properties.put(key, value);
			return this;
		}

		/**
		 * Adds a {@link Variables} supplier to this builder.
		 * @param supplier the variables supplier to add
		 * @return this builder instance
		 */
		public Builder add(@Nullable Variables supplier) {
			if (supplier != null) {
				suppliers.add(supplier);
			}
			return this;
		}

		/**
		 * Adds a map of key-value mappings as a {@link Variables} supplier.
		 * @param m the map of key-value mappings to add
		 * @return this builder instance
		 */
		public Builder add(Map<String, String> m) {
			suppliers.add(m::get);
			return this;
		}

		/**
		 * Adds a set of properties as a {@link Variables} supplier.
		 * @param properties the properties to add
		 * @return this builder instance
		 */
		public Builder add(Properties properties) {
			suppliers.add(Variables.create(properties));
			return this;
		}

		/**
		 * Builds a {@link Variables} instance from the current state of this builder,
		 * combining all added sources into a chain.
		 * @return a new {@link Variables} instance
		 */
		public Variables build() {
			List<Variables> list = new ArrayList<>(suppliers.size() + 1);
			Map<String, String> p = properties;
			if (p != null && !p.isEmpty()) {
				list.add(p::get);
			}
			list.addAll(suppliers);
			if (list.isEmpty()) {
				return empty();
			}
			if (list.size() == 1) {
				return list.get(0);
			}
			return Variables.create(list);
		}

		/**
		 * A {@link Variables} implementation that chains multiple {@code Variables}
		 * sources together, checking each one in sequence for the value of a given key.
		 */
		private static class ChainedVariables implements Variables {

			private Iterable<? extends Variables> variables;

			/**
			 * Constructs a {@code ChainedVariables} instance that checks each
			 * {@code Variables} in sequence for a key's value.
			 * @param variables an iterable of {@code Variables} to chain
			 */
			public ChainedVariables(Iterable<? extends Variables> variables) {
				super();
				this.variables = variables;
			}

			@Override
			public @Nullable String getValue(String name) {
				for (Variables p : variables) {
					String v = p.getValue(name);
					if (v != null)
						return v;
				}
				return null;
			}

			@Override
			public String toString() {
				return "ChainedVariables[" + variables + "]";
			}

		}

	}

	/**
	 * Creates a {@code Variables} instance from the provided properties.
	 * @param properties the properties to use as a variable mapping
	 * @return a new {@code Variables} instance
	 */
	public static Variables create(Properties properties) {
		return properties::getProperty;
	}

	/**
	 * Creates a {@code Variables} instance that chains together multiple
	 * {@link Variables} sources.
	 * @param variables an iterable of {@link Variables} to chain
	 * @return a new {@code Variables} instance
	 */
	public static Variables create(final Iterable<? extends Variables> variables) {
		return new Builder.ChainedVariables(variables);
	}

}

enum EmptyVariables implements Parameters {

	EMPTY_VARIABLES;

	@Override
	public @Nullable String getValue(String key) {
		return null;
	}

	@Override
	public Iterable<String> keys() {
		return List.of();
	}

}

record MapParameters(Map<String, String> map) implements Parameters {
	MapParameters {
		map = new LinkedHashMap<>(map);
	}

	@Override
	public @Nullable String getValue(String key) {
		return map.get(key);
	}

	@Override
	public Iterable<String> keys() {
		return map.keySet();
	}
}
