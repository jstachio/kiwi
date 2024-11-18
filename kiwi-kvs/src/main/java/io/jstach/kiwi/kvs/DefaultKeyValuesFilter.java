package io.jstach.kiwi.kvs;

import java.util.regex.Pattern;

import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesFilter;

enum DefaultKeyValuesFilter implements KeyValuesFilter {

	GREP {

		@Override
		protected KeyValues doFilter(FilterContext context, KeyValues keyValues) {
			String grep = context.parameters().getValue("grep");
			if (grep == null) {
				return keyValues;
			}
			Pattern pattern = Pattern.compile(grep);
			return keyValues.filter(kv -> {
				return pattern.matcher(kv.key()).find();
			});
		}
	};

	@Override
	public KeyValues filter(FilterContext context, KeyValues keyValues, String filter) {
		if (name().equalsIgnoreCase(filter)) {
			return doFilter(context, keyValues);
		}
		return keyValues;
	}

	protected abstract KeyValues doFilter(FilterContext context, KeyValues keyValues);

}
