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
package org.apache.juneau.urlencoding;

import static org.apache.juneau.serializer.SerializerContext.*;
import static org.apache.juneau.urlencoding.UonSerializerContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Serializes POJO models to UON (a notation for URL-encoded query parameter values).
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/uon</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/uon</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	This serializer provides several serialization options.  Typically, one of the predefined DEFAULT serializers will be sufficient.
 * 	However, custom serializers can be constructed to fine-tune behavior.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link UonSerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 * <p>
 * 	The following shows a sample object defined in Javascript:
 * </p>
 * <p class='bcode'>
 * 	{
 * 		id: 1,
 * 		name: <js>'John Smith'</js>,
 * 		uri: <js>'http://sample/addressBook/person/1'</js>,
 * 		addressBookUri: <js>'http://sample/addressBook'</js>,
 * 		birthDate: <js>'1946-08-12T00:00:00Z'</js>,
 * 		otherIds: <jk>null</jk>,
 * 		addresses: [
 * 			{
 * 				uri: <js>'http://sample/addressBook/address/1'</js>,
 * 				personUri: <js>'http://sample/addressBook/person/1'</js>,
 * 				id: 1,
 * 				street: <js>'100 Main Street'</js>,
 * 				city: <js>'Anywhereville'</js>,
 * 				state: <js>'NY'</js>,
 * 				zip: 12345,
 * 				isCurrent: <jk>true</jk>,
 * 			}
 * 		]
 * 	}
 * </p>
 * <p>
 * 	Using the "strict" syntax defined in this document, the equivalent
 * 		UON notation would be as follows:
 * </p>
 * <p class='bcode'>
 * 	$o(
 * 		<xa>id</xa>=$n(<xs>1</xs>),
 * 		<xa>name</xa>=<xs>John+Smith</xs>,
 * 		<xa>uri</xa>=<xs>http://sample/addressBook/person/1</xs>,
 * 		<xa>addressBookUri</xa>=<xs>http://sample/addressBook</xs>,
 * 		<xa>birthDate</xa>=<xs>1946-08-12T00:00:00Z</xs>,
 * 		<xa>otherIds</xa>=<xs>%00</xs>,
 * 		<xa>addresses</xa>=$a(
 * 			$o(
 * 				<xa>uri</xa>=<xs>http://sample/addressBook/address/1</xs>,
 * 				<xa>personUri</xa>=<xs>http://sample/addressBook/person/1</xs>,
 * 				<xa>id</xa>=$n(<xs>1</xs>),
 * 				<xa>street</xa>=<xs>100+Main+Street</xs>,
 * 				<xa>city</xa>=<xs>Anywhereville</xs>,
 * 				<xa>state</xa>=<xs>NY</xs>,
 * 				<xa>zip</xa>=$n(<xs>12345</xs>),
 * 				<xa>isCurrent</xa>=$b(<xs>true</xs>)
 * 			)
 * 		)
 * 	)
 * </p>
 * <p>
 * 	A secondary "lax" syntax is available when the data type of the
 * 		values are already known on the receiving end of the transmission:
 * </p>
 * <p class='bcode'>
 * 	(
 * 		<xa>id</xa>=<xs>1</xs>,
 * 		<xa>name</xa>=<xs>John+Smith</xs>,
 * 		<xa>uri</xa>=<xs>http://sample/addressBook/person/1</xs>,
 * 		<xa>addressBookUri</xa>=<xs>http://sample/addressBook</xs>,
 * 		<xa>birthDate</xa>=<xs>1946-08-12T00:00:00Z</xs>,
 * 		<xa>otherIds</xa>=<xs>%00</xs>,
 * 		<xa>addresses</xa>=(
 * 			(
 * 				<xa>uri</xa>=<xs>http://sample/addressBook/address/1</xs>,
 * 				<xa>personUri</xa>=<xs>http://sample/addressBook/person/1</xs>,
 * 				<xa>id</xa>=<xs>1</xs>,
 * 				<xa>street</xa>=<xs>100+Main+Street</xs>,
 * 				<xa>city</xa>=<xs>Anywhereville</xs>,
 * 				<xa>state</xa>=<xs>NY</xs>,
 * 				<xa>zip</xa>=<xs>12345</xs>,
 * 				<xa>isCurrent</xa>=<xs>true</xs>
 * 			)
 * 		)
 * 	)
 * </p>
 *
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 * 	<jc>// Serialize a Map</jc>
 * 	Map m = <jk>new</jk> ObjectMap(<js>"{a:'b',c:1,d:false,e:['f',1,false],g:{h:'i'}}"</js>);
 *
 * 	<jc>// Serialize to value equivalent to JSON.</jc>
 * 	<jc>// Produces "$o(a=b,c=$n(1),d=$b(false),e=$a(f,$n(1),$b(false)),g=$o(h=i))"</jc>
 * 	String s = UonSerializer.<jsf>DEFAULT</jsf>.serialize(s);
 *
 * 	<jc>// Serialize to simplified value (for when data type is already known by receiver).</jc>
 * 	<jc>// Produces "(a=b,c=1,d=false,e=(f,1,false),g=(h=i))"</jc>
 * 	String s = UonSerializer.<jsf>DEFAULT_SIMPLE</jsf>.serialize(s);
 *
 * 	<jc>// Serialize a bean</jc>
 * 	<jk>public class</jk> Person {
 * 		<jk>public</jk> Person(String s);
 * 		<jk>public</jk> String getName();
 * 		<jk>public int</jk> getAge();
 * 		<jk>public</jk> Address getAddress();
 * 		<jk>public boolean</jk> deceased;
 * 	}
 *
 * 	<jk>public class</jk> Address {
 * 		<jk>public</jk> String getStreet();
 * 		<jk>public</jk> String getCity();
 * 		<jk>public</jk> String getState();
 * 		<jk>public int</jk> getZip();
 * 	}
 *
 * 	Person p = <jk>new</jk> Person(<js>"John Doe"</js>, 23, <js>"123 Main St"</js>, <js>"Anywhere"</js>, <js>"NY"</js>, 12345, <jk>false</jk>);
 *
 * 	<jc>// Produces "$o(name=John Doe,age=23,address=$o(street=123 Main St,city=Anywhere,state=NY,zip=$n(12345)),deceased=$b(false))"</jc>
 * 	String s = UonSerializer.<jsf>DEFAULT</jsf>.serialize(s);
 *
 * 	<jc>// Produces "(name=John Doe,age=23,address=(street=123 Main St,city=Anywhere,state=NY,zip=12345),deceased=false)"</jc>
 * 	String s = UonSerializer.<jsf>DEFAULT_SIMPLE</jsf>.serialize(s);
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Produces("text/uon")
public class UonSerializer extends WriterSerializer {

