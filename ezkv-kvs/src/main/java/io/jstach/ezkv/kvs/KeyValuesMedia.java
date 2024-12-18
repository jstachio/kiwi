package io.jstach.ezkv.kvs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;

import io.jstach.ezkv.kvs.KeyValuesMedia.Parser;
import io.jstach.ezkv.kvs.KeyValuesServiceProvider.KeyValuesMediaFinder;

/**
 * Represents a media type for parsing and formatting key-value pairs. A
 * {@code KeyValuesMedia} provides parsing and formatting capabilities for specific media
 * types and can be searched based on file extensions or media types.
 *
 * <p>
 * Out-of-the-box implementations include properties and URL-encoded formats. <strong>The
 * properties and URL encoded formats do maintain order of the key values parsed and do
 * allow duplicate keys (unlike regular java.util {@link Properties})! </strong>
 * <p>
 * Example usage for parsing an {@code InputStream}:
 * {@snippet :
 * KeyValuesMedia propertiesMedia = KeyValuesMedia.ofProperties();
 * KeyValues parsedKeyValues = propertiesMedia.parser().parse(resource, inputStream);
 * }
 * For a formal specification of media types see:
 * <a href="https://datatracker.ietf.org/doc/html/rfc6838"> Media Type Specifications and
 * Registration Procedures RFC 6838 </a>
 * <p>
 * All KeyValuesMedia implement {@link KeyValuesMediaFinder} and thus each media can be
 * queried if it is a match of file extension of media type. When a
 * {@link KeyValuesSystem} is fully loaded it will have a list of all found
 * {@link KeyValuesMedia} and create a composite {@link KeyValuesMediaFinder}.
 *
 * @see KeyValues
 * @see Parser
 * @see Formatter
 * @see #ofProperties()
 * @see #ofUrlEncoded()
 * @see KeyValuesSystem#mediaFinder()
 * @see KeyValuesResource#mediaType()
 * @see KeyValuesResource#KEY_MEDIA_TYPE
 */
public interface KeyValuesMedia extends KeyValuesMediaFinder {

	/**
	 * Media Type for {@link Properties}.
	 */
	public static String MEDIA_TYPE_PROPERTIES = "text/x-java-properties";

	/**
	 * Media Type for URL encoded pairs which is almost entirely compatible with URI
	 * percent encoding.
	 */
	public static String MEDIA_TYPE_URLENCODED = "application/x-www-form-urlencoded";

	/**
	 * File extension for {@link Properties} files.
	 */
	public static String FILE_EXT_PROPERTIES = "properties";

	/**
	 * Returns the media type as a string and should ideally be in
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6838">rfc6838 format</a>, is
	 * required and should be unique from other {@link KeyValuesMedia} unlike
	 * {@link #getFileExt()}. For implementers if there is not a registered media type a
	 * recommendation is to use <code>application/x-ext</code> where "ext" would be
	 * replaced with the file extension or some name.
	 * @return the media type
	 */
	public String getMediaType();

	/**
	 * Returns the file extension associated with the media type, if any. The first period
	 * "<code>.</code>" should not be included. For example <code>.tar.gz</code> should be
	 * <code>tar.gz</code>.
	 * @return the file extension or {@code null} if not applicable
	 */
	public @Nullable String getFileExt();

	/**
	 * Returns a {@link Parser} for parsing key-value pairs from an input stream.
	 * @return the parser
	 */
	public Parser parser();

	/**
	 * Returns a parser that maybe customized by parameters. By default {@link #parser()}
	 * is called.
	 * @param parameters parameters that can come from
	 * {@link KeyValuesResource#parameters()}.
	 * @return parser
	 */
	default Parser parser(Variables parameters) {
		return parser();
	}

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
	 * Returns a {@code KeyValuesMedia} instance for the {@link Properties} format however
	 * unlike normal {@link Properties} order of the properties key values is maintained
	 * based on the order parsed and <strong>duplicate key names are allowed</strong>.
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
	 * @param inputStream the input stream to read which will not be closed by this call.
	 * @return the string representation of the input stream
	 * @throws IOException if an I/O error occurs
	 */
	public static String inputStreamToString(InputStream inputStream) throws IOException {
		try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
			inputStream.transferTo(result);
			return result.toString(StandardCharsets.UTF_8);
		}

	}

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

		/**
		 * Parses key-value pairs from a string and applies them to a consumer.
		 * @param input the string to parse
		 * @param consumer the consumer to apply the parsed key-value pairs
		 */
		default void parse(String input, BiConsumer<String, String> consumer) {
			try {
				parse(stringToInputStream(input), consumer);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		/**
		 * Parses key-value pairs from a string and returns a {@code KeyValues} instance.
		 * @param input the string to parse
		 * @return a {@code KeyValues} instance containing the parsed key-value pairs
		 */
		default KeyValues parse(String input) {
			var b = KeyValues.builder();
			parse(input, b::add);
			return b.build();
		}

		private static InputStream stringToInputStream(String s) {
			return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
		}

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
class SafeParser implements KeyValuesMedia.Parser {

	private final KeyValuesMedia.Parser delegate;
	private final String mediaType;

	@Override
	public void parse(
			InputStream input,
			BiConsumer<String, String> consumer)
			throws IOException {
		try {
			delegate.parse(input, consumer);
		} 
		catch (KeyValuesException e) {
			throw e;
		}
		catch (RuntimeException e) {
			String message = "Parsing failed. mediaType=" + mediaType + e.toString();
			throw new KeyValuesMediaException(message, e, mediaType);
		}
		
	}

	@Override
	public KeyValues parse(
			KeyValuesResource source,
			InputStream is)
			throws IOException {
		try {
			return delegate.parse(source, is);
		}
		catch (KeyValuesException e) {
			throw e;
		}
		catch (RuntimeException e) {
			String message = "Parsing failed for mediaType='" + mediaType + "'. " + e.getMessage();
			throw new KeyValuesMediaException(message, e, mediaType);
		}
	}

	@Override
	public void parse(
			String input,
			BiConsumer<String, String> consumer) {
		try {
			delegate.parse(input, consumer);
		}
		catch (KeyValuesException e) {
			throw e;
		}
		catch (RuntimeException e) {
			String message = "Parsing failed. mediaType=" + mediaType + e.toString();
			throw new KeyValuesMediaException(message, e, mediaType);
		}
	}

	@Override
	public KeyValues parse(
			String input) {
		return delegate.parse(input);
	}

	public SafeParser(
			Parser delegate, String mediaType) {
		super();
		this.delegate = delegate;
		this.mediaType = mediaType;
	}
	
}
