package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.Optional;
import java.util.function.BiConsumer;

import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesLoaderFinder.LoaderContext;
import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesMediaFinder;
import io.jstach.kiwi.kvs.Variables.Parameters;

/**
 * A service provider interface (SPI) for extending Kiwi's capabilities to support
 * additional media types and URI patterns. Implementations of this interface can be
 * loaded using {@link java.util.ServiceLoader}.
 *
 * <p>
 * This SPI allows customization and extension of Kiwi's loading mechanisms and media type
 * parsing.
 *
 * <p>
 * Note: This interface is sealed, and any sub-interface implementations must be
 * registered using {@code provides KeyValuesServiceProvider with ...} in the
 * {@code module-info.java} or as entries in the
 * {@code META-INF/services/io.jstach.kiwi.kvs.KeyValuesServiceProvider} file.
 * Sub-interfaces such as {@link KeyValuesLoaderFinder} and {@link KeyValuesMediaFinder}
 * should <strong>not</strong> be directly referenced in the service registration.
 *
 * <p>
 * Example use cases include adding support for custom media types or integrating
 * specialized URI loading logic.
 */
public sealed interface KeyValuesServiceProvider {

	/**
	 * A service provider interface for finding {@link KeyValuesLoader} implementations
	 * that can handle specific {@link KeyValuesResource} instances.
	 */
	public non-sealed interface KeyValuesLoaderFinder extends KeyValuesServiceProvider {

		/**
		 * A context provided to {@link KeyValuesLoaderFinder} implementations to supply
		 * necessary dependencies and services for creating a {@link KeyValuesLoader}.
		 */
		public sealed interface LoaderContext {

			/**
			 * Retrieves the {@link KeyValuesEnvironment} for accessing system-level
			 * resources and properties.
			 * @return the environment instance
			 */
			KeyValuesEnvironment environment();

			/**
			 * Retrieves the {@link KeyValuesMediaFinder} for finding media types.
			 * @return the media finder instance
			 */
			KeyValuesMediaFinder mediaFinder();

			/**
			 * Retrieves the {@link Variables} used for interpolation.
			 * @return the variables instance
			 */
			Variables variables();

			/**
			 * Finds and returns a required parser for the specified
			 * {@link KeyValuesResource}. Throws an exception if no parser is found.
			 * @param resource the resource for which to find a parser
			 * @return the parser for the resource
			 * @throws KeyValuesMediaException if no parser is found for the resource's
			 * media type
			 */
			default KeyValuesMedia.Parser requireParser(KeyValuesResource resource) {
				return mediaFinder() //
					.findByResource(resource) //
					.orElseThrow(() -> new KeyValuesMediaException("Media Type not found. resource: " + resource))
					.parser();
			}

			/**
			 * Formats a {@link KeyValuesResource} into its key-value representation and
			 * applies it to a given consumer. This method allows serializing the resource
			 * into key-value pairs.
			 * @param resource the resource to format
			 * @param consumer the consumer to apply the formatted key-value pairs
			 */
			void formatResource(KeyValuesResource resource, BiConsumer<String, String> consumer);

			/**
			 * Formats a resource parameter key how it is represented as key in the parsed
			 * resource.
			 * @param resource resource to base the full parameter name on.
			 * @param parameterName the short parameter from
			 * {@link KeyValuesResource#parameters()}.
			 * @return FQ parameter name.
			 */
			String formatParameterKey(KeyValuesResource resource, String parameterName);

		}

		/**
		 * Finds a {@link KeyValuesLoader} capable of loading the specified
		 * {@link KeyValuesResource} using the provided {@link LoaderContext}.
		 * @param context the context containing dependencies and services
		 * @param resource the resource for which a loader is sought
		 * @return an {@link Optional} containing the loader if found, or empty if not
		 */
		public Optional<? extends KeyValuesLoader> findLoader(LoaderContext context, KeyValuesResource resource);

	}

	public non-sealed interface KeyValuesFilter extends KeyValuesServiceProvider {

		public record FilterContext(KeyValuesEnvironment environment, Parameters parameters) {
		}

		KeyValues filter(FilterContext context, KeyValues keyValues, String filter);

	}

	/**
	 * A service provider interface for finding {@link KeyValuesMedia} implementations
	 * based on various attributes such as file extensions, media types, and URIs.
	 */
	public non-sealed interface KeyValuesMediaFinder extends KeyValuesServiceProvider {

		/**
		 * Finds a {@link KeyValuesMedia} based on the attributes of a given
		 * {@link KeyValuesResource}.
		 * @param resource the resource for which to find the media type
		 * @return an {@link Optional} containing the media if found, or empty if not
		 */
		default Optional<KeyValuesMedia> findByResource(KeyValuesResource resource) {
			String mediaType = resource.mediaType();
			if (mediaType == null) {
				return findByUri(resource.uri());
			}
			return findByMediaType(mediaType);
		}

		/**
		 * Finds a {@link KeyValuesMedia} based on a file extension.
		 * @param ext the file extension (e.g., "properties")
		 * @return an {@link Optional} containing the media if found, or empty if not
		 */
		public Optional<KeyValuesMedia> findByExt(String ext);

		/**
		 * Finds a {@link KeyValuesMedia} based on a media type string.
		 * @param mediaType the media type (e.g., "application/json")
		 * @return an {@link Optional} containing the media if found, or empty if not
		 */
		public Optional<KeyValuesMedia> findByMediaType(String mediaType);

		/**
		 * Finds a {@link KeyValuesMedia} based on a {@link URI}.
		 * @param uri the URI to analyze
		 * @return an {@link Optional} containing the media if found, or empty if not
		 */
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

	@Override
	public String formatParameterKey(KeyValuesResource resource, String parameterName) {
		return resourceParser.formatParameterKey(resource, parameterName);
	}

}