package io.jstach.ezkv.json5;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;

import io.jstach.ezkv.json5.internal.JSONArray;
import io.jstach.ezkv.json5.internal.JSONObject;
import io.jstach.ezkv.json5.internal.JSONOptions;
import io.jstach.ezkv.json5.internal.JSONOptions.DuplicateBehavior;
import io.jstach.ezkv.json5.internal.JSONParser;
import io.jstach.ezkv.kvs.KeyValuesMedia;
import io.jstach.ezkv.kvs.KeyValuesResource;
import io.jstach.ezkv.kvs.Variables;

/**
 * Parses <a href="https://json5.org/">JSON5</a> to KeyValues. The {@link #parser()} can
 * also be used for strict JSON as JSON5 is a true superset.
 * <p>
 * The fields in this class are important and describe parameters that can be set on the
 * {@link KeyValuesResource}.
 * </p>
 *
 * This module does have a service loader registration. If you do not wish to use the
 * Service Loader you can add an instance of this class to
 * {@link io.jstach.ezkv.kvs.KeyValuesSystem.Builder}.
 *
 * @apiNote This is not included with the core ezkv module because JSON5 is still evolving
 * and likely to keep changing.
 * @see #ARRAY_KEY_PARAM
 * @see #SEPARATOR_PARAM
 *
 */
public final class JSON5KeyValuesMedia implements KeyValuesMedia {

	/**
	 * The media type is {@value #MEDIA_TYPE} and not <code>application/json</code>
	 * however this media will work for normal json.
	 */
	public static final String MEDIA_TYPE = "application/json5";

	/**
	 * The file ext is {@value #FILE_EXT} and not <code>json</code> however this media
	 * will work for <code>json</code> extension.
	 */
	public static final String FILE_EXT = "json5";
	static final String JSON_FILE_EXT = "json";
	static final String JSON_MEDIA_TYPE = "application/json";

	// @formatter:off
	/**
	 * This parameter controls the array key naming scheme
	 * and is set with {@link KeyValuesResource#parameters()}.
	 * The two values are
	 *
	 * {@value #ARRAY_KEY_DUPLICATE_VALUE} (default) and
	 * {@value #ARRAY_KEY_ARRAY_VALUE}.
	 *
	 * An example is:
	 * {@snippet lang = properties :
	 *
	 * _load_a=classpath:/a.json5?_param_json5_arraykey=array
	 *
	 * }
	 */
	// @formatter:on
	public static final String ARRAY_KEY_PARAM = "json5_arraykey";

	/**
	 * Will output array keys using array notation like <code>a[0]</code> instead of the
	 * default which is duplicating the keys.
	 */
	public static final String ARRAY_KEY_ARRAY_VALUE = "array";

	// @formatter:off
	/**
	 * Will duplicate keys for array entries.
	 * For example <code>{ "a" : [ 1, 2 ] }</code>
	 * will be
	 * {@snippet lang = properties :
	 * a=1
	 * a=2
	 * }
	 */
	// @formatter:on
	public static final String ARRAY_KEY_DUPLICATE_VALUE = "duplicate";

	/**
	 * Configures the separator by {@link KeyValuesResource#parameters()} with the key
	 * {@value #SEPARATOR_PARAM} and is by default {@value #DEFAULT_SEPARATOR}.
	 */
	public static final String SEPARATOR_PARAM = "json5_separator";

	/**
	 * The default JSON path separator.
	 */
	public static final String DEFAULT_SEPARATOR = ".";

	/**
	 * For service loader.
	 */
	public JSON5KeyValuesMedia() {
	}

	@Override
	public String getMediaType() {
		return MEDIA_TYPE;
	}

	@Override
	public @Nullable String getFileExt() {
		return FILE_EXT;
	}

	@Override
	public Parser parser() {
		return JSON5KeyValuesParser.parser;
	}

	@Override
	public Parser parser(Variables parameters) {
		var arrayKeyOption = ArrayKeyOption.parse(parameters.getValue(ARRAY_KEY_PARAM));
		String separator = parameters.getValue(SEPARATOR_PARAM);
		if (separator == null || separator.isBlank()) {
			separator = DEFAULT_SEPARATOR;
		}
		return new JSON5KeyValuesParser(JSON5KeyValuesParser.defaultOptions, arrayKeyOption, separator);
	}

