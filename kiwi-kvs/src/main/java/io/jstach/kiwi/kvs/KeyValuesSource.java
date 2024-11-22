package io.jstach.kiwi.kvs;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValue.Source;

// TODO need to consolidate all the "source" types.
sealed interface KeyValuesSource permits NamedKeyValuesSource, KeyValue.Source {

	static String validateName(String identifier) {
		if (identifier == null || !identifier.matches("[a-zA-Z0-9]+")) {
			throw new IllegalArgumentException(
					"Invalid source name: must contain only alphanumeric characters (no underscores) and not be null. input: "
							+ identifier);
		}
		return identifier;
	}

	// static String toString(KeyValuesSource source) {
	// return switch(source) {
	// case KeyValuesResource r -> "uri=" + r.uri() + ", name=" + r.name() + ", ref=" +
	// KeyValueReference.toStringRef(r.reference());
	// case KeyValue.Source s -> "uri=" + s.uri() + ", ref=" +
	// KeyValueReference.toStringRef(s.reference());
	// case NamedKeyValues nkvs -> "name=" + nkvs.name();
	//
	// };
	// }

	static StringBuilder describe(StringBuilder sb, KeyValuesSource source) {
		return switch (source) {
			case KeyValuesResource r -> KeyValueReference.describe(sb, r, true);
			case KeyValue.Source s -> KeyValueReference.describe(sb, s, true);
			// DefaultKeyValuesResource.describe(sb, r, true);
			case NamedKeyValues _kvs -> sb.append("In memory provided KeyValues");
		};
	}

	static StringBuilder fullDescribe(StringBuilder sb, KeyValuesSource source) {
		return switch (source) {
			case KeyValuesResource r -> KeyValueReference.fullDescribe(sb, r);
			case KeyValue.Source s -> KeyValueReference.fullDescribe(sb, s);
			// DefaultKeyValuesResource.describe(sb, r, true);
			case NamedKeyValues _kvs -> sb.append("In memory provided KeyValues");
		};
	}

	static Set<LoadFlag> loadFlags(KeyValuesSource resource) {
		return switch (resource) {
			case KeyValue.Source s -> Set.of();
			case DefaultKeyValuesResource d -> d.loadFlags();
			case NamedKeyValues kvs -> Set.of();
		};
	}

}

sealed interface NamedKeyValuesSource extends KeyValuesSource permits NamedKeyValues, KeyValuesResource {

	String name();

}

record NamedKeyValues(String name, KeyValues keyValues) implements NamedKeyValuesSource {
	public NamedKeyValues {
		name = KeyValuesSource.validateName(name);
		keyValues = resource(keyValues, name);
	}

	private static KeyValues resource(KeyValues kvs, String name) {
		var uri = URI.create(Source.NULL_URI.toString() + name);
		return kvs.map(kv -> kv.replaceNullSource(uri)).memoize();
	}
}

// TODO strangely we do not need this class and hence why it is not public.
// Because KeyValuesLoader is good enough at the moment.
interface KeyValuesSourceLoader {

	KeyValues load(List<? extends NamedKeyValuesSource> sources) throws IOException;

}

sealed interface KeyValueReference permits KeyValuesResource, KeyValue.Source {

	public URI uri();

	public @Nullable KeyValue reference();

	/*
	 * TODO: The printing of resources is kind of a mess. A builder is probably a better
	 * solution.
	 */
	static StringBuilder describe(StringBuilder sb, KeyValueReference resource, boolean includeRef) {
		String uri = redactURI(resource, KeyValue.REDACTED_MESSAGE);
		sb.append("uri='").append(uri).append("'");
		var flags = loadFlags(resource);
		if (!flags.isEmpty()) {
			sb.append(" flags=").append(flags);
		}
		if (includeRef) {
			var ref = resource.reference();
			if (ref != null) {
				sb.append(" ");
				describeReference(sb, ref);
			}
		}
		return sb;
	}

	static String toStringRef(KeyValue ref) {
		if (ref == null)
			return "null";
		return "[key='" + ref.key() + "', in='" + ref.meta().source().uri() + "']";
	}

	static StringBuilder describeReference(StringBuilder sb, KeyValue ref) {
		sb.append("specified with key: '").append(ref.key()).append("'");
		sb.append(" in uri='").append(ref.meta().source().uri()).append("'");
		return sb;
	}

	static Set<LoadFlag> loadFlags(KeyValueReference resource) {
		return switch (resource) {
			case KeyValue.Source s -> Set.of();
			case DefaultKeyValuesResource d -> d.loadFlags();
		};
	}

	static StringBuilder fullDescribe(StringBuilder sb, KeyValueReference resource) {
		describe(sb, resource, false);
		var refKey = resource.reference();
		while (refKey != null) {
			sb.append("\n\t<-- ");
			describeReference(sb, refKey);
			refKey = refKey.meta().source().reference();
		}
		return sb;
	}

	static String redactURI(KeyValueReference resource, String redactMessage) {
		String uri = isRedacted(resource) ? (resource.uri().getScheme() + ":" + redactMessage)
				: resource.uri().toString();
		return uri;
	}

	static boolean isRedacted(KeyValueReference self) {
		// TODO hmm if the resource has sensitive info should its URI be redacated as
		// well?
		// if (LoadFlag.SENSITIVE.isSet(loadFlags())) {
		// return true;
		// }
		var ref = self.reference();
		if (ref == null) {
			return false;
		}
		return ref.isSensitive();
	}

}
