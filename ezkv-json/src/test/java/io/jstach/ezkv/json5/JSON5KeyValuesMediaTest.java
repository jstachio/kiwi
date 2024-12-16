package io.jstach.ezkv.json5;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JSON5KeyValuesMediaTest {

	@Test
	void testParseStringBiConsumerOfStringString() {
		var parser = new JSON5KeyValuesMedia().parser();
		String input = """
				{
				  "a": {
				    "b": [1, 2, {"c": 3}],
				    "d": "value"
				  }
				}
				""";
		String expected = """
				
				""";
		String actual = parser.parse(input).toString();
		
		assertEquals(expected, actual);
	}

}
