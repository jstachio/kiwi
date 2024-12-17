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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import io.jstach.ezkv.json5.internal.JSONValue.JSONArray;
import io.jstach.ezkv.json5.internal.JSONValue.JSONObject;

/**
 * Represents options for JSON processing, including stringifying and parsing behaviors.
 *
 * @since 1.1.0
 */
public record JSONOptions(

		Set<JSONParserFlag> flags,

		/**
		 * Specifies the behavior when the same key is encountered multiple times within
		 * the same {@link JSONObject}.
		 * <p>
		 * Default: {@link DuplicateBehavior#UNIQUE UNIQUE}.
		 * <p>
		 * <i>This is a {@link JSONParser Parser}-only option</i>
		 */
		DuplicateBehavior duplicateBehaviour) {
	
	public boolean isFlag(JSONParserFlag flag) {
		return flags.contains(flag);
	}
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
	
	/**
	 * Represents flags for enabling or disabling specific parsing features in the JSON Parser.
	 */
	public enum JSONParserFlag {

//	    /**
//	     * Whether binary literals ({@code 0b10101...}) should be allowed.
//	     * <p>
//	     * Default: {@code false}
//	     * <p>
//	     * <i>This is a {@link JSONParser Parser}-only option</i>
//	     */
//	    BINARY_LITERALS(JSONParserOption.FLAG_BINARY_LITERALS),
//
//	    /**
//	     * Whether octal literals ({@code 0o567...}) should be allowed.
//	     * <p>
//	     * Default: {@code false}
//	     * <p>
//	     * <i>This is a {@link JSONParser Parser}-only option</i>
//	     */
//	    OCTAL_LITERALS(JSONParserOption.FLAG_OCTAL_LITERALS),

	    /**
	     * Whether hexadecimal floating-point literals (e.g. {@code 0xA.BCp+12}) should be allowed.
	     * <p>
	     * Default: {@code false}
	     * <p>
	     * <i>This is a {@link JSONParser Parser}-only option</i>
	     */
	    HEX_FLOATING_LITERALS,

//	    /**
//	     * Whether Java-style digit separators ({@code 123_456}) should be allowed.
//	     * <p>
//	     * Default: {@code false}
//	     * <p>
//	     * <i>This is a {@link JSONParser Parser}-only option</i>
//	     */
//	    JAVA_DIGIT_SEPARATORS(JSONParserOption.FLAG_JAVA_DIGIT_SEPARATORS),
//
//	    /**
//	     * Whether C-style digit separators ({@code 123'456}) should be allowed.
//	     * <p>
//	     * Default: {@code false}
//	     * <p>
//	     * <i>This is a {@link JSONParser Parser}-only option</i>
//	     */
//	    C_DIGIT_SEPARATORS(JSONParserOption.FLAG_C_DIGIT_SEPARATORS),

	    /**
	     * Whether 32-bit Unicode escape sequences ({@code \U00123456}) should be allowed.
	     * <p>
	     * Default: {@code false}
	     * <p>
	     * <i>This is a {@link JSONParser Parser}-only option</i>
	     */
	    LONG_UNICODE_ESCAPES;
	}


	static final JSONOptions defaultOptions = new Builder().build();

	/**
	 * A builder for creating instances of {@link JSONOptions}.
	 */
	public static class Builder {


		private EnumSet<JSONParserFlag> flags = EnumSet.noneOf(JSONParserFlag.class);
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
		
		public Builder flag(JSONParserFlag flag) {
			this.flags.add(flag);
			return this;
		}

		/**
		 * Builds a new {@link JSONOptions} instance using the values set in this builder.
		 * @return a new {@link JSONOptions} instance
		 */
		public JSONOptions build() {
			var flags = Collections.<JSONParserFlag>unmodifiableSet(EnumSet.copyOf(this.flags));
			return new JSONOptions(
					flags,
					duplicateBehaviour);
		}

	}

}
