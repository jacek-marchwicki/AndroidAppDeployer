package com.appjma.appdeployer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.appjma.appdeployer.adapter.ShareAdapter;
import com.appjma.appdeployer.adapter.ShareAdapter.OnShareDeleteListener;
import com.appjma.appdeployer.content.AppContract;
import com.appunite.helpers.ActionBarCustomHelper;
import com.appunite.helpers.EditFragment;
import com.appunite.imageloader.RemoteImageLoader;
import com.appunite.imageloader.RemoteImageLoader.ImageViewHolder;

public class AppEditFragment extends EditFragment implements
		LoaderCallbacks<Cursor>, OnClickListener, OnShareDeleteListener {

	private static final int LOADER_APP = 0;

	private static final String[] PROJECTION = new String[] {
			AppContract.Apps.NAME, AppContract.Apps.TOKEN,
			AppContract.Apps.GUID, AppContract.Apps.IMAGE,
			AppContract.Apps.SHARE_LINK };
	private static final int PROJECTION_NAME = 0;
	private static final int PROJECTION_TOKEN = 1;
	private static final int PROJECTION_GUID = 2;
	private static final int PROJECTION_IMAGE = 3;
	private static final int PROJECTION_SHARE_LINK = 4;

	private static final float IMAGE_SIZE_DP = 128;

	private EditText mNameEditText;
	private ListView mListView;
	private TextView mTokenTextView;
	private TextView mGuidTextView;
	private EditText mShareLinkEditText;
	private View mShareLinkRecreateView;

	private ImageViewHolder mImageHolder;

	private RemoteImageLoader mRemoteImageLoader;

	private CursorAdapter mAdapter;

	@Override
	protected Uri getInsertionUri() {
		return AppContract.Apps.CONTENT_URI;
	}

	@Override
	protected boolean isDataValid() {
		boolean valid = true;
		if (TextUtils.isEmpty(mNameEditText.getText())) {
			mNameEditText
					.setError(getText(R.string.app_edit_name_could_not_be_empty));
			valid = false;
		}
		return valid;
	}

	@Override
	protected Uri realSaveChanges(Uri uri, boolean isEdit) {
		ContentValues values = new ContentValues();
		values.put(AppContract.Apps.NAME, mNameEditText.getText().toString());
		values.put(AppContract.Apps.SYNCED, false);

		ContentResolver cr = getActivity().getContentResolver();
		if (isEdit) {
			cr.update(uri, values, null, null);
		} else {
			uri = cr.insert(uri, values);
		}
		return uri;
	}

	@Override
	protected void realDiscardItem(Uri uri) {
		ContentResolver cr = getActivity().getContentResolver();
		ContentValues values = new ContentValues();
		values.put(AppContract.Apps.DELETED, true);
		values.put(AppContract.Apps.SYNCED, false);
		cr.update(uri, values, null, null);
	}

	@Override
	protected void setupEditState(boolean isEdit) {
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		new ActionBarCustomHelper.Builder(actionBar)
				.addButton(R.drawable.ic_menu_done, R.string.app_edit_save,
						new View.OnClickListener() {

							@Override
							public void onClick(View v) {
								saveChanges();
							}
						})
				.addButton(
						R.drawable.ic_menu_discard,
						isEdit ? R.string.app_edit_cancel
								: R.string.app_edit_discard,
						new View.OnClickListener() {

							@Override
							public void onClick(View v) {
								discardChanges();
							}
						}).withMoreActions().build();
		getActivity().invalidateOptionsMenu();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.app_edit_fragment, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		MenuItem removeItem = menu.findItem(R.id.menu_remove);
		removeItem.setVisible(isEdit());
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_remove:
			discardItem();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected CharSequence getToastSave() {
		return getText(R.string.app_edit_toast_save);
	}

	@Override
	protected CharSequence getDialogNotValidTitle() {
		return getText(R.string.app_edit_dialog_not_valid_title);
	}

	@Override
	protected CharSequence getDialogNotValidMessage() {
		return getText(R.string.app_edit_dialog_not_valid_message);
	}

	@Override
	protected CharSequence getDialogDeleteQuestionTitle() {
		return getText(R.string.app_edit_dialog_delete_question_title);
	}

	@Override
	protected CharSequence getDialogDeleteQuestionMessage() {
		return getText(R.string.app_edit_dialog_delete_question_message);
	}

	@Override
	protected CharSequence getDialogDeleteQuestionRemoveButtonTitle() {
		return getText(R.string.app_edit_dialog_delete_question_remove_button_title);
	}

	@Override
	protected View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState, boolean loadData) {

		View view = inflater.inflate(R.layout.app_edit_fragment, container,
				false);
		View header = inflater.inflate(R.layout.app_edit_header, null);

		mListView = (ListView) view.findViewById(android.R.id.list);
		mListView.addHeaderView(header);
		mNameEditText = (EditText) header.findViewById(R.id.name_edit_text);
		mTokenTextView = (TextView) header.findViewById(R.id.token_text_view);
		mGuidTextView = (TextView) header.findViewById(R.id.guid_text_view);
		mShareLinkEditText = (EditText) header.findViewById(R.id.share_link);
		ImageView imageView = (ImageView) header
				.findViewById(android.R.id.icon);
		mImageHolder = new RemoteImageLoader.ImageViewHolder(imageView);
		mShareLinkRecreateView = header.findViewById(R.id.share_link_recreate);
		mShareLinkRecreateView.setOnClickListener(this);

		Bitmap placeHolder = BitmapFactory.decodeResource(getResources(),
				R.drawable.placeholder);
		mRemoteImageLoader = RemoteImageLoader.createUsingDp(getActivity(),
				placeHolder, IMAGE_SIZE_DP, IMAGE_SIZE_DP);

		mAdapter = new ShareAdapter(getActivity(), this);
		mListView.setAdapter(mAdapter);

		if (loadData) {
			getLoaderManager().initLoader(LOADER_APP, null, this);
		}
		
		setHasOptionsMenu(true);
		return view;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case LOADER_APP:
			return new CursorLoader(getActivity(), getUri(), PROJECTION, null,
					null, null);
		default:
			throw new RuntimeException("Unknown loader id: " + id);
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		switch (loader.getId()) {
		case LOADER_APP:
			if (!cursor.moveToFirst()) {
				return;
			}
			String name = cursor.getString(PROJECTION_NAME);
			String token = cursor.getString(PROJECTION_TOKEN);
			String guid = cursor.getString(PROJECTION_GUID);
			String imageUrl = cursor.getString(PROJECTION_IMAGE);
			String shareLink = cursor.getString(PROJECTION_SHARE_LINK);
			mNameEditText.setText(name);
			if (TextUtils.isEmpty(token)) {
				mTokenTextView.setText(R.string.app_edit_no_token_before_sync);
			} else {
				mTokenTextView.setText(token);
			}
			if (TextUtils.isEmpty(guid)) {
				mGuidTextView.setText(R.string.app_edit_no_guid_before_sync);
			} else {
				mGuidTextView.setText(guid);
			}
			if (TextUtils.isEmpty(shareLink)) {
				mShareLinkEditText.setText(R.string.app_edit_no_share_link);
				mShareLinkRecreateView.setEnabled(false);
			} else {
				mShareLinkEditText.setText(shareLink);
				mShareLinkRecreateView.setEnabled(true);
			}
			mRemoteImageLoader.loadImage(mImageHolder, imageUrl);

			return;

		default:
			throw new RuntimeException("Unknown loader id: " + loader.getId());
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
		case LOADER_APP:
			break;
		default:
			throw new RuntimeException("Unknown loader id: " + loader.getId());
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.share_link_recreate:
			return;
		default:
			throw new RuntimeException("Unknown clicked id: " + v.getId());
		}
	}

	@Override
	public void onDeleteClicked(View v, long mId) {
		// TODO Auto-generated method stub
	}
}
