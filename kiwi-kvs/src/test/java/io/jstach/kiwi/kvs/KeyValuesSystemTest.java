package io.jstach.kiwi.kvs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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

	/*
	 * Yes it is ridiculous how this is all one test at the moment. It is essentially a
	 * smoke test and not a unit test.
	 */
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
					profile1=loaded 2
					profile2=loaded
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
					profile1=loaded 2
					profile2=loaded
					mypassword=1.2.3.4.5
					fromMap1=1
					fromMap2=2
									""";
			assertEquals(expected, actual);
		}

		{
			KeyValuesMedia.Formatter formatter = (a, _kvs) -> {
				for (var kv : _kvs) {
					a.append(kv.toString()).append("\n");
				}
			};

			String actual = formatter.format(kvs);
			String expected = """
					KeyValue[key=stuff, raw=${user.home}, expanded=/home/kenny, source=Source[uri=classpath:/test-props/testLoader.properties, reference=null, index=1], flags=[]]
					KeyValue[key=blah, raw=${MISSING:-${stuff}}, expanded=/home/kenny, source=Source[uri=classpath:/test-props/testLoader.properties, reference=null, index=2], flags=[]]
					KeyValue[key=message, raw=${stuff} hello, expanded=/home/kenny hello, source=Source[uri=classpath:/test-props/testLoader-child.properties, reference=KeyValue[key=_load_child, raw=classpath:/test-props/testLoader-child.properties, expanded=classpath:/test-props/testLoader-child.properties, source=Source[uri=classpath:/test-props/testLoader.properties, reference=null, index=3], flags=[]], index=1], flags=[]]
					KeyValue[key=profile1, raw=loaded, expanded=loaded 2, source=Source[uri=classpath:/test-props/testLoader-profile1.properties, reference=KeyValue[key=_load_profiles0, raw=classpath:/test-props/testLoader-profile1.properties, expanded=classpath:/test-props/testLoader-profile1.properties, source=Source[uri=profile.classpath:/test-props/testLoader-__PROFILE__.properties, reference=KeyValue[key=_load_profiles, raw=profile.classpath:/test-props/testLoader-__PROFILE__.properties, expanded=profile.classpath:/test-props/testLoader-__PROFILE__.properties, source=Source[uri=classpath:/test-props/testLoader-child.properties, reference=KeyValue[key=_load_child, raw=classpath:/test-props/testLoader-child.properties, expanded=classpath:/test-props/testLoader-child.properties, source=Source[uri=classpath:/test-props/testLoader.properties, reference=null, index=3], flags=[]], index=2], flags=[]], index=1], flags=[]], index=1], flags=[]]
					KeyValue[key=profile1, raw=loaded 2, expanded=loaded 2, source=Source[uri=classpath:/test-props/testLoader-profile2.properties, reference=KeyValue[key=_load_profiles1, raw=classpath:/test-props/testLoader-profile2.properties, expanded=classpath:/test-props/testLoader-profile2.properties, source=Source[uri=profile.classpath:/test-props/testLoader-__PROFILE__.properties, reference=KeyValue[key=_load_profiles, raw=profile.classpath:/test-props/testLoader-__PROFILE__.properties, expanded=profile.classpath:/test-props/testLoader-__PROFILE__.properties, source=Source[uri=classpath:/test-props/testLoader-child.properties, reference=KeyValue[key=_load_child, raw=classpath:/test-props/testLoader-child.properties, expanded=classpath:/test-props/testLoader-child.properties, source=Source[uri=classpath:/test-props/testLoader.properties, reference=null, index=3], flags=[]], index=2], flags=[]], index=3], flags=[]], index=1], flags=[]]
					KeyValue[key=profile2, raw=loaded, expanded=loaded, source=Source[uri=classpath:/test-props/testLoader-profile2.properties, reference=KeyValue[key=_load_profiles1, raw=classpath:/test-props/testLoader-profile2.properties, expanded=classpath:/test-props/testLoader-profile2.properties, source=Source[uri=profile.classpath:/test-props/testLoader-__PROFILE__.properties, reference=KeyValue[key=_load_profiles, raw=profile.classpath:/test-props/testLoader-__PROFILE__.properties, expanded=profile.classpath:/test-props/testLoader-__PROFILE__.properties, source=Source[uri=classpath:/test-props/testLoader-child.properties, reference=KeyValue[key=_load_child, raw=classpath:/test-props/testLoader-child.properties, expanded=classpath:/test-props/testLoader-child.properties, source=Source[uri=classpath:/test-props/testLoader.properties, reference=null, index=3], flags=[]], index=2], flags=[]], index=3], flags=[]], index=2], flags=[]]
					KeyValue[key=mypassword, raw=REDACTED, expanded=REDACTED, source=Source[uri=classpath:/test-props/testLoader-sensitive.properties, reference=KeyValue[key=_load_luggage, raw=classpath:/test-props/testLoader-sensitive.properties, expanded=classpath:/test-props/testLoader-sensitive.properties, source=Source[uri=classpath:/test-props/testLoader.properties, reference=null, index=8], flags=[]], index=1], flags=[]]
					KeyValue[key=fromMap1, raw=1, expanded=1, source=Source[uri=null:///extra, reference=null, index=0], flags=[]]
					KeyValue[key=fromMap2, raw=2, expanded=2, source=Source[uri=null:///extra, reference=null, index=0], flags=[]]
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
					[DEBUG] Loading uri='profile.classpath:/test-props/testLoader-__PROFILE__.properties' specified with key: '_load_profiles' in uri='classpath:/test-props/testLoader-child.properties'
					[INFO ] Found profiles: [profile1, profile2]
					[INFO ] Loaded  uri='profile.classpath:/test-props/testLoader-__PROFILE__.properties'
					[DEBUG] Loading uri='classpath:/test-props/testLoader-profile1.properties' specified with key: '_load_profiles0' in uri='profile.classpath:/test-props/testLoader-__PROFILE__.properties'
					[INFO ] Loaded  uri='classpath:/test-props/testLoader-profile1.properties'
					[DEBUG] Loading uri='classpath:/test-props/testLoader-profile2.properties' specified with key: '_load_profiles1' in uri='profile.classpath:/test-props/testLoader-__PROFILE__.properties'
					[INFO ] Loaded  uri='classpath:/test-props/testLoader-profile2.properties'
					[DEBUG] Loading uri='classpath:/test-props/testLoader-doesnotexist.properties' flags=[NO_REQUIRE] specified with key: '_load_noexist' in uri='classpath:/test-props/testLoader.properties'
					[DEBUG] Missing uri='classpath:/test-props/testLoader-doesnotexist.properties' flags=[NO_REQUIRE]
					[DEBUG] Loading uri='classpath:/test-props/testLoader-sensitive.properties' flags=[SENSITIVE] specified with key: '_load_luggage' in uri='classpath:/test-props/testLoader.properties'
					[INFO ] Loaded  uri='classpath:/test-props/testLoader-sensitive.properties' flags=[SENSITIVE]
										""";
			assertEquals(expected, actual);
		}
	}

	@Test
	void testFailure() throws Exception {

		var logger = new TestLogger();
		var environment = new KeyValuesEnvironment() {
			@Override
			public Logger getLogger() {
				return logger;
			}
		};

		try {
			KeyValuesSystem.builder()
				.environment(environment)
				.build() //
				.loader() //
				.add("classpath:/test-props/testFailure.properties")
				.load();
			fail();
		}
		catch (IOException e) {
			String expected = """
					Resource not found. resource: uri='classpath:/test-props/testLoader-doesnotexist.properties'
						<-- specified with key: '_load_noexist' in uri='classpath:/test-props/testFailure.properties'""";
			String actual = e.getMessage();
			assertEquals(expected, actual);
		}
	}

	@Test
	void testKeyValuesResourceToString() {
		var system = KeyValuesResource.builder(URI.create("system:///"))
			.name("system")
			.noInterpolation(true)
			.noAddKeyValues(true)
			.sensitive(true)
			.parameter("custom", "something")
			.build();
		String actual = system.toString();
		String expected = """
				DefaultKeyValuesResource[uri=system:///, name=system, loadFlags=[NO_ADD, NO_INTERPOLATE, SENSITIVE], reference=null, mediaType=null, parameters=MapParameters[map={custom=something}]]"""
			.trim();
		assertEquals(expected, actual);
	}

	static class TestLogger implements Logger {

		final List<String> events = new ArrayList<>();

		public TestLogger() {
			super();
		}

		// public void clear() {
		// events.clear();
		// }
		//
		// public List<String> getEvents() {
		// return events;
		// }

		@Override
		public void debug(String message) {
			events.add("[DEBUG] " + message);
		}

		@Override
		public void info(String message) {
			events.add("[INFO ] " + message);
		}

		@Override
		public void warn(String message) {
			events.add("[WARN ] " + message);
		}

		@Override
		public String toString() {
			return events.stream().map(line -> line + "\n").collect(Collectors.joining());
		}

	}

}
