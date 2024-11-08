package io.jstach.kiwi.kvs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.jstach.kiwi.kvs.interpolate.Interpolator.InterpolationException;

public interface KeyValuesLoader {

	public KeyValues load() throws IOException, FileNotFoundException, InterpolationException;

	public class Builder implements KeyValuesLoader {

		private final Function<Variables, KeyValuesSourceLoader> loaderFactory;

		private final List<KeyValuesSource> sources = new ArrayList<>();

		private Variables variables = Variables.empty();

		Builder(Function<Variables, KeyValuesSourceLoader> loaderFactory) {
			super();
			this.loaderFactory = loaderFactory;
		}

		public Builder add(KeyValuesResource resource) {
			sources.add(resource);
			return this;
		}

		public Builder add(String name, KeyValues keyValues) {
			sources.add(new NamedKeyValues(name, keyValues));
			return this;

		}

		public Builder add(URI uri) {
			return add(KeyValuesResource.builder(uri).build());
		}

		public Builder add(String uri) {
			return add(URI.create(uri));
		}

		public Builder variables(Variables variables) {
			this.variables = variables;
			return this;
		}

		public KeyValuesLoader build() {
			var resources = this.sources.isEmpty() ? List.of(defaultResource()) : List.copyOf(this.sources);
			return () -> loaderFactory.apply(variables).load(resources);
		}

		// TODO should we really have a default resource?
		private static KeyValuesResource defaultResource() {
			return KeyValuesResource.builder(URI.create("classpath:/system.properties")).build();
		}

		@Override
		public KeyValues load() throws IOException, FileNotFoundException {
			return build().load();
		}

	}

}
