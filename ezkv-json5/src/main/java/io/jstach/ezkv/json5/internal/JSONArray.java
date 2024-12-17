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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A JSONArray is an array structure capable of holding multiple values, including other
 * JSONArrays and {@link JSONObject JSONObjects}
 *
 * @author SyntaxError404
 *
 */
public final class JSONArray implements Iterable<Object>, JSONValue {

	private List<Object> values;

	/**
	 * Constructs a new JSONArray
	 */
	public JSONArray() {
		values = new ArrayList<>();
	}

	/**
	 * Constructs a new JSONArray from a JSONParser
	 * @param parser a JSONParser
	 */
	public JSONArray(JSONParser parser) {
		this();

		char c;

		if (parser.nextClean() != '[')
			throw parser.syntaxError("A JSONArray must begin with '['");

		while (true) {
			c = parser.nextClean();

			switch (c) {
				case 0:
					throw parser.syntaxError("A JSONArray must end with ']'");
				case ']':
					return;
				default:
					parser.back();
			}

			JSONValue value = parser.nextValue();

			values.add(value.value());

			c = parser.nextClean();

			if (c == ']')
				return;

			if (c != ',')
				throw parser.syntaxError("Expected ',' or ']' after value, got '" + c + "' instead");
		}
	}

	@Override
	public @Nullable Object value() {
		return this;
	}

	// /**
	// * Converts the JSONArray into a list. All JSONObjects and JSONArrays contained
	// within
	// * this JSONArray will be converted into their Map or List form as well
	// * @return a list of the values of this array
	// */
	// public List<Object> toList() {
	// List<Object> list = new ArrayList<>();
	//
	// for (Object value : this) {
	// if (value instanceof JSONObject)
	// value = ((JSONObject) value).toMap();
	//
	// else if (value instanceof JSONArray)
	// value = ((JSONArray) value).toList();
	//
	// list.add((Object) value);
	// }
	//
	// return list;
	// }

	@Override
	public Iterator<Object> iterator() {
		return values.iterator();
	}

	/**
	 * Returns the number of values in the JSONArray
	 * @return the number of values
	 */
	public int length() {
		return values.size();
	}

	/**
	 * Removes all values from this JSONArray
	 *
	 * @since 1.2.0
	 */
	public void clear() {
		values.clear();
	}

	// -- GET --

	/**
	 * Returns the value for a given index
	 * @param index the index
	 * @return the value
	 * @throws JSONException if the index does not exist
	 */
	public Object get(int index) {
		checkIndex(index);
		return values.get(index);
	}

	// -- ADD --

	/**
	 * Adds a value to the JSONArray
	 * @param value the new value
	 * @return this JSONArray
	 */
	public JSONArray add(Object value) {
		values.add(JSONObject.sanitize(value));
		return this;
	}

	// -- STRINGIFY --

	/**
	 * Converts the JSONArray into its string representation. The indentation factor
	 * enables pretty-printing and defines how many spaces (' ') should be placed before
	 * each value. A factor of {@code < 1} disables pretty-printing and discards any
	 * optional whitespace characters.
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
	 * @param indentFactor the indentation factor
	 * @return the string representation
	 *
	 * @see JSONStringify#toString(JSONArray, int)
	 */
	public String toString(int indentFactor) {
		return JSONStringify.toString(this, indentFactor);
	}

	/**
	 * Converts the JSONArray into its compact string representation.
	 * @return the compact string representation
	 */
	@Override
	public String toString() {
		return toString(0);
	}

	// -- MISCELLANEOUS --

	private Object checkIndex(int index) {
		if (index < 0 || index >= length())
			throw new JSONException("JSONArray[" + index + "] does not exist");

		return values.get(index);
	}

}
