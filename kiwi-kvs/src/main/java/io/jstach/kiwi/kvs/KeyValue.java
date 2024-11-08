package io.jstach.kiwi.kvs;

import java.net.URI;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

public record KeyValue(String key, String expanded, Meta meta) {

	public KeyValue {
		Objects.requireNonNull(key);
		Objects.requireNonNull(expanded);
		Objects.requireNonNull(meta);
	}

	public KeyValue(String key, String raw) {
		this(key, raw, Meta.of(raw, Source.EMPTY, Set.of()));
	}

	public final static String REDACTED_MESSAGE = "REDACTED";

	public String value() {
		return this.expanded;
	}

	public String raw() {
		return meta().raw();
	}

	Set<Flag> flags() {
		return meta().flags();
	}

	public KeyValue withExpanded(String expanded) {
		return new KeyValue(key, expanded, meta);
	}

	public KeyValue withExpanded(@SuppressWarnings("exports") Function<String, @Nullable String> expanded) {
		String n = key();
		String exp = expanded.apply(n);
		if (exp != null) {
			return withExpanded(exp);
		}
		return this;
	}

	public KeyValue addFlags(Collection<Flag> flagsCol) {
		var flags = EnumSet.noneOf(Flag.class);
		flags.addAll(flags());
		flags.addAll(flagsCol);
		var meta = this.meta.withFlags(flags);
		return new KeyValue(key, expanded, meta);
	}

	public sealed interface Meta {

		String raw();

		Source source();

		Set<Flag> flags();

		Meta withFlags(Collection<Flag> flags);

		static Meta of(String raw, Source source, Set<Flag> flags) {
			return new DefaultMeta(raw, source, flags);
		}

	}

	record DefaultMeta(String raw, Source source, Set<Flag> flags) implements Meta {
		public DefaultMeta {
			flags = FlagSet.copyOf(flags, Flag.class);
		}

		public Meta withFlags(Collection<Flag> flags) {
			var flagsCopy = FlagSet.copyOf(flags, Flag.class);
			return new DefaultMeta(raw, source, flagsCopy);
		}
	}

	public record Source(URI uri, @Nullable KeyValue reference, int index) {

		public static URI NULL_URI = URI.create("null:///");

		public static Source EMPTY = new Source();

		public Source() {
			this(NULL_URI, null, 0);
		}

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
		return meta().flags().contains(flag);
	}

	public KeyValue redact() {
		return redact(REDACTED_MESSAGE);
	}

	public KeyValue redact(String redactMessage) {
		if (isSensitive()) {
			String expanded = redactMessage;
			String raw = redactMessage;
			var flags = EnumSet.copyOf(this.meta.flags());
			flags.remove(Flag.SENSITIVE);
			var source = meta.source();
			Meta meta = new DefaultMeta(raw, source, flags);
			return new KeyValue(key, expanded, meta);
		}
		return this;
	}

	@Override
	public String toString() {
		return toString(redact());
	}

	private static String toString(KeyValue kv) {
		return "KeyValue [key=" + kv.key //
				+ ", raw=" + kv.raw() //
				+ ", expanded=" + kv.expanded //
				+ ", source=" + kv.meta().source() //
				+ ", flags=" + kv.flags() //
				+ "]";
	}

}
