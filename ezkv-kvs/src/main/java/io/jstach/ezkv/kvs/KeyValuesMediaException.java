package io.jstach.ezkv.kvs;

import org.jspecify.annotations.Nullable;

/**
 * An exception thrown when an error related to media type handling occurs in the context
 * of key-value parsing or formatting.
 *
 * <p>
 * This exception is used when a required media type cannot be found or when an issue
 * arises while processing media types associated with {@link KeyValuesResource}
 * instances.
 *
 * <p>
 * Example scenarios where this exception might be thrown include:
 * <ul>
 * <li>Failing to find a parser for a specified media type</li>
 * <li>Issues related to media type resolution or handling</li>
 * </ul>
 *
 * <p>
 * This class extends {@link RuntimeException}, so it does not need to be declared in a
 * method's {@code throws} clause.
 */
public final class KeyValuesMediaException extends KeyValuesException {

	private static final long serialVersionUID = 1L;
	final @Nullable String mediaType;

	/**
	 * Constructs a new {@code KeyValuesMediaException} with the specified detail message
	 * and cause.
	 * @param message the detail message, may be {@code null}
	 * @param cause the cause of the exception, may be {@code null}
	 */
	public KeyValuesMediaException(@Nullable String message, @Nullable Throwable cause, @Nullable String mediaType) {
		super(message, cause);
		this.mediaType = mediaType;
	}

	/**
	 * Constructs a new {@code KeyValuesMediaException} with the specified detail message.
	 * @param message the detail message, may be {@code null}
	 */
	public KeyValuesMediaException(String message) {
		super(message);
		this.mediaType = null;
	}

}
