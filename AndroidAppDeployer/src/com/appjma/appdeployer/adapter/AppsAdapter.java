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

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.appjma.appdeployer.R;
import com.appjma.appdeployer.content.AppContract;
import com.appunite.imageloader.RemoteImageLoader;
import com.appunite.imageloader.RemoteImageLoader.ImageViewHolder;

public class AppsAdapter extends CursorAdapter implements OnClickListener {

	public static final String[] PROJECTION = new String[] {
			AppContract.Apps._ID, AppContract.Apps.NAME,
			AppContract.Apps.APP_ID, AppContract.Apps.IMAGE,
			AppContract.Apps.NEWEST_DOWNLOAD_URL};
	private static final int PROJECTION_NAME = 1;
	public static final int PROJECTION_APP_ID = 2;
	private static final int PROJECTION_IMAGE = 3;
	private static final int PROJECTION_DOWNLOAD_URL = 4;

	public static interface OnAppDownloadClickListener {

		void onDownloadClicked(View v, long appId);

	}

	private LayoutInflater mInflater;
	private final OnAppDownloadClickListener mListener;
	private final RemoteImageLoader mRemoteImageLoader;

	public AppsAdapter(Context context, RemoteImageLoader imageLoader,
			OnAppDownloadClickListener listener) {
		super(context, null, 0);
		mRemoteImageLoader = imageLoader;
		mListener = listener;
		mInflater = LayoutInflater.from(context);
	}

	private static class ViewHolder {
		protected TextView mText1;
		public ImageViewHolder mImageHolder;
		public long mId;
		public View mButton;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		String name = cursor.getString(PROJECTION_NAME);
		String url = cursor.getString(PROJECTION_IMAGE);
		long id = cursor.getLong(PROJECTION_APP_ID);
		String downloadUrl = cursor.getString(PROJECTION_DOWNLOAD_URL);
		holder.mId = id;
		holder.mText1.setText(name);
		holder.mButton.setVisibility(downloadUrl == null ? View.INVISIBLE : View.VISIBLE);
		mRemoteImageLoader.loadImage(holder.mImageHolder, url);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = mInflater.inflate(R.layout.apps_item, parent, false);
		ViewHolder holder = new ViewHolder();
		view.setTag(holder);
		holder.mText1 = (TextView) view.findViewById(android.R.id.text1);
		ImageView image = (ImageView) view.findViewById(android.R.id.icon);
		holder.mImageHolder = new RemoteImageLoader.ImageViewHolder(image);
		View button = view.findViewById(android.R.id.button1);
		holder.mButton = button;
		button.setTag(holder);
		button.setOnClickListener(this);
		return view;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case android.R.id.button1:
			ViewHolder holder = (ViewHolder) v.getTag();
			mListener.onDownloadClicked(v, holder.mId);
			return;

		default:
			throw new RuntimeException("Unknown button id: " + v.getId());
		}
	}

}
