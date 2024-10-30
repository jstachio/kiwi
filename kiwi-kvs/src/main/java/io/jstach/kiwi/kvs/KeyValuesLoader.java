package io.jstach.kiwi.kvs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public interface KeyValuesLoader {
	public KeyValues load() throws IOException, FileNotFoundException;
	
	public class Builder implements KeyValuesLoader {
		private final KeyValuesResourceLoader loader;
		private final List<KeyValuesResource> resources = new ArrayList<>();
		
		 Builder(
				KeyValuesResourceLoader loader) {
			super();
			this.loader = loader;
		}
		public Builder add(KeyValuesResource resource) {
			resources.add(resource);
			return this;
		}

		public Builder add(URI uri) {
			return add(KeyValuesResource.builder(uri).build());
		}
		public Builder add(String uri) {
			return add(URI.create(uri));
		}
		public KeyValuesLoader build() {
			var _default = KeyValuesResource.builder(URI.create("classpath:/system.properties")).build();
			var resources = this.resources.isEmpty()? List.of(_default ) : List.copyOf(this.resources);
			return () -> loader.load(resources);
		}
		@Override
		public KeyValues load()
				throws IOException,
				FileNotFoundException {
			return build().load();
		}
		
	}
}
