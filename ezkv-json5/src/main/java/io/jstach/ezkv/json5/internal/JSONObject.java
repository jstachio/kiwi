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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import io.jstach.ezkv.json5.internal.JSONOptions.DuplicateBehavior;

/**
 * A JSONObject is a map (key-value) structure capable of holding multiple values,
 * including other {@link JSONArray JSONArrays} and JSONObjects
 *
 * @author SyntaxError404
 *
 */
public final class JSONObject implements Iterable<Map.Entry<String, Object>>, JSONValue {

	private LinkedHashMap<String, Object> values;

	/**
	 * Constructs a new JSONObject
	 */
	public JSONObject() {
		values = new LinkedHashMap<>();
	}

	/**
	 * Constructs a new JSONObject from a JSONParser
	 * @param parser a JSONParser
	 */
	public JSONObject(JSONParser parser) {
		this();

		char c;
		String key;

		if (parser.nextClean() != '{')
			throw parser.syntaxError("A JSONObject must begin with '{'");

		DuplicateBehavior duplicateBehavior = parser.options.duplicateBehaviour();

		Set<String> duplicates = new HashSet<>();

		while (true) {
			c = parser.nextClean();

			switch (c) {
				case 0:
					throw parser.syntaxError("A JSONObject must end with '}'");
				case '}':
					return;
				default:
					parser.back();
					key = parser.nextMemberName();
			}

			boolean duplicate = has(key);

			if (duplicate && duplicateBehavior == DuplicateBehavior.UNIQUE)
				throw new JSONException("Duplicate key " + JSONStringify.quote(key));

			c = parser.nextClean();

			if (c != ':')
				throw parser.syntaxError("Expected ':' after a key, got '" + c + "' instead");

			Object value = parser.nextValue().value();

			if (duplicate && duplicateBehavior == DuplicateBehavior.DUPLICATE) {
				JSONArray array;

				if (duplicates.contains(key)) {
					array = getArray(key);
				}
				else {
					array = new JSONArray();
					array.add(get(key));

					duplicates.add(key);
				}

				array.add(value);
				value = array;
			}

			values.put(key, value);

			c = parser.nextClean();

			if (c == '}')
				return;

			if (c != ',')
				throw parser.syntaxError("Expected ',' or '}' after value, got '" + c + "' instead");
		}
	}

	@Override
	public @Nullable Object value() {
		return this;
	}

	/**
	 * Returns a set of keys of the JSONObject. Modifying the set will modify the
	 * JSONObject
	 * @return a set of keys
	 *
	 * @see Map#keySet()
	 */
	public Set<String> keySet() {
		return values.keySet();
	}

	/**
	 * Returns a set of entries of the JSONObject. Modifying the set or an entry will
	 * modify the JSONObject
	 *
	 * <p>
	 * Use with caution. Inserting objects of unknown types may cause issues when trying
	 * to use the JSONObject
	 * @return a set of entries
	 *
	 * @see Map#entrySet()
	 */
	public Set<Entry<String, Object>> entrySet() {
		return values.entrySet();
	}

	@Override
	public Iterator<Entry<String, Object>> iterator() {
		return values.entrySet().iterator();
	}

	/**
	 * Returns the number of entries in the JSONObject
	 * @return the number of entries
	 */
	public int length() {
		return values.size();
	}

	// -- CHECK --

	/**
	 * Checks if a key exists within the JSONObject
	 * @param key the key
	 * @return whether the key exists
	 */
	public boolean has(String key) {
		return values.containsKey(key);
	}

	/**
	 * Checks if the value with the specified key is a JSONArray
	 * @param key the key
	 * @return whether the value is a JSONArray
	 * @throws JSONException if the key does not exist
	 */
	public boolean isArray(String key) {
		return checkKey(key) instanceof JSONArray;
	}

	// -- GET --

	/**
	 * Returns the value for a given key
	 * @param key the key
	 * @return the value
	 * @throws JSONException if the key does not exist
	 */
	public Object get(String key) {
		checkKey(key);
		return values.get(key);
	}

