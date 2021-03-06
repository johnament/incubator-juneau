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

import java.io.*;

import org.apache.juneau.internal.*;
import org.junit.*;

public class CT_ByteArrayCache {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		ByteArrayCache bac = new ByteArrayCache();
		byte[] b;

		b = bac.cache(new byte[]{1,2,3});
		assertObjectEquals("[1,2,3]", b);
		assertEquals(1, bac.size());

		b = bac.cache(new byte[]{1,2,3});
		assertObjectEquals("[1,2,3]", b);
		assertEquals(1, bac.size());

		b = bac.cache(new byte[]{1,2,3,4});
		assertObjectEquals("[1,2,3,4]", b);
		b = bac.cache(new byte[]{1,2});
		assertObjectEquals("[1,2]", b);
		assertEquals(3, bac.size());

		b = bac.cache(new byte[]{});
		assertObjectEquals("[]", b);

		b = bac.cache((byte[])null);
		assertNull(b);

		b = bac.cache((InputStream)null);
		assertNull(b);

		b = bac.cache(new ByteArrayInputStream(new byte[]{1,2,3}));
		assertObjectEquals("[1,2,3]", b);
		assertEquals(4, bac.size());
	}
}
