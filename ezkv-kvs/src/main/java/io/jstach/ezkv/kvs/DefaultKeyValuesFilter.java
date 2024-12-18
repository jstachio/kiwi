package io.jstach.ezkv.kvs;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.regex.Pattern;

import io.jstach.ezkv.kvs.KeyValuesServiceProvider.KeyValuesFilter;

enum DefaultKeyValuesFilter implements KeyValuesFilter {

	GREP(KeyValuesResource.FILTER_GREP) {

		@Override
		protected KeyValues doFilter(FilterContext context, KeyValues keyValues, String expression) {
			String grep = expression;
			// TODO error handling.
			Objects.requireNonNull(keyValues);
			Pattern pattern = Pattern.compile(grep);
			return keyValues.filter(kv -> {
				return pattern.matcher(kv.key()).find();
			});
		}
	},
	SED(KeyValuesResource.FILTER_SED) {

		@Override
		protected KeyValues doFilter(FilterContext context, KeyValues keyValues, String expression) {
			String sed = expression;
			Objects.requireNonNull(sed);
			var command = DefaultSedParser.parse(sed);
			return keyValues.flatMap(kv -> {
				String key = command.execute(kv.key());
				if (key == null) {
					return KeyValues.empty();
				}
				if (kv.key().equals(key)) {
					return KeyValues.of(kv);
				}
				return KeyValues.of(kv.withKey(key));
			});
		}

	},
	JOIN(KeyValuesResource.FILTER_JOIN) {
		@Override
		protected KeyValues doFilter(FilterContext context, KeyValues keyValues, String expression) {
			Objects.requireNonNull(keyValues);
			SequencedMap<String, KeyValue> m = new LinkedHashMap<>();
			for (var kv : keyValues) {
				var found = m.get(kv.key());
				if (found != null) {
					m.put(kv.key(), kv.withExpanded(found.expanded() + expression + kv.expanded()));
				}
				else {
					m.put(kv.key(), kv);
				}
			}
			return KeyValues.copyOf(m.values().stream().toList());
		}
	},

	;

	private final String filter;

	private DefaultKeyValuesFilter(String filter) {
		this.filter = filter;
	}

	@Override
	public Optional<KeyValues> filter(FilterContext context, KeyValues keyValues, Filter filter) {
		String filterName = filter.filter();
		if (this.filter.equalsIgnoreCase(filterName)) {
			return Optional.of(doFilter(context, keyValues, filter.expression()));
		}
		return Optional.empty();
	}

	protected abstract KeyValues doFilter(FilterContext context, KeyValues keyValues, String expression);

}
