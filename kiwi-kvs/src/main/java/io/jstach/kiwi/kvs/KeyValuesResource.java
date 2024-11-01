package io.jstach.kiwi.kvs;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.Variables.StaticVariables;

public sealed interface KeyValuesResource {

	public URI uri();

	public Variables parameters();

	public String name();

	public @Nullable KeyValue reference();

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

		private final URI uri;

		private final String name;

		private Map<String, String> parameters = new LinkedHashMap<>();

		private EnumSet<LoadFlag> flags = EnumSet.noneOf(LoadFlag.class);

		private @Nullable KeyValue reference;

		Builder(URI uri, String name) {
			this.uri = uri;
			this.name = validateResourceName(name);
		}

		public Builder parameter(String key, String value) {
			parameters.put(key, value);
			return this;
		}

		Builder _addFlag(LoadFlag flag) {
			flags.add(flag);
			return this;
		}

		public KeyValuesResource build() {
			Map<String, String> parameters = new LinkedHashMap<>(this.parameters);
			LoadFlag.addToParameters(name, parameters, flags);
			StaticVariables variables = new MapStaticVariables(parameters);
			return new DefaultKeyValuesResource(uri, variables, reference, name);
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

record DefaultKeyValuesResource(URI uri, Variables parameters, @Nullable KeyValue reference,
		String name) implements KeyValuesResource {
}
