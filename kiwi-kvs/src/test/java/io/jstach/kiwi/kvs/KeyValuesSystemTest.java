package io.jstach.kiwi.kvs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class KeyValuesSystemTest {

	@Test
	void testLoader() throws FileNotFoundException, IOException {
		Properties properties = new Properties();
		properties.setProperty("user.home", "/home/kenny");

		var environment = new KeyValuesEnvironment() {
			@Override
			public Properties getSystemProperties() {
				return properties;
			}
		};
		var system = KeyValuesResource.builder(URI.create("system:///"))
			.name("system")
			.noInterpolation(true)
			._addFlag(LoadFlag.NO_ADD_KEY_VALUES) // For SentryMan :)
			.build();
		
		{
			var b = KeyValues.builder();
			system.resourceKeyValues(b::add);
			String actual = b.build().toString();
			String expected = """
					KeyValues[
					_flags_system=NO_ADD_KEY_VALUES,NO_INTERPOLATION
					_load_system=system\\:///
					]
					""";
			assertEquals(expected, actual);
		}

		var kvs = KeyValuesSystem.builder()
			.environment(environment)
			.build() //
			.loader() //
			.add(system)
			.add("classpath:/test-props/testLoader.properties")
			.load();

		System.out.println(kvs);
		{
			String actual = kvs.toString();
			String expected = """
					KeyValues[
					stuff=/home/kenny
					blah=/home/kenny
					message=/home/kenny hello
					mypassword=REDACTED
					]
					""";
			assertEquals(expected, actual);
		}

		{
			String actual = KeyValuesMedia.ofProperties().formatter().format(kvs);
			String expected = """
					stuff=/home/kenny
					blah=/home/kenny
					message=/home/kenny hello
					mypassword=1.2.3.4.5
									""";
			assertEquals(expected, actual);
		}
	}

	@Test
	void testKeyValuesResourceToString() {
		var system = KeyValuesResource.builder(URI.create("system:///"))
			.name("system")
			.noInterpolation(true)
			.noAddKeyValues(true)
			.build();
		String actual = system.toString();
		String expected = """
DefaultKeyValuesResource[uri=system:///, name=system, reference=null, mediaType=null, parameters=MapStaticVariables[map={_flags_system=NO_ADD_KEY_VALUES,NO_INTERPOLATION, _load_system=system:///}]]				"""
			.trim();
		assertEquals(expected, actual);
	}

}
