/*
 * Copyright (C) 2012 Jacek Marchwicki <jacek.marchwicki@gmail.com>
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.appjma.appdeployer.R;

/**
 * ActionBarCustomHelper is a class that help to create custom view with action
 * in ActionBar. Usually this is something like: ----------------------------- |
 * x Cancel | v Save | -----------------------------
 * 
 * this class can be created only by builder
 * {@link ActionBarCustomHelper#Builder}
 * 
 * @see ActionBarCustomHelper#Builder
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 * 
 */
public class ActionBarCustomHelper {
	private final LinearLayout mView;

	private ActionBarCustomHelper(LinearLayout view) {
		this.mView = view;
	}

	private static class CustomView {
		int resource = 0;
		public OnClickListener onClickListener = null;
		public CharSequence title = null;
		public Drawable icon = null;
	}

	/**
	 * This builder allow to create {@link ActionBarCustomHelper}
	 * 
	 * <p>
	 * Tipically you will write:
	 * 
	 * <pre class="prettyprint">
	 * new ActionBarCustomHelper.Builder(getSupportActionBar())
	 * 		.addButton(R.drawable.ic_action_discard, &quot;Save for later&quot;,
	 * 				new View.OnClickListener() {
	 * 					&#064;Override
	 * 					public void onClick(View v) {
	 * 						finish();
	 * 					}
	 * 				}).addButton(R.drawable.ic_action_done, R.string.done, null)
	 * 		.build();
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @author mac
	 * 
	 */
	public static class Builder {

		private static final float DIVIDER_WIDTH_DIP = 1;

		private static final float DIVIDER_MARGIN_DIP = 12;

		private final ActionBar mActionBar;

		private final Collection<CustomView> mCusomViews = new ArrayList<CustomView>();

		private LayoutInflater mInflater;

		private boolean mWithMoreActions = false;

		private Drawable mDividerDrawable;

		private int mDividerWidth;

		private int mDividerMargin;

