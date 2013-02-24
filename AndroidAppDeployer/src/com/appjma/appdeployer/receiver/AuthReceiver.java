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
package com.appjma.appdeployer.receiver;

import com.appjma.appdeployer.service.DownloadService;
import com.appunite.syncer.DownloadHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import static com.google.common.base.Preconditions.*;

public class AuthReceiver extends BroadcastReceiver {

	public static final String ACTION_AUTHENTICATED = "com.appjma.appdeployer.ACTION_AUTHENTICATED";
	public static final String ACTION_AUTHENTICATED_EXTRA_URI = "extra_uri";
	public static final String ACTION_AUTHENTICATED_EXTRA_BUNDLE = "extra_bundle";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (ACTION_AUTHENTICATED.equals(action)) {
			Bundle extras = intent.getExtras();
			checkNotNull(extras);
			Uri uri = extras.getParcelable(ACTION_AUTHENTICATED_EXTRA_URI);
			checkNotNull(uri);
			Bundle bundle = extras.getBundle(ACTION_AUTHENTICATED_EXTRA_BUNDLE);
			DownloadHelper.startAsyncDownload(context,
					DownloadService.ACTION_SYNC, uri, bundle, true);
		} else {
			throw new RuntimeException("Unknown action: " + action);
		}
	}

}
