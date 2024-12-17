package io.jstach.ezkv.json5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;

import io.jstach.ezkv.json5.JSON5KeyValuesMedia.ArrayKeyOption;
import io.jstach.ezkv.json5.internal.JSONException;
import io.jstach.ezkv.kvs.KeyValues;
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

	@Test
	void testFormatter() {
		String properties = """
				a.b=1
				a.b=2.0
				a.b.c=3
				""";
		var b = KeyValues.builder();
		KeyValuesMedia.ofProperties().parser().parse(properties, b::add);
		var kvs = b.build();
		String actual = kvs.format(new JSON5KeyValuesMedia());
		String expected = """
				{
				  "a.b": [
				    "1",
				    "2.0"
				  ],
				  "a.b.c": "3"
				}
				""";
		assertEquals(expected.trim(), actual.trim());
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
				// Some comment
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
				}
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
						"""),

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

	@CartesianTest
	void testNumber(@Enum JSON5Number test, @Enum JSONRaw raw) {
		String input = test.json();
		var vars = Variables.builder();
		String rawParam = switch (raw) {
			case RAW -> "true";
			case JAVA -> "false";
		};
		vars.accept(JSON5KeyValuesMedia.NUMBER_RAW_PARAM, rawParam);
		var parser = new JSON5KeyValuesMedia().parser(vars.build());
		var kvs = parser.parse(input);
		String expected = switch (raw) {
			case RAW -> test.raw;
			case JAVA -> test.expected;
		};
		String actual = test.value(kvs);
		assertEquals(expected, actual);
	}

	enum JSONRaw {

		RAW, JAVA

	}

	enum JSON5Number {

		ZERO("0", "0"), //
		INTEGER("123", "123"), //
		WITH_FRACTION_PART("123.456", "123.456"), //
		ONLY_FRACTION_PART(".456", "0.456", ".456"), //
		WITH_EXPONENT("123e-456", "1.23E+458", "123e-456"), //
		POSITIVE_HEX("0xdecaf", "912559", "0xdecaf"), //
		NEGATIVE_HEX("-0xC0FFEE", "-12648430", "-0xC0FFEE"), //
		POSITIVE_INFINITY("Infinity", "Infinity"), //
		NEGATIVE_INFINITY("-Infinity", "-Infinity"), //
		NAN("NaN", "NaN"),

		;

		private final String number;

		private final String expected;

		private final String raw;

		private JSON5Number(String number, String expected) {
			this(number, expected, expected);
		}

		private JSON5Number(String number, String expected, String raw) {
			this.number = number;
			this.expected = expected;
			this.raw = raw;
		}

		String json() {
			return "{ a :" + number + "}";
		}

		@Nullable
		String value(KeyValues kvs) {
			return kvs.toMap().get("a");
		}

	}

	@CartesianTest
	void testLiteral(@Enum JSON5Literal test) {
		String input = test.json();
		var parser = new JSON5KeyValuesMedia().parser();
		var kvs = parser.parse(input);
		String expected = test.expected;
		String actual = test.value(kvs);
		assertEquals(expected, actual);
	}

	enum JSON5Literal {

		FALSE("false", "false"), TRUE("true", "true"), NULL("null", null);

		private final String literal;

		private final @Nullable String expected;

		private JSON5Literal(String literal, String expected) {
			this.literal = literal;
			this.expected = expected;
		}

		String json() {
			return "{ a :" + literal + "}";
		}

		@Nullable
		String value(KeyValues kvs) {
			return kvs.toMap().get("a");
		}

	}

	@CartesianTest
	void testEscape(@Enum StringEscapes test) {
		String input = test.json();
		var parser = new JSON5KeyValuesMedia().parser();
		var kvs = parser.parse(input);
		String expected = test.expected;
		String actual = test.value(kvs);
		assertEquals(expected, actual);
	}

	enum StringEscapes {

		APOSTROPHE("\\'", "'"), //
		Quotation_MARK("\\\"", "\""), //
		Reverse_solidus("\\\\", "\\"), //
		Backspace("\\b", "\b"), //
		Form_feed("\\f", "\f"), //
		Line_feed("\\n", "\n"), //
		Carriage_return("\\r", "\r"), //
		Horizontal_tab("\\t", "\t"), //
		Vertical_tab("\\v", "\u000B"), //
		Null("\\0", "\u0000"), //
		Example_1("\\x5C", "\\"), //
		Example_2("\\uD83C\\uDFBC", "\uD83C\uDFBC");

		private final String literal;

		private final String expected;

		private StringEscapes(String literal, String expected) {
			this.literal = literal.trim();
			this.expected = expected;
		}

		String json() {
			return "{ a :\"" + literal + "\"}";
		}

		@Nullable
		String value(KeyValues kvs) {
			return kvs.toMap().get("a");
		}

	}

	@CartesianTest
	void testBadJson(@Enum BadJSON test) {
		String input = test.input;
		var parser = new JSON5KeyValuesMedia().parser();

		var e = assertThrows(JSONException.class, () -> {
			parser.parse(input);
		});
		assertEquals(test.message, e.getMessage());
	}

	enum BadJSON {

		JUST_CLOSE_BRACE("}", "Illegal value '}' at index 0 [character 1 in line 1]"),
		JUST_CLOSE_BRACKET("]", "Illegal value ']' at index 0 [character 1 in line 1]"),
		JUST_OPEN_BRACE("{", "A JSONObject must end with '}' at index 0 [character 1 in line 1]"),
		JUST_OPEN_BRACKET("[", "A JSONArray must end with ']' at index 0 [character 1 in line 1]"),
		MISSING_MEMBER_NAME("{ : 'blah'}", "Empty key at index 2 [character 3 in line 1]"),
		BAD_ARRAY_DELIM("['a' b]",
				"Expected ',' or ']' after value, got 'b' instead at index 5 [character 6 in line 1]"),
		BAD_CLOSE_ARRAY("[]]", "Illegal value ']' at index 2 [character 3 in line 1]"),
		BAD_CLOSE_OBJECT("{}}", "Illegal value '}' at index 2 [character 3 in line 1]"),

		;

		private final String input;

		private final String message;

		private BadJSON(String input, String message) {
			this.input = input;
			this.message = message;
		}

	}

}
