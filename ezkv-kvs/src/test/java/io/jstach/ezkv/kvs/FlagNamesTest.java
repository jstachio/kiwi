package io.jstach.ezkv.kvs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.jstach.ezkv.kvs.FlagNames;

class FlagNamesTest {

	@Test
	void testFill() {
		List<String> names = List.of("no_require", "optional");
		List<String> reverseNames = List.of("required");

		FlagNames result = new FlagNames(names, reverseNames);

		// Expected output
		List<String> expectedNames = List.of("NO_REQUIRE", "NOT_REQUIRE", "OPTIONAL", "NO_REQUIRED", "NOT_REQUIRED");
		List<String> expectedReverseNames = List.of("REQUIRE", "NO_OPTIONAL", "NOT_OPTIONAL", "REQUIRED");

		assertEquals(expectedNames, result.names());
		assertEquals(expectedReverseNames, result.reverseNames());
	}

	@Test
	void testIdempotent() {
		List<String> names = List.of("no_require", "optional");
		List<String> reverseNames = List.of("required");

		FlagNames result = new FlagNames(names, reverseNames);

		// Expected output
		List<String> expectedNames = List.of("NO_REQUIRE", "NOT_REQUIRE", "OPTIONAL", "NO_REQUIRED", "NOT_REQUIRED");
		List<String> expectedReverseNames = List.of("REQUIRE", "NO_OPTIONAL", "NOT_OPTIONAL", "REQUIRED");

		assertEquals(expectedNames, result.names());
		assertEquals(expectedReverseNames, result.reverseNames());
		result = new FlagNames(expectedNames, expectedReverseNames);

		assertEquals(expectedNames, result.names());
		assertEquals(expectedReverseNames, result.reverseNames());

	}

}
