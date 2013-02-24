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
package com.appunite.helpers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

public abstract class EditFragment extends SherlockFragment {

	private static final String STATE_URI = "uri_state";
	private static final String STATE_IS_EDIT = "edit_state";

	private boolean mIsEdit;
	private boolean mDiscard;
	private Uri mUri;

	public EditFragment() {
		super();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getActivity().getIntent();
		String action = intent.getAction();

		mDiscard = false;

		if (savedInstanceState != null) {
			mUri = savedInstanceState.getParcelable(STATE_URI);
			checkState(mUri != null);
			mIsEdit = savedInstanceState.getBoolean(STATE_IS_EDIT);
		} else if (Intent.ACTION_INSERT.equals(action)
				|| Intent.ACTION_INSERT_OR_EDIT.equals(action)) {
			mUri = intent.getData();
			if (mUri == null) {
				mUri = getInsertionUri();
			}
			checkArgument(mUri != null);
			mIsEdit = false;
		} else if (Intent.ACTION_EDIT.equals(action)) {
			mUri = intent.getData();
			checkArgument(mUri != null);
			mIsEdit = true;
		} else {
			throw new IllegalArgumentException("Unknown action: " + action);
		}
	}

	protected abstract Uri getInsertionUri();

	protected abstract boolean isDataValid();

	protected abstract Uri realSaveChanges(Uri uri, boolean isEdit);

	protected abstract void realDiscardItem(Uri uri);

	protected abstract void setupEditState(boolean isEdit);

	protected abstract CharSequence getToastSave();

	protected abstract CharSequence getDialogNotValidTitle();

	protected abstract CharSequence getDialogNotValidMessage();

	protected abstract CharSequence getDialogDeleteQuestionTitle();

	protected abstract CharSequence getDialogDeleteQuestionMessage();

	protected abstract CharSequence getDialogDeleteQuestionRemoveButtonTitle();

	protected void saveChanges() {
		FragmentActivity activity = getActivity();
		if (!isDataValid()) {
			new AlertDialog.Builder(activity)
					.setTitle(getDialogNotValidTitle())
					.setMessage(getDialogNotValidMessage())
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).show();
			return;
		}
		mUri = realSaveChanges(mUri, mIsEdit);
		checkArgument(mUri != null);
		setEditMode();
		activity.setResult(Activity.RESULT_OK, new Intent().setData(mUri));
		activity.finish();
	}

	protected void discardChanges() {
		mDiscard = true;
		FragmentActivity activity = getActivity();
		activity.setResult(Activity.RESULT_CANCELED);
		activity.finish();
	}

	protected void discardItem() {
		checkState(mIsEdit);
		new AlertDialog.Builder(getActivity())
				.setTitle(getDialogDeleteQuestionTitle())
				.setMessage(getDialogDeleteQuestionMessage())
				.setPositiveButton(getDialogDeleteQuestionRemoveButtonTitle(),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								checkState(mIsEdit);
								realDiscardItem(mUri);

								mDiscard = true;
								FragmentActivity activity = getActivity();
								activity.setResult(Activity.RESULT_OK);
								activity.finish();
							}
						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.cancel();
							}
						}).show();
	}

	protected Uri getUri() {
		return mUri;
	}
	
	protected boolean isEdit() {
		return mIsEdit;
	}

	@Override
	public void onPause() {
		super.onPause();

		if (!getActivity().isChangingConfigurations() && !mDiscard
				&& isDataValid()) {
			Toast.makeText(getActivity(), getToastSave(),
					Toast.LENGTH_SHORT).show();
			mUri = realSaveChanges(mUri, mIsEdit);
			setEditMode();
		}
	}

	private void setEditMode() {
		if (!mIsEdit) {
			mIsEdit = true;
			setupEditState(mIsEdit);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(STATE_IS_EDIT, mIsEdit);
		outState.putParcelable(STATE_URI, mUri);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public final View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		boolean loadData = mIsEdit && savedInstanceState == null;
		boolean isEdit = mIsEdit;
		View view = onCreateView(inflater, container, savedInstanceState, loadData);
		setupEditState(isEdit);
		return view;
	}

	protected abstract View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState, boolean loadData);

}