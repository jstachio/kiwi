package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesFilter.Filter;

/*
 * The idea here is to keep all the parsing logic separated from the core domain so that
 * we can switch the key value patterns of loading.
 */

/**
 * This interface handles parsing and determining special keys for loading additional
 * key-value pairs. The logic for parsing is separated from the core domain to allow
 * flexibility in switching key-value patterns for loading resources.
 *
 * <p>
 * The `KeyValuesResourceParser` processes `KeyValues` streams to extract and format
 * resource references, supporting the recursive loading mechanism in Kiwi.
 *
 * <p>
 * Example usage might include parsing key-values to find resource URIs or formatting a
 * resource into its key-value representation.
 *
 * @apiNote This is not public for a variety of complex reasons that maybe revisited.
 */
sealed interface KeyValuesResourceParser {

	/**
	 * Parses a {@link KeyValues} stream and returns a list of {@link KeyValuesResource}
	 * objects representing resources that should be loaded.
	 * @param keyValues the key-values to parse for resource references
	 * @return a list of resources parsed from the key-values
	 */
	List<? extends InternalKeyValuesResource> parseResources(KeyValues keyValues);

	// TODO we should probably make a ParsedKeyValuesResource or
	// NormalizedKeyValuesResource
	// For now Internal is doing double duty.
	/**
	 * Because the resource URI might contain meta data we may need to reparse to build
	 * the correct resource representation.
	 * @param resource resoure that may need to be reparse.
	 * @return internal key values resource that is normalized.
	 */
	InternalKeyValuesResource normalizeResource(KeyValuesResource resource);

	/**
	 * Filters out the key-values that are related to resource loading from a given
	 * {@link KeyValues} stream. This is to remove all the resource meta key value pairs
	 * that are not needed in the final result as well as avoid collisions in resources
	 * loaded after.
	 * @param keyValues the key-values to filter
	 * @return a {@link KeyValues} instance with resource-related key-values removed
	 */
	KeyValues filterResources(KeyValues keyValues);

	/**
	 * Converts a {@link KeyValuesResource} into its key-value representation and applies
	 * the resulting key-value pairs to a provided consumer. This method effectively
	 * serializes the resource into a format that would be parsed back into a resource by
	 * the library.
	 *
	 * <p>
	 * The output can include keys that represent the URI, parameters, flags, and other
	 * metadata associated with the resource.
	 * @param resource the resource to format
	 * @param consumer the consumer to apply the serialized key-value pairs
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

record LoadResource(String name, URI uri, KeyValue keyValue) {
}

/*
 * The idea here is to keep all the parsing logic separated from the core domain so that
 * we can switch the key value scheme of loading.
 */
enum DefaultKeyValuesResourceParser implements KeyValuesResourceParser {

