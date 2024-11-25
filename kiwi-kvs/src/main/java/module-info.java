/*
 * SPDX-License-Identifier: BSD-3-Clause
 *
 * Copyright (c) 2024 Adam Gent
 * All rights reserved.
 */

/**
 * The {@code io.jstach.kiwi.kvs} module provides the core library for Kiwi, a non-opinionated
 * Java configuration system that supports recursive chain loading of configuration from key-value
 * pairs. This module is designed for use cases where early-stage configuration is needed, even
 * before application logging is set up.
 *
 * <p>Kiwi focuses on loading streams of key-value pairs (represented as {@link io.jstach.kiwi.kvs.KeyValues})
 * from various URI-based resources, such as classpath resources, files, system properties, and environment
 * variables. The system is highly configurable and can be extended to support additional media types and
 * URI patterns using the {@link java.util.ServiceLoader} mechanism.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Zero opinions: No assumptions on loading order or resource names; fully user-defined.</li>
 *   <li>Zero dependencies (besides {@code java.base} and {@code org.jspecify} for nullability annotations).</li>
 *   <li>Immutable and thread-safe design, except for {@code Builder} classes.</li>
 *   <li>Simple interpolation mechanism, configurable through key-value pairs.</li>
 *   <li>Support for sensitive data handling and flexible resource chaining.</li>
 *   <li>Extensibility through custom media type handlers and URI schemes.</li>
 * </ul>
 *
 * <h2>Maven Coordinates</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>io.jstach.kiwi</groupId>
 *     <artifactId>kiwi-kvs</artifactId>
 *     <version>1.0.0</version>
 * </dependency>
 * }</pre>
 *
 * <h2>Basic Usage</h2>
 * Example of loading key-value pairs from various sources:
 * {@snippet :
 * var kvs = KeyValuesSystem.defaults()
 *     .loader()
 *     .add("classpath:/start.properties")
 *     .add("system:///")                   // add system properties to override
 *     .add("env:///")                      // add environment variables to override
 *     .add("cmd:///?_filter_sed=s/^-D//")  // add command line arguments with the -D prefix
 *     .load();
 *
 * // Convert to a map and use it with other frameworks:
 * var map = kvs.toMap();
 * ConfigurableEnvironment env = applicationContext.getEnvironment();
 * env.getPropertySources().addFirst(new MapPropertySource("start", map));
 * }
 *
 * <h2>Example Properties Files</h2>
 * <h3>start.properties</h3>
 * {@snippet lang = properties :
 * message=Hello ${user.name}
 * _load_foo=classpath:/foo.properties
 * port.prefix=1
 * }
 *
 * <h3>foo.properties</h3>
 * {@snippet lang = properties :
 * user.name=Barf
 * message=Merchandising
 * db.port=${port.prefix}5672
 * _load_user=file:/${user.home}/.config/myapp/user.properties
 * _flags_user=sensitive,no_require
 * }
 *
 * <h3>user.properties</h3>
 * {@snippet lang = properties :
 * secret=12345 # my luggage combination
 * port.prefix=3
 * }
 *
 * <h2>Technical Details</h2>
 * <ul>
 *   <li>All classes, except for {@code Builder} classes, are immutable and thread-safe,
 *   ensuring safe concurrent use.</li>
 *   <li>Null values are neither returned nor accepted as input, unless explicitly marked
 *   with {@link org.jspecify.annotations.Nullable}.</li>
 * </ul>
 *
 * <h2>Extensibility</h2>
 * The module supports extension through {@link io.jstach.kiwi.kvs.KeyValuesServiceProvider},
 * which can be implemented to add custom media types and URI loaders. Implementations can
 * be discovered and loaded using the {@link java.util.ServiceLoader} mechanism.
 *
 * @uses io.jstach.kiwi.kvs.KeyValuesServiceProvider
 * @since 1.0
 * @author agentgt
 */
module io.jstach.kiwi.kvs {
	exports io.jstach.kiwi.kvs;
	
	requires org.jspecify;
	
	uses io.jstach.kiwi.kvs.KeyValuesServiceProvider;
}