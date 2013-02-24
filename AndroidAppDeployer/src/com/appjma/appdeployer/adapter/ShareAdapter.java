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
import android.widget.TextView;

import com.appjma.appdeployer.R;
import com.appjma.appdeployer.content.AppContract;

public class ShareAdapter extends CursorAdapter implements OnClickListener {

	public static final String[] PROJECTION = new String[] {
			AppContract.Share._ID, AppContract.Share.NAME,
			AppContract.Share.SHARE_ID };
	private static final int PROJECTION_NAME = 1;
	public static final int PROJECTION_SHARE_ID = 2;
	private final LayoutInflater mInflater;
	private final OnShareDeleteListener mListener;

	public static interface OnShareDeleteListener {
		void onDeleteClicked(View v, long mId);
	}

	private static class ViewHolder {

		public TextView mText1;
		public long mId;

	}

	public ShareAdapter(Context context,
			OnShareDeleteListener listener) {
		super(context, null, 0);
		mListener = listener;
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		String name = cursor.getString(PROJECTION_NAME);
		long id = cursor.getLong(PROJECTION_SHARE_ID);

		holder.mText1.setText(name);
		holder.mId = id;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = mInflater.inflate(R.layout.app_edit_share_item, parent,
				false);
		ViewHolder holder = new ViewHolder();
		view.setTag(holder);
		holder.mText1 = (TextView) view.findViewById(android.R.id.text1);
		View button = (View) view.findViewById(android.R.id.button1);
		button.setOnClickListener(this);
		button.setTag(holder);
		return view;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case android.R.id.button1:
			ViewHolder holder = (ViewHolder) v.getTag();
			mListener.onDeleteClicked(v, holder.mId);
			break;
		default:
			throw new RuntimeException("Unknown button id: " + v.getId());
		}
	}

}
