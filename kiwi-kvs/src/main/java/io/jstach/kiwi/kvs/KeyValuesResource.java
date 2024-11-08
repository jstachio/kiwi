package io.jstach.kiwi.kvs;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.Variables.Parameters;

public sealed interface KeyValuesResource permits InternalKeyValuesResource {

	public URI uri();

	public Parameters parameters();

	public String name();

	public @Nullable KeyValue reference();

	public @Nullable String mediaType();

	public Builder toBuilder();

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

	public final class Builder {

		URI uri;

		String name;

		final Map<String, String> parameters;

		final EnumSet<LoadFlag> flags;

		@Nullable
		KeyValue reference;

		@Nullable
		String mediaType;

		Builder(URI uri, String name) {
			this.uri = uri;
			this.name = DefaultKeyValuesResource.validateResourceName(name);
			this.parameters = new LinkedHashMap<>();
			this.flags = EnumSet.noneOf(LoadFlag.class);
		}

		Builder(URI uri, String name, Map<String, String> parameters, EnumSet<LoadFlag> flags,
				@Nullable KeyValue reference, @Nullable String mediaType) {
			super();
			this.uri = uri;
			this.name = name;
			this.parameters = parameters;
			this.flags = flags;
			this.reference = reference;
			this.mediaType = mediaType;
		}

		public Builder uri(URI uri) {
			this.uri = uri;
			return this;
		}

		public Builder name(String name) {
			this.name = Objects.requireNonNull(name);
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

		Builder _flags(Set<LoadFlag> flags) {
			this.flags.clear();
			this.flags.addAll(flags);
			return this;
		}

		Builder _addFlag(LoadFlag flag) {
			flags.add(flag);
			return this;
		}

		public KeyValuesResource build() {
			return DefaultKeyValuesResource.of(this);
		}

		@Override
		public String toString() {
			return "KeyValuesResource.Builder[uri=" + uri + ", name=" + name + ", parameters=" + parameters + ", flags="
					+ flags + ", reference=" + reference + ", mediaType=" + mediaType + "]";
		}

	}

}

sealed interface InternalKeyValuesResource extends KeyValuesResource {

	Set<LoadFlag> loadFlags();

}

record DefaultKeyValuesResource(URI uri, //
		String name, //
		Set<LoadFlag> loadFlags, //
		@Nullable KeyValue reference, //
		@Nullable String mediaType, //
		Parameters parameters) implements InternalKeyValuesResource {

	DefaultKeyValuesResource {
		validateResourceName(name);
		loadFlags = FlagSet.copyOf(loadFlags, LoadFlag.class);
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
				sb.append("'").append(ref.key()).append("' in uri='").append(ref.meta().source().uri()).append("'");
			}
		}
		return sb;
	}

	static String describe(KeyValuesResource resource, boolean includeRef) {
		return describe(new StringBuilder(), resource, includeRef).toString();
	}
}
