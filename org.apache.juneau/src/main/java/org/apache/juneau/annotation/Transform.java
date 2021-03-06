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

import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Annotation that can be applied to a class to associate a transform with it.
 * <p>
 * 	Typically used to associate {@link PojoTransform PojoTransforms} with classes using annotations
 * 		instead of programatically through a method such as {@link Serializer#addTransforms(Class...)}.
 *
 * <h6 class='topic'>Example</h6>
 * <p>
 * 	In this case, a transform is being applied to a bean that will force it to be serialized as a <code>String</code>
 * <p class='bcode'>
 * 	<jc>// Our bean class</jc>
 * 	<ja>@Transform</ja>(BTransform.<jk>class</jk>)
 * 	<jk>public class</jk> B {
 * 		<jk>public</jk> String <jf>f1</jf>;
 * 	}
 *
 * 	<jc>// Our transform to force the bean to be serialized as a String</jc>
 * 	<jk>public class</jk> BTransform <jk>extends</jk> PojoTransform&lt;B,String&gt; {
 * 		<jk>public</jk> String transform(B o) <jk>throws</jk> SerializeException {
 * 			<jk>return</jk> o.f1;
 * 		}
 * 		<jk>public</jk> B normalize(String f, ClassMeta&lt;?&gt; hint) <jk>throws</jk> ParseException {
 * 			B b1 = <jk>new</jk> B();
 * 			b1.<jf>f1</jf> = f;
 * 			<jk>return</jk> b1;
 * 		}
 * 	}
 *
 * 	<jk>public void</jk> testTransform() <jk>throws</jk> Exception {
 * 		WriterSerializer s = JsonSerializer.<jsf>DEFAULT</jsf>;
 * 		B b = <jk>new</jk> B();
 * 		b.<jf>f1</jf> = <js>"bar"</js>;
 * 		String json = s.serialize(b);
 * 		<jsm>assertEquals</jsm>(<js>"'bar'"</js>, json);
 *
 * 		ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
 * 		b = p.parse(json, B.<jk>class</jk>);
 * 		<jsm>assertEquals</jsm>(<js>"bar"</js>, t.<jf>f1</jf>);
 * 	}
 * </p>
 * <p>
 * 	Note that using this annotation is functionally equivalent to adding transforms to the serializers and parsers:
 * <p class='bcode'>
 * 	WriterSerializer s = <jk>new</jk> JsonSerializer.addTransforms(BTransform.<jk>class</jk>);
 * 	ReaderParser p = <jk>new</jk> JsonParser.addTransforms(BTransform.<jk>class</jk>);
 * </p>
 * <p>
 * 	It is technically possible to associate a {@link BeanTransform} with a bean class using this annotation.
 * 	However in practice, it's almost always less code to simply use the {@link Bean @Bean} annotation.
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface Transform {

	/**
	 * The transform class.
	 */
	Class<? extends org.apache.juneau.transform.Transform> value() default org.apache.juneau.transform.Transform.NULL.class;
}