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
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Used tailor how bean properties get interpreted by the framework.
 * <p>
 * 	Can be used to do the following:
 * <ul class='spaced-list'>
 * 	<li>Override the name of a property.
 * 	<li>Identify a getter or setter with a non-standard naming convention.
 * 	<li>Identify a specific subclass for a property with a general class type.
 * 	<li>Identify class types of elements in properties of type <code>Collection</code> or <code>Map</code>.
 * 	<li>Hide properties during serialization.
 * 	<li>Associate transforms with bean property values, such as a transform to convert a <code>Calendar</code> field to a string.
 * 	<li>Override the list of properties during serialization on child elements of a property of type <code>Collection</code> or <code>Map</code>.
 * 	<li>Identify a property as the URL for a bean.
 * 	<li>Identify a property as the ID for a bean.
 * </ul>
 * <p>
 * 	This annotation is applied to public fields and public getter/setter methods of beans.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Documented
@Target({FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface BeanProperty {

	/**
	 * Identifies the name of the property.
	 * <p>
	 * 	Normally, this is automatically inferred from the field name or getter method name
	 * 	of the property.  However, this property can be used to assign a different
	 * 	property name from the automatically inferred value.
	 * <p>
	 * 	If the {@link BeanContext#BEAN_beanFieldVisibility} setting on the bean context excludes this field (e.g. the visibility
	 * 	is set to PUBLIC, but the field is PROTECTED), this annotation can be used to force the field to be identified as a property.
	 */
	String name() default "";

	/**
	 * Identifies a specialized class type for the property.
	 * <p>
	 * 	Normally this can be inferred through reflection of the field type or getter return type.
	 * 	However, you'll want to specify this value if you're parsing beans where the bean property class
	 * 	is an interface or abstract class to identify the bean type to instantiate.  Otherwise, you may
	 * 	cause an {@link InstantiationException} when trying to set these fields.
	 * <p>
	 * 	This property must denote a concrete bean class with a no-arg constructor.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// Identify concrete map type.</jc>
	 * 		<ja>@BeanProperty</ja>(type=HashMap.<jk>class</jk>)
	 * 		<jk>public</jk> Map <jf>p1</jf>;
	 * 	}
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 */
	Class<?> type() default Object.class;

	/**
	 * For bean properties of maps and collections, this annotation can be used to identify
	 * the class types of the contents of the bean property object when the generic parameter
	 * types are interfaces or abstract classes.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// Identify concrete map type with String keys and Integer values.</jc>
	 * 		<ja>@BeanProperty</ja>(type=HashMap.<jk>class</jk>, params={String.<jk>class</jk>,Integer.<jk>class</jk>})
	 * 		<jk>public</jk> Map <jf>p1</jf>;
	 * 	}
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 */
	Class<?>[] params() default {};

	/**
	 * Associates an object transform with this bean property that will convert it
	 * to a different value during serialization and parsing.
	 * <p>
	 * This annotation supersedes any transform associated with the bean property type
	 * 	class itself.
	 * <p>
	 * Typically used for rendering {@link Date Dates} and {@link Calendar Calendars}
	 * 	as a particular string format.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<jk>public class</jk> MyClass {
	 *
	 * 		<jc>// During serialization, convert to ISO8601 date-time string.</jc>
	 * 		<ja>@BeanProperty</ja>(transform=CalendarTransform.ISO8601DT.<jk>class</jk>)
	 * 		<jk>public</jk> Calendar getTime();
	 * 	}
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 */
	Class<? extends PojoTransform<?,?>> transform() default PojoTransform.NULL.class;

	/**
	 * Used to limit which child properties are rendered by the serializers.
	 * <p>
	 * Can be used on any of the following bean property types:
	 * <ul class='spaced-list'>
	 * 	<li>Beans - Only render the specified properties of the bean.
	 * 	<li>Maps - Only render the specified entries in the map.
	 * 	<li>Bean/Map arrays - Same, but applied to each element in the array.
	 * 	<li>Bean/Map collections - Same, but applied to each element in the collection.
	 * </ul>
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<jk>public class</jk> MyClass {
	 *
	 * 		<jc>// Only render 'f1' when serializing this bean property.</jc>
	 * 		<ja>@BeanProperty</ja>(properties={<js>"f1"</js>})
	 * 		<jk>public</jk> MyChildClass x1 = <jk>new</jk> MyChildClass();
	 * 	}
	 *
	 * 	<jk>public class</jk> MyChildClass {
	 * 		<jk>public int</jk> f1 = 1;
	 * 		<jk>public int</jk> f2 = 2;
	 * 	}
	 *
	 * 	<jc>// Renders "{x1:{f1:1}}"</jc>
	 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jk>new</jk> MyClass());
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 */
	String[] properties() default {};

	/**
	 * Marks a bean property as a resource URI identifier for the bean.
	 * <p>
	 * Has the following effects on the following serializers:
	 * <ul class='spaced-list'>
	 * 	<li>{@link XmlSerializer} - Will be rendered as an XML attribute on the bean element, unless
	 * 		marked with a {@link Xml#format} value of {@link XmlFormat#ELEMENT}.
	 * 	<li>{@link RdfSerializer} - Will be rendered as the value of the <js>"rdf:about"</js> attribute
	 * 		for the bean.
	 * </ul>
	 */
	boolean beanUri() default false;
}

