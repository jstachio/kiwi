package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesFilter.Filter;

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
 *
 * @apiNote This is not public for a variety of complex reasons that maybe revisited.
 */
sealed interface KeyValuesResourceParser {

	/**
	 * Parses a {@link KeyValues} stream and returns a list of {@link KeyValuesResource}
	 * objects representing resources that should be loaded.
	 * @param keyValues the key-values to parse for resource references
	 * @return a list of resources parsed from the key-values
	 * @throws KeyValuesResourceParserException if key values has an incorrect resource
	 * key format.
	 */
	List<? extends InternalKeyValuesResource> parseResources(KeyValues keyValues)
			throws KeyValuesResourceParserException;

	// TODO we should probably make a ParsedKeyValuesResource or
	// NormalizedKeyValuesResource
	// For now Internal is doing double duty.
	/**
	 * Because the resource URI might contain meta data we may need to reparse to build
	 * the correct resource representation.
	 * @param resource resoure that may need to be reparse.
	 * @return internal key values resource that is normalized.
	 * @throws KeyValuesResourceParserException if key values has an incorrect resource
	 * key format.
	 */
	InternalKeyValuesResource normalizeResource(KeyValuesResource resource) throws KeyValuesResourceParserException;

	/**
	 * Filters out the key-values that are related to resource loading from a given
	 * {@link KeyValues} stream. This is to remove all the resource meta key value pairs
	 * that are not needed in the final result as well as avoid collisions in resources
	 * loaded after.
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

	/**
	 * Formats a resource parameter key how it is represented as key in the parsed
	 * resource.
	 * @param resource resource to base the full parameter name on.
	 * @param parameterName the short parameter from
	 * {@link KeyValuesResource#parameters()}.
	 * @return FQ parameter name.
	 */
	String formatParameterKey(KeyValuesResource resource, String parameterName);

}

class KeyValuesResourceParserException extends Exception {

	private static final long serialVersionUID = 2616767401332338068L;

	public KeyValuesResourceParserException(String message) {
		super(message);
	}

	public KeyValuesResourceParserException(String message, Throwable cause) {
		super(message, cause);
	}

}

record LoadResource(String name, URI uri, KeyValue keyValue) {
}

/*
 * The idea here is to keep all the parsing logic separated from the core domain so that
 * we can switch the key value scheme of loading.
 */
enum DefaultKeyValuesResourceParser implements KeyValuesResourceParser {

	DEFAULT {

	};

	// Formatting resource

	@Override
	public void formatResource(KeyValuesResource _resource, BiConsumer<String, String> consumer) {

		InternalKeyValuesResource resource = switch (_resource) {
			case InternalKeyValuesResource ir -> ir;
		};

		for (var rk : ResourceKey.values()) {
			String resourceName = resource.name();
			var uri = resource.uri();
			String alias = rk.formatAlias();
			switch (rk) {
				case LOAD -> {
					var key = externalResourceKey1(rk, alias, resourceName);
					consumer.accept(key, uri.toString());
				}
				case FLAGS -> {
					var flags = resource.loadFlags();
					var key = externalResourceKey1(rk, alias, resourceName);
					if (!flags.isEmpty()) {
						var flagsCsv = LoadFlag.toCSV(flags.stream());
						consumer.accept(key, flagsCsv);
					}
				}
				case MEDIA_TYPE -> {
					var key = externalResourceKey1(rk, alias, resourceName);
					String mediaType = resource.mediaType();
					if (mediaType != null) {
						consumer.accept(key, mediaType);
					}
				}
				case PARAM -> {
					// var key = externalResourceKey2Prefix(rk, resourceName);
					resource.parameters().forKeyValues((k, v) -> {
						var key = formatParameterKey(resource, k);
						consumer.accept(key, v);
					});
				}
				case FILTER -> {
					var filters = resource.filters();
					for (var f : filters) {
						var key = formatFilterKey(resource, f);
						var value = f.expression();
						consumer.accept(key, value);
					}
				}
			}
		}
	}

	@Override
	public String formatParameterKey(KeyValuesResource resource, String parameterName) {
		String alias = ResourceKey.PARAM.formatAlias();
		return externalResourceKey2Prefix(ResourceKey.PARAM, alias, resource.name()) + parameterName;
	}

	// Filtering resource

	@Override
	public KeyValues filterResources(KeyValues keyValues) {
		return keyValues.filter(this::filter);
	}

