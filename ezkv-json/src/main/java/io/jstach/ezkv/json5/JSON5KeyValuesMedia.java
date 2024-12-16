package io.jstach.ezkv.json5;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;

import io.jstach.ezkv.json5.internal.JSONArray;
import io.jstach.ezkv.json5.internal.JSONObject;
import io.jstach.ezkv.json5.internal.JSONParser;
import io.jstach.ezkv.kvs.KeyValuesMedia;

public class JSON5KeyValuesMedia implements KeyValuesMedia, KeyValuesMedia.Parser {

	public static String MEDIA_TYPE = "application/json5";
	public static String FILE_EXT = "json5";
	public static String JSON_FILE_EXT = "json";
	public static String JSON_MEDIA_TYPE = "application/json";

	
	@Override
	public void parse(
			InputStream input,
			BiConsumer<String, String> consumer)
			throws IOException {
		JSONParser parser = new JSONParser(new InputStreamReader(input));
		parse(parser, consumer);
		
	}
	
	@Override
	public void parse(
			String input,
			BiConsumer<String, String> consumer) {
		JSONParser parser = new JSONParser(new StringReader(input));
		parse(parser, consumer);
	}

	private void parse(
			JSONParser parser,
			BiConsumer<String, String> consumer) {
		var json = parser.nextValue();
		
		switch(json) {
		case JSONObject o ->  {
			flattenJsonObject(o, "", consumer);
		}
		case JSONArray a -> {
			flattenJsonArray(a, "", consumer);
		}
		default -> {
			throw new IllegalStateException();
		}
		}
	}

	@Override
	public String getMediaType() {
		return MEDIA_TYPE;
	}

	@Override
	public @Nullable String getFileExt() {
		return FILE_EXT;
	}

	@Override
	public Parser parser() {
		return this;
	}

	@Override
	public Optional<KeyValuesMedia> findByMediaType(
			String mediaType) {
		var json5 =  KeyValuesMedia.super.findByMediaType(mediaType);
		if (json5.isPresent()) {
			return json5;
		}
		if (JSON_MEDIA_TYPE.equalsIgnoreCase(mediaType)) {
			return Optional.of(this);
		}
		return Optional.empty();
	}

	@Override
	public boolean hasFileExt(
			String filename) {
		boolean json5 = KeyValuesMedia.super.hasFileExt(filename);
		if (json5) {
			return true;
		}
		return filename.endsWith("." + JSON_FILE_EXT);
	}
	
	private static void flattenJsonObject(
			JSONObject jsonObject,
			String prefix,
			BiConsumer<String,String> consumer) {
		for (String key : jsonObject.keySet()) {
			Object value = jsonObject.get(key);
			String newKey = prefix.isEmpty() ? key : prefix + "." + key;
			dispatch(newKey, value, consumer);
		}
	}

	private static void flattenJsonArray(
			JSONArray jsonArray,
			String prefix,
			BiConsumer<String,String> consumer) {
		for (int i = 0; i < jsonArray.length(); i++) {
			Object value = jsonArray.get(i);
			String newKey = prefix.isEmpty() ? String.valueOf(i) : prefix + "[" + i + "]";
			dispatch(newKey, value, consumer);
		}
	}
	
	private static void dispatch(
			String newKey,
			Object value,
			BiConsumer<String, String> consumer) {
		switch (value) {
		case JSONObject o -> flattenJsonObject(o, newKey, consumer);
		case JSONArray a -> flattenJsonArray(a, newKey, consumer);
		case null -> {}
		case Object o -> consumer.accept(newKey, "" + o);
		}
	}
	
	

}
