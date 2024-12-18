package io.jstach.ezkv.json5.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;

import org.jspecify.annotations.Nullable;

public sealed interface JSONValue {

	public final class JSONObject implements JSONValue {

		SequencedMap<String, JSONValue> values = new LinkedHashMap<>();

		/**
		 * Constructs a new JSONObject
		 */
		public JSONObject() {
			values = new LinkedHashMap<>();
		}

		/**
		 * Returns a set of keys of the JSONObject. Modifying the set will modify the
		 * JSONObject
		 * @return a set of keys
		 *
		 * @see Map#keySet()
		 */
		@SuppressWarnings("return")
		public Set<String> keySet() {
			return values.keySet();
		}

		/**
		 * Returns the value for a given key
		 * @param key the key
		 * @return the value
		 * @throws JSONException if the key does not exist
		 */
		public JSONValue get(String key) {
			return checkKey(key);
		}

		public @Nullable JSONValue valueOrNull(String key) {
			return values.get(key);
		}

		private JSONValue checkKey(String key) {
			var value = valueOrNull(key);
			if (value == null)
				throw new JSONException("JSONObject[" + JSONStringify.quote(key) + "] does not exist");

			return value;
		}

		// @SuppressWarnings("null") // Eclipse null analysis bug
		public void merge(String key, JSONValue value) {
			var existing = valueOrNull(key);
			switch (existing) {
				case JSONArray a -> a.values.add(value);
				case JSONValue v -> {
					JSONArray array = new JSONArray();
					array.values.add(existing);
					array.values.add(value);
					values.put(key, array);
				}
				case null -> {
					values.put(key, value);
				}
			}
		}

	}

	/**
	 * A JSONArray is an array structure capable of holding multiple values, including
	 * other JSONArrays and {@link JSONObject JSONObjects}
	 *
	 * @author SyntaxError404
	 *
	 */
	public final class JSONArray implements JSONValue {

		List<JSONValue> values;

		/**
		 * Constructs a new JSONArray
		 */
		public JSONArray() {
			values = new ArrayList<>();
		}

		/**
		 * Returns the number of values in the JSONArray
		 * @return the number of values
		 */
		public int length() {
			return values.size();
		}

		/**
		 * Returns the value for a given index
		 * @param index the index
		 * @return the value
		 * @throws JSONException if the index does not exist
		 */
		public JSONValue get(int index) {
			return values.get(index);
		}

	}

	record JSONString(String value) implements JSONValue {
	}

	record JSONNumber(Number value, String raw) implements JSONValue {
	}

	enum JSONBoolean implements JSONValue {

		FALSE {
			@Override
			public boolean val() {
				return false;
			}
		},
		TRUE {
			@Override
			public boolean val() {
				return true;
			}
		};

		public abstract boolean val();

	}

	enum JSONNull implements JSONValue {

		NULL;

	}

}
