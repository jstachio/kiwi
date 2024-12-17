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

import java.util.Map;

import org.jspecify.annotations.Nullable;

import io.jstach.ezkv.json5.internal.JSONValue.JSONArray;
import io.jstach.ezkv.json5.internal.JSONValue.JSONBool;
import io.jstach.ezkv.json5.internal.JSONValue.JSONNull;
import io.jstach.ezkv.json5.internal.JSONValue.JSONNumber;
import io.jstach.ezkv.json5.internal.JSONValue.JSONObject;
import io.jstach.ezkv.json5.internal.JSONValue.JSONString;

/**
 * A utility class for serializing {@link JSONObject JSONObjects} and {@link JSONArray
 * JSONArrays} into their string representations
 *
 * @author SyntaxError404
 *
 */
public class JSONStringify {

	/**
	 * Converts a JSONObject into its string representation. The indentation factor
	 * enables pretty-printing and defines how many spaces (' ') should be placed before
	 * each key/value pair. A factor of {@code < 1} disables pretty-printing and discards
	 * any optional whitespace characters.
	 * <p>
	 * {@code indentFactor = 2}: <pre>
	 * {
	 *   "key0": "value0",
	 *   "key1": {
	 *     "nested": 123
	 *   },
	 *   "key2": false
	 * }
	 * </pre>
	 * <p>
	 * {@code indentFactor = 0}: <pre>
	 * {"key0":"value0","key1":{"nested":123},"key2":false}
	 * </pre>
	 * @param object the JSONObject
	 * @param indentFactor the indentation factor
	 * @param options the options for stringifying
	 * @return the string representation
	 * @since 1.1.0
	 */
	public static String toString(JSONObject object, int indentFactor, @Nullable JSONOptions options) {
		return toString(object, "", Math.max(0, indentFactor), options == null ? JSONOptions.defaultOptions : options);
	}

	/**
	 * Converts a JSONArray into its string representation. The indentation factor enables
	 * pretty-printing and defines how many spaces (' ') should be placed before each
	 * value. A factor of {@code < 1} disables pretty-printing and discards any optional
	 * whitespace characters.
	 * <p>
	 * {@code indentFactor = 2}: <pre>
	 * [
	 *   "value",
	 *   {
	 *     "nested": 123
	 *   },
	 *   false
	 * ]
	 * </pre>
	 * <p>
	 * {@code indentFactor = 0}: <pre>
	 * ["value",{"nested":123},false]
	 * </pre>
	 * @param array the JSONArray
	 * @param indentFactor the indentation factor
	 * @param options the options for stringifying
	 * @return the string representation
	 * @since 1.1.0
	 */
	public static String toString(JSONArray array, int indentFactor, @Nullable JSONOptions options) {
		return toString(array, "", Math.max(0, indentFactor), options == null ? JSONOptions.defaultOptions : options);
	}

	/**
	 * Converts a JSONObject into its string representation. The indentation factor
	 * enables pretty-printing and defines how many spaces (' ') should be placed before
	 * each key/value pair. A factor of {@code < 1} disables pretty-printing and discards
	 * any optional whitespace characters.
	 * <p>
	 * {@code indentFactor = 2}: <pre>
	 * {
	 *   "key0": "value0",
	 *   "key1": {
	 *     "nested": 123
	 *   },
	 *   "key2": false
	 * }
	 * </pre>
	 * <p>
	 * {@code indentFactor = 0}: <pre>
	 * {"key0":"value0","key1":{"nested":123},"key2":false}
	 * </pre> This uses the {@link JSONOptions#getDefaultOptions() default options}
	 * @param object the JSONObject
	 * @param indentFactor the indentation factor
	 * @return the string representation
	 */
	public static String toString(JSONObject object, int indentFactor) {
		return toString(object, indentFactor, null);
	}

	/**
	 * Converts a JSONArray into its string representation. The indentation factor enables
	 * pretty-printing and defines how many spaces (' ') should be placed before each
	 * value. A factor of {@code < 1} disables pretty-printing and discards any optional
	 * whitespace characters.
	 * <p>
	 * {@code indentFactor = 2}: <pre>
	 * [
	 *   "value",
	 *   {
	 *     "nested": 123
	 *   },
	 *   false
	 * ]
	 * </pre>
	 * <p>
	 * {@code indentFactor = 0}: <pre>
	 * ["value",{"nested":123},false]
	 * </pre> This uses the {@link JSONOptions#getDefaultOptions() default options}
	 * @param array the JSONArray
	 * @param indentFactor the indentation factor
	 * @return the string representation
	 */
	public static String toString(JSONArray array, int indentFactor) {
		return toString(array, indentFactor, null);
	}

