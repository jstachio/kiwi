package io.jstach.kiwi.boot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValue;
import io.jstach.kiwi.kvs.KeyValuesEnvironment;
import io.jstach.kiwi.kvs.KeyValuesEnvironment.Logger;
import io.jstach.kiwi.kvs.KeyValuesSystem;

public interface KiwiConfig  {
	
	@Nullable String getProperty(String key);
	String describe(String key);
	Map<String,String> toMap();
	public void reload();
	
	public static KiwiConfig of() {
		return WrapperKiwiConfig.CONFIG;
	}
}

class QueueLogger implements Logger {
	private final ConcurrentLinkedQueue<Event> events = new ConcurrentLinkedQueue<>();
	record Event(System.Logger.Level level, String message) {
	}
	
	@Override
	public void debug(
			String message) {
		events.add(new Event(System.Logger.Level.DEBUG, message));
		
	}
	@Override
	public void info(
			String message) {
		events.add(new Event(System.Logger.Level.INFO, message));
	}
	@Override
	public void closed(
			KeyValuesSystem system) {
		System.Logger logger = System.getLogger("io.jstach.kiwi.boot.KiwiConfig");
		Event e;
		while ((e = events.poll()) != null) {
			logger.log(e.level, e.message);
		}
	}
	@Override
	public void fatal(
			Exception exception) {
		var err = System.err;
		Event e;
		while ((e = events.poll()) != null) {
			err.println("[" + e.level + "] io.jstach.kiwi.boot.KiwiConfig - " + e.message);
		}
		err.println("[ERROR] io.jstach.kiwi.boot.KiwiConfig - " + exception.getMessage());
		exception.printStackTrace(err);
	}
	
}

class WrapperKiwiConfig implements KiwiConfig {
	
	
	volatile KiwiConfig config;
	static KiwiConfig CONFIG = load();

	public WrapperKiwiConfig(
			KiwiConfig config) {
		super();
		this.config = config;
	}

	@Override
	public @Nullable String getProperty(
			String key) {
		return config.getProperty(key);
	}

	@Override
	public String describe(
			String key) {
		return config.describe(key);
	}

	@Override
	public Map<String, String> toMap() {
		return config.toMap();
	}
	
	@Override
	public void reload() {
		config = load();
		
	}
	
	static WrapperKiwiConfig load() {
		
		record BootEnvironment(QueueLogger logger) implements KeyValuesEnvironment{
			@Override
			public Logger getLogger() {
				return this.logger;
			}
		}
		var logger = new QueueLogger();
		try (
				var system = KeyValuesSystem.builder() //
						.useServiceLoader()
						.environment(new BootEnvironment(logger)).build()) {
			var kvs = system //
					.loader()
					.add("classpath:/application.properties")
					.load();
			Map<String, KeyValue> m = new LinkedHashMap<>();
			for (var kv : kvs) {
				m.put(kv.key(), kv);
			}
			return new WrapperKiwiConfig(new DefaultKiwiConfig("application.properties", m));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}

class DefaultKiwiConfig implements KiwiConfig {
	
	private final String description;
	private final Map<String, KeyValue> keyValues;

	public DefaultKiwiConfig(
			String description,
			Map<String, KeyValue> keyValues) {
		super();
		this.description = description;
		this.keyValues = keyValues;
	}

	@Override
	public @Nullable String getProperty(
			String key) {
		var kv = keyValues.get(key);
		if (kv != null) {
			return kv.expanded();
		}
		return null;
	}

	@Override
	public String describe(
			String key) {
		return this.description;
	}

	@Override
	public Map<String, String> toMap() {
		return toMap(keyValues);
	}
	
	private static Map<String,String> toMap(Map<String,KeyValue> kvs) {
		Map<String,String> m = new LinkedHashMap<>();
		for (var e : kvs.entrySet()) {
			m.put(e.getKey(), e.getValue().value());
		}
		return Map.copyOf(m);
	}
	@Override
	public void reload() {
		throw new UnsupportedOperationException();
		
	}
	
}

