package io.jstach.ezkv.json5.internal;

import io.jstach.ezkv.json5.internal.JSONValue.JSONArray;

/**
 * Represents options for JSON processing, including stringifying and parsing behaviors.
 *
 * @since 1.1.0
 */
public record JSONParserOptions(
		/**
		 * Specifies the behavior when the same key is encountered multiple times within
		 * the same {@link JSONObject}.
		 * <p>
		 * Default: {@link DuplicateBehavior#UNIQUE UNIQUE}.
		 * <p>
		 * <i>This is a {@link JSONParser Parser}-only option</i>
		 */
		DuplicateBehavior duplicateBehaviour) {

	/**
	 * An enum containing all supported behaviors for duplicate keys.
	 *
	 * @since 1.3.0
	 */
	public static enum DuplicateBehavior {

		/**
		 * Throws an {@link JSONException exception} when a key is encountered multiple
		 * times within the same object.
		 */
		UNIQUE,

		/**
		 * Only the last encountered value is significant, all previous occurrences are
		 * silently discarded.
		 */
		LAST_WINS,

		/**
		 * Wraps duplicate values inside an {@link JSONArray array}, effectively treating
		 * them as if they were declared as one.
		 */
		DUPLICATE

	}

	static final JSONParserOptions defaultOptions = new Builder().build();

	/**
	 * A builder for creating instances of {@link JSONParserOptions}.
	 */
	public static class Builder {

		private DuplicateBehavior duplicateBehaviour = DuplicateBehavior.UNIQUE;

		/**
		 * Sets the behavior when duplicate keys are encountered.
		 * @param duplicateBehaviour the behavior to set
		 * @return this builder
		 */
		public Builder duplicateBehaviour(DuplicateBehavior duplicateBehaviour) {
			this.duplicateBehaviour = duplicateBehaviour;
			return this;
		}

		/**
		 * Builds a new {@link JSONParserOptions} instance using the values set in this
		 * builder.
		 * @return a new {@link JSONParserOptions} instance
		 */
		public JSONParserOptions build() {
			return new JSONParserOptions(duplicateBehaviour);
		}

	}

}
