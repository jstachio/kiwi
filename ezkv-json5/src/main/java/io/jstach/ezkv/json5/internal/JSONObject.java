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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * A JSONObject is a map (key-value) structure capable of holding multiple values,
 * including other {@link JSONArray JSONArrays} and JSONObjects
 *
 * @author SyntaxError404
 *
 */
public final class JSONObject implements JSONValue {

	LinkedHashMap<String, Object> values;

	/**
	 * Constructs a new JSONObject
	 */
	public JSONObject() {
		values = new LinkedHashMap<>();
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


	private Object checkKey(String key) {
		if (!values.containsKey(key))
			throw new JSONException("JSONObject[" + JSONStringify.quote(key) + "] does not exist");

		return values.get(key);
	}

}
