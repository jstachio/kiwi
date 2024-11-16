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

/**
 * Represents a resource that contains key-value pairs to be loaded and processed. A
 * {@code KeyValuesResource} includes a URI, optional parameters, and metadata flags that
 * determine its behavior during loading.
 *
 * <p>
 * Resources can be configured with flags and parameters to indicate characteristics such
 * as whether the values are sensitive or should be used for interpolation.
 *
 * <p>
 * Example usage for building a {@code KeyValuesResource}:
 * {@snippet :
 * var resource = KeyValuesResource.builder("classpath:/config.properties")
 * 	.name("config")
 * 	.parameter("key1", "value1")
 * 	.sensitive(true)
 * 	.build();
 * }
 *
 * @see KeyValuesSource
 * @see LoadFlag
 */
public sealed interface KeyValuesResource extends NamedKeyValuesSource, KeyValueReference
		permits InternalKeyValuesResource {

	/**
	 * Returns the URI of the resource.
	 * @return the URI of the resource
	 */
	public URI uri();

	/**
	 * Returns the parameters associated with the resource.
	 * @return the parameters as {@link Parameters}
	 */
	public Parameters parameters();

	/**
	 * Returns the name of the resource.
	 * @return the name of the resource
	 */
	@Override
	public String name();

	/**
	 * Returns the {@link KeyValue} that references this resource, if any.
	 * @return the reference {@code KeyValue}, or {@code null} if not applicable
	 */
	@Override
	public @Nullable KeyValue reference();

	/**
	 * Returns the media type of the resource, if specified.
	 * @return the media type, or {@code null} if not specified
	 */
	public @Nullable String mediaType();

	/**
	 * Creates a builder based on the current state of this {@code KeyValuesResource}.
	 * @return a {@code Builder} initialized with the current state
	 */
	public Builder toBuilder();

	// TODO maybe we do not provide this.
	/**
	 * Pretty toString of resource.
	 * @return pretty print resource.
	 */
	default String description() {
		return "uri='" + uri() + "' name='" + name() + "'";
	}

	/**
	 * Creates a builder for a {@code KeyValuesResource} from the given URI.
	 * @param uri the URI for the resource
	 * @return a new {@code Builder} instance
	 */
	public static Builder builder(URI uri) {
		return new Builder(uri, uriToName(uri));
	}

	/**
	 * Creates a builder for a {@code KeyValuesResource} from the given URI string.
	 * @param uri the URI string for the resource
	 * @return a new {@code Builder} instance
	 */
	public static Builder builder(String uri) {
		return builder(URI.create(uri));
	}

	private static String uriToName(URI uri) {
		return Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(uri.toASCIIString().getBytes(StandardCharsets.US_ASCII));
	}

	/**
	 * Builder class for creating instances of {@code KeyValuesResource}.
	 */
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

		/**
		 * Sets the URI for the resource.
		 * @param uri the URI to set
		 * @return this builder instance
		 */
		public Builder uri(URI uri) {
			this.uri = uri;
			return this;
		}

		/**
		 * Sets the name for the resource.
		 * @param name the name to set
		 * @return this builder instance
		 */
		public Builder name(String name) {
			this.name = Objects.requireNonNull(name);
			return this;
		}

		/**
		 * Clears all parameters associated with the resource.
		 * @return this builder instance
		 */
		public Builder clearParameters() {
			parameters.clear();
			return this;
		}

		/**
		 * Adds a parameter key-value pair to the resource.
		 * @param key the parameter key
		 * @param value the parameter value
		 * @return this builder instance
		 */
		public Builder parameter(String key, String value) {
			parameters.put(key, value);
			return this;
		}

		/**
		 * Sets the media type for the resource.
		 * @param mediaType the media type to set
		 * @return this builder instance
		 */
		public Builder mediaType(String mediaType) {
			this.mediaType = mediaType;
			return this;
		}

		/**
		 * Sets or clears the {@link LoadFlag#NO_INTERPOLATE} flag.
		 * @param flag whether to set or clear the flag
		 * @return this builder instance
		 */
		public Builder noInterpolation(boolean flag) {
			LoadFlag.NO_INTERPOLATE.set(flags, flag);
			return this;
		}

		/**
		 * Sets or clears the {@link LoadFlag#NO_ADD} flag.
		 * @param flag whether to set or clear the flag
		 * @return this builder instance
		 */
		public Builder noAddKeyValues(boolean flag) {
			LoadFlag.NO_ADD.set(flags, flag);
			return this;
		}

		/**
		 * Marks the resource as sensitive, meaning its values should not be printed.
		 * @param flag whether to mark the resource as sensitive
		 * @return this builder instance
		 */
		public Builder sensitive(boolean flag) {
			LoadFlag.SENSITIVE.set(flags, flag);
			return this;
		}

		/**
		 * Marks the resource as not required.
		 * @param flag true make the resource not required.
		 * @return this
		 */
		public Builder noRequire(boolean flag) {
			LoadFlag.NO_REQUIRE.set(flags, flag);
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

		/**
		 * Builds and returns a {@code KeyValuesResource} based on the current builder
		 * state.
		 * @return a new {@code KeyValuesResource} instance
		 */
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

	boolean isRedacted();

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
		return KeyValuesSource.validateName(identifier);
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

	@Override
	public boolean isRedacted() {
		// TODO hmm if the resource has sensitive info should its URI be redacated as
		// well?
		// if (LoadFlag.SENSITIVE.isSet(loadFlags())) {
		// return true;
		// }
		var ref = this.reference();
		if (ref == null) {
			return false;
		}
		return ref.isSensitive();
	}

}
