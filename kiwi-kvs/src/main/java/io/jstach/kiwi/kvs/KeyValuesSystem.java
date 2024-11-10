package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesLoaderFinder;
import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesMediaFinder;

public sealed interface KeyValuesSystem {

	public KeyValuesEnvironment environment();

	public KeyValuesLoaderFinder loaderFinder();

	public KeyValuesMediaFinder mediaFinder();

	default KeyValuesLoader.Builder loader() {
		return new KeyValuesLoader.Builder(variables -> DefaultKeyValuesSourceLoader.of(this, variables));
	}

	public static KeyValuesSystem defaults() {
		return builder().build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private @Nullable KeyValuesEnvironment environment;

		private List<KeyValuesLoaderFinder> loadFinders = new ArrayList<>(
				List.of(DefaultKeyValuesLoaderFinder.values()));

		private List<KeyValuesMediaFinder> mediaFinders = new ArrayList<>(List.of(DefaultKeyValuesMedia.values()));

		private @Nullable ServiceLoader<KeyValuesServiceProvider> serviceLoader;

		public Builder environment(KeyValuesEnvironment environment) {
			this.environment = environment;
			return this;
		}

		public Builder loadFinder(KeyValuesLoaderFinder loadFinder) {
			this.loadFinders.add(loadFinder);
			return this;
		}

		public Builder mediaFinder(KeyValuesMediaFinder mediaFinder) {
			this.mediaFinders.add(mediaFinder);
			return this;
		}

		public Builder serviceLoader(ServiceLoader<KeyValuesServiceProvider> serviceLoader) {
			this.serviceLoader = serviceLoader;
			return this;
		}

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

record DefaultKeyValuesSystem(KeyValuesEnvironment environment, KeyValuesLoaderFinder loaderFinder,
		KeyValuesMediaFinder mediaFinder) implements KeyValuesSystem {

}
