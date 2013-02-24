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
package com.appjma.appdeployer.helper;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.appjma.appdeployer.AppPreferences;
import com.appjma.appdeployer.service.DownloadService;
import com.appjma.appdeployer.service.Downloader;
import com.appunite.syncer.DownloadHelper;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

public class AuthAsyncTask extends AsyncTask<Void, Void, Intent> {

	private static final String TAG = "AuthAsyncTask";
	private final Activity mContext;
	private String mAccountName;
	private final Uri mUri;

	public AuthAsyncTask(Activity activity, Uri uri) {
		mContext = activity;
		mUri = uri;
		mAccountName = new AppPreferences(activity).getAccountName();
	}

	@Override
	protected Intent doInBackground(Void... params) {
		try {
			GoogleAuthUtil.getToken(mContext, mAccountName,
					Downloader.SCOPE);
			return null;
		} catch (UserRecoverableAuthException e) {
			return e.getIntent();
		} catch (IOException e) {
			Log.w(TAG, "GoogleAuthException", e);
			return null;
		} catch (GoogleAuthException e) {
			Log.w(TAG, "GoogleAuthException", e);
			return null;
		}
	}

	@Override
	protected void onPostExecute(Intent result) {
		if (result == null) {
			DownloadHelper.startAsyncDownload(mContext,
					DownloadService.ACTION_SYNC, mUri, null, true);
		} else {
			mContext.startActivityForResult(result, 100);
		}
	}

}