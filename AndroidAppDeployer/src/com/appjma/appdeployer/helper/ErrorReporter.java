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

import static com.google.common.base.Preconditions.checkState;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.appjma.appdeployer.AppPreferences;
import com.appjma.appdeployer.R;
import com.appjma.appdeployer.content.AppContract;
import com.appjma.appdeployer.service.DownloadService;
import com.appunite.syncer.AUSyncerStatus;
import com.appunite.syncer.DownloadHelper;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class ErrorReporter implements OnClickListener {

	private static final int REQUEST_PICK_ACCOUNT = 1002;
	private static final int REQUEST_SHOW_ERROR = 1003;
	private View mErrorLayout;
	private TextView mErrorMessageTextView;
	private Button mErrorButton;
	private View mErrorRefreshProgress;
	private AUSyncerStatus mLastStatus;
	private final Activity mActivity;
	private Uri mRefreshUri;

	public ErrorReporter(Activity activity, View view, Uri refreshUri) {

		mActivity = activity;
		mRefreshUri = refreshUri;
		mErrorLayout = view.findViewById(R.id.error_layout);
		mErrorMessageTextView = (TextView) view
				.findViewById(R.id.error_message);
		mErrorButton = (Button) view.findViewById(R.id.error_button);
		mErrorRefreshProgress = view.findViewById(R.id.error_refresh_progress);

		mErrorButton.setOnClickListener(this);
	}

	public void onReportStatus(boolean screenVisible, boolean screenEmpty,
			boolean screenProgress, boolean progressIndicator,
			AUSyncerStatus lastStatus) {
		mErrorLayout.setVisibility(lastStatus.isError() ? View.VISIBLE
				: View.GONE);
		mErrorButton.setVisibility(!progressIndicator ? View.VISIBLE
				: View.GONE);
		mErrorButton.setText(R.string.error_error_refresh);
		mErrorRefreshProgress.setVisibility(progressIndicator ? View.VISIBLE
				: View.GONE);
		mLastStatus = lastStatus;
		if (lastStatus.isError()) {
			long currentTimeMillis = System.currentTimeMillis();
			CharSequence lastErrorString = DateUtils.getRelativeTimeSpanString(
					currentTimeMillis, lastStatus.getStatusTimeMs(), 0,
					DateUtils.FORMAT_ABBREV_ALL);
			String errorFormat = mActivity
					.getString(R.string.error_error_occure);
			CharSequence errorMsg;

			if (lastStatus.isCustomIssue()) {
				String error = lastStatus.getMsgObjectAsStringOrThrow();
				if (DownloadService.STATE_TOKEN_INVALID.equals(error)) {
					errorMsg = mActivity.getText(R.string.error_token_invalid);
					mErrorButton.setText(R.string.error_button_invalid_token);
				} else if (DownloadService.STATE_REAUTHORIZE.equals(error)) {
					errorMsg = mActivity.getText(R.string.error_click_to_login);
					mErrorButton.setText(R.string.error_button_login);
				} else if (DownloadService.STATE_UNSUPPORTED_DEVICE
						.equals(error)) {
					errorMsg = mActivity
							.getText(R.string.error_unsupported_device_click_for_more_info);
					mErrorButton.setText(R.string.error_button_more_info);
				} else {
					throw new RuntimeException("Unknown error: " + error);
				}
			} else if (lastStatus.isNoInternetConnection()) {
				String errorType = mActivity
						.getString(R.string.error_network_connection);
				errorMsg = String.format(errorFormat, errorType,
						lastErrorString);
			} else {
				String errorType = mActivity.getString(R.string.error_unknown);
				errorMsg = String.format(errorFormat, errorType,
						lastErrorString);
			}

			mErrorMessageTextView.setText(errorMsg);

		}
	}

	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		switch (viewId) {
		case R.id.error_button:
			if (mLastStatus.isError() && mLastStatus.isCustomIssue()) {
				String error = mLastStatus.getMsgObjectAsStringOrThrow();
				if (DownloadService.STATE_TOKEN_INVALID.equals(error)) {
					new AuthAsyncTask(mActivity, mRefreshUri).execute();
				} else if (DownloadService.STATE_REAUTHORIZE.equals(error)) {
					Intent intent = AccountPicker.newChooseAccountIntent(null,
							null, new String[] { "com.google" }, false, null,
							null, null, null);
					mActivity.startActivityForResult(intent,
							REQUEST_PICK_ACCOUNT);
				} else if (DownloadService.STATE_UNSUPPORTED_DEVICE
						.equals(error)) {
					int connectionResult = GooglePlayServicesUtil
							.isGooglePlayServicesAvailable(mActivity);
					checkState(connectionResult != ConnectionResult.SUCCESS);
					Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
							connectionResult, mActivity, REQUEST_SHOW_ERROR);
					dialog.show();
				} else {
					throw new RuntimeException("Unknown error: " + error);
				}
			} else {
				DownloadHelper.startAsyncDownload(mActivity,
						DownloadService.ACTION_SYNC, mRefreshUri, null, true);
			}
			return;
		default:
			throw new RuntimeException();
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_SHOW_ERROR:
			return;
		case REQUEST_PICK_ACCOUNT:
			if (resultCode == Activity.RESULT_OK) {
				String accountName = data
						.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				new AppPreferences(mActivity).edit()
						.setAccountName(accountName).commit();
				new AuthAsyncTask(mActivity, AppContract.Apps.CONTENT_URI)
						.execute();
			}
			return;
		default:
			return;
		}
	}

}
