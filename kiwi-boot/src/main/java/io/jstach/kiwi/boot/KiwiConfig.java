package io.jstach.kiwi.boot;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.KeyValue;
import io.jstach.kiwi.kvs.KeyValues;
import io.jstach.kiwi.kvs.KeyValuesEnvironment;
import io.jstach.kiwi.kvs.KeyValuesEnvironment.Logger;
import io.jstach.kiwi.kvs.KeyValuesSystem;
import io.jstach.kiwi.kvs.Variables;

public sealed interface KiwiConfig {

	@Nullable
	String getProperty(String key);

	String describe(String key);

	Map<String, String> toMap();

	public static void setDefaultProperties(Map<String, String> properties) {
		Holder.PROPERTIES = KeyValues.builder().add(properties.entrySet()).build();
	}

	public static ReloadableConfig of() {
		return Holder.Hidden.CONFIG;
	}
	
	non-sealed interface ReloadableConfig extends KiwiConfig {
		public StableConfig stableConfig();
		public KiwiConfig reload();
	}
	
	non-sealed interface StableConfig extends KiwiConfig {
		
	}

}

class QueueLogger implements Logger {

	private final ConcurrentLinkedQueue<Event> events = new ConcurrentLinkedQueue<>();

	record Event(System.Logger.Level level, String message) {
	}

	@Override
	public void init(
			KeyValuesSystem system) {
		events.add(new Event(System.Logger.Level.INFO, "Initializing"));
	}
	
	@Override
	public void debug(String message) {
		events.add(new Event(System.Logger.Level.DEBUG, message));

	}

	@Override
	public void info(String message) {
		events.add(new Event(System.Logger.Level.INFO, message));
	}
	
	@Override
	public void warn(
			String message) {
		events.add(new Event(System.Logger.Level.WARNING, message));
	}

	@Override
	public void closed(KeyValuesSystem system) {
		System.Logger logger = System.getLogger("io.jstach.kiwi.boot.KiwiConfig");
		Event e;
		while ((e = events.poll()) != null) {
			logger.log(e.level, e.message);
		}
	}

	@Override
	public void fatal(Exception exception) {
		var err = System.err;
		Event e;
		while ((e = events.poll()) != null) {
			err.println("[" + Logger.formatLevel(e.level) + "] io.jstach.kiwi.boot.KiwiConfig - " + e.message);
		}
		err.println("[ERROR] io.jstach.kiwi.boot.KiwiConfig - " + exception.getMessage());
		exception.printStackTrace(err);
	}

}

class Holder {
	static volatile @Nullable KeyValues PROPERTIES;
	static class Hidden {
		final static KiwiConfig.ReloadableConfig CONFIG = new WrapperKiwiConfig(load());
		
