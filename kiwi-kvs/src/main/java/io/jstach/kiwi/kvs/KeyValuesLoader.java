package io.jstach.kiwi.kvs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import io.jstach.kiwi.kvs.interpolate.Interpolator.InterpolationException;

/**
 * Represents a loader responsible for loading {@link KeyValues} from configured sources.
 * This interface defines the contract for loading key-value pairs, potentially involving
 * interpolation or resource chaining.
 *
 * <p>
 * Implementations of this interface may load key-values from various sources such as
 * files, classpath resources, or system properties.
 *
 * @see KeyValues
 * @see Variables
 */
public interface KeyValuesLoader {

	/**
	 * Loads key-values from configured sources.
	 * @return a {@link KeyValues} instance containing the loaded key-value pairs
	 * @throws IOException if an I/O error occurs during loading
	 * @throws FileNotFoundException if a specified resource is not found
	 * @throws InterpolationException if an error occurs during value interpolation
	 */
	public KeyValues load() throws IOException, FileNotFoundException, InterpolationException;

	/**
	 * A builder class for constructing instances of {@link KeyValuesLoader}. The builder
	 * allows adding multiple sources from which key-values will be loaded, as well as
	 * setting variables for interpolation. <strong>Note that the order of the "add" and
	 * {@link #variables} methods does matter.</strong>
	 */
	public class Builder implements KeyValuesLoader {

		final Function<Builder, KeyValuesLoader> loaderFactory;

		final List<NamedKeyValuesSource> sources = new ArrayList<>();

		final List<Function<KeyValuesEnvironment, Variables>> variables = new ArrayList<>();

		Builder(Function<Builder, KeyValuesLoader> loaderFactory) {
			super();
			this.loaderFactory = loaderFactory;
		}

		/**
		 * Adds a {@link KeyValuesResource} as a source to the loader.
		 * @param resource the resource to add
		 * @return this builder instance
		 */
		public Builder add(KeyValuesResource resource) {
			sources.add(resource);
			return this;
		}

		/**
		 * Add resource using callback on builder.
		 * @param uri uri of resource
		 * @param builder builder to add additional properties.
		 * @return this.
		 */
		public Builder add(String uri, Consumer<KeyValuesResource.Builder> builder) {
			var b = KeyValuesResource.builder(uri);
			builder.accept(b);
			return add(b.build());
		}

		/**
		 * Adds a named {@link KeyValues} source to the loader.
		 * @param name the name of the source
		 * @param keyValues the key-values to add
		 * @return this builder instance
		 */
		public Builder add(String name, KeyValues keyValues) {
			sources.add(new NamedKeyValues(name, keyValues));
			return this;
		}

		/**
		 * Adds a {@link URI} as a source by wrapping it in a {@link KeyValuesResource}.
		 * @param uri the URI to add
		 * @return this builder instance
		 */
		public Builder add(URI uri) {
			return add(KeyValuesResource.builder(uri).build());
		}

		/**
		 * Adds a URI specified as a string as a source to the loader.
		 * @param uri the URI string to add
		 * @return this builder instance
		 */
		public Builder add(String uri) {
			return add(URI.create(uri));
		}

		/**
		 * Adds {@link Variables} for interpolation when loading key-values.
		 * @param variables the variables to use for interpolation
		 * @return this builder instance
		 */
		public Builder add(Variables variables) {
			this.variables.add(e -> variables);
			return this;
		}

		/**
		 * Adds variables that will be resolved based on the environment. <strong>
		 * Variables resolution order is the opposite of KeyValues. Primacy takes
		 * precedence! </strong> This is useful if you want to use environment things for
		 * variables that are bound to {@link KeyValuesEnvironment}. This is preferred
		 * instead of just creating Variables from {@link System#getProperties()} or
		 * {@link System#getenv()} directly.
		 * @param variablesFactory function to create variables from environment.
		 * @return this
		 * @see Variables#ofSystemProperties(KeyValuesEnvironment)
		 * @see Variables#ofSystemEnv(KeyValuesEnvironment)
		 */
		public Builder variables(Function<KeyValuesEnvironment, Variables> variablesFactory) {
			this.variables.add(variablesFactory);
			return this;
		}

		/**
		 * Builds and returns a new {@link KeyValuesLoader} based on the current state of
		 * the builder.
		 *
		 * <p>
		 * If no sources are specified, a default classpath resource
		 * {@code classpath:/system.properties} is used.
		 * @return a new {@link KeyValuesLoader} instance
		 */
		public KeyValuesLoader build() {
			return loaderFactory.apply(this);
		}

		/**
		 * Loads key-values using the current builder configuration.
		 * @return a {@link KeyValues} instance containing the loaded key-value pairs
		 * @throws IOException if an I/O error occurs during loading
		 * @throws FileNotFoundException if a specified resource is not found
		 */
		@Override
		public KeyValues load() throws IOException, FileNotFoundException {
			return build().load();
		}

	}

}
