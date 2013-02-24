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
package com.appjma.appdeployer;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.appjma.appdeployer.adapter.AppsAdapter;
import com.appjma.appdeployer.adapter.AppsAdapter.OnAppDownloadClickListener;
import com.appjma.appdeployer.content.AppContract;
import com.appjma.appdeployer.helper.ErrorReporter;
import com.appjma.appdeployer.service.DownloadService;
import com.appunite.imageloader.RemoteImageLoader;
import com.appunite.syncer.AUSyncerStatus;
import com.appunite.syncer.DownloadHelper;
import com.appunite.syncer.DownloadHelperStatus;

public class AppsFragment extends SherlockFragment implements
		DownloadHelperStatus, LoaderCallbacks<Cursor>,
		OnItemClickListener, OnAppDownloadClickListener {

	private static final int LOADER_APPS = 0;

	private static final float IMAGE_SIZE_DP = 54;

	private DownloadHelper mDownloadHelper;
	private boolean mProgressIndicator;

	private ListView mListView;
	private View mProgressBar;
	private View mEmptyView;

	private CursorAdapter mAdapter;

	private RemoteImageLoader mRemoteImageLoader;

	private ErrorReporter mErrorReporter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.apps_fragment, container, false);
		
		mListView = (ListView) view.findViewById(android.R.id.list);
		mProgressBar = view.findViewById(android.R.id.progress);
		mEmptyView = view.findViewById(android.R.id.empty);
		mErrorReporter = new ErrorReporter(getActivity(), view,
				AppContract.Apps.CONTENT_URI);

		mListView.setOnItemClickListener(this);
		
		Bitmap placeHolder = BitmapFactory.decodeResource(getResources(),
				R.drawable.placeholder);
		mRemoteImageLoader = RemoteImageLoader.createUsingDp(getActivity(),
				placeHolder, IMAGE_SIZE_DP, IMAGE_SIZE_DP);

		mAdapter = new AppsAdapter(getActivity(), mRemoteImageLoader, this);
		mListView.setAdapter(mAdapter);

		mDownloadHelper = new DownloadHelper(getActivity(),
				DownloadService.ACTION_SYNC, this, AppContract.Apps.CONTENT_URI);
		LoaderManager lm = getLoaderManager();
		lm.initLoader(LOADER_APPS, null, this);

		setHasOptionsMenu(true);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		mDownloadHelper.onActivityResume();
		mDownloadHelper.startDownloading(null, false);
		mRemoteImageLoader.onActivityResume();
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mRemoteImageLoader.onActivityLowMemory();
	}

	@Override
	public void onPause() {
		super.onPause();
		mDownloadHelper.onActivityPause();
		mRemoteImageLoader.onActivityPause();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.apps_fragment, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		MenuItem menuProgress = menu.findItem(R.id.menu_progress);
		MenuItem menuRefresh = menu.findItem(R.id.menu_refresh);
		menuProgress.setVisible(mProgressIndicator);
		menuRefresh.setVisible(!mProgressIndicator);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.menu_refresh:
			mDownloadHelper.startDownloading(null, true);
			return true;
		case R.id.menu_add:
			startActivity(new Intent(Intent.ACTION_INSERT_OR_EDIT,
					AppContract.Apps.CONTENT_URI));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onReportStatus(boolean screenVisible, boolean screenEmpty,
			boolean screenProgress, boolean progressIndicator,
			AUSyncerStatus lastStatus) {
		mEmptyView.setVisibility(screenEmpty ? View.VISIBLE : View.GONE);
		mProgressBar.setVisibility(screenProgress ? View.VISIBLE : View.GONE);

		mErrorReporter.onReportStatus(progressIndicator, progressIndicator,
				progressIndicator, progressIndicator, lastStatus);
		
		if (mProgressIndicator != progressIndicator) {
			this.mProgressIndicator = progressIndicator;
			getActivity().invalidateOptionsMenu();
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case LOADER_APPS:
			return new CursorLoader(getActivity(),
					AppContract.Apps.CONTENT_URI, AppsAdapter.PROJECTION,
					AppContract.Apps.DELETED + " == 0", null,
					AppContract.Apps.UPDATED_AT);
		default:
			throw new RuntimeException("Unknown laoder id: " + id);
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		switch (loader.getId()) {
		case LOADER_APPS:
			mDownloadHelper.updateLocalData(cursor);
			mAdapter.swapCursor(cursor);
			return;
		default:
			throw new RuntimeException("Unknown laoder id: " + loader.getId());
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
		case LOADER_APPS:
			mDownloadHelper.updateLocalData(null);
			mAdapter.swapCursor(null);
			return;
		default:
			throw new RuntimeException("Unknown laoder id: " + loader.getId());
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mErrorReporter.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Cursor c = (Cursor) parent.getItemAtPosition(position);
		String appId = c.getString(AppsAdapter.PROJECTION_APP_ID);
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(
				AppContract.Apps.CONTENT_URI, appId)));
	}

	@Override
	public void onDownloadClicked(View v, long appId) {
		Toast.makeText(getActivity(), "TODO", Toast.LENGTH_SHORT).show();
	}
}