		/**
		 * Create builder with {@link ActionBar}
		 * 
		 * @param supportActionBar
		 *            action bar provided by
		 *            {@link SherlockActivity#getSupportActionBar()}
		 */
		public Builder(ActionBar supportActionBar) {
			this.mActionBar = supportActionBar;
			Context context = supportActionBar.getThemedContext();
			
			TypedArray a = context.obtainStyledAttributes(
					R.style.Theme_Sherlock,
					new int[] { R.attr.dividerVertical });
			mDividerDrawable = a.getDrawable(0);
			a.recycle();
			
			Resources r = context.getResources();
			mDividerWidth = (int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP, DIVIDER_WIDTH_DIP,
					r.getDisplayMetrics());
			mDividerMargin = (int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP, DIVIDER_MARGIN_DIP,
					r.getDisplayMetrics());

			mInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		/**
		 * Add custom view to list of items. If you want to perform some action
		 * on this view you can use {@link ActionBarCustomHelper#getView()} and
		 * {@link View#findViewById(int)}
		 * 
		 * @param resource
		 *            layout to inflate
		 * @return builder
		 */
		public Builder addCustomView(int resource) {
			CustomView customView = new CustomView();
			customView.resource = resource;
			mCusomViews.add(customView);

			return this;
		}

		/**
		 * Add button to actionbar
		 * 
		 * @see #addButton(Drawable, CharSequence, OnClickListener)
		 * @see #addButton(int, CharSequence, OnClickListener)
		 * 
		 * @param iconResource
		 *            icon on the left (can be 0)
		 * @param titleResource
		 *            title of button (can not be 0)
		 * @param onClickListener
		 *            OnClickListener (can be null)
		 * @return builder
		 */
		public Builder addButton(int iconResource, int titleResource,
				View.OnClickListener onClickListener) {
			if (titleResource == 0)
				throw new IllegalArgumentException(
						"text should not be a 0 resource");
			CharSequence title = mInflater.getContext().getText(titleResource);

			return this.addButton(iconResource, title, onClickListener);
		}

		/**
		 * Add button to actionbar
		 * 
		 * @see #addButton(Drawable, CharSequence, OnClickListener)
		 * @see #addButton(int, int, OnClickListener)
		 * 
		 * @param iconResource
		 *            icon on the left (can be 0)
		 * @param title
		 *            title of button (can not be null)
		 * @param onClickListener
		 *            OnClickListener (can be null)
		 * @return builder
		 */
		public Builder addButton(int iconResource, CharSequence title,
				View.OnClickListener onClickListener) {
			Drawable icon = null;
			if (iconResource != 0) {
				icon = mInflater.getContext().getResources()
						.getDrawable(iconResource);
			}
			return this.addButton(icon, title, onClickListener);
		}

		/**
		 * Add button to actionbar
		 * 
		 * @see #addButton(int, CharSequence, OnClickListener)
		 * @see #addButton(int, int, OnClickListener)
		 * 
		 * @param icon
		 *            icon on the left (can be null)
		 * @param title
		 *            title of button (can not be null)
		 * @param onClickListener
		 *            OnClickListener (can be null)
		 * @return builder
		 */
		public Builder addButton(Drawable icon, CharSequence title,
				View.OnClickListener onClickListener) {
			if (title == null)
				throw new IllegalArgumentException(
						"text should not be null resource");

			CustomView customView = new CustomView();
			customView.icon = icon;
			customView.title = title;
			customView.onClickListener = onClickListener;
			mCusomViews.add(customView);
			return this;
		}

		/**
		 * Add divder at the end of list view. This is very usefull when action
		 * bar have more standard action or overflow action
		 * 
		 * @return builder
		 */
		public Builder withMoreActions() {
			mWithMoreActions = true;
			return this;
		}

		/**
		 * Build ActionBarCustomHelper
		 * 
		 * @return instance of action bar custom helper
		 */
		public ActionBarCustomHelper build() {

			View customActionBarView = mInflater.inflate(
					R.layout.actionbar_custom_helper_view, null);

			LinearLayout buttonsView = (LinearLayout) customActionBarView
					.findViewById(R.id.action_bar_buttons);
			Iterator<CustomView> iterator = mCusomViews.iterator();
			while (iterator.hasNext()) {
				ActionBarCustomHelper.CustomView customView = (ActionBarCustomHelper.CustomView) iterator
						.next();
				if (customView.resource != 0) {
					mInflater.inflate(customView.resource, buttonsView, true);
				} else if (customView.title != null) {
					FrameLayout view = (FrameLayout) mInflater.inflate(
							R.layout.actionbar_custom_helper_button,
							buttonsView, false);
					TextView textView = (TextView) view
							.findViewById(android.R.id.text1);
					textView.setText(customView.title);
					textView.setCompoundDrawablesWithIntrinsicBounds(
							customView.icon, null, null, null);
					if (customView.onClickListener != null)
						view.setOnClickListener(customView.onClickListener);
					buttonsView.addView(view);
				} else {
					throw new RuntimeException();
				}
				if (iterator.hasNext())
					addDivider(buttonsView);
			}
			
			setMoreActionsDivider(buttonsView);

			mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
					ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
							| ActionBar.DISPLAY_SHOW_TITLE);
			mActionBar.setCustomView(customActionBarView,
					new ActionBar.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.MATCH_PARENT));
			return new ActionBarCustomHelper(buttonsView);
		}
		
		public static void setBackgroundDrawable(View view, Drawable drawable) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				setBackgroundDrawableJellyBean(view, drawable);
			} else {
				setBackgroundDrawableOld(view, drawable);
			}
		}
		
		@SuppressWarnings("deprecation")
		private static void setBackgroundDrawableOld(View view, Drawable drawable) {
				view.setBackgroundDrawable(drawable);
		}
		
		@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		private static void setBackgroundDrawableJellyBean(View view, Drawable drawable) {
				view.setBackground(drawable);
		}
		
		public void addDivider(LinearLayout view) {
			if (Build.VERSION.SDK_INT >= 11)
				return;
			View imageView = new View(
					view.getContext());
			setBackgroundDrawable(imageView, mDividerDrawable);
			LayoutParams layoutParams = new LinearLayout.LayoutParams(
					(int)mDividerWidth,
					ViewGroup.LayoutParams.MATCH_PARENT);
			layoutParams.setMargins(mDividerMargin, mDividerMargin, mDividerMargin, mDividerMargin);
			view.addView(imageView, layoutParams);
		}

		private void setMoreActionsDivider(LinearLayout buttonsView) {
			if (mWithMoreActions) {
				if (Build.VERSION.SDK_INT >= 11) {
					setMoreActionsDividerApi11(buttonsView);
				} else {
					addDivider(buttonsView);
				}
			}
		}

		@TargetApi(11)
		private void setMoreActionsDividerApi11(LinearLayout buttonsView) {
			buttonsView
					.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE
							| LinearLayout.SHOW_DIVIDER_END);
		}

	}

	public LinearLayout getView() {
		return mView;
	}
}
