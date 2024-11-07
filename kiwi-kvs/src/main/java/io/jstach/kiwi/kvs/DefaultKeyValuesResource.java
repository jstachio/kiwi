package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValuesResource.Builder;
import io.jstach.kiwi.kvs.Variables.StaticVariables;

record DefaultKeyValuesResource(URI uri, String name, @Nullable KeyValue reference, @Nullable String mediaType,
		StaticVariables parameters) implements InternalKeyValuesResource {
	DefaultKeyValuesResource {
		validateResourceName(name);
	}

	public static String validateResourceName(String identifier) {
		return ParseStrategy.of().validateResourceName(identifier);

	}

	@Override
	public void resourceKeyValues(BiConsumer<String, String> consumer) {
		ParseStrategy.of().resourceKeyValues(this, consumer);
	}

	@Override
	public Builder toBuilder() {
		return ParseStrategy.of().parseResource(this);
	}

	static KeyValuesResource of(KeyValuesResource.Builder builder) {
		return ParseStrategy.of().parseBuilder(builder);
	}

	static StringBuilder describe(StringBuilder sb, KeyValuesResource resource, boolean includeRef) {
		sb.append("uri='").append(resource.uri()).append("'");
		var flags = ParseStrategy.of().parseLoadFlags(resource);
		if (!flags.isEmpty()) {
			sb.append(" flags=").append(flags);
		}
		if (includeRef) {
			var ref = resource.reference();
			if (ref != null) {
				sb.append(" specified with key: ");
				sb.append("'").append(ref.key()).append("' in uri='").append(ref.source().uri()).append("'");
			}
		}
		return sb;
	}

	@Override
	public Set<LoadFlag> loadFlags() {
		return ParseStrategy.of().parseLoadFlags(this);
	}

	static String describe(KeyValuesResource resource, boolean includeRef) {
		return describe(new StringBuilder(), resource, includeRef).toString();
	}
}

sealed interface InternalKeyValuesResource extends KeyValuesResource {

	Set<LoadFlag> loadFlags();

}

/*
 * The idea here is to keep all the parsing logic separated from the core domain so that
 * we can switch the key value scheme of loading.
 */
enum ParseStrategy {

	DEFAULT;

	private final Pattern pattern = Pattern.compile("_" + ResourceKey.LOAD.key + "_([a-zA-Z0-9]+)");

	public static ParseStrategy of() {
		return DEFAULT;
	}

	public String validateResourceName(String identifier) {
		if (identifier == null || !identifier.matches("[a-zA-Z0-9]+")) {
			throw new IllegalArgumentException(
					"Invalid identifier: must contain only alphanumeric characters (no underscores) and not be null. input: "
							+ identifier);
		}
		return identifier;
	}

	public void resourceKeyValues(KeyValuesResource resource, BiConsumer<String, String> consumer) {
		resource.parameters().forKeyValues((k, v) -> {
			var rk = ResourceKey.findOrNull(k);
			if (rk != null) {
				consumer.accept(externalResourceKey(rk, resource.name()), v);
			}
		});
	}

	public KeyValuesResource parseBuilder(KeyValuesResource.Builder builder) {
		Map<String, String> parameters = new LinkedHashMap<>(builder.parameters);
		URI uri = builder.uri;
		String name = builder.name;
		var reference = builder.reference;
		var flags = builder.flags;
		if (flags.isEmpty()) {
			var flagCSV = ResourceKey.FLAGS.getValue(parameters);
			if (flagCSV != null) {
				flags = LoadFlag.parseCSV(flagCSV);
			}
		}
		String mediaType = builder.mediaType;
		if (mediaType == null) {
			mediaType = ResourceKey.MEDIA_TYPE.getValue(parameters);
		}

		putParameters(parameters, uri, flags, mediaType);
		StaticVariables variables = new MapStaticVariables(parameters);
		return new DefaultKeyValuesResource(uri, name, reference, mediaType, variables);
	}

	protected void putParameters(Map<String, String> parameters, URI uri, Set<LoadFlag> flags, String mediaType) {
		for (var rk : ResourceKey.values()) {
			switch (rk) {
				case LOAD -> {
					parameters.put(rk.key, uri.toString());
				}
				case FLAGS -> {
					if (!flags.isEmpty()) {
						var flagsCsv = LoadFlag.toCSV(flags.stream());
						parameters.put(rk.key, flagsCsv);
					}
				}
				case MEDIA_TYPE -> {
					if (mediaType != null) {
						parameters.put(rk.key, mediaType);
					}
				}
			}
		}
	}

	public KeyValuesResource.Builder parseResource(KeyValuesResource _resource) {
		var resource = (InternalKeyValuesResource) _resource;
		var uri = resource.uri();
		var name = resource.name();
		var parameters = new LinkedHashMap<String, String>();
		resource.parameters().forKeyValues(parameters::put);
		var flags = FlagSet.enumSetOf(LoadFlag.class, resource.loadFlags());
		var ref = resource.reference();
		var mediaType = _resource.mediaType();
		var b = new KeyValuesResource.Builder(uri, name, parameters, flags, ref, mediaType);
		return b;
	}

	public Set<LoadFlag> parseLoadFlags(KeyValuesResource resource) {
		EnumSet<LoadFlag> flags = EnumSet.noneOf(LoadFlag.class);
		String v = ResourceKey.FLAGS.getValue(resource);
		if (v == null) {
			return flags;
		}
		DefaultKeyValuesMedia.parseCSV(v, k -> LoadFlag.parse(flags, k));
		return flags;
	}

	public @Nullable KeyValuesResource parseOrNull(KeyValue keyValue, KeyValues keyValues) {
		var builder = builderOrNull(keyValue);
		if (builder == null)
			return null;
		parseKeyValues(keyValues, builder.name, (rk, kv) -> {
			builder.parameters.put(rk.key, kv.expanded());
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

	public KeyValues filter(KeyValues keyValues) {
		return KeyValues.of(keyValues.stream().filter(this::filter).toList());
	}

	private boolean filter(KeyValue kv) {
		return resourceKeyOrNull(kv) == null;
	}

	protected Resource parse(KeyValue keyValue) {
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
	ResourceKey resourceKeyOrNull(KeyValue kv) {
		for (var key : ResourceKey.values()) {
			if (kv.key().startsWith(key.prefix)) {
				return key;
			}
		}
		return null;
	}

	String externalResourceKey(ResourceKey key, String resourceName) {
		return key.prefix + resourceName;
	}

	boolean keyValueMatches(ResourceKey rk, KeyValue kv, String resourceName) {
		String key = kv.key();
		return externalResourceKey(rk, resourceName).equals(key);
	}

	void parseKeyValues(KeyValues kvs, String resourceName, BiConsumer<ResourceKey, KeyValue> consumer) {
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

	enum ResourceKey {

		LOAD("load"), //
		FLAGS("flags"), //
		MEDIA_TYPE("mediaType");

		final String key;

		final String prefix;

		private ResourceKey(String key) {
			this.key = key;
			this.prefix = "_" + key + "_";
		}

		@Nullable
		String getValue(Map<String, String> variables) {
			return variables.get(key);
		}

		@Nullable
		String getValue(Variables variables) {
			return variables.getValue(key);
		}

		@Nullable
		String getValue(KeyValuesResource resource) {
			return getValue(resource.parameters());
		}

		@Nullable
		static ResourceKey findOrNull(String key) {
			for (var rk : values()) {
				if (rk.key.equals(key)) {
					return rk;
				}
			}
			return null;
		}

	}

}
