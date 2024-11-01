package io.jstach.kiwi.kvs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValuesServiceProvider.MediaFinder;

public interface KeyValuesMedia extends MediaFinder {

	public String getMediaType();

	public @Nullable String getFileExt();

	public Parser parser();

	default Formatter formatter() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public static KeyValuesMedia ofProperties() {
		return DefaultKeyValuesMedia.PROPERTIES;
	}

	public static KeyValuesMedia ofUrlEncoded() {
		return DefaultKeyValuesMedia.URLENCODED;
	}

	// @Override
	// default Optional<KeyValuesMedia> findMedia(String mediaType) {
	// var mt = getMediaType();
	// if (mt.equals(mediaType)) {
	// return Optional.of(this);
	// }
	// if (hasFileExt(mediaType)) {
	// return Optional.of(this);
	// }
	// return Optional.empty();
	// }

	@Override
	default Optional<KeyValuesMedia> findByExt(String ext) {
		if (getFileExt().equalsIgnoreCase(ext)) {
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

	public static InputStream stringToInputStream(String s) {
		return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
	}

	public interface Parser {

		void parse(InputStream input, BiConsumer<String, String> consumer) throws IOException;

		default KeyValues parse(KeyValuesResource source, InputStream is) throws IOException {
			var b = KeyValues.Builder.of(source.uri());
			parse(is, b::add);
			return b.build();
		}

		default void parse(String input, BiConsumer<String, String> consumer) {
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

		public void format(Appendable appendable, KeyValues kvs) throws IOException;

		default void format(StringBuilder stringBuilder, KeyValues kvs) {
			try {
				format((Appendable) stringBuilder, kvs);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		default String format(KeyValues kvs) {
			StringBuilder sb = new StringBuilder();
			format(sb, kvs);
			return sb.toString();
		}

	}

	public class MediaTypeException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public MediaTypeException(@Nullable String message, @Nullable Throwable cause) {
			super(message, cause);
		}

		public MediaTypeException(@Nullable String message) {
			super(message);
		}

	}

	default boolean hasFileExt(String filename) {
		String ext = getFileExt();
		if (ext == null) {
			return false;
		}
		return filename.endsWith("." + ext);
	}

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
