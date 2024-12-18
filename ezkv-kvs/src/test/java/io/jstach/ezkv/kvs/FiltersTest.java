package io.jstach.ezkv.kvs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.jstach.ezkv.kvs.KeyValuesServiceProvider.KeyValuesFilter.Filter;
import io.jstach.ezkv.kvs.KeyValuesServiceProvider.KeyValuesFilter.FilterContext;
import io.jstach.ezkv.kvs.Variables.Parameters;

class FiltersTest {

	@Test
	void testJoin() {
		String properties = """
				a=1
				b=2
				a=3
				""";
		var kvs = KeyValuesMedia.ofProperties().parser().parse(properties);
		String filterName = "join";
		String expression = ",";

		var result = runFilter(kvs, filterName, expression);

		String actual = result.toString();
		String expected = """
				KeyValues[
				a=1,3
				b=2
				]
				""";
		assertEquals(expected, actual);
	}

	private KeyValues runFilter(KeyValues kvs, String filterName, String expression) {
		Filter filter = new Filter(filterName, expression, "blah");
		var s = KeyValuesSystem.builder().build();
		FilterContext context = new FilterContext(s.environment(), Parameters.of(Map.of()));
		var result = s.filter().filter(context, kvs, filter).orElseThrow();
		return result;
	}

}
