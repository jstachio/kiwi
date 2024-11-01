package io.jstach.kiwi.kvs;

import java.util.Optional;

import io.jstach.kiwi.kvs.KeyValuesServiceProvider.LoaderFinder.LoaderContext;
import io.jstach.kiwi.kvs.KeyValuesServiceProvider.MediaFinder;

public interface KeyValuesServiceProvider {

	public interface LoaderFinder extends KeyValuesServiceProvider {

		public sealed interface LoaderContext {

			KeyValuesEnvironment environment();

			MediaFinder mediaFinder();

			default KeyValuesMedia.Parser requireParser(KeyValuesResource resource) {
				return mediaFinder() //
					.findMedia(resource.uri().getPath()) //
					.orElseThrow(() -> new KeyValuesMediaException("Media Type not found. resource: " + resource))
					.parser();
			}

		}

		public Optional<KeyValuesLoader> findLoader(LoaderContext context, KeyValuesResource resource);

	}

	public interface MediaFinder extends KeyValuesServiceProvider {

		public Optional<KeyValuesMedia> findMedia(String mediaType);

	}

}

record DefaultLoaderContext(KeyValuesEnvironment environment, MediaFinder mediaFinder,
		Variables variables) implements LoaderContext {
	public static LoaderContext of(KeyValuesSystem system, Variables variables) {
		return new DefaultLoaderContext(system.environment(), system.mediaFinder(), variables);
	}

}