		static KiwiConfig.StableConfig load() {

			record BootEnvironment(QueueLogger logger) implements KeyValuesEnvironment {
				@Override
				public Logger getLogger() {
					return this.logger;
				}
			}
			var logger = new QueueLogger();
			try (var system = KeyValuesSystem.builder() //
				.useServiceLoader()
				.environment(new BootEnvironment(logger))
				.build()) {
				var properties = Holder.PROPERTIES;

				var loader = system //
					.loader();
				if (properties != null) {
					loader.add("setDefaultProperties", properties);
				}
				var kvs = loader
						.variables(Variables::ofSystemProperties)
						.variables(Variables::ofSystemEnv)
						.add("classpath:/application.properties", b -> b.name("application").noRequire(true))
						.add("profile.classpath:/application-__PROFILE__.properties", b -> b.name("profiles").noRequire(true))
					.load();
				Map<String, KeyValue> m = new LinkedHashMap<>();
				for (var kv : kvs) {
					m.put(kv.key(), kv);
				}
				var config = new DefaultKiwiConfig("application.properties", m);
				config.setSystemProperties(system);
				return config;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}

class WrapperKiwiConfig implements KiwiConfig.ReloadableConfig {

	volatile KiwiConfig.StableConfig config;

	public WrapperKiwiConfig(KiwiConfig.StableConfig config) {
		super();
		this.config = config;
	}

	@Override
	public @Nullable String getProperty(String key) {
		return config.getProperty(key);
	}

	@Override
	public String describe(String key) {
		return config.describe(key);
	}

	@Override
	public Map<String, String> toMap() {
		return config.toMap();
	}
	
	@Override
	public StableConfig stableConfig() {
		return config;
	}

	@Override
	public KiwiConfig reload() {
		var c = config;
		config = Holder.Hidden.load();
		return c;

	}

}

class DefaultKiwiConfig implements KiwiConfig.StableConfig {

	private final String description;

	private final Map<String, KeyValue> keyValues;

	public DefaultKiwiConfig(String description, Map<String, KeyValue> keyValues) {
		super();
		this.description = description;
		this.keyValues = keyValues;
	}

	@Override
	public @Nullable String getProperty(String key) {
		var kv = keyValues.get(key);
		if (kv != null) {
			return kv.expanded();
		}
		return null;
	}

	@Override
	public String describe(String key) {
		return this.description;
	}

	@Override
	public Map<String, String> toMap() {
		return toMap(keyValues);
	}

	private static Map<String, String> toMap(Map<String, KeyValue> kvs) {
		Map<String, String> m = new LinkedHashMap<>();
		for (var e : kvs.entrySet()) {
			m.put(e.getKey(), e.getValue().value());
		}
		return Map.copyOf(m);
	}
	
	void setSystemProperties(KeyValuesSystem system) {
		var logger = system.environment().getLogger();
		Map<String, String> properties = toMap();

		Map<String, String> propertiesToBeSet = new LinkedHashMap<>();

		final Set<String> commandLineProperties = getJvmCommandLinePropertyNames();
		for (Entry<String, String> e : properties.entrySet()) {
			if (commandLineProperties.contains(e.getKey())) {
				logger.info( "Not overriding command line property: " + e.getKey());
			}
			else if (denyPropertyToBeOverridden(e.getKey())) {
				logger.info("Not overriding restricted property: " + e.getKey());
			}
			else {
				if (Objects.isNull(e.getValue())) {
					logger.warn("Property key: " + e.getKey() + " is null");
				}
				else {
					propertiesToBeSet.put(e.getKey(), e.getValue());
				}
			}
		}
		propertiesToBeSet.put("SYSTEM_PROPERTIES_LOADED", "true");
		var systemProperties = system.environment().getSystemProperties();
		for (var e : propertiesToBeSet.entrySet()) {
			systemProperties.setProperty(e.getKey(), e.getValue());
		}
		
	}
	
	static boolean denyPropertyToBeOverridden(
			@Nullable String property) {
		if (property == null)
			return true;

		return (property.startsWith("java.")
				|| property.startsWith("sun.")
				|| property.equals("user.dir")
				|| property.equals("user.name")
				|| property.equals("path.separator")
				|| property.startsWith("os."));
	}
	
	Set<String> getJvmCommandLinePropertyNames() {
		if (!isJmxModuleAvailable()) {
			return Collections.emptySet();
		}
		try {
			List<String> args = ManagementFactory.getRuntimeMXBean()
				.getInputArguments();
			if (args.isEmpty()) {
				return Collections.emptySet();
			}
			Set<String> props = new HashSet<>(args.size());
			for (String s : args) {
				s = s.trim();
				if (!s.startsWith("-D")) {
					continue;
				}
				int end = s.indexOf("=");
				props.add(s.substring(2, end > 3 ? end : s.length()));
			}
			return props;
		}
		catch (Throwable e) {
			return Collections.emptySet();
		}
	}

	private static boolean isJmxModuleAvailable() {
		ModuleLayer bootLayer = ModuleLayer.boot();
		return bootLayer.findModule("java.management")
			.isPresent();
	}

}
