package io.jstach.kiwi.kvs;

import java.io.IOException;
import java.util.List;

//TODO strangely we do not need this class and hence why it is not public.
// Because KeyValuesLoader is good enough at the moment.
interface KeyValuesResourceLoader {

	KeyValues load(List<? extends KeyValuesResource> resources) throws IOException;

}