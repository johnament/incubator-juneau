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

package org.apache.juneau.server.vars;

import java.io.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.server.*;
import org.apache.juneau.svl.*;

/**
 * Serialized request parameter variable resolver.
 * <p>
 * The format for this var is <js>"$SP{contentType,key}"</js> or <js>"$SP{contentType,key,defaultValue}"</js>.
 * <p>
 * This variable resolver requires that a {@link RestRequest} object be set as a context object on the resolver or a
 * 	session object on the resolver session.
 * <p>
 * Similar to the {@link RequestParamVar} class except uses the {@link RestRequest#getParameter(String)} object
 * 	and passes the value to the {@link Serializer} whose {@link Produces @Produces} annotation matches the specified content type.
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * @see org.apache.juneau.svl
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class SerializedRequestParamVar extends StreamedVar {

	/**
	 * Constructor.
	 */
	public SerializedRequestParamVar() {
		super("SP");
	}

	@Override /* Var */
	public void resolveTo(VarResolverSession session, Writer w, String key) {
		try {
			int i = key.indexOf(',');
			if (i == -1)
				throw new RuntimeException("Invalid format for $SP var.  Must be of the format $SP{contentType,key[,defaultValue]}");
			String[] s2 = StringUtils.split(key, ',');
			RestRequest req = session.getSessionObject(RestRequest.class, RequestVar.SESSION_req);
			if (req != null) {
				Object o = req.getParameter(key, Object.class);
				if (o == null)
					o = key;
				Serializer s = req.getSerializerGroup().getSerializer(s2[0]);
				if (s != null)
					s.serialize(w, o);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