	@Override
	public Optional<KeyValuesMedia> findByMediaType(String mediaType) {
		var json5 = KeyValuesMedia.super.findByMediaType(mediaType);
		if (json5.isPresent()) {
			return json5;
		}
		if (JSON_MEDIA_TYPE.equalsIgnoreCase(mediaType)) {
			return Optional.of(this);
		}
		return Optional.empty();
	}

	@Override
	public boolean hasFileExt(String filename) {
		boolean json5 = KeyValuesMedia.super.hasFileExt(filename);
		if (json5) {
			return true;
		}
		return filename.endsWith("." + JSON_FILE_EXT);
	}

	static enum ArrayKeyOption {

		DUPLICATE(ARRAY_KEY_DUPLICATE_VALUE), ARRAY(ARRAY_KEY_ARRAY_VALUE),;

		private final String value;

		private ArrayKeyOption(String value) {
			this.value = value;
		}

		static ArrayKeyOption defaults() {
			return ArrayKeyOption.DUPLICATE;
		}

		void set(BiConsumer<String, String> consumer) {
			consumer.accept(ARRAY_KEY_PARAM, value);
		}

		static ArrayKeyOption parse(@Nullable String input) {
			if (input == null) {
				return defaults();
			}
			for (var a : ArrayKeyOption.values()) {
				if (a.value.equalsIgnoreCase(input)) {
					return a;
				}
			}
			throw new IllegalArgumentException("Bad ArrayKeyOption: " + input);
		}

	}

	static class JSON5KeyValuesParser implements Parser {

		private final JSONOptions options;

		private final ArrayKeyOption arrayKeyOption;

		private final String separator;

		static final JSONOptions defaultOptions = new JSONOptions.Builder()
			.duplicateBehaviour(DuplicateBehavior.DUPLICATE)
			.build();

		static final JSON5KeyValuesParser parser = new JSON5KeyValuesParser(defaultOptions, ArrayKeyOption.defaults(),
				DEFAULT_SEPARATOR);

		public JSON5KeyValuesParser(JSONOptions options, ArrayKeyOption arrayKeyOption, String separator) {
			super();
			this.options = options;
			this.arrayKeyOption = Objects.requireNonNull(arrayKeyOption);
			this.separator = separator;
		}

		@Override
		public void parse(InputStream input, BiConsumer<String, String> consumer) throws IOException {
			JSONParser parser = new JSONParser(new InputStreamReader(input), options);
			parse(parser, consumer);

		}

		@Override
		public void parse(String input, BiConsumer<String, String> consumer) {
			JSONParser parser = new JSONParser(new StringReader(input), options);
			parse(parser, consumer);
		}

		private void parse(JSONParser parser, BiConsumer<String, String> consumer) {
			var json = parser.nextValue();

			switch (json) {
				case JSONObject o -> {
					flattenJsonObject(o, "", consumer);
				}
				case JSONArray a -> {
					flattenJsonArray(a, "", consumer);
				}
				default -> {
					throw new IllegalStateException();
				}
			}
		}

		private void flattenJsonObject(JSONObject jsonObject, String prefix, BiConsumer<String, String> consumer) {
			for (String key : jsonObject.keySet()) {
				Object value = jsonObject.get(key);
				String newKey = createObjectKey(prefix, key);
				dispatch(newKey, value, consumer);
			}
		}

		private void flattenJsonArray(JSONArray jsonArray, String prefix, BiConsumer<String, String> consumer) {
			for (int i = 0; i < jsonArray.length(); i++) {
				Object value = jsonArray.get(i);
				String newKey = createArrayKey(prefix, i);
				dispatch(newKey, value, consumer);
			}
		}

		private String createObjectKey(String prefix, String key) {
			String newKey = prefix.isEmpty() ? key : prefix + this.separator + key;
			return newKey;
		}

		private String createArrayKey(String prefix, int i) {
			return switch (arrayKeyOption) {
				case ARRAY -> prefix.isEmpty() ? String.valueOf(i) : prefix + "[" + i + "]";
				case DUPLICATE -> prefix.isEmpty() ? String.valueOf(i) : prefix;
			};
		}

		private void dispatch(String newKey, @Nullable Object value, BiConsumer<String, String> consumer) {
			switch (value) {
				case JSONObject o -> flattenJsonObject(o, newKey, consumer);
				case JSONArray a -> flattenJsonArray(a, newKey, consumer);
				case Object o -> consumer.accept(newKey, String.valueOf(o));
				case null -> {
				}
			}
		}

	}

}
