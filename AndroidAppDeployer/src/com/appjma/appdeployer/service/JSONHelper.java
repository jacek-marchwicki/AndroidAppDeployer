/*
 * Copyright (C) 2013 Jacek Marchwicki <jacek.marchwicki@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.appjma.appdeployer.service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONHelper {
	public static final JSONArray EMPTY_ARRAY = new JSONArray();

	public static JSONArray getJSONArrayOrDefault(JSONObject json, String key,
			JSONArray defaultValue) throws JSONException {
		if (json.isNull(key)) {
			return defaultValue;
		}
		return json.getJSONArray(key);
	}

	public static JSONArray getJSONArrayOrNull(JSONObject json, String key)
			throws JSONException {
		return getJSONArrayOrDefault(json, key, null);
	}

	public static JSONArray getJSONArrayOrEmtpy(JSONObject json, String key)
			throws JSONException {
		return getJSONArrayOrDefault(json, key, EMPTY_ARRAY);
	}

	private static void throwIfKeyIsNull(JSONObject json, String key)
			throws JSONException {
		if (json.isNull(key)) {
			throw new JSONException("Key \"" + key + "\" is null");
		}
	}

	public static JSONArray getJSONArrayOrThrow(JSONObject json, String key)
			throws JSONException {
		throwIfKeyIsNull(json, key);
		return json.getJSONArray(key);
	}

	public static String getStringOrDefault(JSONObject json, String key,
			String defaultValue) throws JSONException {
		if (json.isNull(key)) {
			return defaultValue;
		}
		return json.getString(key);
	}

	public static String getStrinOrThrow(JSONObject json, String key)
			throws JSONException {
		throwIfKeyIsNull(json, key);
		return json.getString(key);
	}

	public static String getStrinOrNull(JSONObject json, String key)
			throws JSONException {
		return getStringOrDefault(json, key, null);
	}

}
