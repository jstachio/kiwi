package io.jstach.kiwi.kvs;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.Variables.StaticVariables;

public sealed interface KeyValuesResource {

	public URI uri();

	public StaticVariables parameters();

	public String name();

	public @Nullable KeyValue reference();

	public @Nullable String mediaType();

	default void resourceKeyValues(BiConsumer<String, String> consumer) {
		parameters().forKeyValues(consumer);
	}

	public static Builder builder(URI uri) {
		return new Builder(uri, uriToName(uri));
	}

	public static Builder builder(String uri) {
		return builder(URI.create(uri));
	}

	private static String uriToName(URI uri) {
		return Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(uri.toASCIIString().getBytes(StandardCharsets.US_ASCII));
	}

	public class Builder {

		private URI uri;

		private String name;

		private Map<String, String> parameters = new LinkedHashMap<>();

		private EnumSet<LoadFlag> flags = EnumSet.noneOf(LoadFlag.class);

		private @Nullable KeyValue reference;

		private @Nullable String mediaType;

		Builder(URI uri, String name) {
			this.uri = uri;
			this.name = DefaultKeyValuesResource.validateResourceName(name);
		}

		public Builder copyParameters(KeyValuesResource resource) {
			ResourceKeys.copyKeys(resource, name, parameters::put);
			return this;
		}

		public Builder uri(URI uri) {
			this.uri = uri;
			return this;
		}

		public Builder name(String name) {
			String original = this.name;
			this.name = name;
			if (!name.equals(original) && !parameters.isEmpty()) {
				ResourceKeys.copyKeys(new MapStaticVariables(parameters), original, name, parameters::put);
			}
			return this;
		}

		public Builder parameter(String key, String value) {
			parameters.put(key, value);
			return this;
		}

		public Builder mediaType(String mediaType) {
			this.mediaType = mediaType;
			return this;
		}

		public Builder noInterpolation(boolean flag) {
			LoadFlag.NO_INTERPOLATE.set(flags, flag);
			return this;
		}

		public Builder noAddKeyValues(boolean flag) {
			LoadFlag.NO_ADD.set(flags, flag);
			return this;
		}

		Builder _addFlag(LoadFlag flag) {
			flags.add(flag);
			return this;
		}

		public KeyValuesResource build() {
			Map<String, String> parameters = new LinkedHashMap<>(this.parameters);
			LoadFlag.addToParameters(name, parameters, flags);
			String mediaType = this.mediaType;
			if (mediaType == null) {
				mediaType = DefaultKeyValuesMedia.mediaTypeFromParameters(name, parameters);
			}
			ResourceKeys.LOAD.setValue(name, uri.toString(), parameters::put);
			StaticVariables variables = new MapStaticVariables(parameters);
			return new DefaultKeyValuesResource(uri, name, reference, mediaType, variables);
		}

		static @Nullable Builder maybe(KeyValue reference) {
			var resource = ResourceCommand.LOAD.parse(reference);
			if (resource == null) {
				return null;
			}
			return new Builder(resource.uri(), resource.resourceName());
		}

		static @Nullable KeyValuesResource build(KeyValue keyValue, KeyValues parameters) {
			var builder = maybe(keyValue);
			if (builder == null)
				return null;
			for (var p : parameters) {
				if (p.key().startsWith("_")) {
					builder.parameter(p.key(), p.expanded());
				}
			}
			builder.reference = keyValue;
			return builder.build();
		}

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

record DefaultKeyValuesResource(URI uri, String name, @Nullable KeyValue reference, @Nullable String mediaType,
		StaticVariables parameters) implements KeyValuesResource {
	DefaultKeyValuesResource {
		validateResourceName(name);
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
