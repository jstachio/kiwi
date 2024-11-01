package io.jstach.kiwi.kvs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValue.Flag;

class DefaultKeyValuesResourceLoader implements KeyValuesResourceLoader {

	private final KeyValuesSystem system;

	final Variables variables;

	final Map<String, String> variableStore;

	final Set<String> overrides = new LinkedHashSet<>();

	final Set<String> keys = new LinkedHashSet<>();

	final List<KeyValuesResource> resourcesQueue = new ArrayList<>();

	final List<KeyValue> keyValuesStore = new ArrayList<>();

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

		var fs = this.resourcesQueue;

		KeyValues keyValues = () -> keyValuesStore.stream();

		fs.addAll(0, _resources);
		for (; !fs.isEmpty();) {
			// pop
			var resource = fs.remove(0);
			var flags = LoadFlag.of(resource);
			var kvs = load(resource, flags, variables);

			var kvFlags = LoadFlag.keyValueFlags(flags);
			if (!kvFlags.isEmpty()) {
				kvs = kvs.map(kv -> kv.addFlags(kvFlags));
			}
			if (!LoadFlag.NO_INTERPOLATION.isSet(flags)) {
				kvs = kvs.expand(variables);
			}
			var foundResources = findResources(kvs);
			// push
			fs.addAll(0, foundResources);
			kvs = filter(kvs, flags);
			if (!LoadFlag.NO_ADD_KEY_VALUES.isSet(flags)) {
				kvs.forEach(keyValuesStore::add);
			}
			else {
				variableStore.putAll(kvs.interpolate(variables));
			}
			/*
			 * We interpolate the entire list again. Every tine a resource is loaded so
			 * that the next resource has the previous resources keys as variables for
			 * interpolation.
			 */
			variableStore.putAll(keyValues.interpolate(variables));
		}
		return KeyValues.of(keyValuesStore).expand(variables).memoize();

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
		return !(kv.key().startsWith("_load_") || kv.key().startsWith("_flags_"));
	}

	KeyValues load(KeyValuesResource resource, Set<LoadFlag> flags, Variables variables)
			throws IOException, FileNotFoundException {
		var context = DefaultLoaderContext.of(system, variables);
		try {
			return system.loaderFinder()
				.findLoader(context, resource)
				.orElseThrow(() -> new IOException("Resource Loader could not be found. resource: " + resource))
				.load();
		}
		catch (FileNotFoundException e) {
			if (LoadFlag.NOT_REQUIRED.isSet(flags)) {
				return KeyValues.empty();
			}
			throw new IOException("Resource could not be loaded. resource: " + resource, e);
		}

	}

}

enum LoadFlag {

	/**
	 * Makes it maybe.
	 */
	NOT_REQUIRED, 
	/**
	 * TODO
	 */
	NOT_EMPTY,
	/**
	 * Confusing but this means the resource should not have its properties overriden. Not
	 * to be confused with {@link #NO_REPLACE_EXISTING_KEY_VALUE} which sounds like what
	 * this does.
	 */
	NO_OVERRIDE,
	/**
	 * This basically says the resource can only add new key values.
	 */
	NO_REPLACE_EXISTING_KEY_VALUE,
	/**
	 * Will add the kvs to variables but not to the final resolved key values.
	 */
	NO_ADD_KEY_VALUES, 
	/**
	 * Will only add key values to variable store
	 */
	NO_ADD_VARIABLES,
	/**
	 * If multiple _load resources are found only the first one is used.
	 */
	STOP_ON_FIRST_FOUND,
	/**
	 * Disables _load calls on child.
	 */
	NO_LOAD_CHILDREN,
	/**
	 * Will not interpolate key values loaded ever.
	 */
	NO_INTERPOLATION,
	/**
	 * Will not toString or print out sensitive
	 */
	NO_PRINT_VALUE, // sensitive
	/**
	 * TODO
	 */
	NO_RELOAD, 
	/**
	 * TODO
	 */
	INHERIT;

	interface LoadFlagSupplier {

		Set<LoadFlag> loadFlags();

	}

	private final String reverseKey;

	private LoadFlag() {
		this.reverseKey = reverseKey(name());
	}

	boolean isSet(Set<LoadFlag> flags) {
		return flags.contains(this);
	}

	public static Set<KeyValue.Flag> keyValueFlags(Iterable<LoadFlag> loadFlags) {
		var flags = EnumSet.noneOf(KeyValue.Flag.class);
		for (var lf : loadFlags) {
			switch (lf) {
				case NO_INTERPOLATION -> flags.add(Flag.NO_INTERPOLATION);
				case NO_PRINT_VALUE -> flags.add(Flag.SENSITIVE);
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
			if (flag.name().equalsIgnoreCase(key)) {
				set.add(flag);
				return;
			}
			else if (flag.reverseKey.equalsIgnoreCase(key)) {
				set.remove(flag);
				return;
			}
		}
		throw new IllegalArgumentException("bad load flag: " + key);
	}

	private static String reverseKey(String key) {
		for (String negate : List.of("NOT_", "NO_")) {
			String k = remove(key, negate);
			if (k != null) {
				return k;
			}
		}
		return "NO_" + key;
	}

	static @Nullable String remove(String key, String prefix) {
		if (!key.startsWith(prefix)) {
			return null;
		}
		return key.substring(prefix.length());
	}

	static void addToParameters(String resourceName, Map<String, String> parameters, Set<LoadFlag> flags) {
		String key = "_flags_" + resourceName;
		if (parameters.containsKey(key) || flags.isEmpty()) {
			return;
		}
		String value = flags.stream().map(f -> f.name()).collect(Collectors.joining(","));
		parameters.put(key, value);
	}

	static Set<LoadFlag> of(KeyValuesResource resource) {
		EnumSet<LoadFlag> flags = EnumSet.noneOf(LoadFlag.class);
		String v = resource.parameters().getValue("_flags_" + resource.name());
		if (v == null) {
			return flags;
		}
		Stream.of(v.split(",")).forEach(k -> LoadFlag.parse(flags, k));
		return flags;
	}

}