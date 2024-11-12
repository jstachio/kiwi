package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesLoaderFinder;
import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesMediaFinder;

/**
 * The main entry point into the Kiwi configuration library. This interface provides
 * access to core components for loading and processing key-value resources.
 *
 * <p>
 * {@code KeyValuesSystem} allows the creation and configuration of loaders that can
 * retrieve key-value pairs from various resources such as files, classpath locations, and
 * system properties.
 *
 * <p>
 * Example usage:
 * {@snippet :
 * var kvs = KeyValuesSystem.defaults()
 * 	.loader()
 * 	.add("classpath:/start.properties")
 * 	.add("system:///")
 * 	.add("env:///")
 * 	.add("cmd:///-D")
 * 	.load();
 * }
 *
 * @see KeyValuesLoader
 * @see KeyValuesEnvironment
 * @see KeyValuesServiceProvider
 */
public sealed interface KeyValuesSystem {

	/**
	 * Returns the {@link KeyValuesEnvironment} instance used for system-level
	 * interactions.
	 * @return the environment instance
	 */
	public KeyValuesEnvironment environment();

	/**
	 * Returns a composite {@link KeyValuesLoaderFinder} that aggregates all loader
	 * finders added to the {@link Builder} or found via the {@link ServiceLoader}, if
	 * configured. This composite allows finding loaders that can handle specific
	 * resources by delegating the search to all registered loader finders.
	 * @return the composite loader finder instance
	 */
	public KeyValuesLoaderFinder loaderFinder();

	/**
	 * Returns a composite {@link KeyValuesMediaFinder} that aggregates all media finders
	 * added to the {@link Builder} or found via the {@link ServiceLoader}, if configured.
	 * This composite allows finding media handlers that can handle specific resources by
	 * delegating the search to all registered media finders.
	 * @return the composite media finder instance
	 */
	public KeyValuesMediaFinder mediaFinder();

	/**
	 * Creates and returns a {@link KeyValuesLoader.Builder} for constructing loaders that
	 * can load key-value pairs from various resources.
	 * @return a new {@link KeyValuesLoader.Builder} instance
	 */
	default KeyValuesLoader.Builder loader() {
		return new KeyValuesLoader.Builder(variables -> DefaultKeyValuesSourceLoader.of(this, variables));
	}

	/**
	 * Returns a default implementation of {@code KeyValuesSystem} configured with
	 * standard settings.
	 * @return the default {@code KeyValuesSystem} instance
	 */
	public static KeyValuesSystem defaults() {
		return builder().build();
	}

	/**
	 * Creates and returns a new {@link Builder} for constructing customized
	 * {@code KeyValuesSystem} instances.
	 * @return a new {@link Builder} instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * A builder class for constructing instances of {@link KeyValuesSystem} with
	 * customizable components such as environment, loaders, and media finders.
	 *
	 * <p>
	 * Note: By default, the {@link ServiceLoader} is not enabled. If you wish to include
	 * services discovered via the {@link ServiceLoader}, you need to explicitly set it
	 * using {@link #serviceLoader(ServiceLoader)}.
	 */
	public static class Builder {

		private @Nullable KeyValuesEnvironment environment;

		private List<KeyValuesLoaderFinder> loadFinders = new ArrayList<>(
				List.of(DefaultKeyValuesLoaderFinder.values()));

		private List<KeyValuesMediaFinder> mediaFinders = new ArrayList<>(List.of(DefaultKeyValuesMedia.values()));

		private @Nullable ServiceLoader<KeyValuesServiceProvider> serviceLoader;

		/**
		 * Sets the {@link KeyValuesEnvironment} to be used by the
		 * {@code KeyValuesSystem}.
		 * @param environment the environment to set
		 * @return this builder instance
		 */
		public Builder environment(KeyValuesEnvironment environment) {
			this.environment = environment;
			return this;
		}

		/**
		 * Adds a {@link KeyValuesLoaderFinder} to the list of loader finders.
		 * @param loadFinder the loader finder to add
		 * @return this builder instance
		 */
		public Builder loadFinder(KeyValuesLoaderFinder loadFinder) {
			this.loadFinders.add(loadFinder);
			return this;
		}

		/**
		 * Adds a {@link KeyValuesMediaFinder} to the list of media finders.
		 * @param mediaFinder the media finder to add
		 * @return this builder instance
		 */
		public Builder mediaFinder(KeyValuesMediaFinder mediaFinder) {
			this.mediaFinders.add(mediaFinder);
			return this;
		}

		/**
		 * Sets the {@link ServiceLoader} for loading {@link KeyValuesServiceProvider}
		 * implementations. If this is set, the builder will include any
		 * {@link KeyValuesLoaderFinder} and {@link KeyValuesMediaFinder} instances found
		 * via the {@link ServiceLoader}.
		 *
		 * <p>
		 * Note: This is not enabled by default. You must explicitly set a service loader
		 * to include additional service-provided components.
		 * @param serviceLoader the service loader to set
		 * @return this builder instance
		 */
		public Builder serviceLoader(ServiceLoader<KeyValuesServiceProvider> serviceLoader) {
			this.serviceLoader = serviceLoader;
			return this;
		}

		/**
		 * Builds and returns a new {@link KeyValuesSystem} instance configured with the
		 * provided settings. The composite loader and media finders will include those
		 * added through this builder and, if specified, those found via the service
		 * loader.
		 * @return a new {@link KeyValuesSystem} instance
		 */
		public KeyValuesSystem build() {
			var environment = this.environment;
			if (environment == null) {
				environment = new DefaultKeyValuesEnvironment();
			}
			var loadFinders = new ArrayList<>(this.loadFinders);
			var mediaFinders = new ArrayList<>(this.mediaFinders);
			var serviceLoader = this.serviceLoader;

			if (serviceLoader != null) {
				serviceLoader.forEach(s -> {
					if (s instanceof KeyValuesLoaderFinder rl) {
						loadFinders.add(rl);
					}
					if (s instanceof KeyValuesMediaFinder m) {
						mediaFinders.add(m);
					}
				});
			}

			KeyValuesLoaderFinder loadFinder = (context, resource) -> {
				return loadFinders.stream().flatMap(rl -> rl.findLoader(context, resource).stream()).findFirst();
			};
			KeyValuesMediaFinder mediaFinder = new CompositeMediaFinder(mediaFinders);

			return new DefaultKeyValuesSystem(environment, loadFinder, mediaFinder);
		}

	}

}

/**
 * A composite media finder that aggregates multiple {@link KeyValuesMediaFinder}
 * implementations and searches them in sequence for a matching media type.
 */
record CompositeMediaFinder(List<KeyValuesMediaFinder> finders) implements KeyValuesMediaFinder {
	CompositeMediaFinder {
		finders = List.copyOf(finders);
	}

	@Override
	public Optional<KeyValuesMedia> findByExt(String ext) {
		return finders.stream().flatMap(mf -> mf.findByExt(ext).stream()).findFirst();
	}

	@Override
	public Optional<KeyValuesMedia> findByMediaType(String mediaType) {
		return finders.stream().flatMap(mf -> mf.findByMediaType(mediaType).stream()).findFirst();
	}

	@Override
	public Optional<KeyValuesMedia> findByUri(URI uri) {
		return finders.stream().flatMap(mf -> mf.findByUri(uri).stream()).findFirst();
	}
}

/**
 * A default implementation of {@link KeyValuesSystem}.
 */
record DefaultKeyValuesSystem(KeyValuesEnvironment environment, KeyValuesLoaderFinder loaderFinder,
		KeyValuesMediaFinder mediaFinder) implements KeyValuesSystem {
}
