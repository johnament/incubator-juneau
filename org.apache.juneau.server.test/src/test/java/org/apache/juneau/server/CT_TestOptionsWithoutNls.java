/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server;

import static org.junit.Assert.*;

import org.apache.juneau.client.*;
import org.apache.juneau.json.*;
import org.apache.juneau.server.labels.*;
import org.junit.*;

public class CT_TestOptionsWithoutNls {

	private static String URL = "/testOptionsWithoutNls";

	//====================================================================================================
	// Should get to the options page without errors
	//====================================================================================================
	@Test
	public void testOptions() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		RestCall r = client.doOptions(URL + "/testOptions");
		ResourceOptions o = r.getResponse(ResourceOptions.class);
		assertEquals("", o.getDescription());

		client.closeQuietly();
	}

	//====================================================================================================
	// Missing resource bundle should cause {!!x} string.
	//====================================================================================================
	@Test
	public void testMissingResourceBundle() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		RestCall r = client.doGet(URL + "/testMissingResourceBundle");
		String o = r.getResponse(String.class);
		assertEquals("{!!bad}", o);

		client.closeQuietly();
	}
}
