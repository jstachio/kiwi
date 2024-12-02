package io.jstach.kiwi.kvs.interpolate;

import static java.util.Objects.requireNonNull;

import java.util.NoSuchElementException;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.interpolate.StrSubstitutor.StrLookup;

/**
 * Provides functionality for interpolating values within a string using keys and raw
 * data.
 * <p>
 * An {@code Interpolator} replaces placeholders in a raw string with values derived from
 * a mapping function. It supports custom logic for determining how placeholders are
 * resolved and handles errors when required variables are missing.
 * </p>
 */
public interface Interpolator {

	/**
	 * Creates a new {@code Interpolator} instance using the provided mapping function.
	 * @param f a function that maps keys to their corresponding values, or {@code null}
	 * if the key does not have a mapping
	 * @return a new {@code Interpolator} instance
	 */
	static Interpolator create(Function<String, @Nullable String> f) {
		return new InternalInterpolator(f);
	}

	/**
	 * Interpolates placeholders in the given raw string using the associated key.
	 * @param key the key for which interpolation is being performed
	 * @param raw the raw string containing placeholders to interpolate
	 * @return the interpolated string
	 * @throws InterpolationException if interpolation fails, such as when a required
	 * placeholder cannot be resolved
	 */
	String interpolate(String key, String raw) throws InterpolationException;

	/**
	 * Exception thrown when an interpolation operation fails.
	 */
	static class InterpolationException extends NoSuchElementException {

		private static final long serialVersionUID = 4135719008465817465L;

		private final String key;

		private final String raw;

		/**
		 * Constructs a new {@code InterpolationException}.
		 * @param message the error message
		 * @param key the key involved in the interpolation failure
		 * @param raw the raw string being interpolated
		 */
		public InterpolationException(String message, String key, String raw) {
			super(message);
			this.key = key;
			this.raw = raw;
		}

		/**
		 * Returns the key involved in the interpolation failure.
		 * @return the key
		 */
		public String getKey() {
			return key;
		}

		/**
		 * Returns the raw string that was being interpolated.
		 * @return the raw string
		 */
		public String getRaw() {
			return raw;
		}

	}

	/**
	 * Exception thrown when a required variable is missing during interpolation.
	 */
	static class MissingVariableInterpolationException extends InterpolationException {

		private static final long serialVersionUID = 4135719008465817465L;

		private final String variable;

		/**
		 * Constructs a new {@code MissingVariableInterpolationException}.
		 * @param message the error message
		 * @param key the key involved in the interpolation failure
		 * @param raw the raw string being interpolated
		 * @param variable the missing variable name
		 */
		public MissingVariableInterpolationException(String message, String key, String raw, String variable) {
			super(message, key, raw);
			this.variable = variable;
		}

		/**
		 * Returns the name of the missing variable.
		 * @return the missing variable name
		 */
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
				if (variable == null) {
					throw new NullPointerException("variable was null");
				}
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
