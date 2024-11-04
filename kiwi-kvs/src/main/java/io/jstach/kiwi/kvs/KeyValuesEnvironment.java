package io.jstach.kiwi.kvs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.Properties;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValuesEnvironment.Logger;

public interface KeyValuesEnvironment {

	/*
	 * TODO remove the getter prefix on these methods.
	 */
	default @NonNull String[] getMainArgs() {
		return new @NonNull String[] {};
	}

	default Properties getSystemProperties() {
		return System.getProperties();
	}

	default Map<String, String> getSystemEnv() {
		return System.getenv();
	}

	default InputStream getStandardInput() {
		InputStream i = System.in;
		return i == null ? InputStream.nullInputStream() : i;
	}

	default Logger getLogger() {
		return new DefaultLogger(System.getLogger("io.jstach.kiwi,kvs"));
	}

	default ResourceStreamLoader getResourceStreamLoader() {
		return new ResourceStreamLoader() {

			@Override
			public @Nullable InputStream getResourceAsStream(String path) throws IOException {
				return getClassLoader().getResourceAsStream(path);
			}
		};
	}

	default ClassLoader getClassLoader() {
		return ClassLoader.getSystemClassLoader();
	}

	public interface ResourceStreamLoader {

		public @Nullable InputStream getResourceAsStream(String path) throws IOException;

		default InputStream openStream(String path) throws IOException, FileNotFoundException {
			InputStream s = getResourceAsStream(path);
			if (s == null) {
				throw new FileNotFoundException(path);
			}
			return s;
		}

	}

	public interface Logger {

		public void debug(String message);

		public void info(String message);

		default void load(KeyValuesResource resource) {
			debug(describe(new StringBuilder("Loading "), resource, true).toString());
		}

		default void loaded(KeyValuesResource resource) {
			info(describe(new StringBuilder("Loaded  "), resource, false).toString());
		}

		default void missing(KeyValuesResource resource, FileNotFoundException exception) {
			debug(describe(new StringBuilder("Missing "), resource, false).toString());
		}

		private static StringBuilder describe(StringBuilder sb, KeyValuesResource resource, boolean includeRef) {
			sb.append("uri='").append(resource.uri()).append("'");
			var flags = LoadFlag.of(resource);
			if (!flags.isEmpty()) {
				sb.append(" flags=").append(flags);
			}
			if (includeRef) {
				var ref = resource.reference();
				if (ref != null) {
					sb.append(" specified with key: ");
					sb.append("'").append(ref.key()).append("' in uri='").append(ref.source().uri()).append("'");
				}
			}
			return sb;
		}

	}

}

class DefaultLogger implements Logger {

	private final System.Logger logger;

	DefaultLogger(java.lang.System.Logger logger) {
		super();
		this.logger = logger;
	}

	@Override
	public void debug(String message) {
		logger.log(Level.DEBUG, message);
	}

	@Override
	public void info(String message) {
		logger.log(Level.INFO, message);
	}

}

class DefaultKeyValuesEnvironment implements KeyValuesEnvironment {

}
