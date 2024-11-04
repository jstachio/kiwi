package io.jstach.kiwi.kvs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValue.Flag;
import io.jstach.kiwi.kvs.KeyValuesEnvironment.Logger;

class DefaultKeyValuesResourceLoader implements KeyValuesResourceLoader {

	private final KeyValuesSystem system;

	private final Variables variables;

	private final Map<String, String> variableStore;

	private final Map<String, KeyValue> keys = new LinkedHashMap<>();

	private final List<KeyValuesResource> resourcesStack = new ArrayList<>();

	private final List<KeyValue> keyValuesStore = new ArrayList<>();

	public DefaultKeyValuesResourceLoader(KeyValuesSystem system, Variables rootVariables) {
		super();
		this.system = system;
		this.variableStore = new LinkedHashMap<>();
		this.variables = Variables.builder().add(variableStore).add(rootVariables).build();
	}

	@Override
	public KeyValues load(List<? extends KeyValuesResource> _resources) throws IOException {
		if (_resources.isEmpty()) {
			return KeyValues.empty();
		}

		var fs = this.resourcesStack;
		var logger = system.environment().getLogger();

		KeyValues keyValues = () -> keyValuesStore.stream();

		fs.addAll(0, _resources);
		for (; !fs.isEmpty();) {
			// pop
			var resource = fs.remove(0);
			var flags = LoadFlag.of(resource);
			var kvs = load(resource, flags, variables, logger);

			var kvFlags = LoadFlag.keyValueFlags(flags);
			if (!kvFlags.isEmpty()) {
				kvs = kvs.map(kv -> kv.addFlags(kvFlags));
			}
			if (!LoadFlag.NO_INTERPOLATE.isSet(flags)) {
				// technically this would be a noop
				// anyway because the kv have the
				// no interpolate flag.
				kvs = kvs.expand(variables);
			}
			var foundResources = findResources(kvs);
			// push
			fs.addAll(0, foundResources);
			kvs = filter(kvs, flags);
			boolean added = false;
			if (!LoadFlag.NO_ADD.isSet(flags)) {
				for (var kv : kvs) {
					if (LoadFlag.NO_REPLACE.isSet(flags) && keys.containsKey(kv.key())) {
						continue;
					}
					keys.put(kv.key(), kv);
					keyValuesStore.add(kv);
					added = true;
				}
				if (!added && LoadFlag.NO_EMPTY.isSet(flags)) {
					throw new IOException("Resource did not have any key values and was flagged not empty. resource: "
							+ describe(resource));
				}
			}
			else {
				variableStore.putAll(kvs.interpolate(variables));
			}
			/*
			 * We interpolate the entire list again. Every time a resource is loaded so
			 * that the next resource has the previous resources keys as variables for
			 * interpolation.
			 */
			variableStore.putAll(keyValues.interpolate(variables));
		}
		return KeyValues.of(keyValuesStore).expand(variables).memoize();

	}

	static String describe(KeyValuesResource resource) {
		return DefaultKeyValuesResource.describe(resource, true);
	}

	static List<KeyValuesResource> findResources(KeyValues keyValues) {
		List<KeyValuesResource> resources = new ArrayList<>();
		for (var kv : keyValues) {
			var r = KeyValuesResource.Builder.build(kv, keyValues);
			if (r != null) {
				resources.add(r);
			}
		}
		return List.copyOf(resources);
	}

	KeyValues filter(KeyValues keyValues, Set<LoadFlag> flags) {
		return KeyValues.of(keyValues.stream().filter(this::filter).toList());
	}

	private boolean filter(KeyValue kv) {
		return ResourceKeys.find(kv) == null;
	}

	KeyValues load(KeyValuesResource resource, Set<LoadFlag> flags, Variables variables, Logger logger)
			throws IOException, FileNotFoundException {
		var context = DefaultLoaderContext.of(system, variables);
		try {
			logger.load(resource);
			var kvs = system.loaderFinder()
				.findLoader(context, resource)
				.orElseThrow(
						() -> new IOException("Resource Loader could not be found. resource: " + describe(resource)))
				.load();
			logger.loaded(resource);
			return kvs;
		}
		catch (FileNotFoundException e) {
			logger.missing(resource, e);
			if (LoadFlag.NO_REQUIRE.isSet(flags)) {
				return KeyValues.empty();
			}
			throw new IOException("Resource could not be loaded. resource: " + describe(resource), e);
		}

	}

}

enum ResourceKeys {

	LOAD("load"), FLAGS("flags"), MEDIA_TYPE("mediaType");

	// private final String key;
	private final String prefix;

