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

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import com.appjma.appdeployer.AppPreferences;
import com.appjma.appdeployer.content.AppContract;
import com.appunite.auhttp.HTTPUtils;
import com.appunite.auhttp.HTTPUtils.GetBuilder;
import com.appunite.auhttp.HTTPUtils.WrongHttpResponseCode;

import static com.google.common.base.Preconditions.*;

public class Downloader {

	public static class TooManyRecords extends Exception {
		private static final long serialVersionUID = 1L;

		public TooManyRecords() {
			super("Too many records while quering");
		}
	}

	public static class UnauthorizedResponseCode extends Exception {
		private int mStatusCode;
		private final String mToken;

		public UnauthorizedResponseCode(HttpResponse response, String token) {
			super(String.format("Unauthorized response from server: %d",
					response.getStatusLine().getStatusCode()));
			mToken = token;
			mStatusCode = response.getStatusLine().getStatusCode();
		}
		
		public String getToken() {
			return mToken;
		}

		public int getStatusCode() {
			return mStatusCode;
		}

		private static final long serialVersionUID = 1L;

	}

	private static final String BASE_API_URL = "https://app-deployer.appspot.com/_ah/api/appdeployer/v1";
	// private static final String BASE_API_URL =
	// "http://192.168.111.149:8888/_ah/api/appdeployer/v1";
	private static final int MAX_LAST_TOTKEN_REPETANCE = 10;
	public static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.email";

	private ParserResult mParserResult;
	private Parser mParser;
	private HttpClient mHttpClient;
	private ContentResolver mCr;
	private AppPreferences mAppPreferences;
	private Context mContext;

	public Downloader(Context context) {
		mParserResult = new ProviderParserResult(context);
		mParser = new Parser(mParserResult);
		mCr = context.getContentResolver();
		mAppPreferences = new AppPreferences(context);
		mContext = context;
	}

	private HttpClient getHttpClient() {
		if (mHttpClient != null) {
			return mHttpClient;
		}
		mHttpClient = new DefaultHttpClient();
		return mHttpClient;
	}

