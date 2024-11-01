package io.jstach.kiwi.kvs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.Variables.StaticVariables;
import io.jstach.kiwi.kvs.interpolate.Interpolator;

public interface Variables {

	
	public @Nullable String getValue(String key);

	public sealed interface StaticVariables extends Variables {

	}

	public static StaticVariables empty() {
		return EmptyVariables.EMPTY_VARIABLES;
	}

	default String requireElse(String key, String fallback) {
		String r = getValue(key);
		if (r == null) {
			return Objects.requireNonNull(fallback);
		}
		return r;
	}

	default Optional<Entry<String, String>> findEntry(String... name) {
		for (String k : name) {
			if (k == null)
				continue;
			String prop = getValue(k);
			if (prop != null)
				return Optional.of(Map.entry(k, prop));
		}
		return Optional.empty();
	}

	default boolean matches(Entry<String, String> entry) {
		String v = entry.getValue();
		return v.equals(getValue(entry.getKey()));
	}

	public static Variables.Builder builder() {
		return new Builder();
	}

	public static class Builder {

		@Nullable
		private Map<String, String> properties = null;

		private List<Variables> suppliers = new ArrayList<>();

		public Builder add(String key, String value) {
			Map<String, String> _properties = properties;
			if (_properties == null) {
				_properties = properties = new LinkedHashMap<>();
			}
			_properties.put(key, value);
			return this;
		}

		public Builder add(@Nullable Variables supplier) {
			if (supplier != null) {
				suppliers.add(supplier);
			}
			return this;
		}

		public Builder add(Map<String, String> m) {
			suppliers.add(m::get);
			return this;
		}

		public Builder add(Properties properties) {
			suppliers.add(Variables.create(properties));
			return this;
		}

		public Variables build() {
			List<Variables> list = new ArrayList<>(suppliers.size() + 1);
			Map<String, String> p = properties;
			if (p  != null && !p.isEmpty()) {
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

		private static class ChainedVariables implements Variables {

			private Iterable<? extends Variables> variables;

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

	default Interpolator toInterpolator() {
		return Interpolator.create(this::getValue);
	}

	public static Variables create(Properties properties) {
		return properties::getProperty;
	}

	public static Variables create(final Iterable<? extends Variables> variables) {
		return new Builder.ChainedVariables(variables);
	}

}

enum EmptyVariables implements StaticVariables {

	EMPTY_VARIABLES;

	@Override
	public @Nullable String getValue(String key) {
		return null;
	}

}

record MapStaticVariables(Map<String, String> map) implements StaticVariables {
	MapStaticVariables {
		map = new LinkedHashMap<>(map);
	}

	@Override
	public @Nullable String getValue(String key) {
		return map.get(key);
	}
}
