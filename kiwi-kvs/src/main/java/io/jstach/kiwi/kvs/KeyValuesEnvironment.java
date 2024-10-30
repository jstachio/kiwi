package io.jstach.kiwi.kvs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface KeyValuesEnvironment {

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
	
	default System.Logger getLogger(String name) {
		return System.getLogger(name);
	}

	default ResourceStreamLoader getResourceStreamLoader() {
		return new ResourceStreamLoader() {
			
			@Override
			public @Nullable InputStream getResourceAsStream(
					String path)
					throws IOException {
				return getClassLoader().getResourceAsStream(path);
			}
		};
	}
	
	default ClassLoader getClassLoader() {
		return ClassLoader.getSystemClassLoader();
	}

	public interface ResourceStreamLoader {

		public @Nullable InputStream getResourceAsStream(
				String path)
				throws IOException;

		default InputStream openStream(
				String path)
				throws IOException,
				FileNotFoundException {
			InputStream s = getResourceAsStream(path);
			if (s == null) {
				throw new FileNotFoundException(path);
			}
			return s;
		}

	}
	

}
class DefaultKeyValuesEnvironment implements KeyValuesEnvironment {

}