	DEFAULT {

		// Formatting resource

		@Override
		public void formatResource(KeyValuesResource _resource, BiConsumer<String, String> consumer) {

			InternalKeyValuesResource resource = switch (_resource) {
				case InternalKeyValuesResource ir -> ir;
			};

			for (var rk : ResourceKey.values()) {
				String resourceName = resource.name();
				var uri = resource.uri();
				switch (rk) {
					case LOAD -> {
						var key = externalResourceKey1(rk, resourceName);
						consumer.accept(key, uri.toString());
					}
					case FLAGS -> {
						var flags = resource.loadFlags();
						var key = externalResourceKey1(rk, resourceName);
						if (!flags.isEmpty()) {
							var flagsCsv = LoadFlag.toCSV(flags.stream());
							consumer.accept(key, flagsCsv);
						}
					}
					case MEDIA_TYPE -> {
						var key = externalResourceKey1(rk, resourceName);
						String mediaType = resource.mediaType();
						if (mediaType != null) {
							consumer.accept(key, mediaType);
						}
					}
					case PARAM -> {
						// var key = externalResourceKey2Prefix(rk, resourceName);
						resource.parameters().forKeyValues((k, v) -> {
							var key = formatParameterKey(resource, k);
							consumer.accept(key, v);
						});
					}
					case FILTER -> {
						var filters = resource.filters();
						for (var f : filters) {
							var key = formatFilterKey(resource, f);
							var value = f.expression();
							consumer.accept(key, value);
						}
					}
				}
			}
		}

		@Override
		public String formatParameterKey(KeyValuesResource resource, String parameterName) {
			return externalResourceKey2Prefix(ResourceKey.PARAM, resource.name()) + parameterName;
		}

		// Parsing Resource

		@Override
		public void parseKeyValues(KeyValuesResource.Builder builder, LoadResource resource, KeyValues keyValues) {
			for (var kv : keyValues) {
				for (var rk : ResourceKey.values()) {
					parseResourceKey(builder, resource, rk, kv);
				}
			}
		}

		private void parseResourceKey(KeyValuesResource.Builder builder, LoadResource resource, ResourceKey rk,
				KeyValue kv) {
			if (!isResourceKey(rk, kv, resource.name())) {
				return;
			}
			switch (rk) {
				case LOAD -> {
				}
				case MEDIA_TYPE -> {
					builder.mediaType(kv.expanded());
				}
				case FLAGS -> {
					builder._flags(LoadFlag.parseCSV(kv.expanded()));
				}
				case PARAM -> {
					String name = parseParamName(ResourceKey.PARAM, resource.name(), kv.key());
					builder.parameter(name, kv.expanded());
				}
				case FILTER -> {
					String filter = parseParamName(ResourceKey.FILTER, resource.name(), kv.key());
					String expression = kv.value();
					var f = new Filter(filter, expression, "");
					builder._addFilter(f);
				}
			}
		}

		private boolean isResourceKey(ResourceKey rk, KeyValue kv, String resourceName) {
			String key = kv.key();
			return switch (rk) {
				case PARAM, FILTER -> key.startsWith(externalResourceKey2Prefix(rk, resourceName));
				case LOAD, FLAGS, MEDIA_TYPE -> externalResourceKey1(rk, resourceName).equals(key);
			};
		}

		@Override
		public KeyValues filterResources(KeyValues keyValues) {
			return keyValues.filter(this::filter);
		}

		private boolean filter(KeyValue kv) {
			return resourceKeyOrNull(kv) == null;
		}

		private String parseParamName(ResourceKey rk, String resourceName, String key) {
			String prefix = externalResourceKey2Prefix(rk, resourceName);
			if (!key.startsWith(prefix)) {
				throw new IllegalArgumentException(rk + " prefix is incorrect. " + key);
			}
			String name = key.substring(prefix.length());
			if (name.isBlank()) {
				throw new IllegalArgumentException(rk + " is bad. " + key);
			}
			return name;
		}

	};

	// Parsing Resource

	@Override
	public List<? extends InternalKeyValuesResource> parseResources(KeyValues keyValues) {
		List<InternalKeyValuesResource> resources = new ArrayList<>();
		for (var kv : keyValues) {
			var r = parseResourceOrNull(kv, keyValues);
			if (r != null) {
				resources.add(r);
			}
		}
		return List.copyOf(resources);
	}

	@Nullable
	InternalKeyValuesResource parseResourceOrNull(KeyValue keyValue, KeyValues keyValues) {
		var resource = parseLoadOrNull(keyValue);
		if (resource == null) {
			return null;
		}
		var builder = new KeyValuesResource.Builder(resource.uri(), resource.name());
		parseURI(builder, resource.uri());
		parseKeyValues(builder, resource, keyValues);
		builder.reference = keyValue;
		return builder.buildNormalized();
	}

	@Override
	public InternalKeyValuesResource normalizeResource(KeyValuesResource resource) {
		InternalKeyValuesResource r = switch (resource) {
			case InternalKeyValuesResource ir -> ir;
		};
		if (r.normalized()) {
			return r;
		}
		var builder = KeyValuesResource.builder(r.uri()).name(r.name());
		parseURI(builder, r.uri());
		for (var rk : ResourceKey.values()) {
			switch (rk) {
				case LOAD -> {
				}
				case MEDIA_TYPE -> {
					switch (r.mediaType()) {
						case String s -> builder.mediaType(s);
						case null -> {
						}
					}
				}
				case FLAGS -> {
					for (var f : r.loadFlags()) {
						builder._addFlag(f);
					}
				}
				case PARAM -> {
					r.parameters().forKeyValues((k, v) -> {
						builder.parameter(k, v);
					});
				}
				case FILTER -> {
					for (var f : r.filters()) {
						builder._addFilter(f);
					}
				}
			}
		}
		return builder.buildNormalized();
	}

	protected @Nullable LoadResource parseLoadOrNull(KeyValue keyValue) {
		String key = keyValue.key();
		String prefix = prefix(ResourceKey.LOAD);
		if (!key.startsWith(prefix)) {
			return null;
		}
		String name = key.substring(prefix.length());

		try {
			name = KeyValuesSource.validateName(name);
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Key Value has a malformed name for load resource. keyValue=" + keyValue,
					e);
		}
		var uri = URI.create(keyValue.expanded());
		return new LoadResource(name, uri, keyValue);
	}

	protected abstract void parseKeyValues(KeyValuesResource.Builder builder, LoadResource resource,
			KeyValues keyValues);

