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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValue.Flag;

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
	record Node(KeyValuesSource current, @Nullable Node parent) {
	}

	static KeyValuesSourceLoader of(KeyValuesSystem system, Variables rootVariables) {
		record ReusableLoader(KeyValuesSystem system, Variables rootVariables) implements KeyValuesSourceLoader {

			@Override
			public KeyValues load(List<? extends KeyValuesSource> resources) throws IOException {
				return new DefaultKeyValuesSourceLoader(system, rootVariables).load(resources);
			}
		}
		return new ReusableLoader(system, rootVariables);
	}

	private DefaultKeyValuesSourceLoader(KeyValuesSystem system, Variables rootVariables) {
		super();
		this.system = system;
		this.variableStore = new LinkedHashMap<>();
		this.variables = Variables.builder().add(variableStore).add(rootVariables).build();
		this.logger = system.environment().getLogger();
	}

	@Override
	public KeyValues load(List<? extends KeyValuesSource> sources) throws IOException {
		if (sources.isEmpty()) {
			return KeyValues.empty();
		}

		var fs = this.sourcesStack;

		KeyValues keyValues = () -> keyValuesStore.stream();
		{
			List<Node> nodes = sources.stream().map(s -> new Node(s, null)).toList();
			fs.addAll(0, nodes);
		}
		for (; !fs.isEmpty();) {
			// pop
			var node = fs.remove(0);
			var resource = node.current;
			Set<LoadFlag> flags = switch (resource) {
				case InternalKeyValuesResource ir -> ir.loadFlags();
				case NamedKeyValues _kvs -> Set.of();
			};

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

	static StringBuilder describe(StringBuilder sb, KeyValuesSource source) {
		return switch (source) {
			case KeyValuesResource r -> DefaultKeyValuesResource.fullDescribe(sb, r);
			// DefaultKeyValuesResource.describe(sb, r, true);
			case NamedKeyValues _kvs -> sb.append("In memory provided KeyValues");
		};
	}

	static String describe(Node node) {
		StringBuilder sb = new StringBuilder();
		describe(sb, node);
		return sb.toString();
	}

	static void describe(StringBuilder sb, Node node) {
		describe(sb, node.current);
		// var current = node.parent;
		// while (current != null) {
		// sb.append("\n\t <-- ");
		// sb.append(describe(current.current));
		// current = current.parent;
		// }
	}

	KeyValues load(Node node, KeyValuesResource resource, Set<LoadFlag> flags)
			throws IOException, FileNotFoundException {
		logger.load(resource);
		if (LoadFlag.NO_LOAD_CHILDREN.isSet(flags)) {
			throw new IOException("Resource is not allowed to load children. resource: " + describe(node));
		}
		var context = DefaultLoaderContext.of(system, variables, resourceParser);
		try {
			var kvs = system.loaderFinder()
				.findLoader(context, resource)
				.orElseThrow(() -> new IOException("Resource Loader could not be found. resource: " + describe(node)))
				.load();
			logger.loaded(resource);
			return kvs;
		}
		catch (FileNotFoundException e) {
			logger.missing(resource, e);
			if (LoadFlag.NO_REQUIRE.isSet(flags)) {
				return KeyValues.empty();
			}
			throw new IOException("Resource could not be found. resource: " + describe(node), e);
		}
		catch (IOException e) {
			throw new IOException("Resource could not be loaded. resource: " + describe(node), e);
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

	public static EnumSet<LoadFlag> parseCSV(String csv) {
		EnumSet<LoadFlag> flags = EnumSet.noneOf(LoadFlag.class);
		DefaultKeyValuesMedia.parseCSV(csv, k -> LoadFlag.parse(flags, k));
		return flags;
	}

	private static boolean nameMatches(Set<String> aliases, String name) {
		return aliases.contains(name.toUpperCase(Locale.ROOT));
	}

	static String toCSV(Stream<LoadFlag> loadFlags) {
		return loadFlags.map(f -> f.name()).collect(Collectors.joining(","));
	}

}