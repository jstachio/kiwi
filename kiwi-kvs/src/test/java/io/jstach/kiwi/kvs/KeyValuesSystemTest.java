package io.jstach.kiwi.kvs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import io.jstach.kiwi.kvs.KeyValuesMedia.BuiltinMediaType;

class KeyValuesSystemTest {

	@Test
	void testLoader() throws FileNotFoundException, IOException {
		Properties properties= new Properties();
		properties.setProperty("user.home", "/home/kenny");
		
		var environment = new KeyValuesEnvironment() {
			@Override
			public Properties getSystemProperties() {
				return properties;
			}
		};
		var system = KeyValuesResource.builder(URI.create("system:///"))
				._addFlag(LoadFlag.NO_INTERPOLATION)
				._addFlag(LoadFlag.NO_ADD_KEY_VALUES)
				.build();
		
		var kvs = KeyValuesSystem.builder().environment(environment).build() //
				.loader() //
				.add(system)
				.add("classpath:/example/testLoader.properties")
				.load();
		String actual = BuiltinMediaType.PROPERTIES.format(kvs);
		String expected = """
				stuff=/home/kenny
								""";
		assertEquals(expected, actual);
	}

}
