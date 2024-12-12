package io.jstach.ezkv.dotenv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.jstach.ezkv.kvs.KeyValuesSystem;

class DotEnvKeyValuesMediaTest {

	static Map<String,String> values = Map.of();
	
	@BeforeAll
	static void beforeAll() throws NoSuchFileException, FileNotFoundException, IOException {
		var kvs = KeyValuesSystem.builder()
				.useServiceLoader()
				.build()
				.loader()
				.add("classpath:/test.env")
				.load();
		values = kvs.toMap();
	}
	
	@ParameterizedTest
	@EnumSource(EnvKeyTest.class)
	void test(EnvKeyTest test) throws NoSuchFileException, FileNotFoundException, IOException {
		assertEquals(test.expected, values.get(test.name()));
	}
	
	enum EnvKeyTest {
		BASIC("basic"),
		AFTER_LINE("after_line"),
		EMPTY(""),
		EMPTY_SINGLE_QUOTES(""),
		EMPTY_DOUBLE_QUOTES(""),
		EMPTY_BACKTICKS(""),
		SINGLE_QUOTES("single_quotes"),
		SINGLE_QUOTES_SPACED("    single quotes    "),
		DOUBLE_QUOTES("double_quotes"),
		DOUBLE_QUOTES_SPACED("    double quotes    "),
		DOUBLE_QUOTES_INSIDE_SINGLE("double \"quotes\" work inside single quotes"),
		DOUBLE_QUOTES_WITH_NO_SPACE_BRACKET("{ port: $MONGOLAB_PORT}"),
		SINGLE_QUOTES_INSIDE_DOUBLE("single 'quotes' work inside double quotes"),
		BACKTICKS_INSIDE_SINGLE("`backticks` work inside single quotes"),
		BACKTICKS_INSIDE_DOUBLE("`backticks` work inside double quotes"),
		BACKTICKS("backticks"),
		BACKTICKS_SPACED("    backticks    "),
		DOUBLE_QUOTES_INSIDE_BACKTICKS("double \"quotes\" work inside backticks"),
		SINGLE_QUOTES_INSIDE_BACKTICKS("single 'quotes' work inside backticks"),
		DOUBLE_AND_SINGLE_QUOTES_INSIDE_BACKTICKS("double \"quotes\" and single \'quotes\' work inside backticks"),
		EXPAND_NEWLINES("expand\nnew\nlines"),
		DONT_EXPAND_UNQUOTED("dontexpand\\nnewlines"),
		DONT_EXPAND_SQUOTED("dontexpand\\nnewlines"),
		INLINE_COMMENTS("inline comments"),
		INLINE_COMMENTS_SINGLE_QUOTES("inline comments outside of #singlequotes"),
		INLINE_COMMENTS_DOUBLE_QUOTES("inline comments outside of #doublequotes"),
		INLINE_COMMENTS_BACKTICKS("inline comments outside of #backticks"),
		INLINE_COMMENTS_SPACE("inline comments start with a"),
		EQUAL_SIGNS("equals=="),
		RETAIN_INNER_QUOTES("""  
				{"foo": "bar"}"""),
		RETAIN_INNER_QUOTES_AS_STRING("""
				{"foo": "bar"}"""),
		RETAIN_INNER_QUOTES_AS_BACKTICKS("""
				{"foo": "bar's"}"""),
		TRIM_SPACE_FROM_UNQUOTED("some spaced out string"),
		USERNAME("therealnerdybeast@example.tld"),
		SPACED_KEY("parsed"),

		;
		final String expected;

		private EnvKeyTest(
				String expected) {
			this.expected = expected;
		}
		
	}

}
