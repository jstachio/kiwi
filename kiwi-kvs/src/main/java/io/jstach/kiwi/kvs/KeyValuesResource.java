package io.jstach.kiwi.kvs;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;

import io.jstach.kiwi.kvs.Variables.StaticVariables;

public sealed interface KeyValuesResource permits DefaultKeyValuesResource {

	public URI uri();

	public StaticVariables parameters();

	public String name();

	public @Nullable KeyValue reference();

	public @Nullable String mediaType();

	default void resourceKeyValues(BiConsumer<String, String> consumer) {
		parameters().forKeyValues((k, v) -> {
			var rk = ResourceKeys.parse(k);
			if (rk != null) {
				consumer.accept(rk.key(name()), v);
			}
		});
	}

	public static Builder builder(URI uri) {
		return new Builder(uri, uriToName(uri));
	}

	public static Builder builder(String uri) {
		return builder(URI.create(uri));
	}

	private static String uriToName(URI uri) {
		return Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(uri.toASCIIString().getBytes(StandardCharsets.US_ASCII));
	}

	public final class Builder {

		URI uri;

		String name;

		Map<String, String> parameters = new LinkedHashMap<>();

		EnumSet<LoadFlag> flags = EnumSet.noneOf(LoadFlag.class);

		@Nullable
		KeyValue reference;

		@Nullable
		String mediaType;

		Builder(URI uri, String name) {
			this.uri = uri;
			this.name = DefaultKeyValuesResource.validateResourceName(name);
		}

		public Builder uri(URI uri) {
			this.uri = uri;
			return this;
		}

		public Builder name(String name) {
			this.name = Objects.requireNonNull(name);
			return this;
		}

		public Builder parameter(String key, String value) {
			parameters.put(key, value);
			return this;
		}

		public Builder mediaType(String mediaType) {
			this.mediaType = mediaType;
			return this;
		}

		public Builder noInterpolation(boolean flag) {
			LoadFlag.NO_INTERPOLATE.set(flags, flag);
			return this;
		}

		public Builder noAddKeyValues(boolean flag) {
			LoadFlag.NO_ADD.set(flags, flag);
			return this;
		}

		Builder _addFlag(LoadFlag flag) {
			flags.add(flag);
			return this;
		}

		public KeyValuesResource build() {
			return DefaultKeyValuesResource.of(this);
		}

	}

}
