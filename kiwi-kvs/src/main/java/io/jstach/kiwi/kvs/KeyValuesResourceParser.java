package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;


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
 */
sealed interface KeyValuesResourceParser {

	/**
	 * Parses a {@link KeyValues} stream and returns a list of {@link KeyValuesResource}
	 * objects representing resources that should be loaded.
	 * @param keyValues the key-values to parse for resource references
	 * @return a list of resources parsed from the key-values
	 */
	List<KeyValuesResource> parseResources(KeyValues keyValues);

	/**
	 * Filters out the key-values that are related to resource loading from a given
	 * {@link KeyValues} stream.
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

}

/*
 * The idea here is to keep all the parsing logic separated from the core domain so that
 * we can switch the key value scheme of loading.
 */
enum DefaultKeyValuesResourceParser implements KeyValuesResourceParser {

	DEFAULT;

	private final Pattern pattern = Pattern.compile("_" + ResourceKey.LOAD.key + "_([a-zA-Z0-9]+)");

	public static DefaultKeyValuesResourceParser of() {
		return DEFAULT;
	}

	@Override
	public void formatResource(KeyValuesResource resource, BiConsumer<String, String> consumer) {
		String name = resource.name();
		var uri = resource.uri();
		var flags = DefaultKeyValuesResource.loadFlags(resource);
		String mediaType = resource.mediaType();
		for (var rk : ResourceKey.values()) {
			String key = switch (rk) {
				case LOAD, FLAGS, MEDIA_TYPE -> externalResourceKey1(rk, name);
				case PARAM -> externalResourceKey2Prefix(rk, name);
			};
			switch (rk) {
				case LOAD -> {
					consumer.accept(key, uri.toString());
				}
				case FLAGS -> {
					if (!flags.isEmpty()) {
						var flagsCsv = LoadFlag.toCSV(flags.stream());
						consumer.accept(key, flagsCsv);
					}
				}
				case MEDIA_TYPE -> {
					if (mediaType != null) {
						consumer.accept(key, mediaType);
					}
				}
				case PARAM -> {
					resource.parameters().forKeyValues((k, v) -> {
						consumer.accept(key + k, v);
					});
				}
			}
		}
	}

	@Override
	public List<KeyValuesResource> parseResources(KeyValues keyValues) {
		List<KeyValuesResource> resources = new ArrayList<>();
		for (var kv : keyValues) {
			var r = parseOrNull(kv, keyValues);
			if (r != null) {
				resources.add(r);
			}
		}
		return List.copyOf(resources);
	}

	@Override
	public KeyValues filterResources(KeyValues keyValues) {
		return keyValues.filter(this::filter);
	}

	private @Nullable KeyValuesResource parseOrNull(KeyValue keyValue, KeyValues keyValues) {
		var builder = builderOrNull(keyValue);
		if (builder == null)
			return null;
		parseKeyValues(keyValues, builder.name, (rk, kv) -> {
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
					String name = paramName(builder.name, kv.key());
					builder.parameter(name, kv.expanded());
				}
			}
		});
		builder.reference = keyValue;
		return builder.build();
	}

	private  KeyValuesResource. @Nullable Builder builderOrNull(KeyValue reference) {
		var resource = parseOrNull(reference);
		if (resource == null) {
			return null;
		}
		return new KeyValuesResource.Builder(resource.uri(), resource.resourceName());
	}

	private boolean filter(KeyValue kv) {
		return resourceKeyOrNull(kv) == null;
	}

	private @Nullable Resource parseOrNull(KeyValue keyValue) {
		String key = keyValue.key();
		var matcher = this.pattern.matcher(key);
		if (matcher.find()) {
			String name = matcher.group(1);
			if (name == null) {
				throw new NullPointerException("Pattern did not have group 1. pattern=" + this.pattern);
			}
			var uri = URI.create(keyValue.expanded());
			return new Resource(name, uri);
		}
		return null;
	}

	
	private @Nullable ResourceKey resourceKeyOrNull(KeyValue kv) {
		for (var key : ResourceKey.values()) {
			if (kv.key().startsWith(key.prefix)) {
				return key;
			}
		}
		return null;
	}

	private String externalResourceKey1(ResourceKey key, String resourceName) {
		return key.prefix + resourceName;
	}

	private String externalResourceKey2Prefix(ResourceKey key, String resourceName) {
		return key.prefix + resourceName + "_";
	}

	private String paramName(String resourceName, String key) {
		String prefix = externalResourceKey2Prefix(ResourceKey.PARAM, resourceName);
		if (!key.startsWith(prefix)) {
			throw new IllegalArgumentException("Parameter prefix is incorrect. " + key);
		}
		String name = key.substring(prefix.length());
		if (name.isBlank()) {
			throw new IllegalArgumentException("Resource Parameter is bad. " + key);
		}
		return name;
	}

	private boolean keyValueMatches(ResourceKey rk, KeyValue kv, String resourceName) {
		String key = kv.key();
		return switch (rk) {
			case PARAM -> key.startsWith(externalResourceKey2Prefix(rk, resourceName));
			case LOAD, FLAGS, MEDIA_TYPE -> externalResourceKey1(rk, resourceName).equals(key);
		};
	}

	private void parseKeyValues(KeyValues kvs, String resourceName, BiConsumer<ResourceKey, KeyValue> consumer) {
		// DefaultKeyValuesResource.validateResourceName(resourceName);
		for (var kv : kvs) {
			for (var rk : ResourceKey.values()) {
				if (keyValueMatches(rk, kv, resourceName)) {
					consumer.accept(rk, kv);
				}
			}
		}
	}

	private record Resource(String resourceName, URI uri) {
	}

	private enum ResourceKey {

		LOAD("load"), //
		FLAGS("flags"), //
		MEDIA_TYPE("mediaType"), //
		PARAM("param");

		final String key;

		final String prefix;

		private ResourceKey(String key) {
			this.key = key;
			this.prefix = "_" + key + "_";
		}

	}

}