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
package org.apache.juneau.server.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Annotation that can be applied to a parameter of a {@link RestMethod} annotated method
 * 	to identify it as a variable in a URL path pattern converted to a POJO.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/myurl/{foo}/{bar}/{baz}/*"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res,
 * 			<ja>@Attr</ja> String foo, <ja>@Attr</ja> <jk>int</jk> bar, <ja>@Attr</ja> UUID baz) {
 * 		...
 * 	}
 * </p>
 * <p>
 * 	The <ja>@Attr</ja> annotation is optional if the parameters are specified immediately
 * 	following the <code>RestRequest</code> and <code>RestResponse</code> parameters,
 * 	and are specified in the same order as the variables in the URL path pattern.
 * 	The following example is equivalent to the previous example.
 * </p>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/myurl/{foo}/{bar}/{baz}/*"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res,
 * 			String foo, <jk>int</jk> bar, UUID baz) {
 * 		...
 * 	}
 * </p>
 * <p>
 * 	If the order of parameters is not the default order shown above, the
 * 	attribute names must be specified (since parameter names are lost during compilation).
 * 	The following example is equivalent to the previous example, except
 * 	the parameter order has been switched, requiring the use of the <ja>@Attr</ja>
 * 	annotations.
 * <p>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/myurl/{foo}/{bar}/{baz}/*"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res,
 * 			<ja>@Attr</ja>(<js>"baz"</js>) UUID baz, <ja>@Attr</ja>(<js>"foo"</js>) String foo, <ja>@Attr</ja>(<js>"bar"</js>) <jk>int</jk> bar) {
 * 		...
 * 	}
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Attr {

	/**
	 * URL variable name.
	 * <p>
	 * 	Optional if the attributes are specified in the same order as in the URL path pattern.
	 */
	String value() default "";
}
