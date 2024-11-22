package io.jstach.kiwi.kvs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValue.Flag;
import io.jstach.kiwi.kvs.KeyValuesServiceProvider.KeyValuesFilter.FilterContext;

/*
 * This class is not reusable or threadsafe unless
 * the static of method is used.
 */
class DefaultKeyValuesSourceLoader implements KeyValuesSourceLoader {

	private final KeyValuesSystem system;

	private final Variables variables;

	private final Map<String, String> variableStore;

	private final Map<String, KeyValue> keys = new LinkedHashMap<>();

	private final List<Node> sourcesStack = new ArrayList<>();

	private final List<KeyValue> keyValuesStore = new ArrayList<>();

	private final KeyValuesResourceParser resourceParser = DefaultKeyValuesResourceParser.of();

	private final KeyValuesEnvironment.Logger logger;

	/*
	 * TODO We use a node to wrap a source to represent each branch to fully recover the
	 * load path but we probably do not need to do this as each kv has the source.
	 */
	record Node(NamedKeyValuesSource current, @Nullable Node parent) {
	}

	static KeyValuesLoader of(KeyValuesSystem system, Variables rootVariables,
			List<? extends NamedKeyValuesSource> resources) {
		record ReusableLoader(KeyValuesSystem system, Variables rootVariables,
				List<? extends NamedKeyValuesSource> resources) implements KeyValuesLoader {

			@Override
			public KeyValues load() throws IOException {
				try {
					return new DefaultKeyValuesSourceLoader(system, rootVariables).load(resources);
				}
				catch (RuntimeException e) {
					system.environment().getLogger().fatal(e);
					throw e;
				}
				catch (IOException e) {
					system.environment().getLogger().fatal(e);
					throw e;
				}
			}
		}
		return new ReusableLoader(system, rootVariables, resources);
	}

	private DefaultKeyValuesSourceLoader(KeyValuesSystem system, Variables rootVariables) {
		super();
		this.system = system;
		this.variableStore = new LinkedHashMap<>();
		this.variables = Variables.builder().add(variableStore).add(rootVariables).build();
		this.logger = system.environment().getLogger();
	}

	@Override
	public KeyValues load(List<? extends NamedKeyValuesSource> sources) throws IOException {
		if (sources.isEmpty()) {
			return KeyValues.empty();
		}

		var fs = this.sourcesStack;

		KeyValues keyValues = () -> keyValuesStore.stream();
		{
			List<Node> nodes = sources.stream().map(s -> new Node(s, null)).toList();
			validateNames(nodes);
			fs.addAll(0, nodes);
		}
		for (; !fs.isEmpty();) {
			// pop
			var node = fs.remove(0);
			var resource = node.current;
			Set<LoadFlag> flags = KeyValuesSource.loadFlags(resource);

			var kvs = switch (resource) {
				case InternalKeyValuesResource ir -> load(node, ir, flags);
				case NamedKeyValues _kvs -> _kvs.keyValues();
			};

			var kvFlags = LoadFlag.toKeyValueFlags(flags);
			if (!kvFlags.isEmpty()) {
				kvs = kvs.map(kv -> kv.addFlags(kvFlags));
			}
			if (!LoadFlag.NO_INTERPOLATE.isSet(flags)) {
				// technically this would be a noop
				// anyway because the kv have the
				// no interpolate flag.
				kvs = kvs.expand(variables);
			}
			var foundResources = resourceParser.parseResources(kvs);
			var nodes = foundResources.stream().map(s -> new Node(s, node)).toList();
			validateNames(nodes);
			// push
			fs.addAll(0, nodes);
			kvs = resourceParser.filterResources(kvs);
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
							+ describe(node));
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
		return keyValues.expand(variables).memoize();

	}

