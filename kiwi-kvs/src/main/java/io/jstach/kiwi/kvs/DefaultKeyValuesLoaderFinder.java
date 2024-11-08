package io.jstach.kiwi.kvs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.jstach.kiwi.kvs.KeyValuesEnvironment.ResourceStreamLoader;
import io.jstach.kiwi.kvs.KeyValuesMedia.Parser;
import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesLoaderFinder;

enum DefaultKeyValuesLoaderFinder implements KeyValuesLoaderFinder {

	CLASSPATH {

	},
	SYSTEM {
		@Override
		protected KeyValues load(LoaderContext context, KeyValuesResource resource) throws IOException {
			var builder = KeyValues.builder(resource.uri());
			var properties = context.environment().getSystemProperties();
			for (var sp : properties.stringPropertyNames()) {
				String value = properties.getProperty(sp);
				if (value != null) {
					builder.add(sp, value);
				}
			}
			return builder.build();
		}

	},
	PROFILE {
		private final static String scheme = "profile";

		@Override
		protected KeyValues load(LoaderContext context, KeyValuesResource resource) throws IOException {
			return profiles(scheme, context, resource);
		}

		@Override
		boolean matches(KeyValuesResource resource) {
			String scheme = resource.uri().getScheme();
			if (scheme == null) {
				return false;
			}
			return scheme.startsWith(scheme + ".");
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

	static KeyValues profiles(String scheme, LoaderContext context, KeyValuesResource resource) throws IOException {
		String uriString = resource.uri().toString();
		uriString = uriString.substring(scheme.length() + 1);
		// var uri = URI.create(uriString);
		var profile = context.variables().getValue("_profile");
		if (profile == null) {
			throw new IOException("_profile variable is required. Set it to CSV list of profiles.");
		}
		if (!uriString.contains("__PROFILE__")) {
			throw new IOException(
					"Resource needs '__PROFILE__' in URI to be replaced by extracted profiles. URI: " + uriString);
		}
		List<String> profiles = Stream.of(profile.split(",")).filter(p -> !p.isBlank()).distinct().toList();

		var logger = context.environment().getLogger();
		logger.info("Found profiles: " + profiles);
		var builder = KeyValues.builder(resource.uri());

		int i = 0;
		for (var p : profiles) {
			var value = uriString.replace("__PROFILE__", p);
			var b = resource.toBuilder();
			b.uri(URI.create(value));
			b.name(resource.name() + i);
			var profileResource = b.build();
			context.formatResource(profileResource, builder::add);
		}
		var kvs = builder.build();
		return kvs;
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