	private boolean filter(KeyValue kv) {
		try {
			return resourceKeyOrNull(kv) == null;
		}
		catch (KeyValuesResourceParserException e) {
			return true;
		}
	}

	// Parsing Resource

	@Override
	public List<? extends InternalKeyValuesResource> parseResources(KeyValues keyValues)
			throws KeyValuesResourceParserException {
		List<InternalKeyValuesResource> resources = new ArrayList<>();
		for (var kv : keyValues) {
			var r = parseResourceOrNull(kv, keyValues);
			if (r != null) {
				resources.add(r);
			}
		}
		return List.copyOf(resources);
	}

	// Parsing Resource

	private void parseKeyValues(KeyValuesResource.Builder builder, LoadResource resource, KeyValues keyValues)
			throws KeyValuesResourceParserException {
		for (var kv : keyValues) {
			var rkv = resourceKeyOrNull(kv);
			if (rkv != null) {
				if (resource.name().equals(rkv.resourceName())) {
					parseResourceKey(builder, rkv);
				}
			}
		}
	}

	private void parseResourceKey(KeyValuesResource.Builder builder, ResourceKeyValue rkv)
			throws KeyValuesResourceParserException {
		var type = rkv.type();
		var kv = rkv.keyValue();
		switch (type) {
			case LOAD -> {
			}
			case MEDIA_TYPE -> {
				builder.mediaType(kv.expanded());
			}
			case FLAGS -> {
				builder._flags(LoadFlag.parseCSV(kv.expanded()));
			}
			case PARAM -> {
				String name = rkv.paramName();
				if (name == null) {
					throw new IllegalStateException("bug");
				}
				builder.parameter(name, kv.expanded());
			}
			case FILTER -> {
				String filter = rkv.paramName();
				if (filter == null) {
					throw new IllegalStateException("bug");
				}
				String expression = kv.value();
				var f = new Filter(filter, expression, "");
				builder._addFilter(f);
			}
		}
	}

	private @Nullable InternalKeyValuesResource parseResourceOrNull(KeyValue keyValue, KeyValues keyValues)
			throws KeyValuesResourceParserException {
		var resource = parseLoadOrNull(keyValue);
		if (resource == null) {
			return null;
		}
		var builder = new KeyValuesResource.Builder(resource.uri(), resource.name());
		parseURI(builder, resource.uri());
		parseKeyValues(builder, resource, keyValues);
		builder.reference = keyValue;
		return builder.buildNormalized();
	}

	@Override
	public InternalKeyValuesResource normalizeResource(KeyValuesResource resource)
			throws KeyValuesResourceParserException {
		InternalKeyValuesResource r = switch (resource) {
			case InternalKeyValuesResource ir -> ir;
		};
		if (r.normalized()) {
			return r;
		}
		var builder = KeyValuesResource.builder(r.uri()).name(r.name());
		builder.reference = r.reference();
		parseURI(builder, r.uri());
		for (var rk : ResourceKey.values()) {
			switch (rk) {
				case LOAD -> {
				}
				case MEDIA_TYPE -> {
					switch (r.mediaType()) {
						case String s -> builder.mediaType(s);
						case null -> {
						}
					}
				}
				case FLAGS -> {
					for (var f : r.loadFlags()) {
						builder._addFlag(f);
					}
				}
				case PARAM -> {
					r.parameters().forKeyValues((k, v) -> {
						builder.parameter(k, v);
					});
				}
				case FILTER -> {
					for (var f : r.filters()) {
						builder._addFilter(f);
					}
				}
			}
		}
		return builder.buildNormalized();
	}

	private @Nullable LoadResource parseLoadOrNull(KeyValue keyValue) throws KeyValuesResourceParserException {
		String key = keyValue.key();
		String prefix = prefix(ResourceKey.LOAD.keys().getFirst());
		if (!key.startsWith(prefix)) {
			return null;
		}
		String name = key.substring(prefix.length());

		try {
			name = KeyValuesSource.validateName(name);
		}
		catch (IllegalArgumentException e) {
			throw new KeyValuesResourceParserException(
					"Key Value has a malformed name for load resource. keyValue=" + keyValue, e);
		}
		var uri = URI.create(keyValue.expanded());
		return new LoadResource(name, uri, keyValue);
	}

