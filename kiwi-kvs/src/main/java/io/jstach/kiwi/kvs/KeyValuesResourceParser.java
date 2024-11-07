package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValuesResource.Builder;

sealed interface KeyValuesResourceParser {

	List<KeyValuesResource> parseResources(KeyValues keyValues);

	KeyValues filterResources(KeyValues keyValues);

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

	public static String validateResourceName(String identifier) {
		if (identifier == null || !identifier.matches("[a-zA-Z0-9]+")) {
			throw new IllegalArgumentException(
					"Invalid identifier: must contain only alphanumeric characters (no underscores) and not be null. input: "
							+ identifier);
		}
		return identifier;
	}

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
		return KeyValues.of(keyValues.stream().filter(this::filter).toList());
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

	private @Nullable Builder builderOrNull(KeyValue reference) {
		var resource = parse(reference);
		if (resource == null) {
			return null;
		}
		return new Builder(resource.uri(), resource.resourceName());
	}

	private boolean filter(KeyValue kv) {
		return resourceKeyOrNull(kv) == null;
	}

	private Resource parse(KeyValue keyValue) {
		String key = keyValue.key();
		var matcher = this.pattern.matcher(key);
		if (matcher.find()) {
			String name = matcher.group(1);
			var uri = URI.create(keyValue.expanded());
			return new Resource(name, uri);
		}
		return null;
	}

	@Nullable
	private ResourceKey resourceKeyOrNull(KeyValue kv) {
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
		if (!resourceName.startsWith(prefix)) {
			throw new IllegalArgumentException("Parameter prefix is incorrect. " + key);
		}
		String name = resourceName.substring(prefix.length());
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