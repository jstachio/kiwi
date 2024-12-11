package io.jstach.ezkv.kvs;

import org.jspecify.annotations.Nullable;

/**
 * An exception that indicates an error occurred while trying to interpret key values from
 * a resource. This could be because of an interpolation issue or incorrect
 * {@link KeyValuesResource} resource key.
 *
 * @apiNote Because it is largely unclear how an application should recover from various
 * resource key syntax this runtime exception is rather opaque.
 */
public abstract sealed class KeyValuesException extends RuntimeException
		permits KeyValuesMediaException, io.jstach.ezkv.kvs.interpolate.Interpolator.InterpolationException,
		KeyValuesResourceParserException, KeyValuesResourceNameException {

	private static final long serialVersionUID = 6345926490272278304L;

	KeyValuesException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}

	/**
	 * Overridden for specific key values exceptions.
	 * @param message error message
	 */
	protected KeyValuesException(String message) {
		super(message);
	}

	KeyValuesException(Throwable cause) {
		super(cause);
	}

}

final class KeyValuesResourceNameException extends KeyValuesException {

	private static final long serialVersionUID = 3181821413475470665L;

	KeyValuesResourceNameException(String message) {
		super(message);
	}

}
