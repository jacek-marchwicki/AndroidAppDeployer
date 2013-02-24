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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.appjma.appdeployer.adapter.AppVersionsAdapter;
import com.appjma.appdeployer.adapter.AppVersionsAdapter.OnAppVersionDownloadClickListener;
import com.appjma.appdeployer.adapter.DownloadLoader;
import com.appjma.appdeployer.adapter.DownloadLoader.DownloadItem;
import com.appjma.appdeployer.content.AppContract;
import com.appjma.appdeployer.helper.ErrorReporter;
import com.appjma.appdeployer.service.DownloadService;
import com.appunite.imageloader.RemoteImageLoader;
import com.appunite.imageloader.RemoteImageLoader.ImageViewHolder;
import com.appunite.syncer.AUSyncerStatus;
import com.appunite.syncer.DownloadHelper;
import com.appunite.syncer.DownloadHelperStatus;

public class AppFragment extends SherlockFragment implements
		DownloadHelperStatus, LoaderCallbacks<Cursor>,
		OnAppVersionDownloadClickListener {

	private static final int LOADER_APP = 0;
	private static final int LOADER_APP_VERSIONS = 1;
	private static final int LOADER_DOWNLOADS = 2;

	private static final String[] PROJECTION = new String[] {
			AppContract.Apps.NAME, AppContract.Apps.IMAGE,
			AppContract.Apps.NEWEST_VERSION, AppContract.Apps.TOKEN,
			AppContract.Apps.GUID };

	private static final int PROJECTION_NAME = 0;
	private static final int PROJECTION_IMAGE = 1;
	private static final int PROJECTION_NEWEST_VERSION = 2;
	private static final int PROJECTION_TOKEN = 3;
	private static final int PROJECTION_GUID = 4;

	private static final float IMAGE_SIZE_DP = 120;

	private DownloadHelper mDownloadHelper;
	private boolean mProgressIndicator;

	private ListView mListView;
	private View mProgressBar;
	private View mEmptyView;

	private AppVersionsAdapter mAdapter;
	private Uri mUri;
	private RemoteImageLoader mRemoteImageLoader;
	private ImageView mAppImageView;
	private ImageViewHolder mAppImageViewHolder;
	private TextView mAppNameTextView;
	private TextView mAppVersionTextView;
	private String mTokenFormat;
	private TextView mAppTokenTextView;
	private String mGuidFormat;
	private TextView mAppGuidTextView;
	private LoaderCallbacks<Map<String, DownloadItem>> mLoaderCallback = new LoaderCallbacks<Map<String, DownloadItem>>() {

		@Override
		public Loader<Map<String, DownloadItem>> onCreateLoader(int loaderId,
				Bundle args) {
			switch (loaderId) {
			case LOADER_DOWNLOADS:
				return new DownloadLoader(getActivity());
			default:
				throw new RuntimeException("Unknown loader: " + loaderId);
			}
		}

		@Override
		public void onLoadFinished(Loader<Map<String, DownloadItem>> loader,
				Map<String, DownloadItem> map) {
			switch (loader.getId()) {
			case LOADER_DOWNLOADS:
				mAdapter.setDownloadsMap(map);
				return;
			default:
				throw new RuntimeException("Unknown loader: " + loader.getId());
			}
		}

		@Override
		public void onLoaderReset(Loader<Map<String, DownloadItem>> loader) {
			switch (loader.getId()) {
			case LOADER_DOWNLOADS:
				return;
			default:
				throw new RuntimeException("Unknown loader: " + loader.getId());
			}
		}
	};
	private ErrorReporter mErrorReporter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getActivity().getIntent();
		String action = intent.getAction();
		checkArgument(Intent.ACTION_VIEW.equals(action));

		mUri = intent.getData();
		checkArgument(mUri != null);

		String type = getActivity().getContentResolver().getType(mUri);
		checkArgument(AppContract.Apps.CONTENT_ITEM_TYPE.equals(type));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.app_fragment, container, false);

		mTokenFormat = getString(R.string.app_token_format);
		mGuidFormat = getString(R.string.app_guid_format);

		mListView = (ListView) view.findViewById(android.R.id.list);
		mProgressBar = view.findViewById(android.R.id.progress);
		mEmptyView = view.findViewById(android.R.id.empty);

		mErrorReporter = new ErrorReporter(getActivity(), view,
				AppContract.AppVersions.CONTENT_URI);

		Bitmap placeHolder = BitmapFactory.decodeResource(getResources(),
				R.drawable.placeholder);
		mRemoteImageLoader = RemoteImageLoader.createUsingDp(getActivity(),
				placeHolder, IMAGE_SIZE_DP, IMAGE_SIZE_DP);

		mDownloadHelper = new DownloadHelper(getActivity(),
				DownloadService.ACTION_SYNC, this, Uri.withAppendedPath(mUri,
						AppContract.AppVersions.CONTENT_PATH));

		mListView.addHeaderView(createHeaderView(inflater));
		mAdapter = new AppVersionsAdapter(getActivity(), this);
		mListView.setAdapter(mAdapter);
		LoaderManager lm = getLoaderManager();
		lm.initLoader(LOADER_APP, null, this);
		lm.initLoader(LOADER_APP_VERSIONS, null, this);
		lm.initLoader(LOADER_DOWNLOADS, null, mLoaderCallback);

		setHasOptionsMenu(true);

		return view;
	}

	private View createHeaderView(LayoutInflater inflater) {
		View view = inflater.inflate(R.layout.app_header, null);
		mAppImageView = (ImageView) view.findViewById(android.R.id.icon);
		mAppImageViewHolder = new RemoteImageLoader.ImageViewHolder(
				mAppImageView);
		mRemoteImageLoader.loadImage(mAppImageViewHolder, null);
		mAppNameTextView = (TextView) view.findViewById(android.R.id.text1);
		mAppVersionTextView = (TextView) view.findViewById(android.R.id.text2);
		mAppTokenTextView = (TextView) view.findViewById(R.id.text3);
		mAppGuidTextView = (TextView) view.findViewById(R.id.text4);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		mRemoteImageLoader.onActivityResume();
		mDownloadHelper.onActivityResume();
		mDownloadHelper.startDownloading(null, false);
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
		inflater.inflate(R.menu.app_fragment, menu);
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
		case R.id.menu_edit:
			startActivity(new Intent(Intent.ACTION_EDIT, mUri));
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
		mErrorReporter.onReportStatus(screenVisible, screenEmpty,
				screenProgress, progressIndicator, lastStatus);
		if (mProgressIndicator != progressIndicator) {
			this.mProgressIndicator = progressIndicator;
			getActivity().invalidateOptionsMenu();
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case LOADER_APP_VERSIONS: {
			Uri uri = Uri.withAppendedPath(mUri,
					AppContract.AppVersions.CONTENT_PATH);
			return new CursorLoader(getActivity(), uri,
					AppVersionsAdapter.PROJECTION, null, null,
					AppContract.AppVersions.UPDATED_AT);
		}
		case LOADER_APP: {
			Uri uri = mUri.buildUpon().appendQueryParameter("limit", "1")
					.build();
			return new CursorLoader(getActivity(), uri, PROJECTION, null, null,
					AppContract.Apps.UPDATED_AT);
		}
		default:
			throw new RuntimeException("Unknown laoder id: " + id);
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		switch (loader.getId()) {
		case LOADER_APP_VERSIONS:
			mDownloadHelper.updateLocalData(cursor);
			mAdapter.swapCursor(cursor);
			return;
		case LOADER_APP:
			if (!cursor.moveToFirst()) {
				return;
			}
			String name = cursor.getString(PROJECTION_NAME);
			String image = cursor.getString(PROJECTION_IMAGE);
			String version = cursor.getString(PROJECTION_NEWEST_VERSION);
			String token = cursor.getString(PROJECTION_TOKEN);
			String guid = cursor.getString(PROJECTION_GUID);

			mAppNameTextView.setText(name);
			mAppVersionTextView.setText(version);
			mRemoteImageLoader.loadImage(mAppImageViewHolder, image);
			mAppTokenTextView.setText(token == null ? null : String.format(
					mTokenFormat, token));
			mAppGuidTextView.setText(guid == null ? null : String.format(
					mGuidFormat, guid));
			return;
		default:
			throw new RuntimeException("Unknown laoder id: " + loader.getId());
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
		case LOADER_APP_VERSIONS:
			mDownloadHelper.updateLocalData(null);
			mAdapter.swapCursor(null);
			return;
		case LOADER_APP:
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
	public void onDownloadClick(View v, long appVersionId, int position) {
		Uri uri = AppContract.AppVersions.CONTENT_URI.buildUpon()
				.appendPath(String.valueOf(appVersionId))
				.appendPath(AppContract.Download.CONTENT_PATH).build();
		DownloadHelper.startAsyncDownload(getActivity(),
				DownloadService.ACTION_SYNC, uri, null, true);
	}
}
