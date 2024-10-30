package io.jstach.kiwi.kvs;

import java.io.IOException;
import java.util.List;

public interface KeyValuesResourceLoader {
	public KeyValues load(List<? extends KeyValuesResource> resources) throws IOException;
}