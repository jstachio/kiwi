package io.jstach.ezkv.json5.internal;

/**
 * An exception used by the JSON5 for Java Library if something went wrong
 *
 * @author SyntaxError404
 * @version 1.0.0
 */
@SuppressWarnings("serial")
public class JSONException extends RuntimeException {

	/**
	 * Constructs a new JSONException
	 */
	public JSONException() {
		super();
	}

	/**
	 * Constructs a new JSONException with a detail message
	 * @param message the detail message
	 */
	public JSONException(String message) {
		super(message);
	}

	/**
	 * Constructs a new JSONException with a causing exception
	 * @param cause the causing exception
	 */
	public JSONException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new JSONException with a detail message and a causing exception
	 * @param message the detail message
	 * @param cause the causing exception
	 */
	public JSONException(String message, Throwable cause) {
		super(message, cause);
	}

}
