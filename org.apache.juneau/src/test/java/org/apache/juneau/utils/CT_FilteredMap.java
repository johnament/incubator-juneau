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
package org.apache.juneau.utils;

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.junit.*;

public class CT_FilteredMap {

	Map<?,?> m3;

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		ObjectMap m = new ObjectMap("{a:'1',b:'2'}");
		FilteredMap<String,Object> m2 = new FilteredMap<String,Object>(m, new String[]{"a"});

		assertObjectEquals("{a:'1'}", m2);

		m2.entrySet().iterator().next().setValue("3");
		assertObjectEquals("{a:'3'}", m2);

		try { m3 = new FilteredMap<String,String>(null, new String[0]); fail(); } catch (IllegalArgumentException e) {}
		try { m3 = new FilteredMap<String,Object>(m, null); fail(); } catch (IllegalArgumentException e) {}
	}
}