	protected void parseURI(KeyValuesResource.Builder builder, URI uri) {
		if (uri.getQuery() == null) {
			return;
		}
		var uriParameters = DefaultKeyValuesMedia.parseURI(uri);
		List<KeyValue> filtered = new ArrayList<>();
		boolean hasParameter = false;
		for (var kv : uriParameters) {
			var rk = uriResourceKeyOrNull(kv);
			if (rk != null)
				hasParameter = true;
			switch (rk) {
				case LOAD -> {
				}
				case MEDIA_TYPE -> {
					builder.mediaType(kv.value());
				}
				case FLAGS -> {
					builder.addFlags(kv.value());
				}
				case PARAM -> {
					String param = uriParseParamName(ResourceKey.PARAM, kv.key());
					String value = kv.value();
					builder.parameter(param, value);
				}
				case FILTER -> {
					String filter = uriParseParamName(ResourceKey.FILTER, kv.key());
					String expression = kv.value();
					var f = new Filter(filter, expression, "");
					builder._addFilter(f);
				}
				case null -> {
					filtered.add(kv);
				}
			}
		}
		if (hasParameter) {
			String uriNoQuery = uriWithoutQuery(uri);
			if (filtered.isEmpty()) {
				builder.uri = URI.create(uriNoQuery);
			}
			else {
				KeyValues queryKvs = () -> filtered.stream();
				builder.uri = URI.create(uriNoQuery + "?" + KeyValuesMedia.ofUrlEncoded().formatter().format(queryKvs));
			}
		}

	}

	// TODO consider putting this in KeyValuesMedia if we need this again.
	private static String uriWithoutQuery(URI uri) {
		String s = uri.toString();
		if (uri.getQuery() == null) {
			return s;
		}
		int index = s.indexOf('?');
		return s.substring(0, index);
	}

	protected String prefix() {
		return sep();
	}

	protected String sep() {
		return "_";
	}

	protected String uriParameterPrefix(ResourceKey key) {
		return switch (key) {
			case LOAD, MEDIA_TYPE, FLAGS -> prefix() + key.key;
			case PARAM, FILTER -> prefix() + key.key + sep();
		};
	}

	protected String prefix(ResourceKey key) {
		return prefix() + key.key + sep();
	}

	private String uriParseParamName(ResourceKey type, String key) {
		String prefix = uriParameterPrefix(type);
		if (!key.startsWith(prefix)) {
			throw new IllegalArgumentException(type + " prefix is incorrect. " + key);
		}
		String name = key.substring(prefix.length());
		if (name.isBlank()) {
			throw new IllegalArgumentException("Resource " + type + " is bad. " + key);
		}
		return name;
	}

	protected String externalResourceKey1(ResourceKey key, String resourceName) {
		String prefix = switch (key) {
			case LOAD, FLAGS, MEDIA_TYPE -> prefix(key);
			default -> throw new IllegalArgumentException("key: " + key);

		};
		return prefix + resourceName;
	}

	protected String externalResourceKey2Prefix(ResourceKey key, String resourceName) {
		return prefix(key) + resourceName + sep();
	}

	protected @Nullable ResourceKey uriResourceKeyOrNull(KeyValue kv) {
		for (var key : ResourceKey.values()) {
			switch (key) {
				case LOAD, MEDIA_TYPE, FLAGS -> {
					if (kv.key().equals(uriParameterPrefix(key))) {
						return key;
					}
				}
				case PARAM, FILTER -> {
					if (kv.key().startsWith(uriParameterPrefix(key))) {
						return key;
					}
				}
			}
		}
		return null;
	}

	protected @Nullable ResourceKey resourceKeyOrNull(KeyValue kv) {
		for (var key : ResourceKey.values()) {
			if (kv.key().startsWith(prefix(key))) {
				return key;
			}
		}
		return null;
	}

	protected String formatFilterKey(KeyValuesResource resource, Filter filter) {
		String prefix = externalResourceKey2Prefix(ResourceKey.FILTER, resource.name());
		String key = prefix + filter.filter();
		String name = filter.name();
		if (!name.isBlank()) {
			key = key + sep() + name;
		}
		return key;

	}

	public static DefaultKeyValuesResourceParser of() {
		return DEFAULT;
	}

	private enum ResourceKey {

		LOAD(KeyValuesResource.KEY_LOAD), //
		FLAGS(KeyValuesResource.KEY_FLAGS), //
		MEDIA_TYPE(KeyValuesResource.KEY_MEDIA_TYPE), //
		PARAM(KeyValuesResource.KEY_PARAM), //
		FILTER(KeyValuesResource.KEY_FILTER);

		final String key;

		private ResourceKey(String key) {
			this.key = key;
		}

	}

}