	public void syncApps(String token, Uri uri) throws ClientProtocolException,
			IOException, JSONException, TooManyRecords, WrongHttpResponseCode,
			UnauthorizedResponseCode {
		String selection = AppContract.Apps.SYNCED + " = 0";
		String[] PROJECTION = new String[] { AppContract.Apps.APP_ID,
				AppContract.Apps.GUID, AppContract.Apps.NAME, AppContract.Apps.DELETED};
		Cursor cursor = mCr.query(uri, PROJECTION,
				selection, null, null);
		try {
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor
					.moveToNext()) {
				String id = cursor.getString(0);
				String guid = cursor.getString(1);
				String name = cursor.getString(2);
				boolean deleted = cursor.getInt(3) == 0 ? false : true;
				if (deleted == true) {
					checkState(guid != null,
							"Row could not be marked as deleted if it is not synced");
					deleteApp(token, id, guid);
				} else if (guid == null) {
					insertApp(token, id, name);
				} else {
					updateApp(token, id, guid, name);
				}
			}
		} finally {
			cursor.close();
		}
	}

	private void deleteApp(String token, String id, String guid)
			throws ClientProtocolException, IOException,
			UnauthorizedResponseCode, WrongHttpResponseCode {
		String url = new GetBuilder(BASE_API_URL).appendPathSegment("apps")
				.appendPathSegment(guid).build();
		HttpDelete request = new HttpDelete(url);
		setupDefaultHeaders(request, token);
		HttpResponse response = getHttpClient().execute(request);
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
			throw new UnauthorizedResponseCode(response, token);
		}
		if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_NOT_FOUND) {
			// if not (deleted or already deleted)
			throw new WrongHttpResponseCode(response);
		}
		try {
			mParserResult.deleteApp(id);
			mParserResult.apply();
		} finally {
			mParserResult.clear();
		}
	}

	private void updateApp(String token, String id, String guid, String name)
			throws ClientProtocolException, IOException,
			UnauthorizedResponseCode, WrongHttpResponseCode, JSONException {
		String url = new GetBuilder(BASE_API_URL).appendPathSegment("apps")
				.appendPathSegment(guid).build();
		HttpPut request = new HttpPut(url);
		try {
			JSONObject obj = new JSONObject();
			obj.put("name", name);
			request.setEntity(getJsonEntity(obj));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		setupDefaultHeaders(request, token);
		HttpResponse response = getHttpClient().execute(request);
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
			throw new UnauthorizedResponseCode(response, token);
		}
		if (statusCode != HttpStatus.SC_OK) {
			throw new WrongHttpResponseCode(response);
		}
		JSONObject json = HTTPUtils.getJsonFromResponse(response);
		try {
			mParser.parseApp(id, json);
			mParserResult.apply();
		} finally {
			mParserResult.clear();
		}
	}

	private void insertApp(String token, String id, String name)
			throws WrongHttpResponseCode, UnauthorizedResponseCode,
			ClientProtocolException, IOException, JSONException {
		String url = new GetBuilder(BASE_API_URL).appendPathSegment("apps")
				.build();
		HttpPost request = new HttpPost(url);
		try {
			JSONObject obj = new JSONObject();
			obj.put("name", name);
			request.setEntity(getJsonEntity(obj));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		setupDefaultHeaders(request, token);
		HttpResponse response = getHttpClient().execute(request);
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
			throw new UnauthorizedResponseCode(response, token);
		}
		if (statusCode != HttpStatus.SC_OK) {
			throw new WrongHttpResponseCode(response);
		}
		JSONObject json = HTTPUtils.getJsonFromResponse(response);
		try {
			mParser.parseApp(id, json);
			mParserResult.apply();
		} finally {
			mParserResult.clear();
		}
	}

	public void downloadApps(String token) throws ClientProtocolException,
			IOException, JSONException, TooManyRecords, WrongHttpResponseCode,
			UnauthorizedResponseCode {
		String nextToken = null;
		int repetance = 0;
		while (true) {
			String url = new GetBuilder(BASE_API_URL)
					.appendPathSegment("apps")
					.addParam("limit", "10")
					.addParamIf(nextToken != null, "next_token", nextToken)
					.addParam("fields",
							"items(created_at,guid,name,token,updated_at),next_token")
					.build();
			HttpGet request = new HttpGet(url);
			setupDefaultHeaders(request, token);
			HttpResponse response = getHttpClient().execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
				throw new UnauthorizedResponseCode(response, token);
			}
			if (statusCode != HttpStatus.SC_OK) {
				throw new WrongHttpResponseCode(response);
			}
			JSONObject json = HTTPUtils.getJsonFromResponse(response);
			try {
				mParser.parseApps(json);
				if (nextToken == null) {
					mParserResult.clearOldApps();
				}
				mParserResult.apply();
			} finally {
				mParserResult.clear();
			}
			nextToken = mAppPreferences.getAppsNextToken();
			if (nextToken == null) {
				return;
			}
			if (repetance++ > MAX_LAST_TOTKEN_REPETANCE) {
				throw new TooManyRecords();
			}
		}
	}

	private void setupDefaultHeaders(HttpRequestBase request, String token) {
		HTTPUtils.setupDefaultHeaders(request);
		request.setHeader("Accept", "application/json");
		if (token != null) {
			request.addHeader("Authorization", "Bearer " + token);
		}
	}

	public void downloadApp(Uri uri, String token)
			throws ClientProtocolException, IOException, JSONException {
		String guid = getAppGuid(uri);
		if (guid == null) {
			// We do no have guid for this element - its mean that is not
			// yet synced to server - this mean that we can not download it
			return;
		}
		String url = new GetBuilder(BASE_API_URL)
				.appendPathSegment("apps")
				.appendPathSegment(guid)
				.addParam("fields",
						"items(created_at,guid,name,token,updated_at),next_token")
				.build();
		HttpGet request = new HttpGet(url);
		setupDefaultHeaders(request, token);
		HttpResponse response = getHttpClient().execute(request);
		JSONObject json = HTTPUtils.getJsonFromResponse(response);
		try {
			mParser.parseApp(null, json);
			mParserResult.apply();
		} finally {
			mParserResult.clear();
		}
	}

	private String getAppGuid(Uri uri) {
		String guid;
		Cursor cursor = mCr.query(uri, new String[] { AppContract.Apps.GUID },
				null, null, null);
		try {
			if (!cursor.moveToFirst()) {
				return null;
			}
			guid = cursor.getString(0);
		} finally {
			cursor.close();
		}
		return guid;
	}

	public void downloadAppVersions(String appId, String token)
			throws ClientProtocolException, IOException, JSONException {
		String appGuid = getAppGuid(Uri.withAppendedPath(
				AppContract.Apps.CONTENT_URI, appId));
		if (appGuid == null) {
			// We do no have appGuid for this element - its mean that is not
			// yet synced to server - this mean that we can not download it
			return;
		}
		String url = new GetBuilder(BASE_API_URL).appendPathSegment("apps")
				.appendPathSegment(appGuid).appendPathSegment("versions")
				.build();
		HttpGet request = new HttpGet(url);
		setupDefaultHeaders(request, token);
		HttpResponse response = getHttpClient().execute(request);
		JSONObject json = HTTPUtils.getJsonFromResponse(response);
		try {
			mParser.parseAppVersions(appId, json);
			mParserResult.clearOldAppVersions(appId);
			mParserResult.apply();
		} finally {
			mParserResult.clear();
		}
	}

	private HttpEntity getJsonEntity(JSONObject object) {
		StringEntity entity;
		try {
			entity = new StringEntity(object.toString(), HTTP.UTF_8);
			entity.setContentType("application/json; charset=utf-8");
			return entity;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException();
		}
	}

	public void downloadFile(String appVersion, String token) {
		String url;
		String name;
		String version;
		Cursor cursor = mCr.query(
				AppContract.AppVersions.CONTENT_URI.buildUpon()
						.appendPath(appVersion)
						.appendQueryParameter("limit", "1").build(),
				new String[] { AppContract.AppVersions.VERSION,
						AppContract.Apps.NAME,
						AppContract.AppVersions.DOWNLOAD_URL }, null, null,
				null);
		try {
			if (!cursor.moveToFirst()) {
				return;
			}
			version = cursor.getString(0);
			name = cursor.getString(1);
			url = cursor.getString(2);

		} finally {
			cursor.close();
		}
		if (url == null) {
			return;
		}
		Uri uri = Uri.parse(url);
		DownloadManager downloadManager = (DownloadManager) mContext
				.getSystemService(Context.DOWNLOAD_SERVICE);
		String title = String.format("%s (%s)", name, version);
		Request request = new DownloadManager.Request(uri)
				.addRequestHeader("Authorization", "Bearer " + token)
				.setTitle(title).setVisibleInDownloadsUi(true)
				.setMimeType("application/vnd.android.package-archive");
		setRequestNotificationStatus(request);
		downloadManager.enqueue(request);
	}

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setRequestNotificationStatus(Request request) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
			request.setNotificationVisibility(
					DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		} else {
			request.setShowRunningNotification(true);
		}
	}

	@SuppressWarnings("unused")
	private String getOneFieldOrNull(Uri uri, String id, String field) {
		Cursor cursor = mCr.query(uri.buildUpon().appendPath(id)
				.appendQueryParameter("limit", "1").build(),
				new String[] { field }, null, null, null);
		try {
			if (!cursor.moveToFirst()) {
				return null;
			}
			return cursor.getString(0);
		} finally {
			cursor.close();
		}
	}

}
