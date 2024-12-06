package io.jstach.ezkv.kvs;

import java.util.regex.Pattern;

import io.jstach.ezkv.kvs.KeyValuesServiceProvider.KeyValuesFilter;

enum DefaultKeyValuesFilter implements KeyValuesFilter {

	GREP(KeyValuesResource.FILTER_GREP) {

		@Override
		protected KeyValues doFilter(FilterContext context, KeyValues keyValues, String expression) {
			String grep = expression;
			// TODO error handling.
			if (grep == null) {
				return keyValues;
			}
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
			// TODO error handling.
			if (sed == null) {
				return keyValues;
			}
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

	};

	private final String filter;

	private DefaultKeyValuesFilter(String filter) {
		this.filter = filter;
	}

	@Override
	public KeyValues filter(FilterContext context, KeyValues keyValues, Filter filter) {
		String filterName = filter.filter();
		if (this.filter.equalsIgnoreCase(filterName)) {
			return doFilter(context, keyValues, filter.expression());
		}
		return keyValues;
	}

	protected abstract KeyValues doFilter(FilterContext context, KeyValues keyValues, String expression);

}
