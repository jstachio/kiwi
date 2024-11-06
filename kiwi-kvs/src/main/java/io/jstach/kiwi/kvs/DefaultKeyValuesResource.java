package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.Variables.StaticVariables;

record DefaultKeyValuesResource(URI uri, String name, @Nullable KeyValue reference, @Nullable String mediaType,
		StaticVariables parameters) implements KeyValuesResource {
	DefaultKeyValuesResource {
		validateResourceName(name);
	}

	static KeyValuesResource of(KeyValuesResource.Builder builder) {
		Map<String, String> parameters = new LinkedHashMap<>(builder.parameters);
		URI uri = builder.uri;
		String name = builder.name;
		var reference = builder.reference;
		var flags = builder.flags;
		if (flags.isEmpty()) {
			var flagCSV = ResourceKeys.FLAGS.getValue(parameters);
			if (flagCSV != null) {
				flags = LoadFlag.parseCSV(flagCSV);
			}
		}
		String mediaType = builder.mediaType;
		if (mediaType == null) {
			mediaType = ResourceKeys.MEDIA_TYPE.getValue(parameters);
		}

		for (var rk : ResourceKeys.values()) {
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
		StaticVariables variables = new MapStaticVariables(parameters);
		return new DefaultKeyValuesResource(uri, name, reference, mediaType, variables);
	}

	static KeyValuesResource.Builder builder(KeyValuesResource resource) {
		var builder = new KeyValuesResource.Builder(resource.uri(), resource.name());
		ResourceKeys.copyKeys(resource, builder.parameters::put);
		return builder;
	}

	private static @Nullable Builder maybeBuilder(KeyValue reference) {
		var resource = ResourceCommand.LOAD.parse(reference);
		if (resource == null) {
			return null;
		}
		return new Builder(resource.uri(), resource.resourceName());
	}

	static @Nullable KeyValuesResource maybeOf(KeyValue keyValue, KeyValues parameters) {
		var builder = maybeBuilder(keyValue);
		if (builder == null)
			return null;
		ResourceKeys.parseKeyValues(parameters, builder.name, (rk, kv) -> {
			builder.parameters.put(rk.key, kv.expanded());
		});
		builder.reference = keyValue;
		return builder.build();
	}

	static String validateResourceName(String identifier) {
		if (identifier == null || !identifier.matches("[a-zA-Z0-9]+")) {
			throw new IllegalArgumentException(
					"Invalid identifier: must contain only alphanumeric characters (no underscores) and not be null. input: "
							+ identifier);
		}
		return identifier;
	}

	static StringBuilder describe(StringBuilder sb, KeyValuesResource resource, boolean includeRef) {
		sb.append("uri='").append(resource.uri()).append("'");
		var flags = LoadFlag.of(resource);
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

	static String describe(KeyValuesResource resource, boolean includeRef) {
		return describe(new StringBuilder(), resource, includeRef).toString();
	}
}

enum ResourceCommand {

	LOAD("load");

	ResourceCommand(String command) {
		pattern = Pattern.compile("_" + command + "_([a-zA-Z0-9]+)");
	}

	private final Pattern pattern;

	public @Nullable Resource parse(KeyValue keyValue) {
		String key = keyValue.key();
		var matcher = this.pattern.matcher(key);
		if (matcher.find()) {
			String name = matcher.group(1);
			var uri = URI.create(keyValue.expanded());
			return new Resource(this, name, uri);
		}
		return null;
	}

	record Resource(ResourceCommand command, String resourceName, URI uri) {
	}

}

// record ResourceParameters(KeyValuesResource.Builder builder) {
// public void setParameter(ResourceKeys rk, String value) {
// switch(rk) {
// case LOAD -> {
// var uri = URI.create(value);
// builder.uri(uri);
// builder.parameters.put(rk.key, value);
// }
// case FLAGS -> {
// builder.parameters.put(rk.key, value);
// }
// case MEDIA_TYPE -> {
// String mediaType = value;
// builder.mediaType(mediaType);
// builder.parameters.put(rk.key, value);
// }
// }
// }
// }
enum ResourceKeys {

	LOAD("load"), FLAGS("flags"), MEDIA_TYPE("mediaType");

	final String key;

	private final String prefix;

	private ResourceKeys(String key) {
		this.key = key;
		this.prefix = "_" + key + "_";
	}

	static @Nullable ResourceKeys find(KeyValue kv) {
		for (var key : ResourceKeys.values()) {
			if (kv.key().startsWith(key.prefix)) {
				return key;
			}
		}
		return null;
	}

	static @Nullable ResourceKeys parse(String key) {
		for (var rk : values()) {
			if (rk.key.equals(key)) {
				return rk;
			}
		}
		return null;
	}

	static void parseKeyValues(KeyValues kvs, String resourceName, BiConsumer<ResourceKeys, KeyValue> consumer) {
		DefaultKeyValuesResource.validateResourceName(resourceName);
		for (var kv : kvs) {
			for (var rk : ResourceKeys.values()) {
				if (rk.matches(kv, resourceName)) {
					consumer.accept(rk, kv);
				}
			}
		}
	}

	boolean matches(KeyValue keyValue, String resourceName) {
		return keyValue.key().equals(this.prefix + resourceName);
	}

	@Nullable
	String getValue(Map<String, String> variables) {
		return variables.get(key);
	}

	@Nullable
	String getValue(Variables variables) {
		return variables.getValue(key);
	}

	// @Nullable
	// String getValue(Variables variables, String resourceName) {
	// return variables.getValue(key(resourceName));
	// }

	@Nullable
	String getValue(KeyValuesResource resource) {
		return getValue(resource.parameters());
	}

	// void setValue(String value, BiConsumer<String, String> consumer) {
	// String key = this.key(resourceName);
	// consumer.accept(key, value);
	// }

	String key(String resourceName) {
		DefaultKeyValuesResource.validateResourceName(resourceName);
		return this.prefix + resourceName;
	}

	static void copyKeys(KeyValuesResource resource, BiConsumer<String, String> consumer) {
		resource.parameters().forKeyValues((k, v) -> {
			var rk = ResourceKeys.parse(k);
			if (rk != null) {
				consumer.accept(rk.key, v);
			}
		});
	}

	// static void copyKeys(KeyValuesResource resource, String newResourceName,
	// BiConsumer<String, String> consumer) {
	// for (var rk : ResourceKeys.values()) {
	// String value = rk.getValue(resource);
	// if (value != null) {
	// String key = rk.key(newResourceName);
	// consumer.accept(key, value);
	// }
	// }
	// }
	//
	// static void copyKeys(Variables variables, String oldResourceName, String
	// newResourceName,
	// BiConsumer<String, String> consumer) {
	// for (var rk : ResourceKeys.values()) {
	// String value = rk.getValue(variables, oldResourceName);
	// if (value != null) {
	// String key = rk.key(newResourceName);
	// consumer.accept(key, value);
	// ;
	// }
	// }
	// }

}
