package io.jstach.kiwi.kvs;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesFilter.Filter;
import io.jstach.kiwi.kvs.Variables.Parameters;

/**
 * Represents a resource that contains key-value pairs to be loaded and processed. A
 * {@code KeyValuesResource} includes a URI, optional parameters, and metadata flags that
 * determine its behavior during loading.
 * <p>
 * Resources can have special keys associated with them. The most important key is the
 * load key ({@link #KEY_LOAD}), which assigns a name to the resource. The resource name
 * is the text following the load key prefix, and the value of the key should be a URI.
 * </p>
 * <p>
 * Other keys can be specified in two ways:
 * </p>
 * <ol>
 * <li>As query parameters in the resource's URI (these keys will be removed before the
 * resource is loaded).</li>
 * <li>As additional key-value pairs within the same resource, using the resource name as
 * part of the key (they keys will be removed from the final result).</li>
 * </ol>
 *
 * <em>The parameters are combined from both the URI and key value pairs but the key value
 * pairs take prededence on collision.</em> One advantage that URI query keys have is that
 * they do not need the resource name as that can be deduced.
 *
 * <table class="table">
 * <caption><strong>Resource Keys using default syntax</strong></caption> <thead>
 * <tr>
 * <th>Constant</th>
 * <th>Resource Key</th>
 * <th>URI Query Key</th>
 * <th>Description</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@link io.jstach.kiwi.kvs.KeyValuesResource#KEY_LOAD}</td>
 * <td><code>_load_[resource]</code></td>
 * <td><strong>N/A</strong></td>
 * <td>Specifies a resource to load. The resource name is defined as the content following
 * <code>_load_</code>. It should be alphanumeric with no spaces.</td>
 * </tr>
 * <tr>
 * <td>{@link io.jstach.kiwi.kvs.KeyValuesResource#KEY_FLAGS}</td>
 * <td><code>_flags_[resource]</code></td>
 * <td><code>_flags</code></td>
 * <td>Defines flags associated with a resource. Replace <code>[resource]</code> with the
 * actual resource name.</td>
 * </tr>
 * <tr>
 * <td>{@link io.jstach.kiwi.kvs.KeyValuesResource#KEY_MEDIA_TYPE}</td>
 * <td><code>_mediaType_[resource]</code></td>
 * <td><code>_mediaType</code></td>
 * <td>Specifies the media type of a resource. Replace <code>[resource]</code> with the
 * actual resource name.</td>
 * </tr>
 * <tr>
 * <td>{@link io.jstach.kiwi.kvs.KeyValuesResource#KEY_PARAM}</td>
 * <td><code>_param_[resource]_[name]</code></td>
 * <td><code>_param_[name]</code></td>
 * <td>Defines parameters associated with a resource. Replace <code>[resource]</code> with
 * the actual resource name, and <code>[name]</code> with the parameter name, which should
 * be alphanumeric with no spaces.</td>
 * </tr>
 * <tr>
 * <td>{@link io.jstach.kiwi.kvs.KeyValuesResource#KEY_FILTER}</td>
 * <td><code>_filter_[resource]_[filter]</code></td>
 * <td><code>_filter_[filter]</code></td>
 * <td>Specifies filters to apply to a resource. Replace <code>[resource]</code> with the
 * actual resource name, and <code>[filter]</code> with the filter identifier, both of
 * which should be alphanumeric with no spaces.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * <table class="table">
 * <caption><strong>Resource loading flags</strong></caption> <thead>
 * <tr>
 * <th>Flag Value</th>
 * <th>Description</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@value io.jstach.kiwi.kvs.KeyValuesResource#FLAG_NO_REQUIRE}</td>
 * <td>Indicates that the resource is optional and no error should occur if it is not
 * found. Synonym: {@value io.jstach.kiwi.kvs.KeyValuesResource#FLAG_OPTIONAL}</td>
 * </tr>
 * <tr>
 * <td>{@value io.jstach.kiwi.kvs.KeyValuesResource#FLAG_SENSITIVE}</td>
 * <td>Marks the resource as containing sensitive key-value pairs, which should not be
 * displayed or included in output such as <code>toString</code>.</td>
 * </tr>
 * <tr>
 * <td>{@value io.jstach.kiwi.kvs.KeyValuesResource#FLAG_NO_ADD}</td>
 * <td>Indicates that the key-value pairs from the resource should only be added to
 * variables and not to the final resolved key-value set.</td>
 * </tr>
 * <tr>
 * <td>{@value io.jstach.kiwi.kvs.KeyValuesResource#FLAG_NO_INTERPOLATE}</td>
 * <td>Disables interpolation entirely for key-value pairs loaded from this resource.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * <em>Note: flags can be negated textual by appending <code>NO_</code> or removing
 * <code>NO_</code></em>
 *
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
	 * Indicates that the resource is optional and no error should occur if it is not
	 * found.
	 */
	public static final String FLAG_NO_REQUIRE = "NO_REQUIRE";

	/**
	 * A synonym for {@link #FLAG_NO_REQUIRE}.
	 */
	public static final String FLAG_OPTIONAL = "OPTIONAL";

	/**
	 * A synonym for {@link #FLAG_NO_REQUIRE}.
	 */
	public static final String FLAG_NOT_REQUIRED = "NOT_REQUIRED";

	/**
	 * Indicates that the resource should not be empty. Typically used to enforce that the
	 * resource contains at least one key-value pair.
	 */
	public static final String FLAG_NO_EMPTY = "NO_EMPTY";

	/**
	 * Specifies that the resource is locked and its properties should not be overridden.
	 * This is distinct from {@link #FLAG_NO_REPLACE}, which has different semantics.
	 */
	public static final String FLAG_LOCK = "LOCK";

	/**
	 * Ensures that the resource can only add new key-value pairs and cannot replace
	 * existing ones.
	 */
	public static final String FLAG_NO_REPLACE = "NO_REPLACE";

	/**
	 * Indicates that the key-value pairs from the resource should only be added to
	 * variables and not to the final resolved key-value set.
	 */
	public static final String FLAG_NO_ADD = "NO_ADD";

	/**
	 * Indicates that key-value pairs added to variables should not be used for
	 * interpolation purposes.
	 */
	public static final String FLAG_NO_ADD_VARIABLES = "NO_ADD_VARIABLES";

	/**
	 * Prevents the resource from invoking `_load` calls to load additional child
	 * resources.
	 */
	public static final String FLAG_NO_LOAD_CHILDREN = "NO_LOAD_CHILDREN";

	/**
	 * Disables interpolation entirely for key-value pairs loaded from this resource.
	 */
	public static final String FLAG_NO_INTERPOLATE = "NO_INTERPOLATE";

	/**
	 * Marks the resource as containing sensitive key-value pairs, which should not be
	 * displayed or included in output such as `toString`.
	 */
	public static final String FLAG_SENSITIVE = "SENSITIVE";

	/**
	 * Specifies that the resource should not be reloaded once it has been loaded.
	 */
	public static final String FLAG_NO_RELOAD = "NO_RELOAD";

	/**
	 * Indicates that the resource's properties should inherit from parent or default
	 * configurations.
	 */
	public static final String FLAG_INHERIT = "INHERIT";

	/**
	 * Represents the key for specifying resources to load.
	 */
	public static final String KEY_LOAD = "load";

	/**
	 * Represents the key for specifying flags associated with a resource.
	 */
	public static final String KEY_FLAGS = "flags";

	/**
	 * Represents the key for specifying the media type of a resource.
	 */
	public static final String KEY_MEDIA_TYPE = "mediaType";

	/**
	 * Represents the key for specifying parameters associated with a resource.
	 */
	public static final String KEY_PARAM = "param";

	/**
	 * Represents the key for specifying filters to apply to the resource.
	 */
	public static final String KEY_FILTER = "filter";

	/**
	 * Returns the URI of the resource.
	 * @return the URI of the resource
	 */
	@Override
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
		return "uri='" + uri() + "' name='" + name() + "'" + ", reference="
				+ KeyValueReference.toStringRef(this.reference());
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

		final List<Filter> filters;

		@Nullable
		KeyValue reference;

		@Nullable
		String mediaType;

		Builder(URI uri, String name) {
			this.uri = uri;
			this.name = DefaultKeyValuesResource.validateResourceName(name);
			this.parameters = new LinkedHashMap<>();
			this.flags = EnumSet.noneOf(LoadFlag.class);
			this.filters = new ArrayList<>();
		}

		Builder(URI uri, String name, Map<String, String> parameters, EnumSet<LoadFlag> flags,
				@Nullable KeyValue reference, @Nullable String mediaType, List<Filter> filters) {
			super();
			this.uri = uri;
			this.name = name;
			this.parameters = parameters;
			this.flags = flags;
			this.reference = reference;
			this.mediaType = mediaType;
			this.filters = filters;
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

		Builder _addFilter(Filter filter) {
			filters.add(filter);
			return this;
		}

		/**
		 * Parses a comma-separated string of resource flags and adds or removes the flags
		 * from this builder.
		 *
		 * <p>
		 * See the constants on this class for the available flag names. Flags are
		 * case-insensitive and can be negated or reversed by prefixing them with
		 * <code>"NO_"</code> or <code>"NOT_"</code>, or by removing these prefixes if
		 * they are already present. If a flag is specified multiple times in the input
		 * string, the last occurrence takes precedence.
		 * </p>
		 * <p>
		 * The following demonstrates how to add and reverse resource flags using this
		 * method:
		 * </p>
		 * {@snippet :
		 * var builder = KeyValuesResource.builder(URI.create("classpath:/config.properties"));
		 * builder.addFlags("no_require,no_interpolate,sensitive");
		 *
		 * // The flags "NO_REQUIRE", "NO_INTERPOLATE", and "SENSITIVE" are added to the builder.
		 * }
		 * @param csv a comma-separated, case-insensitive string of flag names
		 * @return this builder instance for chaining
		 */
		public Builder addFlags(String csv) {
			LoadFlag.parseCSV(this.flags, csv);
			return this;
		}

		/**
		 * Builds and returns a {@code KeyValuesResource} based on the current builder
		 * state.
		 * @return a new {@code KeyValuesResource} instance
		 */
		public KeyValuesResource build() {
			return DefaultKeyValuesResource.of(this, false);
		}

		InternalKeyValuesResource buildNormalized() {
			return DefaultKeyValuesResource.of(this, true);
		}

		// For unit test
		InternalKeyValuesResource build(KeyValuesResourceParser parser) {
			return parser.normalizeResource(this.build());
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

	public List<Filter> filters();

	boolean normalized();

}

record DefaultKeyValuesResource(URI uri, //
		String name, //
		Set<LoadFlag> loadFlags, //
		@Nullable KeyValue reference, //
		@Nullable String mediaType, //
		Parameters parameters, List<Filter> filters, boolean normalized) implements InternalKeyValuesResource {

	DefaultKeyValuesResource {
		validateResourceName(name);
		loadFlags = FlagSet.copyOf(loadFlags, LoadFlag.class);
		filters = List.copyOf(filters);
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
		var filters = resource.filters();
		var b = new KeyValuesResource.Builder(uri, name, parameters, flags, ref, mediaType, filters);
		return b;
	}

	static InternalKeyValuesResource of(KeyValuesResource.Builder builder, boolean normalized) {
		Map<String, String> parameters = new LinkedHashMap<>(builder.parameters);
		URI uri = builder.uri;
		String name = builder.name;
		var reference = builder.reference;
		var loadFlags = builder.flags;
		var mediaType = builder.mediaType;
		Parameters variables = Parameters.of(parameters);
		var filters = builder.filters;
		return new DefaultKeyValuesResource(uri, name, loadFlags, reference, mediaType, variables, filters, normalized);
	}

	// @Override
	// public List<Filter> filters() {
	// var csv = parameters.getValue("filter");
	// if (csv == null) {
	// return List.of();
	// }
	// List<String> filters = new ArrayList<>();
	// DefaultKeyValuesMedia.parseCSV(csv, filters::add);
	// return List.copyOf(filters);
	// }

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
