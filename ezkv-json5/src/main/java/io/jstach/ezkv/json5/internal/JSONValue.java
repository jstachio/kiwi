package io.jstach.ezkv.json5.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

public sealed interface JSONValue {

	/**
	 * A JSONObject is a map (key-value) structure capable of holding multiple values,
	 * including other {@link JSONArray JSONArrays} and JSONObjects
	 *
	 * @author SyntaxError404
	 *
	 */
	public final class JSONObject implements JSONValue {

		LinkedHashMap<String, JSONValue> values;

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
			checkKey(key);
			return values.get(key);
		}

		public @Nullable JSONValue valueOrNull(String key) {
			return values.get(key);
		}

		private Object checkKey(String key) {
			if (!values.containsKey(key))
				throw new JSONException("JSONObject[" + JSONStringify.quote(key) + "] does not exist");

			return values.get(key);
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

	record JSONNumber(Number value) implements JSONValue {
	}

	enum JSONBool implements JSONValue {

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
