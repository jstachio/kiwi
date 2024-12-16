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



/**
 * Represents options for JSON processing, including stringifying and parsing behaviors.
 *
 * @since 1.1.0
 */
public record JSONOptions(

    /**
     * Whether instants should be stringified as Unix timestamps.
     * If this is {@code false}, instants will be stringified as strings
     * (according to <a href="https://datatracker.ietf.org/doc/html/rfc3339#section-5.6">RFC 3339, Section 5.6</a>).
     * <p>
     * Default: {@code false}
     * <p>
     * <i>This is a {@link JSONStringify Stringify}-only option</i>
     */
    boolean stringifyUnixInstants,

    /**
     * Whether stringifying should only yield ASCII strings.
     * All non-ASCII characters will be converted to their
     * Unicode escape sequence (<code>&#92;uXXXX</code>).
     * <p>
     * Default: {@code false}
     * <p>
     * <i>This is a {@link JSONStringify Stringify}-only option</i>
     */
    boolean stringifyAscii,

    /**
     * Whether {@code NaN} should be allowed as a number.
     * <p>
     * Default: {@code true}
     */
    boolean allowNaN,

    /**
     * Whether {@code Infinity} should be allowed as a number.
     * This applies to both {@code +Infinity} and {@code -Infinity}.
     * <p>
     * Default: {@code true}
     */
    boolean allowInfinity,

    /**
     * Whether invalid Unicode surrogate pairs should be allowed.
     * <p>
     * Default: {@code true}
     * <p>
     * <i>This is a {@link JSONParser Parser}-only option</i>
     */
    boolean allowInvalidSurrogates,

    /**
     * Whether strings should be single-quoted ({@code '}) instead of double-quoted ({@code "}).
     * This also includes a {@link JSONObject JSONObject's} member names.
     * <p>
     * Default: {@code false}
     * <p>
     * <i>This is a {@link JSONStringify Stringify}-only option</i>
     */
    boolean quoteSingle,

    /**
     * Whether binary literals ({@code 0b10101...}) should be allowed.
     * <p>
     * Default: {@code false}
     * <p>
     * <i>This is a {@link JSONParser Parser}-only option</i>
     */
    boolean allowBinaryLiterals,

    /**
     * Whether octal literals ({@code 0o567...}) should be allowed.
     * <p>
     * Default: {@code false}
     * <p>
     * <i>This is a {@link JSONParser Parser}-only option</i>
     */
    boolean allowOctalLiterals,

    /**
     * Whether hexadecimal floating-point literals (e.g. {@code 0xA.BCp+12}) should be allowed.
     * <p>
     * Default: {@code false}
     * <p>
     * <i>This is a {@link JSONParser Parser}-only option</i>
     */
    boolean allowHexFloatingLiterals,

    /**
     * Whether Java-style digit separators ({@code 123_456}) should be allowed.
     * <p>
     * Default: {@code false}
     * <p>
     * <i>This is a {@link JSONParser Parser}-only option</i>
     */
    boolean allowJavaDigitSeparators,

    /**
     * Whether C-style digit separators ({@code 123'456}) should be allowed.
     * <p>
     * Default: {@code false}
     * <p>
     * <i>This is a {@link JSONParser Parser}-only option</i>
     */
    boolean allowCDigitSeparators,

    /**
     * Whether 32-bit Unicode escape sequences ({@code \U00123456}) should be allowed.
     * <p>
     * Default: {@code false}
     * <p>
     * <i>This is a {@link JSONParser Parser}-only option</i>
     */
    boolean allowLongUnicodeEscapes,

    /**
     * Specifies the behavior when the same key is encountered multiple times within the same {@link JSONObject}.
     * <p>
     * Default: {@link DuplicateBehavior#UNIQUE UNIQUE}.
     * <p>
     * <i>This is a {@link JSONParser Parser}-only option</i>
     */
    DuplicateBehavior duplicateBehaviour
) {

    /**
     * An enum containing all supported behaviors for duplicate keys.
     *
     * @since 1.3.0
     */
    public static enum DuplicateBehavior {

        /**
         * Throws an {@link JSONException exception} when a key
         * is encountered multiple times within the same object.
         */
        UNIQUE,

        /**
         * Only the last encountered value is significant,
         * all previous occurrences are silently discarded.
         */
        LAST_WINS,

        /**
         * Wraps duplicate values inside an {@link JSONArray array},
         * effectively treating them as if they were declared as one.
         */
        DUPLICATE
    }
    
    static final JSONOptions defaultOptions = new Builder().build();
    
    /**
     * A builder for creating instances of {@link JSONOptions}.
     */
    public static class Builder {
        
        boolean stringifyUnixInstants = false;
        boolean stringifyAscii = false;
        boolean allowNaN = true;
        boolean allowInfinity = true;
        boolean allowInvalidSurrogates = true;
        boolean quoteSingle = false;
        boolean allowBinaryLiterals = false;
        boolean allowOctalLiterals = false;
        boolean allowHexFloatingLiterals = false;
        boolean allowJavaDigitSeparators = false;
        boolean allowCDigitSeparators = false;
        boolean allowLongUnicodeEscapes = false;
        DuplicateBehavior duplicateBehaviour = DuplicateBehavior.UNIQUE;

        /**
         * Sets whether instants should be stringified as Unix timestamps.
         * @param stringifyUnixInstants the value to set
         * @return this builder
         */
        public Builder stringifyUnixInstants(boolean stringifyUnixInstants) {
            this.stringifyUnixInstants = stringifyUnixInstants;
            return this;
        }

        /**
         * Sets whether stringifying should only yield ASCII strings.
         * @param stringifyAscii the value to set
         * @return this builder
         */
        public Builder stringifyAscii(boolean stringifyAscii) {
            this.stringifyAscii = stringifyAscii;
            return this;
        }

        /**
         * Sets whether {@code NaN} should be allowed as a number.
         * @param allowNaN the value to set
         * @return this builder
         */
        public Builder allowNaN(boolean allowNaN) {
            this.allowNaN = allowNaN;
            return this;
        }

        /**
         * Sets whether {@code Infinity} should be allowed as a number.
         * @param allowInfinity the value to set
         * @return this builder
         */
        public Builder allowInfinity(boolean allowInfinity) {
            this.allowInfinity = allowInfinity;
            return this;
        }

        /**
         * Sets whether invalid Unicode surrogate pairs should be allowed.
         * @param allowInvalidSurrogates the value to set
         * @return this builder
         */
        public Builder allowInvalidSurrogates(boolean allowInvalidSurrogates) {
            this.allowInvalidSurrogates = allowInvalidSurrogates;
            return this;
        }

        /**
         * Sets whether strings should be single-quoted.
         * @param quoteSingle the value to set
         * @return this builder
         */
        public Builder quoteSingle(boolean quoteSingle) {
            this.quoteSingle = quoteSingle;
            return this;
        }

        /**
         * Sets whether binary literals should be allowed.
         * @param allowBinaryLiterals the value to set
         * @return this builder
         */
        public Builder allowBinaryLiterals(boolean allowBinaryLiterals) {
            this.allowBinaryLiterals = allowBinaryLiterals;
            return this;
        }

        /**
         * Sets whether octal literals should be allowed.
         * @param allowOctalLiterals the value to set
         * @return this builder
         */
        public Builder allowOctalLiterals(boolean allowOctalLiterals) {
            this.allowOctalLiterals = allowOctalLiterals;
            return this;
        }

        /**
         * Sets whether hexadecimal floating-point literals should be allowed.
         * @param allowHexFloatingLiterals the value to set
         * @return this builder
         */
        public Builder allowHexFloatingLiterals(boolean allowHexFloatingLiterals) {
            this.allowHexFloatingLiterals = allowHexFloatingLiterals;
            return this;
        }

        /**
         * Sets whether Java-style digit separators should be allowed.
         * @param allowJavaDigitSeparators the value to set
         * @return this builder
         */
        public Builder allowJavaDigitSeparators(boolean allowJavaDigitSeparators) {
            this.allowJavaDigitSeparators = allowJavaDigitSeparators;
            return this;
        }

        /**
         * Sets whether C-style digit separators should be allowed.
         * @param allowCDigitSeparators the value to set
         * @return this builder
         */
        public Builder allowCDigitSeparators(boolean allowCDigitSeparators) {
            this.allowCDigitSeparators = allowCDigitSeparators;
            return this;
        }

        /**
         * Sets whether 32-bit Unicode escape sequences should be allowed.
         * @param allowLongUnicodeEscapes the value to set
         * @return this builder
         */
        public Builder allowLongUnicodeEscapes(boolean allowLongUnicodeEscapes) {
            this.allowLongUnicodeEscapes = allowLongUnicodeEscapes;
            return this;
        }

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
         * Builds a new {@link JSONOptions} instance using the values set in this builder.
         * @return a new {@link JSONOptions} instance
         */
        public JSONOptions build() {
            return new JSONOptions(
                stringifyUnixInstants,
                stringifyAscii,
                allowNaN,
                allowInfinity,
                allowInvalidSurrogates,
                quoteSingle,
                allowBinaryLiterals,
                allowOctalLiterals,
                allowHexFloatingLiterals,
                allowJavaDigitSeparators,
                allowCDigitSeparators,
                allowLongUnicodeEscapes,
                duplicateBehaviour
            );
        }
    }

}

