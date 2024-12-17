/*
 * MIT License
 *
 * Copyright (c) 2021 SyntaxError404
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
