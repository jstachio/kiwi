package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 *
 * _kvs_stuff=password,_load_classpath
 *
 * _stuff_no_interpolate=true _stuff_require=true
 *
 * _load_classpath=classpath://
 *
 * _bind_stuff=
 *
 */
public record KeyValue(String key, //
		String raw, //
		String expanded, //
		Source source, //
		Set<Flag> flags) {

	public KeyValue {
		flags = FlagSet.copyOf(flags, Flag.class);
		Objects.requireNonNull(key);
		Objects.requireNonNull(raw);
		Objects.requireNonNull(expanded);
		Objects.requireNonNull(source);
	}

	public final static String REDACTED_MESSAGE = "REDACTED";

	public KeyValue withExpanded(String expanded) {
		return new KeyValue(key, raw, expanded, source, flags);
	}

	public KeyValue withExpanded(@SuppressWarnings("exports") Function<String, @Nullable String> expanded) {
		String n = key();
		String exp = expanded.apply(n);
		if (exp != null) {
			return withExpanded(exp);
		}
		return this;
	}

	public KeyValue withFlags(Collection<Flag> flagsCol) {
		var flags = EnumSet.noneOf(Flag.class);
		flags.addAll(flagsCol);
		return new KeyValue(key, raw, expanded, source, flags);
	}

	public KeyValue addFlags(Collection<Flag> flagsCol) {
		var flags = EnumSet.noneOf(Flag.class);
		flags.addAll(flags());
		flags.addAll(flagsCol);
		return new KeyValue(key, raw, expanded, source, flags);
	}

	public record Source(URI uri, KeyValue reference, int index) {

	}

	public enum Flag {

		NO_INTERPOLATION, SENSITIVE;

		public boolean isSet(KeyValue kv) {
			return kv.isFlag(NO_INTERPOLATION);
		}

	}

	public boolean isNoInterpolation() {
		return isFlag(Flag.NO_INTERPOLATION);
	}

	public boolean isSensitive() {
		return isFlag(Flag.SENSITIVE);
	}

	public boolean isFlag(Flag flag) {
		return flags().contains(flag);
	}

	public KeyValue redact() {
		return redact(REDACTED_MESSAGE);
	}

	public KeyValue redact(String redactMessage) {
		if (isSensitive()) {
			String raw = redactMessage;
			String expanded = redactMessage;
			var flags = EnumSet.copyOf(this.flags);
			flags = EnumSet.copyOf(flags);
			flags.remove(Flag.SENSITIVE);
			return new KeyValue(key, raw, expanded, source, flags);
		}
		return this;
	}

	@Override
	public String toString() {
		return toString(redact());
	}

	private static String toString(KeyValue kv) {
		return "KeyValue [key=" + kv.key //
				+ ", raw=" + kv.raw //
				+ ", expanded=" + kv.expanded //
				+ ", source=" + kv.source //
				+ ", flags=" + kv.flags //
				+ "]";
	}

}
