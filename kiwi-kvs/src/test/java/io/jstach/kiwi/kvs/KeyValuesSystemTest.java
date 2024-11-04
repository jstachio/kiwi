package io.jstach.kiwi.kvs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.jstach.kiwi.kvs.KeyValuesEnvironment.Logger;

class KeyValuesSystemTest {

	@Test
	void testLoader() throws FileNotFoundException, IOException {
		Properties properties = new Properties();
		properties.setProperty("user.home", "/home/kenny");

		var logger = new TestLogger();
		
		var environment = new KeyValuesEnvironment() {
			@Override
			public Properties getSystemProperties() {
				return properties;
			}
			@Override
			public Logger getLogger() {
				return logger;
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
		
		{

			String actual = logger.toString();
			String expected =
					"""
							[DEBUG] Loading uri='system:///' flags=[NO_ADD_KEY_VALUES, NO_INTERPOLATION]
							[INFO ] Loaded  uri='system:///' flags=[NO_ADD_KEY_VALUES, NO_INTERPOLATION]
							[DEBUG] Loading uri='classpath:/test-props/testLoader.properties'
							[INFO ] Loaded  uri='classpath:/test-props/testLoader.properties'
							[DEBUG] Loading uri='classpath:/test-props/testLoader-child.properties' specified with key: '_load_child' in uri='classpath:/test-props/testLoader.properties'
							[INFO ] Loaded  uri='classpath:/test-props/testLoader-child.properties'
							[DEBUG] Loading uri='classpath:/test-props/testLoader-doesnotexist.properties' flags=[NO_REQUIRE] specified with key: '_load_noexist' in uri='classpath:/test-props/testLoader.properties'
							[DEBUG] Missing uri='classpath:/test-props/testLoader-doesnotexist.properties' flags=[NO_REQUIRE]
							[DEBUG] Loading uri='classpath:/test-props/testLoader-sensitive.properties' flags=[SENSITIVE] specified with key: '_load_luggage' in uri='classpath:/test-props/testLoader.properties'
							[INFO ] Loaded  uri='classpath:/test-props/testLoader-sensitive.properties' flags=[SENSITIVE]
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
DefaultKeyValuesResource[uri=system:///, name=system, reference=null, mediaType=null, parameters=MapStaticVariables[map={_flags_system=NO_ADD_KEY_VALUES,NO_INTERPOLATION, _load_system=system:///}]]"""
			.trim();
		assertEquals(expected, actual);
	}
	
	class TestLogger implements Logger {
		private final List<String> events = new ArrayList<>();

		public TestLogger() {
			super();
		}

		public void clear() {
			events.clear();
		}
		public List<String> getEvents() {
			return events;
		}
		@Override
		public void debug(
				String message) {
			events.add("[DEBUG] " + message);
		}

		@Override
		public void info(
				String message) {
			events.add("[INFO ] " + message);
		}
		@Override
		public String toString() {
			return events.stream()
					.map(line -> line + "\n")
					.collect(Collectors.joining());
		}
	}

}
