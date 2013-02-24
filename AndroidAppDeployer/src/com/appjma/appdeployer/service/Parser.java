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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Parser {

	private final ParserResult mParserResult;
	private SimpleDateFormat mDateFormat;

	public Parser(ParserResult parserResult) {
		mParserResult = parserResult;
		mDateFormat = new SimpleDateFormat("MMM F, y h:m:s a", Locale.UK);
	}

	public void parseApps(JSONObject json) throws JSONException {
		JSONArray items = JSONHelper.getJSONArrayOrEmtpy(json, "items");
		for (int i = 0; i < items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			parseApp(null, item);
		}
		String nextToken = JSONHelper.getStrinOrNull(json, "next_token");
		mParserResult.setAppsNextToken(nextToken);
	}

	public void parseApp(String id, JSONObject json) throws JSONException {
		String guid = JSONHelper.getStrinOrThrow(json, "guid");
		String name = JSONHelper.getStrinOrThrow(json, "name");
		String token = JSONHelper.getStrinOrThrow(json, "token");
		Date createdAt = getTimeOrThrow(json, "created_at");
		Date updatedAt = getTimeOrThrow(json, "updated_at");
		mParserResult.addApp(id, guid, name, token, createdAt, updatedAt);
	}

	public void parseAppVersions(String appId, JSONObject json)
			throws JSONException {
		JSONArray items = JSONHelper.getJSONArrayOrEmtpy(json, "items");
		for (int i = 0; i < items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			parseAppVersion(appId, item);
		}
	}

	private Date getTimeOrThrow(JSONObject json, String key)
			throws JSONException {
		String time = JSONHelper.getStrinOrNull(json, key);
		try {
			return mDateFormat.parse(time);
		} catch (ParseException e) {
			throw new JSONException("Could not parse time field \"" + key
					+ "\" because: " + e.getMessage());
		}
	}

	private void parseAppVersion(String appId, JSONObject json)
			throws JSONException {
		String guid = JSONHelper.getStrinOrThrow(json, "guid");
		String version = JSONHelper.getStrinOrThrow(json, "version");
		String downloadUrl = JSONHelper.getStrinOrThrow(json,
				"app_download_url");
		Date createdAt = getTimeOrThrow(json, "created_at");
		Date updatedAt = getTimeOrThrow(json, "updated_at");
		mParserResult.addAppVersion(appId, guid, version, downloadUrl,
				createdAt, updatedAt);

	}

}
