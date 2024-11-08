package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.Optional;
import java.util.function.BiConsumer;

import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesLoaderFinder.LoaderContext;
import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesMediaFinder;

public sealed interface KeyValuesServiceProvider {

	public non-sealed interface KeyValuesLoaderFinder extends KeyValuesServiceProvider {

		public sealed interface LoaderContext {

			KeyValuesEnvironment environment();

			KeyValuesMediaFinder mediaFinder();

			Variables variables();

			default KeyValuesMedia.Parser requireParser(KeyValuesResource resource) {
				return mediaFinder() //
					.findByResource(resource) //
					.orElseThrow(() -> new KeyValuesMediaException("Media Type not found. resource: " + resource))
					.parser();
			}

			void formatResource(KeyValuesResource resource, BiConsumer<String, String> consumer);

		}

		public Optional<KeyValuesLoader> findLoader(LoaderContext context, KeyValuesResource resource);

	}

	public non-sealed interface KeyValuesMediaFinder extends KeyValuesServiceProvider {

		default Optional<KeyValuesMedia> findByResource(KeyValuesResource resource) {
			String mediaType = resource.mediaType();
			if (mediaType == null) {
				return findByUri(resource.uri());
			}
			return findByMediaType(mediaType);
		}

		public Optional<KeyValuesMedia> findByExt(String ext);

		public Optional<KeyValuesMedia> findByMediaType(String mediaType);

		public Optional<KeyValuesMedia> findByUri(URI uri);

	}

}

record DefaultLoaderContext(KeyValuesEnvironment environment, KeyValuesMediaFinder mediaFinder, Variables variables,
		KeyValuesResourceParser resourceParser) implements LoaderContext {
	static LoaderContext of(KeyValuesSystem system, Variables variables, KeyValuesResourceParser resourceParser) {
		return new DefaultLoaderContext(system.environment(), system.mediaFinder(), variables, resourceParser);
	}

	@Override
	public void formatResource(KeyValuesResource resource, BiConsumer<String, String> consumer) {
		resourceParser.formatResource(resource, consumer);
	}

}