	private void parseURI(KeyValuesResource.Builder builder, URI uri) throws KeyValuesResourceParserException {
		if (uri.getQuery() == null) {
			return;
		}
		var uriParameters = DefaultKeyValuesMedia.parseURI(uri);
		String resourceName = builder.name;
		List<KeyValue> filtered = new ArrayList<>();
		boolean hasParameter = false;
		for (var kv : uriParameters) {
			ResourceKeyValue rk;
			try {
				rk = uriResourceKeyOrNull(kv, resourceName);
			}
			catch (KeyValuesResourceParserException e) {
				throw new KeyValuesResourceParserException("URI query parameter resource key invalid in uri: " + uri,
						e);
			}
			if (rk == null) {
				continue;
			}
			hasParameter = true;
			switch (rk.type()) {
				case LOAD -> {
				}
				case MEDIA_TYPE -> {
					builder.mediaType(kv.value());
				}
				case FLAGS -> {
					builder.addFlags(kv.value());
				}
				case PARAM -> {
					String param = rk.paramName();
					if (param == null) {
						throw new IllegalStateException("bug");
					}
					String value = kv.value();
					builder.parameter(param, value);
				}
				case FILTER -> {
					String filter = rk.paramName();
					if (filter == null) {
						throw new IllegalStateException("bug");
					}
					String expression = kv.value();
					var f = new Filter(filter, expression, "");
					builder._addFilter(f);
				}
				case null -> {
					filtered.add(kv);
				}
			}
		}
		if (hasParameter) {
			String uriNoQuery = uriWithoutQuery(uri);
			if (filtered.isEmpty()) {
				builder.uri = URI.create(uriNoQuery);
			}
			else {
				KeyValues queryKvs = () -> filtered.stream();
				builder.uri = URI.create(uriNoQuery + "?" + KeyValuesMedia.ofUrlEncoded().formatter().format(queryKvs));
			}
		}

	}

	// TODO consider putting this in KeyValuesMedia if we need this again.
	private static String uriWithoutQuery(URI uri) {
		String s = uri.toString();
		if (uri.getQuery() == null) {
			return s;
		}
		int index = s.indexOf('?');
		return s.substring(0, index);
	}

	protected String prefix() {
		return sep();
	}

	protected String sep() {
		return "_";
	}

	protected String prefix(String keyAlias) {
		return prefix() + keyAlias + sep();
	}

	// private String uriParameterPrefix(ResourceKey key, String alias) {
	// return switch (key) {
	// case LOAD, MEDIA_TYPE, FLAGS -> prefix() + alias;
	// case PARAM, FILTER -> prefix() + alias + sep();
	// };
	// }

	private String externalResourceKey1(ResourceKey type, String alias, String resourceName) {
		return switch (type) {
			case LOAD, FLAGS, MEDIA_TYPE -> prefix(alias) + resourceName;
			default -> throw new IllegalArgumentException("key: " + type);

		};
	}

	private String externalResourceKey2Prefix(ResourceKey type, String alias, String resourceName) {
		return switch (type) {
			case FILTER, PARAM -> prefix(alias) + resourceName + sep();
			default -> throw new IllegalArgumentException("key: " + type);

		};
	}

	protected @Nullable ResourceKeyValue uriResourceKeyOrNull(KeyValue kv, String resourceName)
			throws KeyValuesResourceParserException {
		for (var rk : ResourceKey.values()) {
			for (String alias : rk.keys()) {
				ResourceKeyValue r = uriResourceKeyOrNull(kv, resourceName, rk, alias);
				if (r != null) {
					return r;
				}
			}
		}
		return null;
	}

	private @Nullable ResourceKeyValue uriResourceKeyOrNull(KeyValue kv, String resourceName, ResourceKey rk,
			String alias) throws KeyValuesResourceParserException {
		String key = kv.key();
		String prefix = prefix() + alias;
		if (!key.startsWith(prefix)) {
			return null;
		}
		String rest = key.substring(prefix.length());
		ResourceKeyValue r = switch (rk) {
			case LOAD, MEDIA_TYPE, FLAGS -> {
				if (key.equals(prefix)) {
					yield new ResourceKeyValue(rk, alias, kv, resourceName, null);
				}
				if (rest.startsWith(sep())) {
					throw new KeyValuesResourceParserException("Bad resource key. type: " + rk + " keyvalue: " + kv);
				}
				yield null;
			}
			case PARAM, FILTER -> {
				if (key.equals(prefix)) {
					throw new KeyValuesResourceParserException("Bad resource key. type: " + rk + " keyvalue: " + kv);
				}
				if (!rest.startsWith(sep())) {
					yield null;
				}
				String paramName = rest.substring(sep().length());
				if (paramName.isBlank()) {
					throw new KeyValuesResourceParserException("Bad resource key. type: " + rk + " keyvalue: " + kv);
				}
				yield new ResourceKeyValue(rk, alias, kv, resourceName, paramName);
			}
		};
		return r;
	}

