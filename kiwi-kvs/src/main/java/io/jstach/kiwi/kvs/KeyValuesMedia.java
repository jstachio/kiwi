package io.jstach.kiwi.kvs;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValuesServiceProvider.MediaFinder;

public interface KeyValuesMedia extends MediaFinder {


	public String getMediaType();
	public @Nullable String getFileExt();
	public Parser parser();
	default Formatter formatter() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	default Optional<KeyValuesMedia> findMedia(
			String mediaType) {
		var mt = getMediaType();
		if (mt.equals(mediaType)) {
			return Optional.of(this);
		}
		if (hasFileExt(mediaType)) {
			return Optional.of(this);
		}
		return Optional.empty();
	}
	
	public static String inputStreamToString(
			InputStream inputStream)
			throws IOException {
		try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[1024];
			int length;
			while ((length = inputStream.read(buffer)) != -1) {
				result.write(buffer, 0, length);
			}
			return result.toString(StandardCharsets.UTF_8.name());
		}

	}
	public static InputStream stringToInputStream(
			String s) {
		return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
	}
	
	public interface Parser {

		void parse(InputStream input, BiConsumer<String,String> consumer) throws IOException;

		default KeyValues parse(
				KeyValuesResource source,
				InputStream is)
				throws IOException {
			var b = KeyValues.Builder.of(source.uri());
			parse(is, b::add);
			return b.build();
		}
		
		default void parse(String input, BiConsumer<String,String> consumer)  {
			try {
				parse(stringToInputStream(input), consumer);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		
		default KeyValues parse(String input) {
			var b = KeyValues.Builder.of(URI.create("null://"));
			parse(input, b::add);
			return b.build();
		}

	}
	
	public interface Formatter {

		public void format(
				Appendable appendable,
				KeyValues kvs)
				throws IOException;

		default void format(
				StringBuilder stringBuilder,
				KeyValues kvs) {
			try {
				format((Appendable) stringBuilder, kvs);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		default String format(
				KeyValues kvs) {
			StringBuilder sb = new StringBuilder();
			format(sb, kvs);
			return sb.toString();
		}

	}
	
	public class MediaTypeException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public MediaTypeException(
				@Nullable String message,
				@Nullable Throwable cause) {
			super(
					message,
					cause);
		}

		public MediaTypeException(
				@Nullable String message) {
			super(
					message);
		}

	}

	default boolean hasFileExt(
			String filename) {
		String ext = getFileExt();
		if (ext == null) {
			return false;
		}
		return filename.endsWith("." + ext);
	}

	default boolean hasFileExt(
			URI uri) {
		String p = uri.getPath();
		if (p == null)
			return false;
		return hasFileExt(p);
	}

	default boolean matchesURI(
			URI resource) {
		return hasFileExt(resource);
	}

	default boolean matches(
			String mediaTypeLikeString) {
		return mediaTypeLikeString.equals(getMediaType()) || mediaTypeLikeString.equals(getFileExt());
	}

	default boolean matches(
			KeyValuesMedia keyValuesMedia) {
		return this == keyValuesMedia
				|| this.equals(keyValuesMedia)
				|| this.getMediaType()
					.equals(keyValuesMedia.getMediaType());
	}

	default void prettyPrint(
			StringBuilder sb) {
		sb.append(toString())
			.append("(")
			.append(getMediaType())
			.append(", ");
		var fe = getFileExt();
		if (fe != null) {
			sb.append(fe);
		}
		sb.append(")");
	}
	

	public enum BuiltinMediaType implements KeyValuesMedia, Parser, Formatter, MediaFinder {
		PROPERTIES("text/x-java-properties", "properties"){			
			@Override
			public void parse(
					InputStream is,
					BiConsumer<String, String> consumer)
					throws IOException {
				PropertiesParser.readProperties(new InputStreamReader(is), consumer);
			}
			
			@Override
			public void format(
					Appendable appendable,
					KeyValues kvs)
					throws IOException {
				var map = kvs.toMap();
				PropertiesParser.writeProperties(map, appendable);
			}
		},

		URLENCODED("application/x-www-form-urlencoded", null) {
			@Override
			public void parse(
					InputStream input,
					BiConsumer<String, String> consumer)
					throws IOException {
				String i = inputStreamToString(input);
				parseUriQuery(i, true, consumer);
			}

			@Override
			public void format(
					Appendable appendable,
					KeyValues kvs)
					throws IOException {
				boolean first = true;
				for (var kv : kvs) {
					if (first) {
						first = false;
					}
					else {
						appendable.append('&');
					}
					String key = URLEncoder.encode(kv.key(), StandardCharsets.UTF_8);
					String value = URLEncoder.encode(kv.expanded(), StandardCharsets.UTF_8);
					appendable.append(key).append('=').append(value);
				}
				
			}
		}
		
		;

		private final String mediaType;
		private final @Nullable String fileExt;

		private BuiltinMediaType(
				String mediaType,
				@Nullable String fileExt) {
			this.mediaType = mediaType;
			this.fileExt = fileExt;
		}

		@Override
		public String getMediaType() {
			return mediaType;
		}
		@Override
		public @Nullable String getFileExt() {
			return fileExt;
		}
		
		@Override
		public Parser parser() {
			return this;
		}
		@Override
		public @Nullable Formatter formatter() {
			return this;
		}
		
		static void parseUriQuery(String query, boolean decode, BiConsumer<String, String> consumer) {
			if (query == null) {
				return;
			}

			String[] pairs = query.split("&");
			for (String pair : pairs) {
				int idx = pair.indexOf("=");
				String key;
				String value;
				if (idx == 0) {
					continue;
				}
				else if (idx < 0) {
					key = pair;
					value = "";
				}
				else {
					key = pair.substring(0, idx);
					value = pair.substring(idx + 1);
				}
				if (decode) {
					key = URLDecoder.decode(key, StandardCharsets.UTF_8);
					value = URLDecoder.decode(value, StandardCharsets.UTF_8);

				}
				if (key.isBlank()) {
					continue;
				}
				consumer.accept(key, value);
			}
		}
		
		static void parseCSV(String csv, Consumer<String> consumer) {
			Stream.of(csv.split(","))
				.map(s -> s.trim())
				.filter(s -> ! s.isBlank())
				.forEach(consumer);
		}

	}

	public static String fileFromPath(
			@Nullable String p) {
		if (p == null || p.endsWith("/")) {
			return "";
		}
		return p.substring(p.lastIndexOf("/") + 1)
			.trim();
	}

	public static String removeFileExt(
			String path,
			@Nullable String fileExtension) {
		if (fileExtension == null || fileExtension.isBlank())
			return path;
		String rawExt = "." + fileExtension;
		if (path.endsWith(rawExt)) {
			return path.substring(0, rawExt.length());
		}
		return path;
	}
	


}
/**
 * Writes and parses properties.
 */
final class PropertiesParser {

	private PropertiesParser() {
	}

	/**
	 * Writes properties.
	 * @param map properties map.
	 * @return properties as a string.
	 */
	public static String writeProperties(Map<String, String> map) {
		StringBuilder sb = new StringBuilder();
		try {
			writeProperties(map, sb);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return sb.toString();

	}

	/**
	 * Read properties.
	 * @param input from.
	 * @return properties as a map.
	 */
	public static Map<String, String> readProperties(String input) {
		Map<String, String> m = new LinkedHashMap<>();
		StringReader sr = new StringReader(input);
		try {
			readProperties(sr, m::put);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return m;
	}

	/**
	 * Read properties.
	 * @param reader from
	 * @param consumer to
	 * @throws IOException on read failure
	 */
	static void readProperties(Reader reader, BiConsumer<String, String> consumer) throws IOException {
		Properties bp = prepareProperties(consumer);
		bp.load(reader);

	}

	@SuppressWarnings({ "serial" })
	static void writeProperties(Map<String, String> map, Appendable sb) throws IOException {
		StringWriter sw = new StringWriter();
		new Properties() {
			@Override
			@SuppressWarnings({ "unchecked", "rawtypes", "UnsynchronizedOverridesSynchronized", "null" })
			public java.util.Enumeration keys() {
				return Collections.enumeration(map.keySet());
			}

			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public java.util.Set entrySet() {
				return map.entrySet();
			}

			@SuppressWarnings({ "nullness", "UnsynchronizedOverridesSynchronized" }) // checker
																						// bug
			@Override
			public @Nullable Object get(Object key) {
				return map.get(key);
			}
		}.store(sw, null);
		LineNumberReader lr = new LineNumberReader(new StringReader(sw.toString()));

		String line;
		while ((line = lr.readLine()) != null) {
			if (!line.startsWith("#")) {
				sb.append(line).append(System.lineSeparator());
			}
		}
	}

	private static Properties prepareProperties(BiConsumer<String, String> consumer) throws IOException {

		// Hack to use properties class to load but our map for preserved order
		@SuppressWarnings({ "serial", "nullness" })
		Properties bp = new Properties() {
			@Override
			@SuppressWarnings({ "nullness", "keyfor", "UnsynchronizedOverridesSynchronized" }) // checker
																								// bug
			public @Nullable Object put(Object key, Object value) {
				Objects.requireNonNull(key);
				Objects.requireNonNull(value);
				consumer.accept((String) key, (String) value);
				return null;
			}
		};
		return bp;
	}

}

//final class PercentCodec {
//
//	static final BitSet GEN_DELIMS = new BitSet(256);
//	static final BitSet SUB_DELIMS = new BitSet(256);
//	static final BitSet UNRESERVED = new BitSet(256);
//	static final BitSet URIC = new BitSet(256);
//
//	static {
//		GEN_DELIMS.set(':');
//		GEN_DELIMS.set('/');
//		GEN_DELIMS.set('?');
//		GEN_DELIMS.set('#');
//		GEN_DELIMS.set('[');
//		GEN_DELIMS.set(']');
//		GEN_DELIMS.set('@');
//
//		SUB_DELIMS.set('!');
//		SUB_DELIMS.set('$');
//		SUB_DELIMS.set('&');
//		SUB_DELIMS.set('\'');
//		SUB_DELIMS.set('(');
//		SUB_DELIMS.set(')');
//		SUB_DELIMS.set('*');
//		SUB_DELIMS.set('+');
//		SUB_DELIMS.set(',');
//		SUB_DELIMS.set(';');
//		SUB_DELIMS.set('=');
//
//		for (int i = 'a'; i <= 'z'; i++) {
//			UNRESERVED.set(i);
//		}
//		for (int i = 'A'; i <= 'Z'; i++) {
//			UNRESERVED.set(i);
//		}
//		// numeric characters
//		for (int i = '0'; i <= '9'; i++) {
//			UNRESERVED.set(i);
//		}
//		UNRESERVED.set('-');
//		UNRESERVED.set('.');
//		UNRESERVED.set('_');
//		UNRESERVED.set('~');
//		URIC.or(SUB_DELIMS);
//		URIC.or(UNRESERVED);
//	}
//
//	private static final int RADIX = 16;
//
//	static void encode(final StringBuilder buf, final CharSequence content, Charset charset, final BitSet safechars) {
//		final CharBuffer cb = CharBuffer.wrap(content);
//		final ByteBuffer bb = charset.encode(cb);
//		while (bb.hasRemaining()) {
//			final int b = bb.get() & 0xff;
//			if (safechars.get(b)) {
//				buf.append((char) b);
//			}
//			else {
//				buf.append("%");
//				final char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, RADIX));
//				final char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, RADIX));
//				buf.append(hex1);
//				buf.append(hex2);
//			}
//		}
//	}
//
//	public static void encode(final StringBuilder buf, final CharSequence content, final Charset charset) {
//		encode(buf, content, charset, UNRESERVED);
//	}
//
//	public static String encode(final CharSequence content, final Charset charset) {
//		final StringBuilder buf = new StringBuilder();
//		encode(buf, content, charset);
//		return buf.toString();
//	}
//
//	public static String decode(final CharSequence content, Charset charset) {
//		final ByteBuffer bb = ByteBuffer.allocate(content.length());
//		final CharBuffer cb = CharBuffer.wrap(content);
//		while (cb.hasRemaining()) {
//			final char c = cb.get();
//			if (c == '%' && cb.remaining() >= 2) {
//				final char uc = cb.get();
//				final char lc = cb.get();
//				final int u = Character.digit(uc, RADIX);
//				final int l = Character.digit(lc, RADIX);
//				if (u != -1 && l != -1) {
//					bb.put((byte) ((u << 4) + l));
//				}
//				else {
//					bb.put((byte) '%');
//					bb.put((byte) uc);
//					bb.put((byte) lc);
//				}
//			}
//			else {
//				bb.put((byte) c);
//			}
//		}
//		bb.flip();
//		return charset.decode(bb).toString();
//	}
//
//}