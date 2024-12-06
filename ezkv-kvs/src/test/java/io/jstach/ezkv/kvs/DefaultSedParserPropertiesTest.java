package io.jstach.ezkv.kvs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Objects;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.jstach.ezkv.kvs.DefaultSedParser.Command;
import io.jstach.ezkv.kvs.DefaultSedParser.Tokenizer;

class DefaultSedParserPropertiesTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("provideTestCases")
	void testSedParser(String testName, String sedPattern, String input, @Nullable String expectedOutput) {
		try {
			Command command = DefaultSedParser.parseCommand(sedPattern);
			// Execute the command and verify the output
			String result = command.execute(input);
			if (!Objects.equals(result, expectedOutput)) {
				System.out.format("test: %s, pattern: %s, input: %s, expected: %s\n", testName, sedPattern, input,
						expectedOutput);
				System.out.println(command);
				System.out.println(new Tokenizer(sedPattern).tokenize());
			}
			assertEquals(expectedOutput, result);
		}
		catch (SedParserException e) {
			String result = e.error.toString();
			if (!Objects.equals(result, expectedOutput)) {
				System.out.format("test: %s, pattern: %s, input: %s, expected: %s\n", testName, sedPattern, input,
						expectedOutput);
			}
			assertEquals(expectedOutput, result);
		}
	}

	private static Stream<@Nullable Object[]> provideTestCases() {
		String testData = """
				Substitute_single_occurrence.pattern=s/foo/bar/
				Substitute_single_occurrence.input=foo is here
				Substitute_single_occurrence.expect=bar is here

				Substitute_no_match.pattern=s/foo/bar/
				Substitute_no_match.input=no match here
				Substitute_no_match.expect=no match here

				Substitute_global_occurrence.pattern=s/foo/bar/g
				Substitute_global_occurrence.input=foo foo foo
				Substitute_global_occurrence.expect=bar bar bar

				Substitute_with_address_match.pattern=/match/ s/foo/bar/
				Substitute_with_address_match.input=this is a match foo
				Substitute_with_address_match.expect=this is a match bar

				Delete_no_address.pattern=d
				Delete_no_address.input=delete this line
				Delete_no_address.expect=null

				Delete_with_address_match.pattern=/delete/ d
				Delete_with_address_match.input=this line contains delete
				Delete_with_address_match.expect=null

				Delete_with_address_no_match.pattern=/delete/ d
				Delete_with_address_no_match.input=this line is safe
				Delete_with_address_no_match.expect=this line is safe

				# Edge Cases
				Delete_regex_start_match.pattern=/^start/ d
				Delete_regex_start_match.input=start of something
				Delete_regex_start_match.expect=null

				Delete_regex_start_no_match.pattern=/^start/ d
				Delete_regex_start_no_match.input=does not start here
				Delete_regex_start_no_match.expect=does not start here

				Substitute_escaped_delimiter.pattern=s/foo\\\\/bar/baz/
				Substitute_escaped_delimiter.input=foo/bar here
				Substitute_escaped_delimiter.expect=baz here

				Delete_escaped_regex.pattern=/escaped\\\\/slash/ d
				Delete_escaped_regex.input=contains escaped/slash here
				Delete_escaped_regex.expect=null

				Delete_no_match_escaped_regex.pattern=/escaped\\\\/slash/ d
				Delete_no_match_escaped_regex.input=no match for escaped slash
				Delete_no_match_escaped_regex.expect=no match for escaped slash

				# Error Cases
				Error_unterminated_regex.pattern=/unterminated d
				Error_unterminated_regex.input=any input
				Error_unterminated_regex.expect=INVALID_REGEX_ADDRESS

				Error_invalid_command.pattern=/match/ x/foo/bar/
				Error_invalid_command.input=invalid command test
				Error_invalid_command.expect=INVALID_COMMAND

				Error_missing_delimiter.pattern=s/foo/bar
				Error_missing_delimiter.input=missing delimiter
				Error_missing_delimiter.expect=MISSING_CLOSING_DELIMITER

				Error_no_command_after_address.pattern=/address_only/
				Error_no_command_after_address.input=no command provided
				Error_no_command_after_address.expect=COMMAND_EXPECTED

				Error_invalid_flag.pattern=s/foo/bar/q
				Error_invalid_flag.input=invalid flag test
				Error_invalid_flag.expect=INVALID_FLAG
				""";

		var kvs = KeyValuesMedia.ofProperties().parser().parse(testData);
		var properties = kvs.toMap();

		return properties.keySet()
			.stream()
			.map(name -> name.split("\\.", 2)[0]) // Extract the test_name
			.distinct()
			.map(testName -> new Object[] { testName, require(properties.get(testName + ".pattern")),
					require(properties.get(testName + ".input")),
					parseExpected(require(properties.get(testName + ".expect"))) });
	}

	private static <T> T require(@Nullable T v) {
		if (v == null) {
			throw new AssertionError();
		}
		return v;
	}

	private static @Nullable String parseExpected(String expected) {
		return "null".equals(expected) ? null : expected;
	}

}