	/** Reusable instance of {@link UonSerializer}, all default settings. */
	public static final UonSerializer DEFAULT = new UonSerializer().lock();

	/** Reusable instance of {@link UonSerializer.Simple}. */
	public static final UonSerializer DEFAULT_SIMPLE = new Simple().lock();

	/** Reusable instance of {@link UonSerializer.Readable}. */
	public static final UonSerializer DEFAULT_READABLE = new Readable().lock();

	/** Reusable instance of {@link UonSerializer.Encoding}. */
	public static final UonSerializer DEFAULT_ENCODING = new Encoding().lock();

	/** Reusable instance of {@link UonSerializer.SimpleEncoding}. */
	public static final UonSerializer DEFAULT_SIMPLE_ENCODING = new SimpleEncoding().lock();

	/**
	 * Equivalent to <code><jk>new</jk> UonSerializer().setProperty(UonSerializerContext.<jsf>UON_simpleMode</jsf>,<jk>true</jk>);</code>.
	 */
	@Produces(value={"text/uon-simple"},contentType="text/uon")
	public static class Simple extends UonSerializer {
		/** Constructor */
		public Simple() {
			setProperty(UON_simpleMode, true);
		}
	}

	/**
	 * Equivalent to <code><jk>new</jk> UonSerializer().setProperty(UonSerializerContext.<jsf>UON_useWhitespace</jsf>,<jk>true</jk>);</code>.
	 */
	public static class Readable extends UonSerializer {
		/** Constructor */
		public Readable() {
			setProperty(UON_useWhitespace, true);
			setProperty(SERIALIZER_useIndentation, true);
		}
	}

