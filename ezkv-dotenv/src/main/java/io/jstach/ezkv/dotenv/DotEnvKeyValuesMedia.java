package io.jstach.ezkv.dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import io.jstach.ezkv.kvs.KeyValuesMedia;

/**
 * Dotenv (<code>.env</code>) format based on
 * <a href="https://github.com/motdotla/dotenv">Node JS dotenv</a> library. A couple of
 * difference between other implementations:
 * <ul>
 * <li>Interpolation is done by EZKV and not an external bash process (or not at all in
 * the case of dotenv-java).</li>
 * <li>No decryption is available like the newer dotenvx</li>
 * <li>Tab and newline (both cr and ln) escaping is allowed but not other escape
 * sequences</li>
 * <li>EZKV makes no assumption where the env files is or if it is just called
 * <code>.env</code></li>
 * </ul>
 * The extension is {@value #FILE_EXT} and thus a resource named <code>.env</code> will
 * match this media.
 * <p>
 *
 * <strong>It is highly recommended to load environment variables</strong> with
 * {@link io.jstach.ezkv.kvs.KeyValuesLoader.Builder#variables(java.util.function.Function)}
 * and
 * {@link io.jstach.ezkv.kvs.Variables#ofSystemEnv(io.jstach.ezkv.kvs.KeyValuesEnvironment)}
 * for compatibility but is not required.
 * <p>
 * Note that there is no formatter for this media at this time as formatting dotenv is
 * unclear.
 *
 * <h2>Example</h2>
 * {@snippet :
 * var kvs = KeyValuesSystem.defaults()  // The service loader will pick up this format
 * 	.loader()
 * 	.variables(Variables::ofSystemEnv)   // Env variables for interpolation
 * 	.add("./.env")                       // Load dot env file (take note of file name)
 * 	.load();
 * }
 *
 * If you do not wish to use the Service Loader you can add an instance of this class to
 * {@link io.jstach.ezkv.kvs.KeyValuesSystem.Builder}.
 *
 * @apiNote This is not included with the core ezkv module because dotenv is still
 * evolving and likely to keep changing.
 */
public final class DotEnvKeyValuesMedia implements KeyValuesMedia, KeyValuesMedia.Parser {

	/**
	 * For service loader.
	 */
	public DotEnvKeyValuesMedia() {
	}

	/*
	 * This regex was adapted/inspired from https://github.com/motdotla/dotenv
	 */
	private static final Pattern LINE = Pattern.compile(
			"(?:^|^)\\s*(?:export\\s+)?([\\w.-]+)(?:\\s*=\\s*?|:\\s+?)(\\s*'(?:\\\\'|[^'])*'|\\s*\"(?:\\\\\"|[^\"])*\"|\\s*`(?:\\\\`|[^`])*`|[^#\\r\\n]+)?\\s*(?:#.*)?(?:$|$)",
			Pattern.MULTILINE);

	/**
	 * Dotenv file extension.
	 */
	public static final String FILE_EXT = "env";

	/**
	 * Dotenv media type does not really exist so we use a custom one of
	 * {@value #MEDIA_TYPE}.
	 */
	public static final String MEDIA_TYPE = "application/x-env";

	/**
	 * Dotenv media type does not really exist so we use a custom one of
	 * {@value #MEDIA_TYPE}.
	 * @return {@value #MEDIA_TYPE}.
	 */
	@Override
	public String getMediaType() {
		return MEDIA_TYPE;
	}

	/**
	 * Returns dotenv file extension.
	 * @return {@value #FILE_EXT}
	 */
	@Override
	public @Nullable String getFileExt() {
		return FILE_EXT;
	}

	@Override
	public Parser parser() {
		return this;
	}

	@Override
	public void parse(InputStream input, BiConsumer<String, String> consumer) throws IOException {
		String s = KeyValuesMedia.inputStreamToString(input);
		parse(s, consumer);
	}

	@Override
	public void parse(String input, BiConsumer<String, String> consumer) {
		String lines = input.replaceAll("\\r\\n?", "\n");

		Matcher matcher = LINE.matcher(lines);

		while (matcher.find()) {

			String key = switch (matcher.group(1)) {
				case String s -> s;
				case null -> throw new IllegalStateException();
			};

			// Default undefined or null to empty string
			String value = switch (matcher.group(2)) {
				case String s -> s.trim();
				case null -> "";
			};

			// Check if double quoted
			char maybeQuote = value.isEmpty() ? '\0' : value.charAt(0);

			// Remove surrounding quotes
			value = value.replaceAll("^(['\"`])(.*)\\1$", "$2");

			// Expand newlines if double quoted
			if (maybeQuote == '"') {
				value = value.replace("\\n", "\n")
					.replace("\\r", "\r")
					// tab expansion is in dotenvx
					// as well as many others so might as well.
					.replace("\\t", "\t");
			}
			consumer.accept(key, value);
			;
		}
	}

}
