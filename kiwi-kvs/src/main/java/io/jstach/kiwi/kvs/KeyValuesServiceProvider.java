package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.Optional;

import io.jstach.kiwi.kvs.KeyValuesServiceProvider.LoaderFinder.LoaderContext;
import io.jstach.kiwi.kvs.KeyValuesServiceProvider.MediaFinder;

public interface KeyValuesServiceProvider {

	public interface LoaderFinder extends KeyValuesServiceProvider {

		public sealed interface LoaderContext {

			KeyValuesEnvironment environment();

			MediaFinder mediaFinder();

			Variables variables();

			default KeyValuesMedia.Parser requireParser(KeyValuesResource resource) {
				return mediaFinder() //
					.findByResource(resource) //
					.orElseThrow(() -> new KeyValuesMediaException("Media Type not found. resource: " + resource))
					.parser();
			}

		}

		public Optional<KeyValuesLoader> findLoader(LoaderContext context, KeyValuesResource resource);

	}

	public interface MediaFinder extends KeyValuesServiceProvider {

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

record DefaultLoaderContext(KeyValuesEnvironment environment, MediaFinder mediaFinder,
		Variables variables) implements LoaderContext {
	public static LoaderContext of(KeyValuesSystem system, Variables variables) {
		return new DefaultLoaderContext(system.environment(), system.mediaFinder(), variables);
	}

}