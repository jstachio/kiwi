package io.jstach.ezkv.kvs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintStream;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

class KeyValuesTest {

	static PrintStream out = Objects.requireNonNull(System.out);

	@Test
	void testExpand() {

		// Create a builder and add key-value pairs
		KeyValues.Builder builder = KeyValues.builder();
		builder.add("key1", "first");
		builder.add("key2", "${key1}-${key3}");
		builder.add("key1", "value1");

		// Build the KeyValues collection
		KeyValues kvs = builder.build();

		var formatter = KeyValuesMedia.ofProperties().formatter();
		{
			String actual = formatter.format(kvs);
			String expected = """
					key1=first
					key2=${key1}-${key3}
					key1=value1
					""";
			assertEquals(expected, actual);
		}

		// Interpolate values using a Variables map
		Variables variables = Variables.builder().add("key1", "interpolated").add("key3", "value3").build();

		KeyValues expanded = kvs.expand(variables);

		// Convert to a map
		{
			Map<String, String> actual = expanded.toMap();
			out.println(actual); // {key1=value1, key2=value1-value3}

			Map<String, String> expected = Map.of("key1", "value1", "key2", "value1-value3");
			assertEquals(expected, actual);
		}
		{
			String actual = formatter.format(expanded);
			String expected = """
					key1=first
					key2=value1-value3
					key1=value1
					""";
			assertEquals(expected, actual);
		}

	}

}
