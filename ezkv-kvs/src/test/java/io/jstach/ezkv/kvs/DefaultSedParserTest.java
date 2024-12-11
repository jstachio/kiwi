package io.jstach.ezkv.kvs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.PrintStream;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import io.jstach.ezkv.kvs.DefaultSedParser.Command;
import io.jstach.ezkv.kvs.DefaultSedParser.Tokenizer;

class DefaultSedParserTest {

	static PrintStream out = Objects.requireNonNull(System.out);

	@Test
	void testSubstitutionCommandWithoutAddress() {
		{
			String input = "s/foo/bar/";
			Command command = DefaultSedParser.parseCommand(input);
			// Test execution
			String expected = "barbaz";
			String actual = command.execute("foobaz");
			assertEquals(expected, actual);

			actual = command.execute("foobar");
			expected = "barbar";

			assertEquals(expected, actual);
		}
		{
			String input = "s|foo|bar|g";
			Command command = DefaultSedParser.parseCommand(input);
			assertEquals("barbaz", command.execute("foobaz"));
			assertEquals("barbar", command.execute("foofoo"));
		}
	}

	@Test
	void testSubstitutionCommandWithAddress() {
		String input = "/baz/ s/foo/bar/g";
		Command command = DefaultSedParser.parseCommand(input);

		// Test execution
		assertEquals("barbazbar", command.execute("foobazfoo")); // Global
																	// replacement
		assertEquals("nomatch", command.execute("nomatch")); // Address does
																// not match
	}

	@Test
	void testDeleteCommandWithoutAddress() {
		String input = "d";
		Command command = DefaultSedParser.parseCommand(input);

		assertNotNull(command); // Command should not be null
		assertNull(command.execute("anyline")); // Always deletes
		assertNull(command.execute("anotherline")); // Always deletes
	}

	@Test
	void testDeleteCommandWithAddress() {
		String input = "/delete/ d";
		Command command = DefaultSedParser.parseCommand(input);

		assertNotNull(command); // Command should not be null

		// Delete when the address matches
		assertNull(command.execute("this line contains delete"));

		// Retain the line when the address does not match
		assertEquals("this line is fine", command.execute("this line is fine"));
	}

	@Test
	void testDeleteCommandEdgeCases() {
		String input = "/^start/ d"; // Matches lines that start with "start"
		Command command = DefaultSedParser.parseCommand(input);

		assertNotNull(command); // Command should not be null

		// Match: Delete lines starting with "start"
		assertNull(command.execute("start of something"));

		// No Match: Retain other lines
		assertEquals("not starting with that", command.execute("not starting with that"));
	}

	@Test
	void testInvalidDeleteCommandSyntax() {
		String input = "/unclosed d";
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			DefaultSedParser.parseCommand(input);
		});

		assertEquals("Unterminated regex address", exception.getMessage());
	}

	@Test
	void testInvalidCommand() {
		String input = "/match/ x/foo/bar/";
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			out.println(new Tokenizer(input).tokenize());
			DefaultSedParser.parseCommand(input);
		});

		assertEquals("Unsupported command: x", exception.getMessage());
	}

	@Test
	void testSubstitutionCommandInvalidSyntax() {
		String input = "s/foo/bar";
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			out.println(new Tokenizer(input).tokenize());
			DefaultSedParser.parseCommand(input);
		});

		assertEquals("Expected closing delimiter", exception.getMessage());
	}

}
