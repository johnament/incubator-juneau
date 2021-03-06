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

import org.apache.juneau.encoders.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.server.*;

/**
 * Identifies a REST Java method on a {@link RestServlet} implementation class.
 * <p>
 *		Refer to {@link org.apache.juneau.server} doc for information on using this class.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
@Inherited
public @interface RestMethod {

	/**
	 * REST method name.
	 * <p>
	 * 	Typically <js>"GET"</js>, <js>"PUT"</js>, <js>"POST"</js>, <js>"DELETE"</js>, or <js>"OPTIONS"</js>.
	 * <p>
	 * 	Can also be a non-HTTP-standard name that is passed in through a <code>&amp;method=methodName</code> URL parameter.
	 * <p>
	 * 	Method names are case-insensitive (always folded to upper-case).
	 * <p>
	 * 	If a method name is not specified, then the method name is determined based on the Java method name.<br>
	 * 	For example, if the method is <code>doPost(...)</code>, then the method name is automatically detected as <js>"POST"</js>.

	 */
	String name() default "";

	/**
	 * Optional path pattern for the specified method.
	 * <p>
	 * 	Appending <js>"/*"</js> to the end of the path pattern will make it match any remainder too.<br>
	 * 	Not appending <js>"/*"</js> to the end of the pattern will cause a 404 (Not found) error to occur
	 * 	if the exact pattern is not found.
	 */
	String path() default "/*";

	/**
	 * URL path pattern priority.
	 * <p>
	 * 	To force path patterns to be checked before other path patterns, use a higher priority number.
	 * <p>
	 * 	By default, it's <code>0</code>, which means it will use an internal heuristic to
	 * 	determine a best match.
	 */
	int priority() default 0;

	/**
	 * Method guards.
	 * <p>
	 * 	Associates one or more {@link RestGuard RestGuards} with a method call.
	 * 	These guards get called immediately before execution of the REST method.
	 * <p>
	 * 	Typically, guards will be used for permissions checking on the user making the request,
	 * 	but it can also be used for other purposes like pre-call validation of a request.
	 */
	Class<? extends RestGuard>[] guards() default {};

	/**
	 * Method response converters.
	 * <p>
	 * 	Associates one or more {@link RestConverter RestConverters} with a method call.
	 * 	These converters get called immediately after execution of the REST method in the same
	 * 		order specified in the annotation.
	 * <p>
	 * 	Can be used for performing post-processing on the response object before serialization.
	 * <p>
	 * 	Default converters are available in the {@link org.apache.juneau.server.converters} package.
	 */
	Class<? extends RestConverter>[] converters() default {};

	/**
	 * Method matchers.
	 * <p>
	 * 	Associates one more more {@link RestMatcher RestMatchers} with this method.
	 * <p>
	 * 	Matchers are used to allow multiple Java methods to handle requests assigned to the same
	 * 		URL path pattern, but differing based on some request attribute, such as a specific header value.
	 * <p>
	 * 	See {@link RestMatcher} for details.
	 */
	Class<? extends RestMatcher>[] matchers() default {};

	/**
	 * Overrides the list of serializers assigned at the method level.
	 * <p>
	 * Use this annotation when the list of serializers assigned to a method differs from the list of serializers assigned at the servlet level.
	 * <p>
	 * To append to the list of serializers assigned at the servlet level, use <code>serializersInherit=<jsf>SERIALIZERS</jsf></code>.
	 *
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 *
	 * 		<ja>@RestMethod</ja>(
	 * 			name=<js>"GET"</js>,
	 * 			path=<js>"/foo"</js>,
	 * 			serializers=MySpecialSerializer.<jk>class</jk>,
	 * 			serializersInherit=<jsf>SERIALIZERS</jsf>
	 * 		)
	 * 		<jk>public</jk> Object doGetWithSpecialAcceptType() {
	 * 			<jc>// Handle request for special Accept type</jc>
	 * 		}
	 * 	}
	 * </p>
	 */
	Class<? extends Serializer>[] serializers() default {};

	/**
	 * Used in conjunction with {@link #serializers()} to identify what class-level settings are inherited by the method serializer group.
	 * <p>
	 * Possible values:
	 * </p>
	 * <ul>
	 * 	<li>{@link Inherit#SERIALIZERS} - Inherit class-level serializers.
	 * 	<li>{@link Inherit#PROPERTIES} - Inherit class-level properties.
	 * 	<li>{@link Inherit#TRANSFORMS} - Inherit class-level transforms.
	 * </ul>
	 * <p>
	 * 	For example, to inherit all serializers, properties, and transforms from the servlet class:
	 * </p>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		path=<js>"/foo"</js>,
	 * 		serializers=MySpecialSerializer.<jk>class</jk>,
	 * 		serializersInherit={<jsf>SERIALIZERS</jsf>,<jsf>PROPERTIES</jsf>,<jsf>TRANSFORMS</jsf>}
	 * 	)
	 * </p>
	 */
	Inherit[] serializersInherit() default {};

	/**
	 * Overrides the list of parsers assigned at the method level.
	 * <p>
	 * Use this annotation when the list of parsers assigned to a method differs from the list of parsers assigned at the servlet level.
	 * <p>
	 * To append to the list of serializers assigned at the servlet level, use <code>serializersInherit=<jsf>SERIALIZERS</jsf></code>.
	 *
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 *
	 * 		<ja>@RestMethod</ja>(
	 * 			name=<js>"PUT"</js>,
	 * 			path=<js>"/foo"</js>,
	 * 			parsers=MySpecialParser.<jk>class</jk>,
	 * 			parsersInherit=<jsf>PARSERS</jsf>
	 * 		)
	 * 		<jk>public</jk> Object doGetWithSpecialAcceptType() {
	 * 			<jc>// Handle request for special Accept type</jc>
	 * 		}
	 * 	}
	 * </p>
	 */
	Class<? extends Parser>[] parsers() default {};

	/**
	 * Used in conjunction with {@link #parsers()} to identify what class-level settings are inherited by the method parser group.
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li>{@link Inherit#PARSERS} - Inherit class-level parsers.
	 * 	<li>{@link Inherit#PROPERTIES} - Inherit class-level properties.
	 * 	<li>{@link Inherit#TRANSFORMS} - Inherit class-level transforms.
	 * </ul>
	 * <p>
	 * 	For example, to inherit all parsers, properties, and transforms from the servlet class:
	 * </p>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		path=<js>"/foo"</js>,
	 * 		parsers=MySpecialParser.<jk>class</jk>,
	 * 		parsersInherit={<jsf>PARSERS</jsf>,<jsf>PROPERTIES</jsf>,<jsf>TRANSFORMS</jsf>}
	 * 	)
	 * </p>
	 */
	Inherit[] parsersInherit() default {};

	/**
	 * Appends to the list of {@link Encoder encoders} specified on the servlet.
	 * <p>
	 * Use this annotation when the list of encoders assigned to a method differs from the list of encoders assigned at the servlet level.
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 *
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 *
	 * 		<ja>@RestMethod</ja>(
	 * 			name=<js>"PUT"</js>,
	 * 			path=<js>"/foo"</js>,
	 * 			encoders={GzipEncoder.<jk>class</jk>}
	 * 		)
	 * 		<jk>public</jk> Object doGetWithSpecialEncoding() {
	 * 			<jc>// Handle request with special encoding</jc>
	 * 		}
	 * 	}
	 * </p>
	 *
	 * If you want to OVERRIDE the set of encoders specified by the servlet, combine this annotation with <code><ja>@RestMethod</ja>(inheritEncoders=<jk>false</jk>)</code>.
	 */
	Class<? extends Encoder>[] encoders() default {};

	/**
	 * Specifies whether the method should inherit encoders from the servlet.
	 */
	boolean inheritEncoders() default true;

	/**
	 * Same as {@link RestResource#properties()}, except defines property values by default when this method is called.
	 * <p>
	 * This is equivalent to simply calling <code>res.addProperties()</code> in the Java method, but is provided for convenience.
	 */
	Property[] properties() default {};

	/**
	 * Appends the specified transforms to all serializers and parsers used by this method.
	 */
	Class<?>[] transforms() default {};

	/**
	 * Possible HTTP response codes from this method.
	 * <p>
	 * 	This annotation is provided for documentation purposes.
	 * 	It is used in conjunction with the following NLS resource bundle keys to populate options pages:
	 * <ul class='spaced-list'>
	 * 	<li><js>"(className.?)[javaMethodName].res.[rc]"</js> - Response description (defaults to HTTP status message).
	 * 		e.g. <js>"MyClass.myMethod.res.404 = Sorry...I couldn't find that."</js> or <js>"myMethod.res.404 = Sorry...I couldn't find that."</js>
	 * 	<li><js>"(className.?)[javaMethodName].res.[rc].[varCategory].[varName]"</js> - Response variable .
	 * 		e.g. <js>"MyClass.myMethod.res.302.[header].[Location] = Set to the location where the thing can be found."</js> or  <js>"myMethod.res.302.[header].[Location] = Set to the location where the thing can be found."</js>
	 * </ul>
	 * <p>
	 * Note that you can still define the keys in the resource bundle and not use this annotation.
	 * However, the annotation is provided if you just want the OPTIONS page to show the possible status codes with
	 * default HTTP status messages.
	 */
	int[] rc() default {};

	/**
	 * Specifies default values for request headers.
	 * <p>
	 * Strings are of the format <js>"Header-Name: header-value"</js>.
	 * <p>
	 * Affects values returned by {@link RestRequest#getHeader(String)} when the header is not present on the request.
	 * <p>
	 * The most useful reason for this annotation is to provide a default <code>Accept</code> header when one is not specified
	 * 	so that a particular default {@link Serializer} is picked.
	 * <p>
	 * Only one header value can be specified per entry (i.e. it's not a delimited list of header entries).
	 * <p>
	 * Header values specified at the method level override header values specified at the servlet level.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/*"</js>, defaultRequestHeaders={<js>"Accept: text/json"</js>})
	 * 	<jk>public</jk> String doGet() {
	 * 		...
	 * 	}
	 * </p>
	 * 	</dd>
	 * </dl>
	 */
	String[] defaultRequestHeaders() default {};

	/**
	 * Optional description.
	 * <p>
	 * 	This description is used in the following locations:
	 * 	<ul class='spaced-list'>
	 * 		<li>The value returned by {@link RestRequest#getMethodDescription()}.
	 * 		<li>The <js>"$R{methodDescription}"</js> variable.
	 * 		<li>The description of the method in the OPTIONS page.
	 * 	</ul>
	 * <p>
	 * 	The default value pulls the description from the <code>(className.?)[javaMethodName]</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"MyClass.myMethod = foo"</js> or <js>"myMethod = foo"</js>).
	 * <p>
	 * 	This field value can contain variables (e.g. "$L{my.localized.variable}").
	 */
	String description() default "";

	/**
	 * Optional input description.
	 * <p>
	 * 	This annotation is provided for documentation purposes and is used to populate the method <js>"input"</js> column
	 * 		on the OPTIONS page.
	 * <p>
	 * Example:
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		name=<js>"POST"</js>, path=<js>"/{a}"</js>,
	 * 		description=<js>"This is my method."</js>,
	 * 		input={
	 * 			<ja>@Var</ja>(category=<js>"attr"</js>, name=<js>"a"</js>, description=<js>"The 'a' attribute"</js>),
	 * 			<ja>@Var</ja>(category=<js>"param"</js>, name=<js>"b"</js>, description=<js>"The 'b' parameter"</js>),
	 * 			<ja>@Var</ja>(category=<js>"content"</js>, description=<js>"The HTTP content"</js>),
	 * 			<ja>@Var</ja>(category=<js>"header"</js>, name=<js>"D"</js>, description=<js>"The 'D' header"</js>),
	 * 			<ja>@Var</ja>(category=<js>"foo"</js>, name=<js>"bar"</js>, description=<js>"An arbitrary category"</js>)
	 * 		}
	 * 	)
	 * </p>
	 * This is functionally equivalent to specifying the following keys in the resource bundle for the class, except in this case
	 * the strings are internationalized.
	 * <p class='bcode'>
	 * 	<jk>MyClass.myMethod</jk> = <js>This is my method.</js>
	 * 	<jk>MyClass.myMethod.req.attr.a</jk> = <js>The 'a' attribute</js>
	 * 	<jk>MyClass.myMethod.req.param.b</jk> = <js>The 'b' parameter</js>
	 * 	<jk>MyClass.myMethod.req.content</jk> = <js>The HTTP content</js>
	 * 	<jk>MyClass.myMethod.req.header.d</jk> = <js>The 'D' header</js>
	 * 	<jk>MyClass.myMethod.req.foo.bar</jk> = <js>An arbitrary category</js>
	 * <p>
	 * As a general rule, use annotations when you don't care about internationalization (i.e. you only want to support English),
	 * and use resource bundles if you need to support localization.
	 * <p>
	 * 	These annotations can contain variables (e.g. "$L{my.localized.variable}").
	 */
	Var[] input() default {};


	/**
	 * Optional output description.
	 * <p>
	 * 	This annotation is provided for documentation purposes and is used to populate the method <js>"responses"</js> column
	 * 		on the OPTIONS page.
	 * <p>
	 * Example:
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		name=<js>"GET"</js>, path=<js>"/"</js>,
	 * 		responses={
	 * 			<ja>@Response</ja>(200),
	 * 			<ja>@Response</ja>(
	 * 				value=302,
	 * 				description=<js>"Thing wasn't found here"</js>,
	 * 				output={
	 * 					<ja>@Var</ja>(category=<js>"header"</js>, name=<js>"Location"</js>, description=<js>"The place to find the thing"</js>)
	 * 				}
	 * 			)
	 * 		}
	 * 	)
	 * </p>
	 * This is functionally equivalent to specifying the following keys in the resource bundle for the class, except in this case
	 * the strings are internationalized.
	 * <p class='bcode'>
	 * 	<jk>MyClass.myMethod.res.200</jk> = <js>OK</js>
	 * 	<jk>MyClass.myMethod.res.302</jk> = <js>Thing wasn't found here</js>
	 * 	<jk>MyClass.myMethod.res.302.header.Location</jk> = <js>The place to find the thing</js>
	 * <p>
	 * As a general rule, use annotations when you don't care about internationalization (i.e. you only want to support English),
	 * and use resource bundles if you need to support localization.
	 * <p>
	 * 	These annotations can contain variables (e.g. "$L{my.localized.variable}").
	 */
	Response[] responses() default {};

	/**
	 * Specifies whether this method can be called based on the client version.
	 * <p>
	 * 	The client version is identified via the HTTP request header identified by {@link RestResource#clientVersionHeader()} which
	 * 	by default is <js>"X-Client-Version"</js>.
	 * <p>
	 * 	This is a specialized kind of {@link RestMatcher} that allows you to invoke different Java methods for the same method/path based
	 * 	on the client version.
	 * <p>
	 * 	The format of the client version range is similar to that of OSGi versions.
	 * <p>
	 * 	In the following example, the Java methods are mapped to the same HTTP method and URL <js>"/foobar"</js>.
	 * <p class='bcode'>
	 * 	<jc>// Call this method if X-Client-Version is at least 2.0.
	 * 	// Note that this also matches 2.0.1.</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> Object method1() {
	 * 		...
	 * 	}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
	 * 	<jk>public</jk> Object method2() {
	 * 		...
	 * 	}
	 *
	 * 	<jc>// Call this method if X-Client-Version is less than 1.1.</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
	 * 	<jk>public</jk> Object method3() {
	 * 		...
	 * 	}
	 * </p>
	 * <p>
	 * 	It's common to combine the client version with transforms that will convert new POJOs into older POJOs for backwards compatability.
	 * <p class='bcode'>
	 * 	<jc>// Call this method if X-Client-Version is at least 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> NewPojo newMethod() {
	 * 		...
	 * 	}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>, transforms={NewToOldPojoTransform.<jk>class</jk>})
	 * 	<jk>public</jk> NewPojo oldMethod() {
	 * 		<jk>return</jk> newMethod()
	 * 	}
	 * <p>
	 * 	Note that in the previous example, we're returning the exact same POJO, but using a transform to convert it into an older form.
	 * 	The old method could also just return back a completely different object.
	 * 	The range can be any of the following:
	 * <ul>
	 * 	<li><js>"[0,1.0)"</js> = Less than 1.0.  1.0 and 1.0.0 does not match.
	 * 	<li><js>"[0,1.0]"</js> = Less than or equal to 1.0.  Note that 1.0.1 will match.
	 * 	<li><js>"1.0"</js> = At least 1.0.  1.0 and 2.0 will match.
	 * </ul>
	 */
	String clientVersion() default "";
}
