package io.jstach.kiwi.kvs;

import java.util.List;
import java.util.function.BiConsumer;

interface KeyValuesResourceParser {

	List<KeyValuesResource> parseResources(KeyValues keyValues);

	KeyValues filterResources(KeyValues keyValues);

	void formatResource(KeyValuesResource resource, BiConsumer<String, String> consumer);

}