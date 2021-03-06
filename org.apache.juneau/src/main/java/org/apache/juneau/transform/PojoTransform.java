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
package org.apache.juneau.transform;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * Used to convert non-serializable objects to a serializable form.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	<code>PojoTransforms</code> are used to extend the functionality of the serializers and parsers to be able to handle POJOs
 * 	that aren't automatically handled by the serializers or parsers.  For example, JSON does not have a standard
 * 	representation for rendering dates.  By defining a special {@code Date} transform and associating it with a serializer and
 * 	parser, you can convert a {@code Date} object to a {@code String} during serialization, and convert that {@code String} object back into
 * 	a {@code Date} object during parsing.
 * <p>
 * 	Object transforms MUST declare a public no-arg constructor so that the bean context can instantiate them.
 * <p>
 * 	<code>PojoTransforms</code> are associated with instances of {@link BeanContext BeanContexts} by passing the transform class to
 * 	the {@link CoreApi#addTransforms(Class...)} method.<br>
 * 	When associated with a bean context, fields of the specified type will automatically be converted when the
 * 	{@link BeanMap#get(Object)} or {@link BeanMap#put(String, Object)} methods are called.<br>
 * <p>
 * 	<code>PojoTransforms</code> have two parameters:
 * 	<ol>
 * 		<li>{@code <F>} - The transformed representation of an object.
 * 		<li>{@code <T>} - The normal representation of an object.
 * 	</ol>
 * 	<br>
 * 	{@link Serializer Serializers} use object transforms to convert objects of type T into objects of type F, and on calls to {@link BeanMap#get(Object)}.<br>
 * 	{@link Parser Parsers} use object transforms to convert objects of type F into objects of type T, and on calls to {@link BeanMap#put(String,Object)}.
 *
 *
 * <h6 class='topic'>Transformed Class Type {@code <T>}</h6>
 * <p>
 * 	The transformed object representation of an object must be an object type that the serializers can
 * 	natively convert to JSON (or language-specific equivalent).  The list of valid transformed types are as follows...
 * 	<ul class='spaced-list'>
 * 		<li>{@link String}
 * 		<li>{@link Number}
 * 		<li>{@link Boolean}
 * 		<li>{@link Collection} containing anything on this list.
 * 		<li>{@link Map} containing anything on this list.
 * 		<li>A java bean with properties of anything on this list.
 * 		<li>An array of anything on this list.
 * 	</ul>
 *
 *
 * <h6 class='topic'>Normal Class Type {@code <N>}</h6>
 * <p>
 * 	The normal object representation of an object.<br>
 *
 *
 * <h6 class='topic'>One-way vs. Two-way Serialization</h6>
 * <p>
 * 	Note that while there is a unified interface for handling transforming during both serialization and parsing,
 * 	in many cases only one of the {@link #transform(Object)} or {@link #normalize(Object, ClassMeta)} methods will be defined
 * 	because the transform is one-way.  For example, a transform may be defined to convert an {@code Iterator} to a {@code ObjectList}, but
 * 	it's not possible to untransform an {@code Iterator}.  In that case, the {@code generalize(Object}} method would
 * 	be implemented, but the {@code narrow(ObjectMap)} object would not, and the transform would be associated on
 * 	the serializer, but not the parser.  Also, you may choose to serialize objects like {@code Dates} to readable {@code Strings},
 * 	in which case it's not possible to reparse it back into a {@code Date}, since there is no way for the {@code Parser} to
 * 	know it's a {@code Date} from just the JSON or XML text.
 *
 *
 * <h6 class='topic'>Additional information</h6>
 * 	See {@link org.apache.juneau.transform} for more information.
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 * @param <N> The normal form of the class.
 * @param <T> The transformed form of the class.
 */
public abstract class PojoTransform<N,T> extends Transform {

	/** Represents no transform. */
	public static class NULL extends PojoTransform<Object,Object> {}

	Class<N> normalClass;
	Class<T> transformedClass;
	ClassMeta<T> transformedClassMeta;

	/**
	 * Constructor.
	 */
	@SuppressWarnings("unchecked")
	protected PojoTransform() {
		super();

		Class<?> c = this.getClass().getSuperclass();
		Type t = this.getClass().getGenericSuperclass();
		while (c != PojoTransform.class) {
			t = c.getGenericSuperclass();
			c = c.getSuperclass();
		}

		// Attempt to determine the T and G classes using reflection.
		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)t;
			Type[] pta = pt.getActualTypeArguments();
			if (pta.length == 2) {
				Type nType = pta[0];
				if (nType instanceof Class) {
					this.normalClass = (Class<N>)nType;

				// <byte[],x> ends up containing a GenericArrayType, so it has to
				// be handled as a special case.
				} else if (nType instanceof GenericArrayType) {
					Class<?> cmpntType = (Class<?>)((GenericArrayType)nType).getGenericComponentType();
					this.normalClass = (Class<N>)Array.newInstance(cmpntType, 0).getClass();

				// <Class<?>,x> ends up containing a ParameterizedType, so just use the raw type.
				} else if (nType instanceof ParameterizedType) {
					this.normalClass = (Class<N>)((ParameterizedType)nType).getRawType();

				} else
					throw new RuntimeException("Unsupported parameter type: " + nType);
				if (pta[1] instanceof Class)
					this.transformedClass = (Class<T>)pta[1];
				else if (pta[1] instanceof ParameterizedType)
					this.transformedClass = (Class<T>)((ParameterizedType)pta[1]).getRawType();
				else
					throw new RuntimeException("Unexpected transformed class type: " + pta[1].getClass().getName());
			}
		}
	}

	/**
	 * Constructor for when the normal and transformed classes are already known.
	 *
	 * @param normalClass The normal class (cannot be serialized).
	 * @param transformedClass The transformed class (serializable).
	 */
	protected PojoTransform(Class<N> normalClass, Class<T> transformedClass) {
		this.normalClass = normalClass;
		this.transformedClass = transformedClass;
	}

	/**
	 * If this transform is to be used to serialize non-serializable POJOs, it must implement this method.
	 * <p>
	 * 	The object must be converted into one of the following serializable types:
	 * 	<ul class='spaced-list'>
	 * 		<li>{@link String}
	 * 		<li>{@link Number}
	 * 		<li>{@link Boolean}
	 * 		<li>{@link Collection} containing anything on this list.
	 * 		<li>{@link Map} containing anything on this list.
	 * 		<li>A java bean with properties of anything on this list.
	 * 		<li>An array of anything on this list.
	 * 	</ul>
	 *
	 * @param o The object to be transformed.
	 * @return The transformed object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public T transform(N o) throws SerializeException {
		throw new SerializeException("Generalize method not implemented on transform ''{0}''", this.getClass().getName());
	}

	/**
	 * If this transform is to be used to reconstitute POJOs that aren't true Java beans, it must implement this method.
	 *
	 * @param f The transformed object.
	 * @param hint If possible, the parser will try to tell you the object type being created.  For example,
	 * 	on a serialized date, this may tell you that the object being created must be of type {@code GregorianCalendar}.<br>
	 * 	This may be <jk>null</jk> if the parser cannot make this determination.
	 * @return The narrowed object.
	 * @throws ParseException If this method is not implemented.
	 */
	public N normalize(T f, ClassMeta<?> hint) throws ParseException {
		throw new ParseException("Narrow method not implemented on transform ''{0}''", this.getClass().getName());
	}

	/**
	 * Returns the T class, the normalized form of the class.
	 *
	 * @return The normal form of this class.
	 */
	public Class<N> getNormalClass() {
		return normalClass;
	}

	/**
	 * Returns the G class, the generialized form of the class.
	 * <p>
	 * 	Subclasses must override this method if the generialized class is {@code Object},
	 * 	meaning it can produce multiple generialized forms.
	 *
	 * @return The transformed form of this class.
	 */
	public Class<T> getTransformedClass() {
		return transformedClass;
	}

	/**
	 * Returns the {@link ClassMeta} of the transformed class type.
	 * This value is cached for quick lookup.
	 *
	 * @return The {@link ClassMeta} of the transformed class type.
	 */
	public ClassMeta<T> getTransformedClassMeta() {
		if (transformedClassMeta == null)
			transformedClassMeta = beanContext.getClassMeta(transformedClass);
		return transformedClassMeta;
	}

	/**
	 * Checks if the specified object is an instance of the normal class defined on this transform.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the specified object is a subclass of the normal class defined on this transform.
	 * 	<jk>null</jk> always return <jk>false</jk>.
	 */
	public boolean isNormalObject(Object o) {
		if (o == null)
			return false;
		return ClassUtils.isParentClass(normalClass, o.getClass());
	}

	/**
	 * Checks if the specified object is an instance of the transformed class defined on this transform.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the specified object is a subclass of the transformed class defined on this transform.
	 * 	<jk>null</jk> always return <jk>false</jk>.
	 */
	public boolean isTransformedObject(Object o) {
		if (o == null)
			return false;
		return ClassUtils.isParentClass(transformedClass, o.getClass());
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Transform */
	public Class<?> forClass() {
		return normalClass;
	}

	@Override /* Object */
	public String toString() {
		return getClass().getSimpleName() + '<' + getNormalClass().getSimpleName() + "," + getTransformedClass().getSimpleName() + '>';
	}
}
