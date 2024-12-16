package io.jstach.ezkv.json5.internal;

import org.jspecify.annotations.Nullable;

public sealed interface JSONValue permits JSONObject, JSONArray, JSONValue.JSONString, JSONValue.JSONNumber,
		JSONValue.JSONBool, JSONValue.JSONNull {

	@Nullable
	Object value();

	record JSONString(String value) implements JSONValue {
	}

	record JSONNumber(Number value) implements JSONValue {
	}

	enum JSONBool implements JSONValue {

		FALSE {
			@Override
			public Object value() {
				return false;
			}
		},
		TRUE {
			@Override
			public Object value() {
				return true;
			}
		};

	}

	enum JSONNull implements JSONValue {

		NULL;

		@Override
		public @Nullable Object value() {
			return null;
		}

	}

}
