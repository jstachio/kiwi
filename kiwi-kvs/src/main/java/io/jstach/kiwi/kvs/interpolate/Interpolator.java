package io.jstach.kiwi.kvs.interpolate;

import static java.util.Objects.requireNonNull;

import java.util.NoSuchElementException;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.interpolate.StrSubstitutor.StrLookup;

public interface Interpolator {

	public static Interpolator create(@SuppressWarnings("exports") Function<String, @Nullable String> f) {
		return new InternalInterpolator(f);
	}

	public String interpolate(String key, String raw) throws InterpolationException;

	public static class InterpolationException extends NoSuchElementException {

		private static final long serialVersionUID = 4135719008465817465L;

		private final String key;

		private final String raw;

		public InterpolationException(String message, String key, String raw) {
			super(message);
			this.key = key;
			this.raw = raw;
		}

		public String getKey() {
			return key;
		}

		public String getRaw() {
			return raw;
		}

	}

	public static class MissingVariableInterpolationException extends InterpolationException {

		private static final long serialVersionUID = 4135719008465817465L;

		private final String variable;

		public MissingVariableInterpolationException(String message, String key, String raw, String variable) {
			super(message, key, raw);
			this.variable = variable;
		}

		public String getVariable() {
			return variable;
		}

	}

}

class InternalInterpolator implements Interpolator {

	// private final StrSubstitutor strSubstitutor;
	private final Function<String, @Nullable String> lookup;

	InternalInterpolator(Function<String, @Nullable String> lookup) {
		this.lookup = lookup;
	}

	@Override
	public String interpolate(String key, String original) {

		if (original.indexOf('$') < 0) {
			return original;
		}

		StrSubstitutor st = new StrSubstitutor(new StrLookup() {
			@Override
			public @Nullable String lookup(@Nullable String variable, boolean defaultValue) {
				requireNonNull(variable, "variable was null");
				String v = lookup.apply(variable);
				if (v == null) {
					if (key.equals(variable) || defaultValue) {
						return null;
					}
					throw new MissingVariableInterpolationException("Variable is missing for key. key: '" + key + "'"
							+ ", variable: '" + variable + "'" + ", raw: '" + original + "'", key, original, variable);
				}
				return v;
			}
		});
		// st.setEnableSubstitutionInVariables(true);

		try {
			String result = st.replace(original);
			return requireNonNull(result);
		}
		catch (IllegalStateException e) {
			throw new InterpolationException("Infinite recursion for key. key: '" + key + "', reason: '"
					+ e.getMessage() + "'" + ", raw: '" + original + "'", key, original);
		}

	}

}
