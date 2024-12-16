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
import java.time.DateTimeException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
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
	 * Constructs a new JSONObject from a string
	 * @param source a string
	 */
	public JSONObject(String source) {
		this(new JSONParser(source));
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

				if (duplicates.contains(key))
					array = getArray(key);

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

	//

	/**
	 * Creates a shallow copy of the JSONObject
	 * @return the new JSONObject
	 * @since 2.0.0
	 */
	public JSONObject copy() {
		JSONObject copy = new JSONObject();
		copy.values.putAll(values);

		return copy;
	}

	/**
	 * Creates a deep copy of the JSONObject
	 * @return the new JSONObject
	 * @since 2.0.0
	 */
	public JSONObject deepCopy() {
		JSONObject copy = new JSONObject();

		for (Map.Entry<String, Object> entry : values.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if (value instanceof JSONArray)
				value = ((JSONArray) value).deepCopy();

			else if (value instanceof JSONObject)
				value = ((JSONObject) value).deepCopy();

			copy.values.put(key, value);
		}

		return copy;
	}

	/**
	 * Converts the JSONObject into a map. All JSONObjects and JSONArrays contained within
	 * this JSONObject will be converted into their Map or List form as well
	 * @return a map of the entries of this object
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<>();

		for (Entry<String, Object> entry : this) {
			Object value = entry.getValue();

			if (value instanceof JSONObject)
				value = ((JSONObject) value).toMap();

			else if (value instanceof JSONArray)
				value = ((JSONArray) value).toList();

			map.put(entry.getKey(), value);
		}

		return map;
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
	 * Iterates over the whole JSONObject and performs the given action for each element.
	 * <p>
	 * For each entry in the object, the predicate receives the key and its associated
	 * value.
	 * @param action The action for each entry
	 * @since 2.0.0
	 */
	public void forEach(BiConsumer<String, Object> action) {
		forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
	}

	/**
	 * Returns the number of entries in the JSONObject
	 * @return the number of entries
	 */
	public int length() {
		return values.size();
	}

	// -- REMOVE --

	/**
	 * Removes all values from this JSONObject
	 *
	 * @since 1.2.0
	 */
	public void clear() {
		values.clear();
	}

	/**
	 * Removes a key from a JSONObject
	 * @param key the key to be removed
	 * @since 1.2.0
	 * @throws JSONException if the key does not exist
	 */
	public void remove(String key) {
		checkKey(key);
		values.remove(key);
	}

	// -- REMOVE IF --

	/**
	 * Removes all entries from a JSONObject where the predicate returns {@code true}.
	 * <p>
	 * For each entry in the object, the predicate receives the key and its associated
	 * value.
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 */
	public JSONObject removeIf(BiPredicate<String, Object> predicate) {
		Iterator<Map.Entry<String, Object>> iter = values.entrySet().iterator();

		while (iter.hasNext()) {
			Map.Entry<String, Object> entry = iter.next();

			if (predicate.test(entry.getKey(), entry.getValue()))
				iter.remove();
		}

		return this;
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 */
	public JSONObject removeIf(String key, Predicate<Object> predicate) {
		if (predicate.test(checkKey(key)))
			values.remove(key);

		return this;
	}

	private <T> JSONObject removeIf(String key, Predicate<T> predicate, Function<String, T> getter) {
		checkKey(key);

		if (predicate.test(getter.apply(key)))
			values.remove(key);

		return this;
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a boolean
	 */
	public JSONObject removeBooleanIf(String key, Predicate<Boolean> predicate) {
		return removeIf(key, predicate, this::getBoolean);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a byte
	 */
	public JSONObject removeByteIf(String key, Predicate<Byte> predicate) {
		return removeIf(key, predicate, this::getByte);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a short
	 */
	public JSONObject removeShortIf(String key, Predicate<Short> predicate) {
		return removeIf(key, predicate, this::getShort);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not an int
	 */
	public JSONObject removeIntIf(String key, Predicate<Integer> predicate) {
		return removeIf(key, predicate, this::getInt);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a long
	 */
	public JSONObject removeLongIf(String key, Predicate<Long> predicate) {
		return removeIf(key, predicate, this::getLong);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a float
	 */
	public JSONObject removeFloatIf(String key, Predicate<Float> predicate) {
		return removeIf(key, predicate, this::getFloat);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a double
	 */
	public JSONObject removeDoubleIf(String key, Predicate<Double> predicate) {
		return removeIf(key, predicate, this::getDouble);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a byte, or
	 * if the value does not fit into a byte
	 */
	public JSONObject removeByteExactIf(String key, Predicate<Byte> predicate) {
		return removeIf(key, predicate, this::getByteExact);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a short, or
	 * if the value does not fit into a short
	 */
	public JSONObject removeShortExactIf(String key, Predicate<Short> predicate) {
		return removeIf(key, predicate, this::getShortExact);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not an int, or
	 * if the value does not fit into an int
	 */
	public JSONObject removeIntExactIf(String key, Predicate<Integer> predicate) {
		return removeIf(key, predicate, this::getIntExact);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a long, or
	 * if the value does not fit into a long
	 */
	public JSONObject removeLongExactIf(String key, Predicate<Long> predicate) {
		return removeIf(key, predicate, this::getLongExact);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a float, or
	 * if the value does not fit into a float
	 */
	public JSONObject removeFloatExactIf(String key, Predicate<Float> predicate) {
		return removeIf(key, predicate, this::getFloatExact);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a double,
	 * or if the value does not fit into a double
	 */
	public JSONObject removeDoubleExactIf(String key, Predicate<Double> predicate) {
		return removeIf(key, predicate, this::getDoubleExact);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a
	 * JSONObject
	 */
	public JSONObject removeObjectIf(String key, Predicate<JSONObject> predicate) {
		return removeIf(key, predicate, this::getObject);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a JSONArray
	 */
	public JSONObject removeArrayIf(String key, Predicate<JSONArray> predicate) {
		return removeIf(key, predicate, this::getArray);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code true}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not an instant
	 */
	public JSONObject removeInstantIf(String key, Predicate<Instant> predicate) {
		return removeIf(key, predicate, this::getInstant);
	}

	// -- REMOVE KEYS --

	/**
	 * Removes all the keys if the same key exists within the other JSONObject too.
	 * <p>
	 * This does not compare the values.
	 * @param obj the other JSONObject
	 * @return this JSONObject
	 * @since 2.0.0
	 */
	public JSONObject removeKeys(JSONObject obj) {
		removeIf((key, value) -> obj.has(key));
		return this;
	}

	// -- RETAIN IF --

	/**
	 * Removes all entries from a JSONObject where the predicate returns {@code false}.
	 * <p>
	 * For each entry in the object, the predicate receives the key and its associated
	 * value.
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 */
	public JSONObject retainIf(BiPredicate<String, Object> predicate) {
		return removeIf(predicate.negate());
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 */
	public JSONObject retainIf(String key, Predicate<Object> predicate) {
		return removeIf(key, predicate.negate());
	}

	private <T> JSONObject retainIf(String key, Predicate<T> predicate, Function<String, T> getter) {
		return removeIf(key, predicate.negate(), getter);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a boolean
	 */
	public JSONObject retainBooleanIf(String key, Predicate<Boolean> predicate) {
		return retainIf(key, predicate, this::getBoolean);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a byte
	 */
	public JSONObject retainByteIf(String key, Predicate<Byte> predicate) {
		return retainIf(key, predicate, this::getByte);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a short
	 */
	public JSONObject retainShortIf(String key, Predicate<Short> predicate) {
		return retainIf(key, predicate, this::getShort);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not an int
	 */
	public JSONObject retainIntIf(String key, Predicate<Integer> predicate) {
		return retainIf(key, predicate, this::getInt);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a long
	 */
	public JSONObject retainLongIf(String key, Predicate<Long> predicate) {
		return retainIf(key, predicate, this::getLong);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a float
	 */
	public JSONObject retainFloatIf(String key, Predicate<Float> predicate) {
		return retainIf(key, predicate, this::getFloat);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a double
	 */
	public JSONObject retainDoubleIf(String key, Predicate<Double> predicate) {
		return retainIf(key, predicate, this::getDouble);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a byte, or
	 * if the value does not fit into a byte
	 */
	public JSONObject retainByteExactIf(String key, Predicate<Byte> predicate) {
		return retainIf(key, predicate, this::getByteExact);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a short, or
	 * if the value does not fit into a short
	 */
	public JSONObject retainShortExactIf(String key, Predicate<Short> predicate) {
		return retainIf(key, predicate, this::getShortExact);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not an int, or
	 * if the value does not fit into an int
	 */
	public JSONObject retainIntExactIf(String key, Predicate<Integer> predicate) {
		return retainIf(key, predicate, this::getIntExact);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a long, or
	 * if the value does not fit into a long
	 */
	public JSONObject retainLongExactIf(String key, Predicate<Long> predicate) {
		return retainIf(key, predicate, this::getLongExact);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a float, or
	 * if the value does not fit into a float
	 */
	public JSONObject retainFloatExactIf(String key, Predicate<Float> predicate) {
		return retainIf(key, predicate, this::getFloatExact);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a double,
	 * or if the value does not fit into a double
	 */
	public JSONObject retainDoubleExactIf(String key, Predicate<Double> predicate) {
		return retainIf(key, predicate, this::getDoubleExact);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a
	 * JSONObject
	 */
	public JSONObject retainObjectIf(String key, Predicate<JSONObject> predicate) {
		return retainIf(key, predicate, this::getObject);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not a JSONArray
	 */
	public JSONObject retainArrayIf(String key, Predicate<JSONArray> predicate) {
		return retainIf(key, predicate, this::getArray);
	}

	/**
	 * Removes a key from a JSONObject if the predicate returns {@code false}.
	 * @param key the key
	 * @param predicate the predicate
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the key does not exist, or if the value is not an instant
	 */
	public JSONObject retainInstantIf(String key, Predicate<Instant> predicate) {
		return retainIf(key, predicate, this::getInstant);
	}

	// -- RETAIN KEYS --

	/**
	 * Retains only the keys if the same key exists within the other JSONObject too.
	 * <p>
	 * This does not compare the values.
	 * @param obj the other JSONObject
	 * @return this JSONObject
	 * @since 2.0.0
	 */
	public JSONObject retainKeys(JSONObject obj) {
		removeIf((key, value) -> !obj.has(key));
		return this;
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
	 * Checks if the value with the specified key is {@code null}
	 * @param key the key
	 * @return whether the value is {@code null}
	 * @throws JSONException if the key does not exist
	 */
	public boolean isNull(String key) {
		return checkKey(key) == null;
	}

	/**
	 * Checks if the value with the specified key is a boolean
	 * @param key the key
	 * @return whether the value is a boolean
	 * @throws JSONException if the key does not exist
	 */
	public boolean isBoolean(String key) {
		return checkKey(key) instanceof Boolean;
	}

	/**
	 * Checks if the value with the specified key is a string
	 * @param key the key
	 * @return whether the value is a string
	 * @throws JSONException if the key does not exist
	 */
	public boolean isString(String key) {
		Object value = checkKey(key);
		return value instanceof String;
	}

	/**
	 * Checks if the value with the specified key is a number
	 * @param key the key
	 * @return whether the value is a number
	 * @throws JSONException if the key does not exist
	 */
	public boolean isNumber(String key) {
		Object value = checkKey(key);
		return value instanceof Number;
	}

	/**
	 * Checks if the value with the specified key is a JSONObject
	 * @param key the key
	 * @return whether the value is a JSONObject
	 * @throws JSONException if the key does not exist
	 */
	public boolean isObject(String key) {
		return checkKey(key) instanceof JSONObject;
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

	/**
	 * Checks if the value with the specified key can be converted to an {@link Instant}.
	 * @param key the key
	 * @return whether the value is an Instant
	 * @see #parseInstant(Object)
	 * @since 1.1.0
	 * @throws JSONException if the key does not exist
	 */
	public boolean isInstant(String key) {
		Object val = checkKey(key);

		try {
			parseInstant(val);
			return true;
		}
		catch (Exception e) {
			return false;
		}
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
	 * Returns the value as a boolean for a given key
	 * @param key the key
	 * @return the boolean
	 * @throws JSONException if the key does not exist, or if the value is not a boolean
	 */
	public boolean getBoolean(String key) {
		return checkType(this::isBoolean, key, "boolean");
	}

	/**
	 * Returns the value as a string for a given key
	 * @param key the key
	 * @return the string
	 * @throws JSONException if the key does not exist, or if the value is not a string
	 */
	public String getString(String key) {
		return checkType(this::isString, key, "string");
	}

	/**
	 * Returns the value as a number for a given key
	 * @param key the key
	 * @return the number
	 * @throws JSONException if the key does not exist, or if the value is not a number
	 */
	public Number getNumber(String key) {
		if (isInstant(key))
			return getInstant(key).getEpochSecond();

		return checkType(this::isNumber, key, "number");
	}

	/**
	 * Returns the value as a byte for a given key
	 * @param key the key
	 * @return the byte
	 * @throws JSONException if the key does not exist, or if the value is not a byte
	 */
	public byte getByte(String key) {
		return getNumber(key).byteValue();
	}

	/**
	 * Returns the value as a short for a given key
	 * @param key the key
	 * @return the short
	 * @throws JSONException if the key does not exist, or if the value is not a short
	 */
	public short getShort(String key) {
		return getNumber(key).shortValue();
	}

	/**
	 * Returns the value as an int for a given key
	 * @param key the key
	 * @return the int
	 * @throws JSONException if the key does not exist, or if the value is not an int
	 */
	public int getInt(String key) {
		return getNumber(key).intValue();
	}

	/**
	 * Returns the value as a long for a given key
	 * @param key the key
	 * @return the long
	 * @throws JSONException if the key does not exist, or if the value is not a long
	 */
	public long getLong(String key) {
		return getNumber(key).longValue();
	}

	/**
	 * Returns the value as a float for a given key
	 * @param key the key
	 * @return the float
	 * @throws JSONException if the key does not exist, or if the value is not a float
	 */
	public float getFloat(String key) {
		return getNumber(key).floatValue();
	}

	/**
	 * Returns the value as a double for a given key
	 * @param key the key
	 * @return the double
	 * @throws JSONException if the key does not exist, or if the value is not a double
	 */
	public double getDouble(String key) {
		return getNumber(key).doubleValue();
	}

	private <T> T getNumberExact(String key, String type, Function<BigInteger, T> bigint,
			Function<BigDecimal, T> bigdec) {
		Number number = getNumber(key);

		try {
			if (number instanceof BigInteger)
				return bigint.apply((BigInteger) number);

			if (number instanceof BigDecimal)
				return bigdec.apply((BigDecimal) number);

		}
		catch (Exception e) {
		}

		throw mismatch(key, type);
	}

	/**
	 * Returns the exact value as a byte for a given key. This fails if the value does not
	 * fit into a byte
	 * @param key the key
	 * @return the byte
	 * @throws JSONException if the key does not exist, the value is not a byte, or if the
	 * value does not fit into a byte
	 */
	public byte getByteExact(String key) {
		return getNumberExact(key, "byte", BigInteger::byteValueExact, BigDecimal::byteValueExact);
	}

	/**
	 * Returns the exact value as a short for a given key. This fails if the value does
	 * not fit into a short
	 * @param key the key
	 * @return the short
	 * @throws JSONException if the key does not exist, the value is not a short, or if
	 * the value does not fit into a short
	 */
	public short getShortExact(String key) {
		return getNumberExact(key, "short", BigInteger::shortValueExact, BigDecimal::shortValueExact);
	}

	/**
	 * Returns the exact value as an int for a given key. This fails if the value does not
	 * fit into an int
	 * @param key the key
	 * @return the int
	 * @throws JSONException if the key does not exist, the value is not an int, or if the
	 * value does not fit into an int
	 */
	public int getIntExact(String key) {
		return getNumberExact(key, "int", BigInteger::intValueExact, BigDecimal::intValueExact);
	}

	/**
	 * Returns the exact value as a long for a given key. This fails if the value does not
	 * fit into a long
	 * @param key the key
	 * @return the long
	 * @throws JSONException if the key does not exist, the value is not a long, or if the
	 * value does not fit into a long
	 */
	public long getLongExact(String key) {
		return getNumberExact(key, "long", BigInteger::longValueExact, BigDecimal::longValueExact);
	}

	/**
	 * Returns the exact value as a float for a given key. This fails if the value does
	 * not fit into a float
	 * @param key the key
	 * @return the float
	 * @throws JSONException if the key does not exist, the value is not a float, or if
	 * the value does not fit into a float
	 */
	public float getFloatExact(String key) {
		Number num = getNumber(key);

		if (num instanceof Double) // NaN and Infinity
			return ((Double) num).floatValue();

		float f = num.floatValue();

		if (!Float.isFinite(f))
			throw mismatch(key, "float");

		return f;
	}

	/**
	 * Returns the exact value as a double for a given key. This fails if the value does
	 * not fit into a double
	 * @param key the key
	 * @return the double
	 * @throws JSONException if the key does not exist, the value is not a double, or if
	 * the value does not fit into a double
	 */
	public double getDoubleExact(String key) {
		Number num = getNumber(key);

		if (num instanceof Double) // NaN and Infinity
			return (Double) num;

		double d = num.doubleValue();

		if (!Double.isFinite(d))
			throw mismatch(key, "double");

		return d;
	}

	/**
	 * Returns the value as a JSONObject for a given key
	 * @param key the key
	 * @return the JSONObject
	 * @throws JSONException if the key does not exist, or if the value is not a
	 * JSONObject
	 */
	public JSONObject getObject(String key) {
		return checkType(this::isObject, key, "object");
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

	/**
	 * Returns the value as an Instant for a given key.
	 * @param key the key
	 * @return the Instant
	 * @see #parseInstant(Object)
	 * @since 1.1.0
	 * @throws JSONException if the key does not exist, or if the value is not an Instant
	 */
	public Instant getInstant(String key) {
		Object val = checkKey(key);

		try {
			return parseInstant(val);
		}
		catch (Exception e) {
			throw mismatch(key, "instant");
		}
	}

	// -- OPTIONAL --

	private <T> T getOpt(String key, Function<String, T> supplier, T defaults) {
		try {
			return supplier.apply(key);
		}
		catch (Exception e) {
			return defaults;
		}
	}

	/**
	 * Returns the value for a given key, or the default value if the operation is not
	 * possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the value
	 */
	public Object get(String key, Object defaults) {
		return getOpt(key, this::get, defaults);
	}

	/**
	 * Returns the value as a boolean for a given key, or the default value if the
	 * operation is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the boolean
	 */
	public boolean getBoolean(String key, boolean defaults) {
		return getOpt(key, this::getBoolean, defaults);
	}

	/**
	 * Returns the value as a string for a given key, or the default value if the
	 * operation is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the string
	 */
	public String getString(String key, String defaults) {
		return getOpt(key, this::getString, defaults);
	}

	/**
	 * Returns the value as a number for a given key, or the default value if the
	 * operation is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the number
	 */
	public Number getNumber(String key, Number defaults) {
		return getOpt(key, this::getNumber, defaults);
	}

	/**
	 * Returns the value as a byte for a given key, or the default value if the operation
	 * is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the byte
	 */
	public byte getByte(String key, byte defaults) {
		return getOpt(key, this::getByte, defaults);
	}

	/**
	 * Returns the value as a short for a given key, or the default value if the operation
	 * is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the short
	 */
	public short getShort(String key, short defaults) {
		return getOpt(key, this::getShort, defaults);
	}

	/**
	 * Returns the value as an int for a given key, or the default value if the operation
	 * is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the int
	 */
	public int getInt(String key, int defaults) {
		return getOpt(key, this::getInt, defaults);
	}

	/**
	 * Returns the value as a long for a given key, or the default value if the operation
	 * is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the long
	 */
	public long getLong(String key, long defaults) {
		return getOpt(key, this::getLong, defaults);
	}

	/**
	 * Returns the value as a float for a given key, or the default value if the operation
	 * is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the float
	 */
	public float getFloat(String key, float defaults) {
		return getOpt(key, this::getFloat, defaults);
	}

	/**
	 * Returns the value as a double for a given key, or the default value if the
	 * operation is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the double
	 */
	public double getDouble(String key, double defaults) {
		return getOpt(key, this::getDouble, defaults);
	}

	/**
	 * Returns the exact value as a byte for a given key, or the default value if the
	 * operation is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the byte
	 */
	public byte getByteExact(String key, byte defaults) {
		return getOpt(key, this::getByteExact, defaults);
	}

	/**
	 * Returns the exact value as a short for a given key, or the default value if the
	 * operation is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the short
	 */
	public short getShortExact(String key, short defaults) {
		return getOpt(key, this::getShortExact, defaults);
	}

	/**
	 * Returns the exact value as an int for a given key, or the default value if the
	 * operation is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the int
	 */
	public int getIntExact(String key, int defaults) {
		return getOpt(key, this::getIntExact, defaults);
	}

	/**
	 * Returns the exact value as a long for a given key, or the default value if the
	 * operation is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the long
	 */
	public long getLongExact(String key, long defaults) {
		return getOpt(key, this::getLongExact, defaults);
	}

	/**
	 * Returns the exact value as a float for a given key, or the default value if the
	 * operation is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the float
	 */
	public float getFloatExact(String key, float defaults) {
		return getOpt(key, this::getFloatExact, defaults);
	}

	/**
	 * Returns the exact value as a double for a given key, or the default value if the
	 * operation is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the double
	 */
	public double getDoubleExact(String key, double defaults) {
		return getOpt(key, this::getDoubleExact, defaults);
	}

	/**
	 * Returns the value as a JSONObject for a given key, or the default value if the
	 * operation is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the JSONObject
	 */
	public JSONObject getObject(String key, JSONObject defaults) {
		return getOpt(key, this::getObject, defaults);
	}

	/**
	 * Returns the value as a JSONArray for a given key, or the default value if the
	 * operation is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the JSONArray
	 */
	public JSONArray getArray(String key, JSONArray defaults) {
		return getOpt(key, this::getArray, defaults);
	}

	/**
	 * Returns the value as an Instant for a given key, or the default value if the
	 * operation is not possible
	 * @param key the key
	 * @param defaults the default value
	 * @return the Instant
	 * @since 1.1.0
	 */
	public Instant getInstant(String key, Instant defaults) {
		return getOpt(key, this::getInstant, defaults);
	}

	// -- PUT --

	/**
	 * Adds the values of the given JSONObject to this JSONObject. Afterwards, changes in
	 * nested JSONObjects or JSONArrays of one object are reflected in the other object
	 * too.
	 * <p>
	 * This effectively shallow copies one JSONObject into another.
	 * @param obj the other JSONObject
	 * @return this JSONObject
	 * @since 2.0.0
	 */
	public JSONObject putAll(JSONObject obj) {
		values.putAll(obj.values);
		return this;
	}

	/**
	 * Adds the values of the given JSONObject to this JSONObject. For all nested
	 * JSONObjects and JSONArrays, {@link #deepCopy() deep copies} are created.
	 * Afterwards, changes in nested JSONObjects or JSONArrays of one object are
	 * <i>not</i> reflected in the other object.
	 * <p>
	 * This effectively deep copies one JSONObject into another.
	 * @param obj the other JSONObject
	 * @return this JSONObject
	 * @since 2.0.0
	 */
	public JSONObject putAllDeep(JSONObject obj) {
		values.putAll(obj.deepCopy().values);
		return this;
	}

	// -- SET --

	/**
	 * Sets the value at a given key
	 * @param key the key
	 * @param value the new value
	 * @return this JSONObject
	 */
	public JSONObject set(String key, Object value) {
		values.put(key, sanitize(value));
		return this;
	}

	/**
	 * Sets the value at a given key if there is no value associated with the key yet
	 * @param key the key
	 * @param value the new value
	 * @return this JSONObject
	 * @since 2.0.0
	 */
	public JSONObject setIfAbsent(String key, Object value) {
		if (!has(key))
			set(key, value);

		return this;
	}

	/**
	 * Sets the value at a given key if there is already a value associated with the key
	 * @param key the key
	 * @param value the new value
	 * @return this JSONObject
	 * @since 2.0.0
	 */
	public JSONObject setIfPresent(String key, Object value) {
		if (has(key))
			set(key, value);

		return this;
	}

	// -- COMPUTE --

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value, or {@code null}
	 * if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 */
	public JSONObject compute(String key, BiFunction<String, Object, Object> remappingFunction) {
		values.compute(key, remappingFunction);
		return this;
	}

	private <T> JSONObject compute(String key, BiFunction<String, T, Object> remappingFunction,
			Function<String, T> getter) {
		T value = has(key) ? getter.apply(key) : null;
		return set(key, remappingFunction.apply(key, value));
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a boolean, or
	 * {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a boolean
	 */
	public JSONObject computeBoolean(String key, BiFunction<String, Boolean, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getBoolean);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a byte, or
	 * {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a byte
	 */
	public JSONObject computeByte(String key, BiFunction<String, Byte, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getByte);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a short, or
	 * {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a short
	 */
	public JSONObject computeShort(String key, BiFunction<String, Short, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getShort);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as an int, or
	 * {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not an int
	 */
	public JSONObject computeInt(String key, BiFunction<String, Integer, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getInt);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a long, or
	 * {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a long
	 */
	public JSONObject computeLong(String key, BiFunction<String, Long, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getLong);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a float, or
	 * {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a float
	 */
	public JSONObject computeFloat(String key, BiFunction<String, Float, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getFloat);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a double, or
	 * {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a double
	 */
	public JSONObject computeDouble(String key, BiFunction<String, Double, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getDouble);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a byte (exact
	 * value), or {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a byte, or if the value does not fit into
	 * a byte
	 */
	public JSONObject computeByteExact(String key, BiFunction<String, Byte, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getByteExact);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a short (exact
	 * value), or {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a short, or if the value does not fit
	 * into a short
	 */
	public JSONObject computeShortExact(String key, BiFunction<String, Short, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getShortExact);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as an int (exact
	 * value), or {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not an int, or if the value does not fit into
	 * an int
	 */
	public JSONObject computeIntExact(String key, BiFunction<String, Integer, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getIntExact);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a long (exact
	 * value), or {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a long, or if the value does not fit into
	 * a long
	 */
	public JSONObject computeLongExact(String key, BiFunction<String, Long, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getLongExact);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a float (exact
	 * value), or {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a float, or if the value does not fit
	 * into a float
	 */
	public JSONObject computeFloatExact(String key, BiFunction<String, Float, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getFloatExact);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a double (exact
	 * value), or {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a double, or if the value does not fit
	 * into a double
	 */
	public JSONObject computeDoubleExact(String key, BiFunction<String, Double, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getDoubleExact);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a JSONObject,
	 * or {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a JSONObject
	 */
	public JSONObject computeObject(String key, BiFunction<String, JSONObject, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getObject);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a JSONArray, or
	 * {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a JSONArray
	 */
	public JSONObject computeArray(String key, BiFunction<String, JSONArray, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getArray);
	}

	/**
	 * Replaces or sets the value associated with the given key with the value returned by
	 * the remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as an instant, or
	 * {@code null} if there is currently no value associated with the key.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not an instant
	 */
	public JSONObject computeInstant(String key, BiFunction<String, Instant, Object> remappingFunction) {
		return compute(key, remappingFunction, this::getInstant);
	}

	// -- COMPUTE IF PRESENT --

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 */
	public JSONObject computeIfPresent(String key, BiFunction<String, Object, Object> remappingFunction) {
		values.computeIfPresent(key, remappingFunction);
		return this;
	}

	private <T> JSONObject computeIfPresent(String key, BiFunction<String, T, Object> remappingFunction,
			Function<String, T> getter) {
		if (!has(key))
			return this;

		return set(key, remappingFunction.apply(key, getter.apply(key)));
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a boolean.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a boolean
	 */
	public JSONObject computeBooleanIfPresent(String key, BiFunction<String, Boolean, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getBoolean);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a byte.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a byte
	 */
	public JSONObject computeByteIfPresent(String key, BiFunction<String, Byte, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getByte);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a short.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a short
	 */
	public JSONObject computeShortIfPresent(String key, BiFunction<String, Short, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getShort);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as an int.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not an int
	 */
	public JSONObject computeIntIfPresent(String key, BiFunction<String, Integer, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getInt);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a long.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a long
	 */
	public JSONObject computeLongIfPresent(String key, BiFunction<String, Long, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getLong);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a float.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a float
	 */
	public JSONObject computeFloatIfPresent(String key, BiFunction<String, Float, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getFloat);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a double.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a double
	 */
	public JSONObject computeDoubleIfPresent(String key, BiFunction<String, Double, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getDouble);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a byte (exact
	 * value).
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a byte, or if the value does not fit into
	 * a byte
	 */
	public JSONObject computeByteExactIfPresent(String key, BiFunction<String, Byte, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getByteExact);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a short (exact
	 * value).
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a short, or if the value does not fit
	 * into a short
	 */
	public JSONObject computeShortExactIfPresent(String key, BiFunction<String, Short, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getShortExact);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as an int (exact
	 * value).
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not an int, or if the value does not fit into
	 * an int
	 */
	public JSONObject computeIntExactIfPresent(String key, BiFunction<String, Integer, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getIntExact);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a long (exact
	 * value).
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a long, or if the value does not fit into
	 * a long
	 */
	public JSONObject computeLongExactIfPresent(String key, BiFunction<String, Long, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getLongExact);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a float (exact
	 * value).
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a float, or if the value does not fit
	 * into a float
	 */
	public JSONObject computeFloatExactIfPresent(String key, BiFunction<String, Float, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getFloatExact);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a double (exact
	 * value).
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a double, or if the value does not fit
	 * into a double
	 */
	public JSONObject computeDoubleExactIfPresent(String key, BiFunction<String, Double, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getDoubleExact);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a JSONObject.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a JSONObject
	 */
	public JSONObject computeObjectIfPresent(String key, BiFunction<String, JSONObject, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getObject);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as a JSONArray.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not a JSONArray
	 */
	public JSONObject computeArrayIfPresent(String key, BiFunction<String, JSONArray, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getArray);
	}

	/**
	 * Replaces the value associated with the given key with the value returned by the
	 * remapping function.
	 * <p>
	 * The remapping function receives the key and its associated value as an instant.
	 * @param key the key
	 * @param remappingFunction the remapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 * @throws JSONException if the value is not an instant
	 */
	public JSONObject computeInstantIfPresent(String key, BiFunction<String, Instant, Object> remappingFunction) {
		return computeIfPresent(key, remappingFunction, this::getInstant);
	}

	// -- COMPUTE IF ABSENT --

	/**
	 * Associates the value returned by the mapping function with the given key if the key
	 * is not already associated with a value.
	 * <p>
	 * The mapping function receives the key.
	 * @param key the key
	 * @param mappingFunction the mapping function
	 * @return this JSONObject
	 * @since 2.0.0
	 */
	public JSONObject computeIfAbsent(String key, Function<String, Object> mappingFunction) {
		if (!has(key))
			set(key, mappingFunction.apply(key));

		return this;
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

	/**
	 * Tries to convert an object to an Instant.
	 * <p>
	 * In order for the conversion to succeed, the value must be either
	 * <ul>
	 * <li>an {@link Instant},</li>
	 * <li>a String formatted according to
	 * <a href="https://datatracker.ietf.org/doc/html/rfc3339#section-5.6">RFC 3339,
	 * Section 5.6</a>, or</li>
	 * <li>an integer (primitive type or {@link BigInteger}) that does not exceed the
	 * limits of {@link Instant#ofEpochSecond(long)}</li>
	 * </ul>
	 * @param value the value
	 * @return the parsed Instant
	 * @see #isInstant(String)
	 * @throws DateTimeException if the value is malformed or out of range
	 * @throws JSONException if the value has an invalid type
	 */
	public static Instant parseInstant(Object value) {
		if (value instanceof Instant)
			return (Instant) value;

		if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
			return Instant.ofEpochSecond((long) value);

		if (value instanceof BigInteger)
			return Instant.ofEpochSecond(((BigInteger) value).longValueExact());

		if (value instanceof String)
			return Instant.parse((String) value);

		String className = value == null ? "null" : value.getClass().getSimpleName();

		throw new JSONException(className + " cannot be converted to Instant");
	}

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
