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
package com.appjma.appdeployer.content;

import com.appjma.appdeployer.AppPreferences;
import com.appjma.appdeployer.content.AppContract.AppVersions;
import com.appjma.appdeployer.content.AppContract.Apps;
import com.appjma.appdeployer.content.AppContract.Share;
import com.appunite.contentprovider.ContractDesc;
import com.appunite.contentprovider.ContractDesc.FieldType;
import com.appunite.contentprovider.ContractFullDesc;
import com.appunite.contentprovider.ContractFullDesc.SelectionVars;
import com.appunite.contentprovider.OnInsertTrigger;
import com.appunite.contentprovider.OnUpdateTrigger;
import com.appunite.contentprovider.QueryInterface;
import com.appunite.syncer.DownloadSharedPreference;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class DBHelper extends SQLiteOpenHelper {

	private static final String DB_NAME = "app.db";
	private static final int DB_VERSION = 1;

	private static class AutoCratedAt implements OnInsertTrigger,
			OnUpdateTrigger {

		private final String mCreatedAtField;
		private final String mUpdatedAtField;

		public AutoCratedAt(String createdAtField, String updatedAtField) {
			mCreatedAtField = createdAtField;
			mUpdatedAtField = updatedAtField;
		}

		@Override
		public Uri onInsert(QueryInterface queryInterface, Uri uri,
				SelectionVars table, ContentValues newValues) {
			long now = System.currentTimeMillis();
			if (!newValues.containsKey(mCreatedAtField)) {
				newValues.put(mCreatedAtField, now);
			}
			if (!newValues.containsKey(mUpdatedAtField)) {
				newValues.put(mUpdatedAtField, now);
			}
			return null;
		}

		@Override
		public void onUpdate(QueryInterface queryInterface, Uri uri,
				SelectionVars selectionVars, ContentValues values,
				String selection, String[] selectionArgs) {
			if (!values.containsKey(mUpdatedAtField)) {
				long now = System.currentTimeMillis();
				values.put(mUpdatedAtField, now);
			}
		}

	}

	static final AutoCratedAt DESC_APPS_TRIGGER = new AutoCratedAt(
			Apps.CREATED_AT, Apps.UPDATED_AT);

	private static final String DESC_APPS_NEWEST_VERSION_SELECT = "(SELECT "
			+ AppVersions.VERSION + " FROM " + AppVersions.DB_TABLE + " WHERE "
			+ AppVersions.APP_ID + " == " + Apps.APP_ID + " ORDER BY "
			+ AppVersions.UPDATED_AT + " ASC" + " LIMIT 1)";
	
	private static final String DESC_APPS_NEWEST_DOWNLOAD_URL_SELECT = "(SELECT "
			+ AppVersions.DOWNLOAD_URL + " FROM " + AppVersions.DB_TABLE + " WHERE "
			+ AppVersions.APP_ID + " == " + Apps.APP_ID + " ORDER BY "
			+ AppVersions.UPDATED_AT + " ASC" + " LIMIT 1)";

	static final ContractDesc DESC_APPS_DB_SQL = new ContractDesc.Builder(
			Apps.DB_TABLE, Apps.APP_ID, Apps.CONTENT_DIR_TYPE,
			Apps.CONTENT_ITEM_TYPE).setGuidField(Apps.GUID)
			.addTableField(Apps.NAME, FieldType.TEXT)
			.addTableField(Apps.SYNC_TOKEN, FieldType.INTEGER)
			.addTableField(Apps.TOKEN, FieldType.TEXT)
			.addTableField(Apps.CREATED_AT, FieldType.INTEGER)
			.addTableField(Apps.UPDATED_AT, FieldType.INTEGER)
			.addTableField(Apps.IMAGE, FieldType.TEXT)
			.addTableField(Apps.SHARE_LINK, FieldType.TEXT)
			.addTableField(Apps.SYNCED, FieldType.INTEGER)
			.addTableField(Apps.DELETED, FieldType.INTEGER)
			.addFakeField(Apps.NEWEST_VERSION, DESC_APPS_NEWEST_VERSION_SELECT)
			.addFakeField(Apps.NEWEST_DOWNLOAD_URL, DESC_APPS_NEWEST_DOWNLOAD_URL_SELECT)
			.addOnInsertTrigger(DESC_APPS_TRIGGER)
			.addOnUpdateTrigger(DESC_APPS_TRIGGER).build();

	static final AutoCratedAt DESC_APP_VERSIONS_TRIGGER = new AutoCratedAt(
			AppVersions.CREATED_AT, AppVersions.UPDATED_AT);

	static final ContractDesc DESC_APP_VERSIONS_DB_SQL = new ContractDesc.Builder(
			AppVersions.DB_TABLE, AppVersions.APP_VERSION_ID,
			Apps.CONTENT_DIR_TYPE, AppVersions.CONTENT_ITEM_TYPE)
			.setGuidField(AppVersions.GUID)
			.addTableField(AppVersions.APP_ID, FieldType.INTEGER)
			.addTableField(AppVersions.VERSION, FieldType.TEXT)
			.addTableField(AppVersions.SYNC_TOKEN, FieldType.INTEGER)
			.addTableField(AppVersions.CREATED_AT, FieldType.INTEGER)
			.addTableField(AppVersions.UPDATED_AT, FieldType.INTEGER)
			.addTableField(AppVersions.DOWNLOAD_URL, FieldType.TEXT)
			.addTableField(AppVersions.SYNCED, FieldType.INTEGER)
			.addTableField(AppVersions.DOWNLOAD_MANAGER_ID, FieldType.TEXT)
			.addOnInsertTrigger(DESC_APP_VERSIONS_TRIGGER)
			.addOnUpdateTrigger(DESC_APP_VERSIONS_TRIGGER).build();

	static final AutoCratedAt DESC_SHARES_TRIGGER = new AutoCratedAt(
			Share.CREATED_AT, Share.UPDATED_AT);

	static final ContractDesc DESC_SHARES_DB_SQL = new ContractDesc.Builder(
			Share.DB_TABLE, Share.SHARE_ID, Share.CONTENT_DIR_TYPE,
			AppVersions.CONTENT_ITEM_TYPE).setGuidField(Share.GUID)
			.addTableField(Share.APP_ID, FieldType.INTEGER)
			.addTableField(Share.NAME, FieldType.TEXT)
			.addTableField(Share.SYNCED, FieldType.INTEGER)
			.addOnInsertTrigger(DESC_SHARES_TRIGGER)
			.addOnUpdateTrigger(DESC_SHARES_TRIGGER).build();

	static final ContractFullDesc FULL_DESC = new ContractFullDesc.Builder(
			AppContract.AUTHORITY)
			.addTable(DESC_APPS_DB_SQL)
			.addTable(DESC_APP_VERSIONS_DB_SQL)
			.addTable(DESC_SHARES_DB_SQL)
			.addConnection1n(DESC_APPS_DB_SQL, Apps.APP_ID,
					DESC_APP_VERSIONS_DB_SQL, AppVersions.APP_ID)
			.addConnection1n(DESC_APPS_DB_SQL, Apps.APP_ID, DESC_SHARES_DB_SQL,
					Share.APP_ID).build();

	private Context mContext;

	public DBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		FULL_DESC.sqlCreateAll(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Clearing sync status
		DownloadSharedPreference preferences = new DownloadSharedPreference(
				mContext);
		preferences.clear();

		// Clearing user preferences
		new AppPreferences(mContext).edit().clear().commit();

		// Removing all tables
		FULL_DESC.sqlDropAll(db);

		// Recreating databases
		onCreate(db);
	}

}
