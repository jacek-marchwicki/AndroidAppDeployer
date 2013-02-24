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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class AppPreferences {
	private static final String PREFERENCES_NAME = "preferences";
	
	private static final String PREFERENCE_APPS_NEXT_TOKEN = "apps_next_token";
	private static final String PREFERENCE_ACCOUNT_NAME = "account_name";

	
	public static class AppEdit {

		private Editor mEditor;

		protected AppEdit(Editor editor) {
			mEditor = editor;
		}
		
		public AppEdit setAppsNextToken(String lastToken) {
			mEditor.putString(PREFERENCE_APPS_NEXT_TOKEN, lastToken);
			return this;
		}
		
		public AppEdit setAccountName(String accountName) {
			mEditor.putString(PREFERENCE_ACCOUNT_NAME, accountName);
			return this;
		}

		public AppEdit clear() {
			mEditor.clear();
			return this;
		}
		
		public boolean commit() {
			return mEditor.commit();
		}
		
	}
	
	private SharedPreferences mPreferences;

	public AppPreferences(Context context) {
		mPreferences = context.getSharedPreferences(PREFERENCES_NAME, 0);
	}
	
	public AppEdit edit() {
		return new AppEdit(mPreferences.edit());
	}
	
	public String getAppsNextToken() {
		return mPreferences.getString(PREFERENCE_APPS_NEXT_TOKEN, null);
	}

	public String getAccountName() {
		return mPreferences.getString(PREFERENCE_ACCOUNT_NAME, null);
	}
}