	/**
	 * Returns the value as a JSONArray for a given key
	 * @param key the key
	 * @return the JSONArray
	 * @throws JSONException if the key does not exist, or if the value is not a JSONArray
	 */
	public JSONArray getArray(String key) {
		return checkType(this::isArray, key, "array");
	}

	// -- STRINGIFY --

	/**
	 * Converts the JSONObject into its string representation. The indentation factor
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
	 * @param indentFactor the indentation factor
	 * @return the string representation
	 *
	 * @see JSONStringify#toString(JSONObject, int)
	 */
	public String toString(int indentFactor) {
		return JSONStringify.toString(this, indentFactor);
	}

	/**
	 * Converts the JSONObject into its compact string representation.
	 * @return the compact string representation
	 */
	@Override
	public String toString() {
		return toString(0);
	}

	// -- MISCELLANEOUS --

	private Object checkKey(String key) {
		if (!values.containsKey(key))
			throw new JSONException("JSONObject[" + JSONStringify.quote(key) + "] does not exist");

		return values.get(key);
	}

	@SuppressWarnings("unchecked")
	private <T> T checkType(Predicate<String> predicate, String key, String type) {
		if (!predicate.test(key))
			throw mismatch(key, type);

		return (T) values.get(key);
	}

	//

	private static JSONException mismatch(String key, String type) {
		return new JSONException("JSONObject[" + JSONStringify.quote(key) + "] is not of type " + type);
	}
	//
	// /**
	// * Tries to convert an object to an Instant.
	// * <p>
	// * In order for the conversion to succeed, the value must be either
	// * <ul>
	// * <li>an {@link Instant},</li>
	// * <li>a String formatted according to
	// * <a href="https://datatracker.ietf.org/doc/html/rfc3339#section-5.6">RFC 3339,
	// * Section 5.6</a>, or</li>
	// * <li>an integer (primitive type or {@link BigInteger}) that does not exceed the
	// * limits of {@link Instant#ofEpochSecond(long)}</li>
	// * </ul>
	// * @param value the value
	// * @return the parsed Instant
	// * @see #isInstant(String)
	// * @throws DateTimeException if the value is malformed or out of range
	// * @throws JSONException if the value has an invalid type
	// */
	// public static Instant parseInstant(Object value) {
	// if (value instanceof Instant)
	// return (Instant) value;
	//
	// if (value instanceof Byte || value instanceof Short || value instanceof Integer ||
	// value instanceof Long)
	// return Instant.ofEpochSecond((long) value);
	//
	// if (value instanceof BigInteger)
	// return Instant.ofEpochSecond(((BigInteger) value).longValueExact());
	//
	// if (value instanceof String)
	// return Instant.parse((String) value);
	//
	// String className = value == null ? "null" : value.getClass().getSimpleName();
	//
	// throw new JSONException(className + " cannot be converted to Instant");
	// }

	/**
	 * Sanitizes an input value
	 * @param value the value
	 * @return the sanitized value
	 * @throws JSONException if the value is illegal
	 */
	static Object sanitize(Object value) {
		if (value == null)
			return null;

		if (value instanceof Boolean || value instanceof String || value instanceof JSONObject
				|| value instanceof JSONArray || value instanceof Instant)
			return value;

		else if (value instanceof Number) {
			Number num = (Number) value;

			if (value instanceof Double) {
				double d = (Double) num;

				if (Double.isFinite(d))
					return BigDecimal.valueOf(d);
			}

			else if (value instanceof Float) {
				float f = (Float) num;

				if (Float.isFinite(f))
					return BigDecimal.valueOf(f);

				// NaN and Infinity
				return num.doubleValue();
			}

			else if (value instanceof Byte || value instanceof Short || value instanceof Integer
					|| value instanceof Long)
				return BigInteger.valueOf(num.longValue());

			else if (!(value instanceof BigDecimal || value instanceof BigInteger))
				return BigDecimal.valueOf(num.doubleValue());

			return num;
		}

		else
			throw new JSONException("Illegal type '" + value.getClass() + "'");
	}

}
