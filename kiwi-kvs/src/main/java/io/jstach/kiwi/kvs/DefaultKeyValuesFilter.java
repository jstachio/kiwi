package io.jstach.kiwi.kvs;

import java.util.regex.Pattern;

import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesFilter;

enum DefaultKeyValuesFilter implements KeyValuesFilter {

	GREP {

		@Override
		protected KeyValues doFilter(FilterContext context, KeyValues keyValues) {
			String grep = context.parameters().getValue("grep");
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
	SED {

		@Override
		protected KeyValues doFilter(FilterContext context, KeyValues keyValues) {
			String sed = context.parameters().getValue("sed");
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

	@Override
	public KeyValues filter(FilterContext context, KeyValues keyValues, String filter) {
		if (name().equalsIgnoreCase(filter)) {
			return doFilter(context, keyValues);
		}
		return keyValues;
	}

	protected abstract KeyValues doFilter(FilterContext context, KeyValues keyValues);

}
