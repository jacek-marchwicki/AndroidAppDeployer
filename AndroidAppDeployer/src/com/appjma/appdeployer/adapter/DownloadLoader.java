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
package com.appjma.appdeployer.adapter;

import java.util.Map;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;

import com.google.common.collect.Maps;

public class DownloadLoader extends
		AsyncTaskLoader<Map<String, DownloadLoader.DownloadItem>> {

	public static class DownloadItem {

		public final String mId;
		public final int mStatus;

		public DownloadItem(String id, int status) {
			mId = id;
			mStatus = status;
		}
	}

	private DownloadManager mDownloadManager;

	public DownloadLoader(Context context) {
		super(context);
		mDownloadManager = (DownloadManager) context
				.getSystemService(Context.DOWNLOAD_SERVICE);
	}

	@Override
	public Map<String, DownloadLoader.DownloadItem> loadInBackground() {
		Map<String, DownloadLoader.DownloadItem> map = Maps.newHashMap();
		Cursor cursor = mDownloadManager.query(new DownloadManager.Query());
		int columnId = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
		int columnStatus = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
		try {
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor
					.moveToNext()) {
				String id = cursor.getString(columnId);
				int status = cursor.getInt(columnStatus);
				map.put(id, new DownloadItem(id, status));
			}
		} finally {
			cursor.close();
		}
		return map;
	}

}
