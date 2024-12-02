/**
 * Kiwi Key Value loading system that allows loading of key values from URI-based
 * resources.
 *
 * <p>
 * Kiwi's core domain is {@link io.jstach.ezkv.kvs.KeyValues}, which can be loaded using
 * the {@link io.jstach.ezkv.kvs.KeyValuesSystem}. This package provides the necessary
 * interfaces and classes for loading, parsing, and managing key-value pairs from a
 * variety of sources including classpath resources, files, and system properties.
 *
 * <h2>Technical Details</h2>
 * <ul>
 * <li>Most classes in this package, with the exception of the {@code Builder} classes,
 * are immutable and thread-safe. This ensures that instances can be used concurrently
 * across multiple threads without additional synchronization.</li>
 * <li>By default, null values are not returned or accepted as input in method parameters
 * unless explicitly marked with {@link org.jspecify.annotations.Nullable}. This helps
 * maintain consistency and reduce the risk of {@code NullPointerException}.</li>
 * <li>The design prioritizes immutability and thread safety to support reliable,
 * multi-threaded operations and to minimize side effects in application code.</li>
 * </ul>
 *
 * <p>
 * The package also supports extension through the
 * {@link io.jstach.ezkv.kvs.KeyValuesServiceProvider} interface, which allows for custom
 * implementations to be loaded using {@link java.util.ServiceLoader}. This enables
 * developers to extend Kiwi with additional media types or URI handling mechanisms.
 *
 * <h2>Key Interfaces and Classes</h2>
 * <ul>
 * <li>{@link io.jstach.ezkv.kvs.KeyValuesSystem} - The main entry point for creating and
 * configuring loaders for key-value resources.</li>
 * <li>{@link io.jstach.ezkv.kvs.KeyValuesLoader} - Responsible for loading key-value
 * pairs from configured sources.</li>
 * <li>{@link io.jstach.ezkv.kvs.Variables} - A function that maps keys to values,
 * primarily used for variable interpolation during configuration loading.</li>
 * <li>{@link io.jstach.ezkv.kvs.KeyValuesResource} - Represents a resource containing
 * key-value pairs, with associated metadata for controlling the loading behavior.</li>
 * </ul>
 *
 * @see io.jstach.ezkv.kvs.KeyValuesSystem
 * @see io.jstach.ezkv.kvs.KeyValuesLoader
 * @see io.jstach.ezkv.kvs.Variables
 * @see io.jstach.ezkv.kvs.KeyValuesResource
 */
@org.jspecify.annotations.NullMarked
package io.jstach.ezkv.kvs;
