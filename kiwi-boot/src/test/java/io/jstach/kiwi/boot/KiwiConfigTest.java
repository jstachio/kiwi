package io.jstach.kiwi.boot;

import java.util.Map;

import org.junit.jupiter.api.Test;

class KiwiConfigTest {

	@Test
	void testOf() {
		var m = Map.of("logging.systemlogger.initialize", "true", "logging.systemlogger.intialize", "true",
				"logging.level.io.jstach.kiwi", "DEBUG");
		KiwiConfig.setDefaultProperties(m);
		var config = KiwiConfig.of();
		config.reload();

	}

}