	private ResourceKeys(String key) {
		// this.key = key;
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

	@Nullable
	String getValue(Variables variables, String resourceName) {
		return variables.getValue(key(resourceName));
	}

	@Nullable
	String getValue(KeyValuesResource resource) {
		return getValue(resource.parameters(), resource.name());
	}

	void setValue(String resourceName, String value, BiConsumer<String, String> consumer) {
		String key = this.key(resourceName);
		consumer.accept(key, value);
	}

	String key(String resourceName) {
		DefaultKeyValuesResource.validateResourceName(resourceName);
		return this.prefix + resourceName;
	}

	static void copyKeys(KeyValuesResource resource, String newResourceName, BiConsumer<String, String> consumer) {
		for (var rk : ResourceKeys.values()) {
			String value = rk.getValue(resource);
			if (value != null) {
				String key = rk.key(newResourceName);
				consumer.accept(key, newResourceName);
				;
			}
		}
	}

	static void copyKeys(Variables variables, String oldResourceName, String newResourceName,
			BiConsumer<String, String> consumer) {
		for (var rk : ResourceKeys.values()) {
			String value = rk.getValue(variables, oldResourceName);
			if (value != null) {
				String key = rk.key(newResourceName);
				consumer.accept(key, value);
				;
			}
		}
	}

}

enum LoadFlag {

	/**
	 * Makes it maybe.
	 */
	NO_REQUIRE,
	/**
	 * TODO
	 */
	NO_EMPTY,
	/**
	 * Confusing but this means the resource should not have its properties overriden. Not
	 * to be confused with {@link #NO_REPLACE_EXISTING_KEY_VALUE} which sounds like what
	 * this does.
	 */
	LOCK,
	/**
	 * This basically says the resource can only add new key values.
	 */
	NO_REPLACE,
	/**
	 * Will add the kvs to variables but not to the final resolved key values.
	 */
	NO_ADD,
	/**
	 * Will add key values but are not allowed for interpolation.
	 */
	NO_ADD_VARIABLES,
	/**
	 * Disables _load calls on child.
	 */
	NO_LOAD_CHILDREN,
	/**
	 * Will not interpolate key values loaded ever.
	 */
	NO_INTERPOLATE,
	/**
	 * Will not toString or print out sensitive
	 */
	SENSITIVE, // sensitive
	/**
	 * TODO
	 */
	NO_RELOAD,
	/**
	 * TODO
	 */
	INHERIT;

	private final Set<String> names;

	private final Set<String> reverseNames;

	private LoadFlag() {
		List<String> names = new ArrayList<>();
		List<String> reverseNames = new ArrayList<>();
		String originalName = this.name();

		String name = originalName;
		boolean no = false;
		for (String negate : List.of("NO_", "NOT_")) {
			String n = remove(originalName, negate);
			if (n != null) {
				no = true;
				name = n;
				break;
			}
		}
		var nos = no ? names : reverseNames;
		var yeses = no ? reverseNames : names;

		for (String negate : List.of("NO_", "NOT_")) {
			nos.add(negate + name);
		}
		yeses.add(name);

		this.names = Set.copyOf(names);
		this.reverseNames = Set.copyOf(reverseNames);

	}

	private static @Nullable String remove(String key, String prefix) {
		if (!key.startsWith(prefix)) {
			return null;
		}
		return key.substring(prefix.length());
	}

	boolean isSet(Set<LoadFlag> flags) {
		return flags.contains(this);
	}

	void set(EnumSet<LoadFlag> set, boolean add) {
		if (add) {
			set.add(this);
		}
		else {
			set.remove(this);
		}
	}

	public static Set<KeyValue.Flag> keyValueFlags(Iterable<LoadFlag> loadFlags) {
		EnumSet<KeyValue.Flag> flags = EnumSet.noneOf(KeyValue.Flag.class);
		for (var lf : loadFlags) {
			switch (lf) {
				case NO_INTERPOLATE -> flags.add(Flag.NO_INTERPOLATION);
				case SENSITIVE -> flags.add(Flag.SENSITIVE);
				default -> {
				}
			}
		}
		return flags;
	}

	public static void parse(EnumSet<LoadFlag> set, String key) {
		var flags = LoadFlag.values();
		key = key.toUpperCase(Locale.ROOT);
		for (var flag : flags) {
			if (nameMatches(flag.names, key)) {
				flag.set(set, true);
				return;
			}
			else if (nameMatches(flag.reverseNames, key)) {
				flag.set(set, false);
				return;
			}
		}
		throw new IllegalArgumentException("bad load flag: " + key);
	}

	private static boolean nameMatches(Set<String> aliases, String name) {
		return aliases.contains(name.toUpperCase(Locale.ROOT));
	}

	static void addToParameters(String resourceName, Map<String, String> parameters, Set<LoadFlag> flags) {
		String key = ResourceKeys.FLAGS.key(resourceName);
		if (parameters.containsKey(key) || flags.isEmpty()) {
			return;
		}
		String value = flags.stream().map(f -> f.name()).collect(Collectors.joining(","));
		parameters.put(key, value);
	}

	static Set<LoadFlag> of(KeyValuesResource resource) {
		EnumSet<LoadFlag> flags = EnumSet.noneOf(LoadFlag.class);
		String v = ResourceKeys.FLAGS.getValue(resource);
		if (v == null) {
			return flags;
		}
		DefaultKeyValuesMedia.parseCSV(v, k -> LoadFlag.parse(flags, k));
		return flags;
	}

}