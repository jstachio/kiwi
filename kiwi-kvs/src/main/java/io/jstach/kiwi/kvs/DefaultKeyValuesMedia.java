package io.jstach.kiwi.kvs;

import static io.jstach.kiwi.kvs.KeyValuesMedia.inputStreamToString;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValuesMedia.Formatter;
import io.jstach.kiwi.kvs.KeyValuesMedia.Parser;
import io.jstach.kiwi.kvs.KeyValuesServiceProvider.MediaFinder;

enum DefaultKeyValuesMedia implements KeyValuesMedia, Parser, Formatter, MediaFinder {

	PROPERTIES("text/x-java-properties", "properties") {
		@Override
		public void parse(InputStream is, BiConsumer<String, String> consumer) throws IOException {
			PropertiesParser.readProperties(new InputStreamReader(is), consumer);
		}

		@Override
		public void format(Appendable appendable, KeyValues kvs) throws IOException {
			var map = kvs.toMap();
			PropertiesParser.writeProperties(map, appendable);
		}
	},

	URLENCODED("application/x-www-form-urlencoded", null) {
		@Override
		public void parse(InputStream input, BiConsumer<String, String> consumer) throws IOException {
			String i = inputStreamToString(input);
			parseUriQuery(i, true, consumer);
		}

		@Override
		public void format(Appendable appendable, KeyValues kvs) throws IOException {
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

	private DefaultKeyValuesMedia(String mediaType, @Nullable String fileExt) {
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
	public Formatter formatter() {
		return this;
	}

	static void parseUriQuery(String query, boolean decode, BiConsumer<String, String> consumer) {
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
		Stream.of(csv.split(",")).map(s -> s.trim()).filter(s -> !s.isBlank()).forEach(consumer);
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
