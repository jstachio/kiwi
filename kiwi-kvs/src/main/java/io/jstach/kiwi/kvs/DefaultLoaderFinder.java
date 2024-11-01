package io.jstach.kiwi.kvs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.jstach.kiwi.kvs.KeyValuesEnvironment.ResourceStreamLoader;
import io.jstach.kiwi.kvs.KeyValuesMedia.Parser;
import io.jstach.kiwi.kvs.KeyValuesServiceProvider.LoaderFinder;

enum DefaultLoaderFinder implements LoaderFinder {

	CLASSPATH {

	},
	SYSTEM {
		@Override
		protected KeyValues load(LoaderContext context, KeyValuesResource resource) throws IOException {
			var builder = KeyValues.builder(resource.uri());
			var properties = context.environment().getSystemProperties();
			for (var sp : properties.stringPropertyNames()) {
				builder.add(sp, properties.getProperty(sp));
			}
			return builder.build();
		}

	};

	@Override
	public Optional<KeyValuesLoader> findLoader(LoaderContext context, KeyValuesResource resource) {
		if (!matches(resource)) {
			return Optional.empty();
		}
		KeyValuesLoader loader = loader(context, resource);
		return Optional.of(loader);
	}

	protected KeyValuesLoader loader(LoaderContext context, KeyValuesResource resource) {
		KeyValuesLoader loader = () -> {
			return load(context, resource);
		};
		return loader;
	}

	protected KeyValues load(LoaderContext context, KeyValuesResource resource) throws IOException {
		var parser = context.requireParser(resource);
		return load(context, resource, parser);
	}

	protected KeyValues load(LoaderContext context, KeyValuesResource resource, Parser parser) throws IOException {
		var is = openURI(resource.uri(), context.environment().getResourceStreamLoader());
		return parser.parse(resource, is);
	}

	boolean matches(KeyValuesResource resource) {
		return name().toLowerCase().equals(resource.uri().getScheme());
	}

	static InputStream openURI(URI u, ResourceStreamLoader loader) throws IOException {
		if ("classpath".equals(u.getScheme())) {
			String path = u.getPath();
			if (path == null) {
				throw new MalformedURLException("Classpath scheme URI requires a path. URI: " + u);
			}
			else if (path.startsWith("/")) {
				path = path.substring(1);
			}

			InputStream stream = loader.getResourceAsStream(path);
			if (stream == null) {
				throw new FileNotFoundException("Classpath URI: " + u + " cannot be found with loader: " + loader);
			}
			return stream;
		}
		else if ("file".equals(u.getScheme())) {
			return Files.newInputStream(Path.of(u));
		}
		else if (u.getScheme() == null && u.getPath() != null) {
			return Files.newInputStream(Path.of(u.getPath()));
		}
		else {
			return u.toURL().openStream();
		}
	}

}