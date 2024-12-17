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
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A JSONArray is an array structure capable of holding multiple values, including other
 * JSONArrays and {@link JSONObject JSONObjects}
 *
 * @author SyntaxError404
 *
 */
public final class JSONArray implements JSONValue {

	List<Object> values;

	/**
	 * Constructs a new JSONArray
	 */
	public JSONArray() {
		values = new ArrayList<>();
	}

	@Override
	public @Nullable Object value() {
		return this;
	}

	/**
	 * Returns the number of values in the JSONArray
	 * @return the number of values
	 */
	public int length() {
		return values.size();
	}

	// -- GET --

	/**
	 * Returns the value for a given index
	 * @param index the index
	 * @return the value
	 * @throws JSONException if the index does not exist
	 */
	public Object get(int index) {
		return values.get(index);
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

}
