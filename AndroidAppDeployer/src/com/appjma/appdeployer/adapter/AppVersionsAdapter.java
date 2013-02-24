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
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.appjma.appdeployer.R;
import com.appjma.appdeployer.adapter.DownloadLoader.DownloadItem;
import com.appjma.appdeployer.content.AppContract;
import com.google.common.collect.Maps;

public class AppVersionsAdapter extends CursorAdapter implements
		OnClickListener {

	public static final String[] PROJECTION = new String[] {
			AppContract.AppVersions._ID, AppContract.AppVersions.VERSION,
			AppContract.AppVersions.APP_VERSION_ID,
			AppContract.AppVersions.UPDATED_AT,
			AppContract.AppVersions.DOWNLOAD_MANAGER_ID,
			AppContract.AppVersions.DOWNLOAD_URL};
	private static final int PROJECTION_VERSION = 1;
	public static final int PROJECTION_APP_VERSION_ID = 2;
	public static final int PROJECTION_UPDATED_AT = 3;
	public static final int PROJECTION_DOWNLOAD_MANAGER_ID = 4;
	public static final int PROJECTION_DOWNLOAD_URL = 5;

	private LayoutInflater mInflater;
	private final OnAppVersionDownloadClickListener mListener;
	private long mNow;
	private String mVersionFormat;
	private Map<String, DownloadItem> mMap = Maps.newHashMap();

	public static interface OnAppVersionDownloadClickListener {

		void onDownloadClick(View v, long appVersionId, int position);

	}

	public AppVersionsAdapter(Context context,
			OnAppVersionDownloadClickListener listener) {
		super(context, null, 0);
		mVersionFormat = context.getString(R.string.app_version_format);
		mListener = listener;
		mInflater = LayoutInflater.from(context);
		mNow = System.currentTimeMillis();
	}

	private static class ViewHolder {
		protected TextView mText1;
		public TextView mText2;
		public long mId;
		public View mButton;
		public int mPosition;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		String version = cursor.getString(PROJECTION_VERSION);
		long updatedAt = cursor.getLong(PROJECTION_UPDATED_AT);
		long id = cursor.getLong(PROJECTION_APP_VERSION_ID);
		String downloadManagerId = cursor
				.getString(PROJECTION_DOWNLOAD_MANAGER_ID);

		int status = -1;
		if (downloadManagerId != null) {
			DownloadItem downloadItem = mMap.get(downloadManagerId);
			if (downloadItem != null) {
				status = downloadItem.mStatus;
			}
		}
		if (status == DownloadManager.STATUS_PENDING
				|| status == DownloadManager.STATUS_RUNNING) {
			holder.mButton
					.setBackgroundResource(R.drawable.ic_list_item_downloading);
		} else if (status == DownloadManager.STATUS_SUCCESSFUL) {
			holder.mButton
					.setBackgroundResource(R.drawable.ic_list_item_downloaded);
		} else {
			holder.mButton
					.setBackgroundResource(R.drawable.ic_list_item_download);
		}
		holder.mPosition = cursor.getPosition();
		holder.mText1.setText(String.format(mVersionFormat, version));
		CharSequence updatedAtText = DateUtils.getRelativeTimeSpanString(
				updatedAt, mNow, DateUtils.MINUTE_IN_MILLIS);
		holder.mText2.setText(updatedAtText);
		holder.mId = id;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = mInflater.inflate(R.layout.app_item, parent, false);
		ViewHolder holder = new ViewHolder();
		view.setTag(holder);
		holder.mText1 = (TextView) view.findViewById(android.R.id.text1);
		holder.mText2 = (TextView) view.findViewById(android.R.id.text2);
		View button = (View) view.findViewById(android.R.id.button1);
		button.setOnClickListener(this);
		button.setTag(holder);
		holder.mButton = button;
		return view;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case android.R.id.button1:
			ViewHolder holder = (ViewHolder) v.getTag();
			mListener.onDownloadClick(v, holder.mId, holder.mPosition);
			break;
		default:
			throw new RuntimeException("Unknown button id: " + v.getId());
		}
	}

	public void setDownloadsMap(Map<String, DownloadItem> map) {
		this.mMap = map;
		notifyDataSetChanged();
	}

}