	protected @Nullable ResourceKeyValue resourceKeyOrNull(KeyValue kv) throws KeyValuesResourceParserException {
		for (var type : ResourceKey.values()) {
			for (String alias : type.keys()) {
				ResourceKeyValue r = resourceKeyOrNull(kv, type, alias);
				if (r != null) {
					return r;
				}
			}
		}
		return null;
	}

	private @Nullable ResourceKeyValue resourceKeyOrNull(KeyValue kv, ResourceKey type, String alias)
			throws KeyValuesResourceParserException {
		String prefix = prefix(alias);
		var key = kv.key();
		if (key.equals(prefix)) {
			throw new KeyValuesResourceParserException("Bad resource key. type: " + type + " keyvalue: " + kv);
		}
		if (!key.startsWith(prefix)) {
			return null;
		}
		String rest = key.substring(prefix.length());
		String[] names = rest.split(sep(), 2);
		if (names.length == 0) {
			throw new KeyValuesResourceParserException("Bad resource key. type: " + type + " keyvalue: " + kv);
		}
		String resourceName = names[0];
		try {
			resourceName = KeyValuesSource.validateName(resourceName);
		}
		catch (IllegalArgumentException e) {
			throw new KeyValuesResourceParserException(
					"Key Value has a malformed name for load resource. keyValue=" + kv, e);
		}
		return switch (type) {
			case LOAD, MEDIA_TYPE, FLAGS -> {
				if (names.length == 1) {
					yield new ResourceKeyValue(type, alias, kv, resourceName, null);
				}
				throw new KeyValuesResourceParserException("Bad resource key. type: " + type + " keyvalue: " + kv);
			}
			case PARAM, FILTER -> {
				if (names.length == 2) {
					yield new ResourceKeyValue(type, alias, kv, resourceName, names[1]);
				}
				throw new KeyValuesResourceParserException("Bad resource key. type: " + type + " keyvalue: " + kv);
			}
		};
	}

	// private @Nullable String parseResourceName(KeyValue keyValue, String alias) {
	// String key = keyValue.key();
	// String prefix = prefix(alias);
	// if (!key.startsWith(prefix)) {
	// return null;
	// }
	// String name = key.substring(prefix.length());
	//
	// try {
	// name = KeyValuesSource.validateName(name);
	// }
	// catch (IllegalArgumentException e) {
	// throw new KeyValuesResourceParserException("Key Value has a malformed name for load
	// resource. keyValue=" + keyValue,
	// e);
	// }
	// }

	protected String formatFilterKey(KeyValuesResource resource, Filter filter) {
		String alias = ResourceKey.FILTER.formatAlias();
		String prefix = externalResourceKey2Prefix(ResourceKey.FILTER, alias, resource.name());
		String key = prefix + filter.filter();
		String name = filter.name();
		if (!name.isBlank()) {
			key = key + sep() + name;
		}
		return key;

	}

	public static DefaultKeyValuesResourceParser of() {
		return DEFAULT;
	}

	record ResourceKeyValue(ResourceKey type, String alias, KeyValue keyValue, String resourceName,
			@Nullable String paramName) {

	}

	@SuppressWarnings({ "ImmutableEnumChecker" })
	private enum ResourceKey {

		LOAD(KeyValuesResource.KEY_LOAD), //
		FLAGS(KeyValuesResource.KEY_FLAGS, KeyValuesResource.KEY_FLAG), //
		MEDIA_TYPE(KeyValuesResource.KEY_MEDIA_TYPE, KeyValuesResource.KEY_MIME), //
		PARAM(KeyValuesResource.KEY_PARAM, KeyValuesResource.KEY_PARM), //
		FILTER(KeyValuesResource.KEY_FILTER, KeyValuesResource.KEY_FILT);

		final List<String> keys;

		private ResourceKey(String key, String... rest) {
			this(Stream.concat(Stream.of(key), Stream.of(rest)).toList());
		}

		private ResourceKey(String key) {
			this(List.of(key));
		}

		private ResourceKey(List<String> keys) {
			this.keys = keys;
		}

		String formatAlias() {
			return keys().getFirst();
		}

		List<String> keys() {
			return this.keys;
		}

	}

}