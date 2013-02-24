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

import com.appunite.contentprovider.DataHelper;

import android.net.Uri;
import android.provider.BaseColumns;

public class AppContract {

	public static final String AUTHORITY = "com.appjma.appdeployer";
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

	public static class Apps implements BaseColumns {
		public static final String CONTENT_PATH = "apps";
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, CONTENT_PATH);

		public static final String CONTENT_ITEM_TYPE = DataHelper
				.contentItemType(AUTHORITY, CONTENT_PATH);
		public static final String CONTENT_DIR_TYPE = DataHelper.contentType(
				AUTHORITY, CONTENT_PATH);
		static final String DB_TABLE = CONTENT_PATH;

		public static final String APP_ID = DataHelper.field(DB_TABLE, _ID);
		public static final String GUID = DataHelper.field(DB_TABLE, "guid");
		public static final String NAME = DataHelper.field(DB_TABLE, "name");
		public static final String SYNC_TOKEN = DataHelper.field(DB_TABLE,
				"sync_token");
		public static final String CREATED_AT = DataHelper.field(DB_TABLE,
				"created_at");
		public static final String UPDATED_AT = DataHelper.field(DB_TABLE,
				"updated_at");
		public static final String TOKEN = DataHelper.field(DB_TABLE, "token");
		public static final String IMAGE = DataHelper.field(DB_TABLE, "image");
		public static final String NEWEST_VERSION = DataHelper.field(DB_TABLE,
				"newest_version");
		public static final String NEWEST_DOWNLOAD_URL = DataHelper.field(DB_TABLE,
				"newest_download_url");
		public static final String SHARE_LINK = DataHelper.field(DB_TABLE,
				"share_link");
		public static final String SYNCED = DataHelper.field(DB_TABLE, "synced");
		public static final String DELETED = DataHelper.field(DB_TABLE, "delete");
	}

	public static class AppVersions implements BaseColumns {
		public static final String CONTENT_PATH = "app_versions";
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, CONTENT_PATH);

		public static final String CONTENT_ITEM_TYPE = DataHelper
				.contentItemType(AUTHORITY, CONTENT_PATH);
		public static final String CONTENT_DIR_TYPE = DataHelper.contentType(
				AUTHORITY, CONTENT_PATH);
		static final String DB_TABLE = CONTENT_PATH;

		public static final String APP_VERSION_ID = DataHelper.field(DB_TABLE, _ID);
		public static final String APP_ID = DataHelper.field(DB_TABLE, "app_id");
		public static final String GUID = DataHelper.field(DB_TABLE, "guid");
		public static final String VERSION = DataHelper.field(DB_TABLE, "version");
		public static final String SYNC_TOKEN = DataHelper.field(DB_TABLE,
				"sync_token");
		public static final String CREATED_AT = DataHelper.field(DB_TABLE,
				"created_at");
		public static final String UPDATED_AT = DataHelper.field(DB_TABLE,
				"updated_at");
		public static final String DOWNLOAD_URL = DataHelper.field(DB_TABLE,
				"download_url");
		public static final String SYNCED = DataHelper
				.field(DB_TABLE, "synced");
		public static final String DOWNLOAD_MANAGER_ID = DataHelper.field(
				DB_TABLE, "download_manager_id");
	}

	public static class Share implements BaseColumns {
		public static final String CONTENT_PATH = "shares";
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, CONTENT_PATH);

		public static final String CONTENT_ITEM_TYPE = DataHelper
				.contentItemType(AUTHORITY, CONTENT_PATH);
		public static final String CONTENT_DIR_TYPE = DataHelper.contentType(
				AUTHORITY, CONTENT_PATH);
		static final String DB_TABLE = CONTENT_PATH;

		public static final String SHARE_ID = DataHelper.field(DB_TABLE, _ID);
		public static final String APP_ID = DataHelper.field(DB_TABLE, "app_id");
		public static final String NAME = DataHelper.field(DB_TABLE, "name");
		public static final String GUID = DataHelper.field(DB_TABLE, "guid");
		public static final String SYNC_TOKEN = DataHelper.field(DB_TABLE,
				"sync_token");
		public static final String CREATED_AT = DataHelper.field(DB_TABLE,
				"created_at");
		public static final String UPDATED_AT = DataHelper.field(DB_TABLE,
				"updated_at");
		public static final String SYNCED = DataHelper.field(DB_TABLE, "synced");
	}
	
	public static class Download {
		public static final String CONTENT_PATH = "download";
	}

}
