package io.jstach.kiwi.kvs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
			._addFlag(LoadFlag.NO_ADD) // For SentryMan :)
			.build();

		{
			var b = KeyValues.builder();
			DefaultKeyValuesResourceParser.of().formatResource(system, b::add);
			String actual = b.build().toString();
			String expected = """
					KeyValues[
					_load_system=system\\:///
					_flags_system=NO_ADD,NO_INTERPOLATE
					]
					""";
			assertEquals(expected, actual);
		}

		var map = Map.of("fromMap2", "2", "fromMap1", "1");

		var kvs = KeyValuesSystem.builder()
			.environment(environment)
			.build() //
			.loader() //
			.add(system)
			.add("classpath:/test-props/testLoader.properties")
			.add("extra", KeyValues.builder().add(map.entrySet()).build())
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
					fromMap1=1
					fromMap2=2
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
					fromMap1=1
					fromMap2=2
									""";
			assertEquals(expected, actual);
		}

		{

			String actual = logger.toString();
			String expected = """
					[DEBUG] Loading uri='system:///' flags=[NO_ADD, NO_INTERPOLATE]
					[INFO ] Loaded  uri='system:///' flags=[NO_ADD, NO_INTERPOLATE]
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
			.parameter("custom", "something")
			.build();
		String actual = system.toString();
		String expected = """
				DefaultKeyValuesResource[uri=system:///, name=system, loadFlags=[NO_ADD, NO_INTERPOLATE], reference=null, mediaType=null, parameters=MapParameters[map={custom=something}]]"""
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
		public void debug(String message) {
			events.add("[DEBUG] " + message);
		}

		@Override
		public void info(String message) {
			events.add("[INFO ] " + message);
		}

		@Override
		public String toString() {
			return events.stream().map(line -> line + "\n").collect(Collectors.joining());
		}

	}

}
