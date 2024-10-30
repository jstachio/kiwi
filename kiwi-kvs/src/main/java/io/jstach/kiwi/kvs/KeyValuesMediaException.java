package io.jstach.kiwi.kvs;

import org.jspecify.annotations.Nullable;

public class KeyValuesMediaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public KeyValuesMediaException(
			@Nullable String message,
			@Nullable Throwable cause) {
		super(
				message,
				cause);
	}

	public KeyValuesMediaException(
			@Nullable String message) {
		super(
				message);
	}

}