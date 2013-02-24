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

import java.util.ArrayList;
import java.util.Date;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;

import com.appjma.appdeployer.AppPreferences;
import com.appjma.appdeployer.AppPreferences.AppEdit;
import com.appjma.appdeployer.content.AppContract;

public class ProviderParserResult implements ParserResult {

	private ContentResolver mCr;
	private AppPreferences mAppPreferences;
	private ArrayList<ContentProviderOperation> mOps;
	private AppEdit mEditor;
	private long mSyncToken;

	public ProviderParserResult(Context context) {
		mCr = context.getContentResolver();
		mAppPreferences = new AppPreferences(context);
		clear();
	}

	@Override
	public void apply() {
		try {
			mCr.applyBatch(AppContract.AUTHORITY, mOps);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		} catch (OperationApplicationException e) {
			throw new RuntimeException(e);
		}
		mEditor.commit();
	}

	@Override
	public void clear() {
		mOps = new ArrayList<ContentProviderOperation>();
		mEditor = mAppPreferences.edit();
		mSyncToken = System.currentTimeMillis();
	}

	@Override
	public void setAppsNextToken(String nextToken) {
		mEditor.setAppsNextToken(nextToken);
	}

	@Override
	public void clearOldApps() {
		String selection = AppContract.Apps.GUID + " IS NOT NULL AND "
				+ AppContract.Apps.SYNC_TOKEN + " != ? AND "
				+ AppContract.Apps.SYNCED + " != 0";
		String[] selectionArgs = new String[] { String.valueOf(mSyncToken) };
		mOps.add(ContentProviderOperation
				.newDelete(AppContract.Apps.CONTENT_URI)
				.withSelection(selection, selectionArgs).build());
	}

	@Override
	public void clearOldAppVersions(String appId) {
		String selection = AppContract.AppVersions.GUID + " IS NOT NULL AND "
				+ AppContract.AppVersions.SYNC_TOKEN + " != ? AND "
				+ AppContract.AppVersions.SYNCED + " != 0 AND "
				+ AppContract.AppVersions.APP_ID + " = ?";
		String[] selectionArgs = new String[] { String.valueOf(mSyncToken),
				appId };
		mOps.add(ContentProviderOperation
				.newDelete(AppContract.AppVersions.CONTENT_URI)
				.withSelection(selection, selectionArgs).build());
	}
	
	private ContentProviderOperation.Builder insertOrUpdate(Uri uri, String id) {
		if (id == null) {
			return ContentProviderOperation.newInsert(uri);
		} else {
			return ContentProviderOperation.newUpdate(Uri.withAppendedPath(uri,
					id));
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void addApp(String id, String guid, String name, String token,
			Date createdAt, Date updatedAt) {
		mOps.add(insertOrUpdate(AppContract.Apps.CONTENT_URI, id)
				.withValue(AppContract.Apps.GUID, guid)
				.withValue(AppContract.Apps.NAME, name)
				.withValue(AppContract.Apps.TOKEN, token)
				.withValue(AppContract.Apps.SYNC_TOKEN, mSyncToken)
				.withValue(AppContract.Apps.CREATED_AT, createdAt.getDate())
				.withValue(AppContract.Apps.UPDATED_AT, updatedAt.getDate())
				.withValue(AppContract.Apps.DELETED, false)
				.withValue(AppContract.Apps.SYNCED, true).build());
	}

	@SuppressWarnings("deprecation")
	@Override
	public void addAppVersion(String appId, String guid, String version,
			String downloadUrl, Date createdAt, Date updatedAt) {
		mOps.add(ContentProviderOperation
				.newInsert(AppContract.AppVersions.CONTENT_URI)
				.withValue(AppContract.AppVersions.APP_ID, appId)
				.withValue(AppContract.AppVersions.GUID, guid)
				.withValue(AppContract.AppVersions.VERSION, version)
				.withValue(AppContract.AppVersions.DOWNLOAD_URL, downloadUrl)
				.withValue(AppContract.AppVersions.SYNC_TOKEN, mSyncToken)
				.withValue(AppContract.AppVersions.CREATED_AT,
						createdAt.getDate())
				.withValue(AppContract.AppVersions.UPDATED_AT,
						updatedAt.getDate())
				.withValue(AppContract.AppVersions.SYNCED, true).build());
	}

	@Override
	public void deleteApp(String id) {
		Uri appUri = Uri.withAppendedPath(AppContract.Apps.CONTENT_URI, id);
		mOps.add(ContentProviderOperation.newDelete(appUri).build());
		Uri appVersionsUri = Uri.withAppendedPath(appUri,
				AppContract.AppVersions.CONTENT_PATH);
		mOps.add(ContentProviderOperation.newDelete(appVersionsUri).build());
	}

}
