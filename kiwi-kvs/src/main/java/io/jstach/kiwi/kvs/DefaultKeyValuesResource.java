package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.Variables.Parameters;

record DefaultKeyValuesResource(URI uri, //
		String name, //
		Set<LoadFlag> loadFlags, //
		@Nullable KeyValue reference, //
		@Nullable String mediaType, //
		Parameters parameters) implements InternalKeyValuesResource {

	DefaultKeyValuesResource {
		validateResourceName(name);
	}

	public static String validateResourceName(String identifier) {
		return DefaultKeyValuesResourceParser.validateResourceName(identifier);
	}

	@Override
	public Builder toBuilder() {
		var resource = this;
		var uri = resource.uri();
		var name = resource.name();
		var parameters = new LinkedHashMap<String, String>();
		resource.parameters().forKeyValues(parameters::put);
		var flags = FlagSet.enumSetOf(LoadFlag.class, resource.loadFlags());
		var ref = resource.reference();
		var mediaType = resource.mediaType();
		var b = new KeyValuesResource.Builder(uri, name, parameters, flags, ref, mediaType);
		return b;
	}

	static KeyValuesResource of(KeyValuesResource.Builder builder) {
		Map<String, String> parameters = new LinkedHashMap<>(builder.parameters);
		URI uri = builder.uri;
		String name = builder.name;
		var reference = builder.reference;
		var loadFlags = builder.flags;
		var mediaType = builder.mediaType;
		// DefaultKeyValuesResourceParser.putParameters(uri, loadFlags, mediaType,
		// parameters::put);
		Parameters variables = Parameters.of(parameters);
		return new DefaultKeyValuesResource(uri, name, loadFlags, reference, mediaType, variables);
	}

	static Set<LoadFlag> loadFlags(KeyValuesResource resource) {
		var defaultResource = switch (resource) {
			case DefaultKeyValuesResource d -> d;
		};
		return defaultResource.loadFlags();
	}

	static StringBuilder describe(StringBuilder sb, KeyValuesResource resource, boolean includeRef) {

		sb.append("uri='").append(resource.uri()).append("'");
		var flags = loadFlags(resource);
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

sealed interface InternalKeyValuesResource extends KeyValuesResource {

	Set<LoadFlag> loadFlags();

}
