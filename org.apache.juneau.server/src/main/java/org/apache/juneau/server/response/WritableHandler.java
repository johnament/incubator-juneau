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
package org.apache.juneau.server.response;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.server.*;

/**
 * Response handler for {@link Writable} and {@link ReaderResource} objects.
 * <p>
 * Uses the {@link Writable#writeTo(Writer)} method to send the contents to the {@link RestResponse#getNegotiatedWriter()} writer.
 * <p>
 * This handler is registered by default on {@link RestServlet RestServlets} via the
 * 	default implementation of the {@link RestServlet#createResponseHandlers} method.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class WritableHandler implements ResponseHandler {

	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res, Object output) throws IOException, RestException {
		if (output instanceof Writable) {
			if (output instanceof ReaderResource) {
				ReaderResource r = (ReaderResource)output;
				String mediaType = r.getMediaType();
				if (mediaType != null)
					res.setContentType(mediaType);
				for (Map.Entry<String,String> h : r.getHeaders().entrySet())
					res.setHeader(h.getKey(), h.getValue());
			}
			Writer w = res.getNegotiatedWriter();
			((Writable)output).writeTo(w);
			w.flush();
			w.close();
			return true;
		}
		return false;
	}
}