	static List<Node> validateNames(List<Node> nodes) {
		Set<String> names = new HashSet<>();
		for (var n : nodes) {
			String name = n.current.name();
			if (!names.add(name)) {
				throw new IllegalStateException("Duplicate name found in grouped resources. name=" + name);
			}
		}
		return nodes;
	}

	static String describe(Node node) {
		StringBuilder sb = new StringBuilder();
		describe(sb, node);
		return sb.toString();
	}

	static void describe(StringBuilder sb, Node node) {
		KeyValuesSource.fullDescribe(sb, node.current);
	}

	KeyValues load(Node node, InternalKeyValuesResource resource, Set<LoadFlag> flags)
			throws IOException, FileNotFoundException {
		logger.load(resource);
		if (LoadFlag.NO_LOAD_CHILDREN.isSet(flags)) {
			throw new IOException("Resource not allowed to chain. resource: " + describe(node));
		}
		var context = DefaultLoaderContext.of(system, variables, resourceParser);
		try {
			var kvs = system.loaderFinder()
				.findLoader(context, resource)
				.orElseThrow(() -> new IOException("Resource Loader not found. resource: " + describe(node)))
				.load();
			logger.loaded(resource);
			return filter(resource, kvs);
		}
		catch (FileNotFoundException e) {
			logger.missing(resource, e);
			if (LoadFlag.NO_REQUIRE.isSet(flags)) {
				return KeyValues.empty();
			}
			throw new IOException("Resource not found. resource: " + describe(node), e);
		}
		catch (IOException e) {
			throw new IOException("Resource load fail. resource: " + describe(node), e);
		}

	}

	KeyValues filter(InternalKeyValuesResource resource, KeyValues keyValues) {
		var filters = resource.filters();
		if (filters.isEmpty()) {
			return keyValues;
		}
		FilterContext context = new FilterContext(system.environment(), resource.parameters());
		for (var f : filters) {
			keyValues = system.filter().filter(context, keyValues, f);
		}
		return keyValues;
	}

}

enum LoadFlag {

	/**
	 * Makes the resource optional so that if it is not found an error does not happen.
	 */
	NO_REQUIRE(List.of("NO_REQUIRE", "OPTIONAL", "NOT_REQUIRED")),
	/**
	 * TODO
	 */
	NO_EMPTY,
	/**
	 * Confusing but this means the resource should not have its properties overriden. Not
	 * to be confused with {@link #NO_REPLACE} which sounds like what this does.
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

	@SuppressWarnings("ImmutableEnumChecker")
	private final Set<String> names;

	@SuppressWarnings("ImmutableEnumChecker")
	private final Set<String> reverseNames;

	private LoadFlag(List<String> names, List<String> reverseNames) {
		if (names.isEmpty()) {
			names = List.of(name());
		}
		FlagNames fn = new FlagNames(names, reverseNames);
		this.names = Set.copyOf(fn.names());
		this.reverseNames = Set.copyOf(fn.reverseNames());
	}

	private LoadFlag(List<String> names) {
		this(names, List.of());

	}

	private LoadFlag(String name) {
		this(List.of(name));
	}

	private LoadFlag() {
		this(List.of(), List.of());
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

	public static Set<KeyValue.Flag> toKeyValueFlags(Iterable<LoadFlag> loadFlags) {
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

	public static void parseCSV(EnumSet<LoadFlag> flags, String csv) {
		DefaultKeyValuesMedia.parseCSV(csv, k -> LoadFlag.parse(flags, k));
	}

	public static EnumSet<LoadFlag> parseCSV(String csv) {
		EnumSet<LoadFlag> flags = EnumSet.noneOf(LoadFlag.class);
		parseCSV(flags, csv);
		return flags;
	}

	private static boolean nameMatches(Set<String> aliases, String name) {
		return aliases.contains(name.toUpperCase(Locale.ROOT));
	}

	static String toCSV(Stream<LoadFlag> loadFlags) {
		return loadFlags.map(f -> f.name()).collect(Collectors.joining(","));
	}

}