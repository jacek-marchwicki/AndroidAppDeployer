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
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.appjma.appdeployer.AppPreferences;
import com.appjma.appdeployer.BuildConfig;
import com.appjma.appdeployer.content.AppContract;
import com.appjma.appdeployer.receiver.AuthReceiver;
import com.appjma.appdeployer.service.Downloader.TooManyRecords;
import com.appjma.appdeployer.service.Downloader.UnauthorizedResponseCode;
import com.appunite.auhttp.HTTPUtils.WrongHttpResponseCode;
import com.appunite.syncer.AUSyncerStatus;
import com.appunite.syncer.AbsDownloadService;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.UserRecoverableNotifiedException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class DownloadService extends AbsDownloadService {

	private static class UnsupportedDevice extends Exception {
		private static final long serialVersionUID = 1L;

	}

	public static final String ACTION_SYNC = "com.appjma.appdeployer.ACTION_SYNC";

	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);

	private static final int APPS = 0;
	private static final int APP_ID = 1;
	private static final int APP_VERSIONS = 2;
	private static final int DOWNLOAD = 3;

	private static final String TAG = "DownloadService";

	public static final String STATE_TOKEN_INVALID = "token_invalid";
	public static final String STATE_REAUTHORIZE = "reauthorize";
	public static final String STATE_UNSUPPORTED_DEVICE = "unsupported_device";


	private AppPreferences mAppPreferences;

	static {
		sURIMatcher.addURI(AppContract.AUTHORITY,
				AppContract.Apps.CONTENT_PATH, APPS);
		sURIMatcher.addURI(AppContract.AUTHORITY, AppContract.Apps.CONTENT_PATH
				+ "/#", APP_ID);
		sURIMatcher.addURI(AppContract.AUTHORITY, AppContract.Apps.CONTENT_PATH
				+ "/#/" + AppContract.AppVersions.CONTENT_PATH, APP_VERSIONS);
		sURIMatcher.addURI(AppContract.AUTHORITY,
				AppContract.AppVersions.CONTENT_PATH + "/#/"
						+ AppContract.Download.CONTENT_PATH, DOWNLOAD);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mAppPreferences = new AppPreferences(this);
	}

	@Override
	protected AUSyncerStatus onHandleUri(Uri uri, Bundle bundle,
			boolean withForce) {
		// to enable HTTP logging run:
		// adb shell setprop log.tag.org.apache.http VERBOSE
		// adb shell setprop log.tag.org.apache.http.wire VERBOSE
		// adb shell setprop log.tag.org.apache.http.headers VERBOSE
		// and uncomment
		// java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
		// java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
		// be sure to disable logging before deploing application to user
		// logging a lot can cause security problems and slow down you
		// application
		int match = sURIMatcher.match(uri);
		Downloader downloader = new Downloader(this);
		try {
			switch (match) {
			case APPS: {
				String token = authenticate(uri, bundle);
				downloader.syncApps(token, uri);
				downloader.downloadApps(token);
				return AUSyncerStatus.statusSuccess();
			}
			case APP_ID: {
				String token = authenticate(uri, bundle);
				downloader.syncApps(token, uri);
				downloader.downloadApp(uri, token);
				return AUSyncerStatus.statusSuccess();
			}
			case APP_VERSIONS: {

				List<String> pathSegments = uri.getPathSegments();
				String appId = pathSegments.get(1);
				String token = authenticate(uri, bundle);
				downloader.downloadAppVersions(appId, token);
				return AUSyncerStatus.statusSuccess();
			}
			case DOWNLOAD: {
				List<String> pathSegments = uri.getPathSegments();
				String appVersion = pathSegments.get(1);
				String token = authenticate(uri, bundle);
				downloader.downloadFile(appVersion, token);
				return AUSyncerStatus.statusSuccess();
			}
			default:
				throw new IllegalArgumentException();
			}
		} catch (ClientProtocolException e) {
			logError(uri, e);
			return AUSyncerStatus.statusInternalIssue();
		} catch (IOException e) {
			logError(uri, e);
			return AUSyncerStatus.statusNoInternetConnection();
		} catch (JSONException e) {
			logError(uri, e);
			return AUSyncerStatus.statusInternalIssue();
		} catch (TooManyRecords e) {
			logError(uri, e);
			return AUSyncerStatus.statusInternalIssue();
		} catch (WrongHttpResponseCode e) {
			logError(uri, e);
			return AUSyncerStatus.statusInternalIssue();
		} catch (UserRecoverableAuthException e) {
			logError(uri, e);
			return AUSyncerStatus.statusCustomError(STATE_TOKEN_INVALID);
		} catch (UserRecoverableNotifiedException e) {
			logError(uri, e);
			return AUSyncerStatus.statusCustomError(STATE_TOKEN_INVALID);
		} catch (GoogleAuthException e) {
			logError(uri, e);
			return AUSyncerStatus.statusCustomError(STATE_REAUTHORIZE);
		} catch (UnauthorizedResponseCode e) {
			logError(uri, e);
			GoogleAuthUtil.invalidateToken(getApplicationContext(), e.getToken());
			return AUSyncerStatus.statusCustomError(STATE_TOKEN_INVALID);
		} catch (UnsupportedDevice e) {
			logError(uri, e);
			return AUSyncerStatus.statusCustomError(STATE_UNSUPPORTED_DEVICE);
		}
	}

	private String authenticate(Uri uri, Bundle bundle) throws IOException,
			GoogleAuthException, UnsupportedDevice {
		if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
			throw new UnsupportedDevice();
		}
		String accountName = mAppPreferences.getAccountName();
		if (accountName == null) {
			throw new GoogleAuthException("No authentification");
		}
		Intent callback = new Intent(AuthReceiver.ACTION_AUTHENTICATED)
				.putExtra(AuthReceiver.ACTION_AUTHENTICATED_EXTRA_URI, uri)
				.putExtra(AuthReceiver.ACTION_AUTHENTICATED_EXTRA_BUNDLE,
						bundle);
		return GoogleAuthUtil.getTokenWithNotification(this, accountName,
				Downloader.SCOPE, null, callback);
	}

	private void logError(Uri uri, Exception e) {
		if (!BuildConfig.DEBUG) {
			return;
		}
		Log.v(TAG,
				String.format("Eror occure while downloading: %s",
						uri.toString()), e);
	}

	@Override
	protected boolean forceDownload(Uri uri, long lastSuccessMillis,
			long currentTimeMillis) {
		return currentTimeMillis - lastSuccessMillis > 10000;
	}

	@Override
	protected boolean isNetworkNeeded(Uri uri, Bundle bundle) {
		int match = sURIMatcher.match(uri);
		switch (match) {
		case APPS:
		case APP_ID:
		case APP_VERSIONS:
			return true;
		case DOWNLOAD:
			return false;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	protected long taskWakeLockTimeout(Uri uri, Bundle bundle) {
		int match = sURIMatcher.match(uri);
		switch (match) {
		case APPS:
			return 5* 60 * 1000; // 5 min
		case APP_ID:
			return 5 * 60 * 1000; // 5 min
		case APP_VERSIONS:
			return 1 * 60 * 1000; // 1 min
		case DOWNLOAD:
			return 1 * 1000; // 1s
		default:
			throw new IllegalArgumentException();
		}

	}

}