	/**
	 * Equivalent to <code><jk>new</jk> UonSerializer().setProperty(UonSerializerContext.<jsf>UON_encodeChars</jsf>,<jk>true</jk>);</code>.
	 */
	public static class Encoding extends UonSerializer {
		/** Constructor */
		public Encoding() {
			setProperty(UON_encodeChars, true);
		}
	}

	/**
	 * Equivalent to <code><jk>new</jk> UonSerializer().setProperty(UonSerializerContext.<jsf>UON_simpleMode</jsf>,<jk>true</jk>).setProperty(UonSerializerContext.<jsf>UON_encodeChars</jsf>,<jk>true</jk>);</code>.
	 */
	@Produces(value={"text/uon-simple"},contentType="text/uon")
	public static class SimpleEncoding extends UonSerializer {
		/** Constructor */
		public SimpleEncoding() {
			setProperty(UON_simpleMode, true);
			setProperty(UON_encodeChars, true);
		}
	}


	/**
	 * Workhorse method. Determines the type of object, and then calls the
	 * appropriate type-specific serialization method.
	 * @param session The context that exist for the duration of a serialize.
	 * @param out The writer to serialize to.
	 * @param o The object being serialized.
	 * @param eType The expected type of the object if this is a bean property.
	 * @param attrName The bean property name if this is a bean property.  <jk>null</jk> if this isn't a bean property being serialized.
	 * @param pMeta The bean property metadata.
	 * @param quoteEmptyStrings <jk>true</jk> if this is the first entry in an array.
	 * @param isTop If we haven't recursively called this method.
	 *
	 * @return The same writer passed in.
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected SerializerWriter serializeAnything(UonSerializerSession session, UonWriter out, Object o, ClassMeta<?> eType,
			String attrName, BeanPropertyMeta pMeta, boolean quoteEmptyStrings, boolean isTop) throws Exception {
		BeanContext bc = session.getBeanContext();

		if (o == null) {
			out.appendObject(null, false, false, isTop);
			return out;
		}

		if (eType == null)
			eType = object();

		boolean addClassAttr;		// Add "_class" attribute to element?
		ClassMeta<?> aType;			// The actual type
		ClassMeta<?> gType;			// The generic type

		aType = session.push(attrName, o, eType);
		boolean isRecursion = aType == null;

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

		gType = aType.getTransformedClassMeta();
		addClassAttr = (session.isAddClassAttrs() && ! eType.equals(aType));

		// Transform if necessary
		PojoTransform transform = aType.getPojoTransform();				// The transform
		if (transform != null) {
			o = transform.transform(o);

			// If the transform's getTransformedClass() method returns Object, we need to figure out
			// the actual type now.
			if (gType.isObject())
				gType = bc.getClassMetaForObject(o);
		}

		// '\0' characters are considered null.
		if (o == null || (gType.isChar() && ((Character)o).charValue() == 0))
			out.appendObject(null, false, false, isTop);
		else if (gType.hasToObjectMapMethod())
			serializeMap(session, out, gType.toObjectMap(o), eType);
		else if (gType.isBean())
			serializeBeanMap(session, out, bc.forBean(o), addClassAttr);
		else if (gType.isUri() || (pMeta != null && (pMeta.isUri() || pMeta.isBeanUri())))
			out.appendUri(o, isTop);
		else if (gType.isMap()) {
			if (o instanceof BeanMap)
				serializeBeanMap(session, out, (BeanMap)o, addClassAttr);
			else
				serializeMap(session, out, (Map)o, eType);
		}
		else if (gType.isCollection()) {
			if (addClassAttr)
				serializeCollectionMap(session, out, (Collection)o, gType);
			else
				serializeCollection(session, out, (Collection) o, eType);
		}
		else if (gType.isArray()) {
			if (addClassAttr)
				serializeCollectionMap(session, out, toList(gType.getInnerClass(), o), gType);
			else
				serializeCollection(session, out, toList(gType.getInnerClass(), o), eType);
		}
		else {
			out.appendObject(o, quoteEmptyStrings, false, isTop);
		}

		if (! isRecursion)
			session.pop();
		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeMap(UonSerializerSession session, UonWriter out, Map m, ClassMeta<?> type) throws Exception {

		m = session.sort(m);

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		int depth = session.getIndent();
		out.startFlag('o');

		Iterator mapEntries = m.entrySet().iterator();

		while (mapEntries.hasNext()) {
			Map.Entry e = (Map.Entry) mapEntries.next();
			Object value = e.getValue();
			Object key = session.generalize(e.getKey(), keyType);
			out.cr(depth).appendObject(key, session.isUseWhitespace(), false, false).append('=');
			serializeAnything(session, out, value, valueType, (key == null ? null : session.toString(key)), null, session.isUseWhitespace(), false);
			if (mapEntries.hasNext())
				out.append(',');
		}

		if (m.size() > 0)
			out.cr(depth-1);
		out.append(')');

		return out;
	}

	@SuppressWarnings({ "rawtypes" })
	private SerializerWriter serializeCollectionMap(UonSerializerSession session, UonWriter out, Collection o, ClassMeta<?> type) throws Exception {
		int i = session.getIndent();
		out.startFlag('o').nl();
		out.append(i, "_class=").appendObject(type, false, false, false).append(',').nl();
		out.append(i, "items=");
		session.indent++;
		serializeCollection(session, out, o, type);
		session.indent--;

		if (o.size() > 0)
			out.cr(i-1);
		out.append(')');

		return out;
	}

	@SuppressWarnings({ "rawtypes" })
	private SerializerWriter serializeBeanMap(UonSerializerSession session, UonWriter out, BeanMap m, boolean addClassAttr) throws Exception {
		int depth = session.getIndent();

		out.startFlag('o');

		Iterator mapEntries = m.entrySet(session.isTrimNulls()).iterator();

		// Print out "_class" attribute on this bean if required.
		if (addClassAttr) {
			String attr = "_class";
			out.cr(depth).appendObject(attr, false, false, false).append('=').append(m.getClassMeta().getInnerClass().getName());
			if (mapEntries.hasNext())
				out.append(',');
		}

		boolean addComma = false;

		while (mapEntries.hasNext()) {
			BeanMapEntry p = (BeanMapEntry)mapEntries.next();
			BeanPropertyMeta pMeta = p.getMeta();

			String key = p.getKey();
			Object value = null;
			try {
				value = p.getValue();
			} catch (StackOverflowError e) {
				throw e;
			} catch (Throwable t) {
				session.addBeanGetterWarning(pMeta, t);
			}

			if (session.canIgnoreValue(pMeta.getClassMeta(), key, value))
				continue;

			if (addComma)
				out.append(',');

			out.cr(depth).appendObject(key, false, false, false).append('=');

			serializeAnything(session, out, value, pMeta.getClassMeta(), key, pMeta, false, false);

			addComma = true;
		}

		if (m.size() > 0)
			out.cr(depth-1);
		out.append(')');

		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeCollection(UonSerializerSession session, UonWriter out, Collection c, ClassMeta<?> type) throws Exception {

		ClassMeta<?> elementType = type.getElementType();

		c = session.sort(c);

		out.startFlag('a');

		int depth = session.getIndent();
		boolean quoteEmptyString = (c.size() == 1 || session.isUseWhitespace());

		for (Iterator i = c.iterator(); i.hasNext();) {
			out.cr(depth);
			serializeAnything(session, out, i.next(), elementType, "<iterator>", null, quoteEmptyString, false);
			if (i.hasNext())
				out.append(',');
		}

		if (c.size() > 0)
			out.cr(depth-1);
		out.append(')');

		return out;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public UonSerializerSession createSession(Object output, ObjectMap properties, Method javaMethod) {
		return new UonSerializerSession(getContext(UonSerializerContext.class), getBeanContext(), output, properties, javaMethod);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		UonSerializerSession s = (UonSerializerSession)session;
		serializeAnything(s, s.getWriter(), o, null, "root", null, false, true);
	}

	@Override /* CoreApi */
	public UonSerializer setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public UonSerializer setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public UonSerializer addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public UonSerializer addTransforms(Class<?>...classes) throws LockedException {
		super.addTransforms(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> UonSerializer addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public UonSerializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public UonSerializer lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public UonSerializer clone() {
		try {
			UonSerializer c = (UonSerializer)super.clone();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
