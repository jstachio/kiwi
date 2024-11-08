package io.jstach.kiwi.kvs;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import io.jstach.kiwi.kvs.KeyValue.Source;

sealed interface KeyValuesSource permits NamedKeyValues, KeyValuesResource {

	static String validateName(String identifier) {
		if (identifier == null || !identifier.matches("[a-zA-Z0-9]+")) {
			throw new IllegalArgumentException(
					"Invalid source name: must contain only alphanumeric characters (no underscores) and not be null. input: "
							+ identifier);
		}
		return identifier;
	}

}

// TODO strangely we do not need this class and hence why it is not public.
// Because KeyValuesLoader is good enough at the moment.
interface KeyValuesSourceLoader {

	KeyValues load(List<? extends KeyValuesSource> sources) throws IOException;

}

record NamedKeyValues(String name, KeyValues keyValues) implements KeyValuesSource {
	public NamedKeyValues {
		name = KeyValuesSource.validateName(name);
		keyValues = resource(keyValues, name);
	}

	private static KeyValues resource(KeyValues kvs, String name) {
		var uri = URI.create(Source.NULL_URI.toString() + name);
		return kvs.map(kv -> kv.replaceNullSource(uri)).memoize();
	}
}
