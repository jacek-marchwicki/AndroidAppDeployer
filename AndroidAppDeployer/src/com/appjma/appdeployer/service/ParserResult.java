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
package com.appjma.appdeployer.service;

import java.util.Date;

public interface ParserResult {

	void apply();

	void clear();

	void setAppsNextToken(String nextToken);

	void clearOldApps();

	void addApp(String id, String guid, String name, String token, Date createdAt,
			Date updatedAt);

	void addAppVersion(String appId, String guid, String version,
			String downloadUrl, Date createdAt, Date updatedAt);

	void clearOldAppVersions(String appId);

	void deleteApp(String id);

}