	private static String toString(JSONObject object, String indent, int indentFactor, JSONOptions options) {
		StringBuilder sb = new StringBuilder();

		String childIndent = indent + " ".repeat(indentFactor);

		sb.append('{');

		for (Map.Entry<String, JSONValue> entry : object.values.entrySet()) {
			if (sb.length() != 1)
				sb.append(',');

			if (indentFactor > 0)
				sb.append('\n').append(childIndent);

			sb.append(quote(entry.getKey(), options)).append(':');

			if (indentFactor > 0)
				sb.append(' ');

			sb.append(toString(entry.getValue(), childIndent, indentFactor, options));
		}

		if (indentFactor > 0)
			sb.append('\n').append(indent);

		sb.append('}');

		return sb.toString();
	}

	private static String toString(JSONArray array, String indent, int indentFactor, JSONOptions options) {
		StringBuilder sb = new StringBuilder();

		String childIndent = indent + " ".repeat(indentFactor);

		sb.append('[');

		for (JSONValue value : array.values) {
			if (sb.length() != 1)
				sb.append(',');

			if (indentFactor > 0)
				sb.append('\n').append(childIndent);

			sb.append(toString(value, childIndent, indentFactor, options));
		}

		if (indentFactor > 0)
			sb.append('\n').append(indent);

		sb.append(']');

		return sb.toString();
	}

	private static String toString(JSONValue value, String indent, int indentFactor, JSONOptions options) {
		return switch (value) {
			case JSONObject o -> toString(o, indent, indentFactor, options);
			case JSONArray o -> toString(o, indent, indentFactor, options);
			case JSONString s -> s.value();
			case JSONBool b -> String.valueOf(b.val());
			case JSONNumber n -> toString(n.value(), options);
			case JSONNull n -> "null";
		};
	}

	private static String toString(Number number, JSONOptions options) {
		return switch (number) {
			case Double d -> {
				if (!options.allowNaN() && Double.isNaN(d)) {
					throw new JSONException("Illegal NaN in JSON");
				}
				if (!options.allowInfinity() && Double.isInfinite(d)) {
					throw new JSONException("Illegal Infinity in JSON");
				}
				yield String.valueOf(d);
			}
			default -> String.valueOf(number);
		};
	}

	static String quote(String string) {
		return quote(string, null);
	}

	private static String quote(String string, @Nullable JSONOptions options) {
		options = options == null ? JSONOptions.defaultOptions : options;

		if (string == null || string.isEmpty())
			return options.quoteSingle() ? "''" : "\"\"";

		final char qt = options.quoteSingle() ? '\'' : '"';

		StringBuilder quoted = new StringBuilder(string.length() + 2);
		boolean ascii = options.stringifyAscii();

		quoted.append(qt);

		for (char c : string.toCharArray()) {
			if (c == qt) {
				quoted.append('\\');
				quoted.append(c);
				continue;
			}

			switch (c) {
				case '\\':
					quoted.append("\\\\");
					break;
				case '\b':
					quoted.append("\\b");
					break;
				case '\f':
					quoted.append("\\f");
					break;
				case '\n':
					quoted.append("\\n");
					break;
				case '\r':
					quoted.append("\\r");
					break;
				case '\t':
					quoted.append("\\t");
					break;
				case 0x0B: // Vertical Tab
					quoted.append("\\v");
					break;
				default:
					boolean unicode = false;

					if (!ascii) {
						// escape non-graphical characters
						// (https://www.unicode.org/versions/Unicode13.0.0/ch02.pdf#G286941)
						switch (Character.getType(c)) {
							case Character.FORMAT:
							case Character.LINE_SEPARATOR:
							case Character.PARAGRAPH_SEPARATOR:
							case Character.CONTROL:
							case Character.PRIVATE_USE:
							case Character.SURROGATE:
							case Character.UNASSIGNED:
								unicode = true;
								break;
							default:
								break;
						}
					}
					else
						unicode = c > 0x7F;

					if (unicode) {
						quoted.append("\\u");
						quoted.append(String.format("%04X", (int) c));
					}
					else
						quoted.append(c);
			}
		}

		quoted.append(qt);

		return quoted.toString();
	}

}
