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
package org.apache.juneau.server.labels;

import java.util.*;

import org.apache.juneau.server.*;

/**
 * A POJO structure that describes the list of child resources associated with a resource.
 * <p>
 * Typically used in top-level GET methods of router resources to render a list of
 * 	available child resources.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class ChildResourceDescriptions extends LinkedList<ResourceDescription> {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param servlet The servlet that this bean describes.
	 * @param req The HTTP servlet request.
	 */
	public ChildResourceDescriptions(RestServlet servlet, RestRequest req) {
		this(servlet, req, false);
	}

	/**
	 * Constructor.
	 *
	 * @param servlet The servlet that this bean describes.
	 * @param req The HTTP servlet request.
	 * @param sort If <jk>true</jk>, list will be ordered by name alphabetically.
	 * 	Default is to maintain the order as specified in the annotation.
	 */
	public ChildResourceDescriptions(RestServlet servlet, RestRequest req, boolean sort) {
		String uri = req.getTrimmedRequestURI();
		for (Map.Entry<String,RestServlet> e : servlet.getChildResources().entrySet())
			add(new ResourceDescription(uri, e.getKey(), e.getValue().getLabel(req)));
		if (sort)
			Collections.sort(this);
	}

	/**
	 * Bean constructor.
	 */
	public ChildResourceDescriptions() {
		super();
	}
}
