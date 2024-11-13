package io.jstach.kiwi.kvs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesMediaFinder;

/**
 * Represents a media type for parsing and formatting key-value pairs. A
 * {@code KeyValuesMedia} provides parsing and formatting capabilities for specific media
 * types and can be searched based on file extensions or media types.
 *
 * <p>
 * Out-of-the-box implementations include properties and URL-encoded formats.
 *
 * <p>
 * Example usage for parsing an {@code InputStream}:
 * {@snippet :
 * KeyValuesMedia propertiesMedia = KeyValuesMedia.ofProperties();
 * KeyValues parsedKeyValues = propertiesMedia.parser().parse(resource, inputStream);
 * }
 *
 * @see KeyValues
 * @see Parser
 * @see Formatter
 */
public interface KeyValuesMedia extends KeyValuesMediaFinder {

	/**
	 * Returns the media type as a string.
	 * @return the media type
	 */
	public String getMediaType();

	/**
	 * Returns the file extension associated with the media type, if any.
	 * @return the file extension or {@code null} if not applicable
	 */
	public @Nullable String getFileExt();

	/**
	 * Returns a {@link Parser} for parsing key-value pairs from an input stream.
	 * @return the parser
	 */
	public Parser parser();

	/**
	 * Returns a {@link Formatter} for formatting key-value pairs to an appendable target.
	 * By default, this method throws {@link UnsupportedOperationException} if formatting
	 * is not supported.
	 * @return the formatter
	 * @throws UnsupportedOperationException if formatting is not supported
	 */
	default Formatter formatter() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns a {@code KeyValuesMedia} instance for the properties format.
	 * @return a properties format media type
	 */
	public static KeyValuesMedia ofProperties() {
		return DefaultKeyValuesMedia.PROPERTIES;
	}

	/**
	 * Returns a {@code KeyValuesMedia} instance for the URL-encoded format.
	 * @return a URL-encoded format media type
	 */
	public static KeyValuesMedia ofUrlEncoded() {
		return DefaultKeyValuesMedia.URLENCODED;
	}

	@Override
	default Optional<KeyValuesMedia> findByExt(String ext) {
		if (ext.equalsIgnoreCase(getFileExt())) {
			return Optional.of(this);
		}
		return Optional.empty();
	}

	@Override
	default Optional<KeyValuesMedia> findByMediaType(String mediaType) {
		if (getMediaType().equalsIgnoreCase(mediaType)) {
			return Optional.of(this);
		}
		return Optional.empty();
	}

	@Override
	default Optional<KeyValuesMedia> findByUri(URI uri) {
		if (hasFileExt(uri)) {
			return Optional.of(this);
		}
		return Optional.empty();
	}

	/**
	 * Reads an {@link InputStream} and converts it to a UTF-8 encoded string.
	 * @param inputStream the input stream to read
	 * @return the string representation of the input stream
	 * @throws IOException if an I/O error occurs
	 */
	public static String inputStreamToString(InputStream inputStream) throws IOException {
		try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[1024];
			int length;
			while ((length = inputStream.read(buffer)) != -1) {
				result.write(buffer, 0, length);
			}
			return result.toString(StandardCharsets.UTF_8.name());
		}

	}
	//
	// public static InputStream stringToInputStream(String s) {
	// return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
	// }

	/**
	 * A parser for reading key-value pairs from an input source.
	 */
	public interface Parser {

		/**
		 * Parses key-value pairs from an input stream and applies them to a consumer.
		 * @param input the input stream to read from
		 * @param consumer the consumer to apply the parsed key-value pairs
		 * @throws IOException if an I/O error occurs during parsing
		 */
		void parse(InputStream input, BiConsumer<String, String> consumer) throws IOException;

		/**
		 * Parses key-value pairs from an input stream and associates them with a given
		 * resource.
		 * @param source the resource from which the key-values are sourced
		 * @param is the input stream to read from
		 * @return a {@code KeyValues} instance containing the parsed key-value pairs
		 * @throws IOException if an I/O error occurs during parsing
		 */
		default KeyValues parse(KeyValuesResource source, InputStream is) throws IOException {
			var b = KeyValues.builder(source);
			parse(is, b::add);
			return b.build();
		}

		// default void parse(String input, BiConsumer<String, String> consumer) {
		// try {
		// parse(stringToInputStream(input), consumer);
		// }
		// catch (IOException e) {
		// throw new UncheckedIOException(e);
		// }
		// }
		//
		// default KeyValues parse(String input) {
		// var b = KeyValues.builder();
		// parse(input, b::add);
		// return b.build();
		// }

	}

	/**
	 * A formatter for writing key-value pairs to an output.
	 */
	public interface Formatter {

		/**
		 * Formats key-value pairs into the provided {@link Appendable}.
		 * @param appendable the appendable to write to
		 * @param kvs the key-value pairs to format
		 * @throws IOException if an I/O error occurs during formatting
		 */
		public void format(Appendable appendable, KeyValues kvs) throws IOException;

		/**
		 * Formats key-value pairs into a {@link StringBuilder}.
		 * @param stringBuilder the string builder to write to
		 * @param kvs the key-value pairs to format
		 */
		default void format(StringBuilder stringBuilder, KeyValues kvs) {
			try {
				format((Appendable) stringBuilder, kvs);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		/**
		 * Formats key-value pairs and returns them as a string.
		 * @param kvs the key-value pairs to format
		 * @return the formatted key-value pairs as a string
		 */
		default String format(KeyValues kvs) {
			StringBuilder sb = new StringBuilder();
			format(sb, kvs);
			return sb.toString();
		}

	}

	/**
	 * Checks if a given filename has the file extension associated with this media type.
	 * @param filename the filename to check
	 * @return {@code true} if the filename ends with the media type's file extension,
	 * otherwise {@code false}
	 */
	default boolean hasFileExt(String filename) {
		String ext = getFileExt();
		if (ext == null) {
			return false;
		}
		return filename.endsWith("." + ext);
	}

	/**
	 * Checks if the URI's path has the file extension associated with this media type.
	 * @param uri the URI to check
	 * @return {@code true} if the URI's path ends with the media type's file extension,
	 * otherwise {@code false}
	 */
	default boolean hasFileExt(URI uri) {
		String p = uri.getPath();
		if (p == null)
			return false;
		return hasFileExt(p);
	}

	// default void prettyPrint(StringBuilder sb) {
	// sb.append(toString()).append("(").append(getMediaType()).append(", ");
	// var fe = getFileExt();
	// if (fe != null) {
	// sb.append(fe);
	// }
	// sb.append(")");
	// }

}
