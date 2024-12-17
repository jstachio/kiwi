package io.jstach.ezkv.json5;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;

import io.jstach.ezkv.json5.JSON5KeyValuesMedia.ArrayKeyOption;
import io.jstach.ezkv.kvs.KeyValuesMedia;
import io.jstach.ezkv.kvs.Variables;

class JSON5KeyValuesMediaTest {

	@Test
	void testToStringKvs() {
		var parser = new JSON5KeyValuesMedia().parser();
		String input = """
				{
					"a": {
						"b": [1, 2.0, {"c": 3}],
						"d": "value"
					}
				}
				""";
		String expected = """
				KeyValues[
				a.b=1
				a.b=2.0
				a.b.c=3
				a.d=value
				]
				""";
		String actual = parser.parse(input).toString();

		assertEquals(expected, actual);
	}

	@CartesianTest
	void test(@Enum JSON5Test test, @Enum ArrayKeyOption arrayKeyOption) {
		String input = test.input;
		var vars = Variables.builder();
		arrayKeyOption.set(vars);
		var parser = new JSON5KeyValuesMedia().parser(vars.build());

		String expected = switch (arrayKeyOption) {
			case ARRAY -> test.expectedArray;
			case DUPLICATE -> test.expectedDuplicate;
		};

		var actual = parser.parse(input).format(KeyValuesMedia.ofProperties());
		assertEquals(expected, actual);

	}

	enum JSON5Test {

		OBJECT("""
				{
				/*
				 * test comment
				 */
					"a": {
						"b": [1, 2.0, {"c": 3}],
						"d": "value"
					}
				}
				""", //
				"""
						a.b=1
						a.b=2.0
						a.b.c=3
						a.d=value
						""", //
				"""
						a.b[0]=1
						a.b[1]=2.0
						a.b[2].c=3
						a.d=value
						"""), //

		ARRAY("""
				[ "a", "b", {
					c : [1,2.0],
					d : "value"
					}
				]
				""", //
				"""
						0=a
						1=b
						2.c=1
						2.c=2.0
						2.d=value
						""", //
				"""
						0=a
						1=b
						2.c[0]=1
						2.c[1]=2.0
						2.d=value
						"""),
		DUPLICATE("""
				{
				"a" : 1,
				"a" :2,
				"c" : {
					last : "false"
				},
				"b" : [1, 2],
				"c" : {
					first : "true"
					},
				"b" : [3, 4],
				"b" : [5, 6],
				"d" : true,
				},
				""", //
				"""
						a=1
						a=2
						c.last=false
						c.first=true
						b=1
						b=2
						b=3
						b=4
						b=5
						b=6
						d=true
						""", //
				"""
						a[0]=1
						a[1]=2
						c[0].last=false
						c[1].first=true
						b[0][0]=1
						b[0][1]=2
						b[1][0]=3
						b[1][1]=4
						b[2][0]=5
						b[2][1]=6
						d=true
						""")

		;

		private final String input;

		private final String expectedDuplicate;

		private final String expectedArray;

		private JSON5Test(String input, String expectedDuplicate, String expectedArray) {
			this.input = input;
			this.expectedDuplicate = expectedDuplicate;
			this.expectedArray = expectedArray;
		}

	}

}
