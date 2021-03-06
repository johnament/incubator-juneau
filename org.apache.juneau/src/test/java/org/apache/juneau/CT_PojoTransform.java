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
package org.apache.juneau;

import static org.junit.Assert.*;

import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.junit.*;

public class CT_PojoTransform {

	//====================================================================================================
	// Test same type
	// If you define a PojoTransform<String,String> filter, then it should be invoked on all strings.
	//====================================================================================================
	@Test
	public void testSameType() throws Exception {
		JsonSerializer s = JsonSerializer.DEFAULT_LAX.clone().addTransforms(ATransform.class);
		JsonParser p = JsonParser.DEFAULT.clone().addTransforms(ATransform.class);
		String r;

		r = s.serialize("foobar");
		assertEquals("'xfoobarx'", r);
		r = p.parse(r, String.class);
		assertEquals("foobar", r);

		ObjectMap m = new ObjectMap("{foo:'bar'}");
		r = s.serialize(m);
		assertEquals("{xfoox:'xbarx'}", r);
	}

	public static class ATransform extends PojoTransform<String,String> {
		@Override
		public String transform(String o) throws SerializeException {
			return "x" + o + "x";
		}

		@Override
		public String normalize(String f, ClassMeta<?> hint) throws ParseException {
			return f.substring(1, f.length()-1);
		}
	}
}
