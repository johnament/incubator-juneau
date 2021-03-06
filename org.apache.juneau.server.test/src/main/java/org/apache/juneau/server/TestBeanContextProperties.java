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

import java.io.*;
import java.util.*;

import org.apache.juneau.server.annotation.*;
import org.apache.juneau.transforms.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testBeanContext",
	transforms=DateTransform.ISO8601DTZ.class
)
public class TestBeanContextProperties extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Validate that transforms defined on class transform to underlying bean context.
	//====================================================================================================
	@RestMethod(name="GET", path="/testClassTransforms/{d1}")
	public Reader testClassTransforms(@Attr("d1") Date d1, @Param("d2") Date d2, @Header("X-D3") Date d3) throws Exception {
		DateTransform df = DateTransform.ISO8601DTZ.class.newInstance();
		return new StringReader(
			"d1="+df.transform(d1)+",d2="+df.transform(d2)+",d3="+df.transform(d3)+""
		);
	}
}
