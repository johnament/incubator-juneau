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
package org.apache.juneau.json;

import static org.apache.juneau.json.JsonSerializerContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Serializes POJO models to JSON.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>application/json, text/json</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>application/json</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	The conversion is as follows...
 * 	<ul class='spaced-list'>
 * 		<li>Maps (e.g. {@link HashMap HashMaps}, {@link TreeMap TreeMaps}) are converted to JSON objects.
 * 		<li>Collections (e.g. {@link HashSet HashSets}, {@link LinkedList LinkedLists}) and Java arrays are converted to JSON arrays.
 * 		<li>{@link String Strings} are converted to JSON strings.
 * 		<li>{@link Number Numbers} (e.g. {@link Integer}, {@link Long}, {@link Double}) are converted to JSON numbers.
 * 		<li>{@link Boolean Booleans} are converted to JSON booleans.
 * 		<li>{@code nulls} are converted to JSON nulls.
 * 		<li>{@code arrays} are converted to JSON arrays.
 * 		<li>{@code beans} are converted to JSON objects.
 * 	</ul>
 * <p>
 * 	The types above are considered "JSON-primitive" object types.  Any non-JSON-primitive object types are transformed
 * 		into JSON-primitive object types through {@link org.apache.juneau.transform.Transform Transforms} associated through the {@link CoreApi#addTransforms(Class...)}
 * 		method.  Several default transforms are provided for transforming Dates, Enums, Iterators, etc...
 * <p>
 * 	This serializer provides several serialization options.  Typically, one of the predefined DEFAULT serializers will be sufficient.
 * 	However, custom serializers can be constructed to fine-tune behavior.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link JsonSerializerContext}
 * 	<li>{@link SerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 *
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * 	The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>{@link Simple} - Default serializer, single quotes, simple mode.
 * 	<li>{@link SimpleReadable} - Default serializer, single quotes, simple mode, with whitespace.
 * </ul>
 *
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(someObject);
 *
 * 	<jc>// Create a custom serializer for lax syntax using single quote characters</jc>
 * 	JsonSerializer serializer = <jk>new</jk> JsonSerializer()
 * 		.setProperty(JsonSerializerContext.<jsf>JSON_simpleMode</jsf>, <jk>true</jk>)
 * 		.setProperty(SerializerContext.<jsf>SERIALIZER_quoteChar</jsf>, <js>'\''</js>);
 *
 * 	<jc>// Clone an existing serializer and modify it to use single-quotes</jc>
 * 	JsonSerializer serializer = JsonSerializer.<jsf>DEFAULT</jsf>.clone()
 * 		.setProperty(SerializerContext.<jsf>SERIALIZER_quoteChar</jsf>, <js>'\''</js>);
 *
 * 	<jc>// Serialize a POJO to JSON</jc>
 * 	String json = serializer.serialize(someObject);
 * </p>
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Produces({"application/json","text/json"})
public class JsonSerializer extends WriterSerializer {

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT = new JsonSerializer().lock();

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT_READABLE = new Readable().lock();

	/** Default serializer, single quotes, simple mode. */
	public static final JsonSerializer DEFAULT_LAX = new Simple().lock();

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static final JsonSerializer DEFAULT_LAX_READABLE = new SimpleReadable().lock();

	/**
	 * Default serializer, single quotes, simple mode, with whitespace and recursion detection.
	 * Note that recursion detection introduces a small performance penalty.
	 */
	public static final JsonSerializer DEFAULT_LAX_READABLE_SAFE = new SimpleReadableSafe().lock();

	/** Default serializer, with whitespace. */
	public static class Readable extends JsonSerializer {
		/** Constructor */
		public Readable() {
			setProperty(JSON_useWhitespace, true);
			setProperty(SERIALIZER_useIndentation, true);
		}
	}

	/** Default serializer, single quotes, simple mode. */
	@Produces(value={"application/json+simple","text/json+simple"},contentType="application/json")
	public static class Simple extends JsonSerializer {
		/** Constructor */
		public Simple() {
			setProperty(JSON_simpleMode, true);
			setProperty(SERIALIZER_quoteChar, '\'');
		}
	}

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static class SimpleReadable extends Simple {
		/** Constructor */
		public SimpleReadable() {
			setProperty(JSON_useWhitespace, true);
			setProperty(SERIALIZER_useIndentation, true);
		}
	}

	/**
	 * Default serializer, single quotes, simple mode, with whitespace and recursion detection.
	 * Note that recursion detection introduces a small performance penalty.
	 */
	public static class SimpleReadableSafe extends SimpleReadable {
		/** Constructor */
		public SimpleReadableSafe() {
			setProperty(SERIALIZER_detectRecursions, true);
		}
	}

	/**
	 * Workhorse method. Determines the type of object, and then calls the
	 * appropriate type-specific serialization method.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	SerializerWriter serializeAnything(JsonSerializerSession session, JsonWriter out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws Exception {
		BeanContext bc = session.getBeanContext();

		if (o == null) {
			out.append("null");
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

		String wrapperAttr = gType.getJsonMeta().getWrapperAttr();
		if (wrapperAttr != null) {
			out.append('{').cr(session.indent).attr(wrapperAttr).append(':').s();
			session.indent++;
		}

		// '\0' characters are considered null.
		if (o == null || (gType.isChar() && ((Character)o).charValue() == 0))
			out.append("null");
		else if (gType.isNumber() || gType.isBoolean())
			out.append(o);
		else if (gType.hasToObjectMapMethod())
			serializeMap(session, out, gType.toObjectMap(o), gType);
		else if (gType.isBean())
			serializeBeanMap(session, out, bc.forBean(o), addClassAttr);
		else if (gType.isUri() || (pMeta != null && (pMeta.isUri() || pMeta.isBeanUri())))
			out.q().appendUri(o).q();
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
		else
			out.stringValue(session.toString(o));

		if (wrapperAttr != null) {
			session.indent--;
			out.cr(session.indent-1).append('}');
		}

		if (! isRecursion)
			session.pop();
		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeMap(JsonSerializerSession session, JsonWriter out, Map m, ClassMeta<?> type) throws Exception {

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		m = session.sort(m);

		int depth = session.getIndent();
		out.append('{');

		Iterator mapEntries = m.entrySet().iterator();

		while (mapEntries.hasNext()) {
			Map.Entry e = (Map.Entry) mapEntries.next();
			Object value = e.getValue();

			Object key = session.generalize(e.getKey(), keyType);

			out.cr(depth).attr(session.toString(key)).append(':').s();

			serializeAnything(session, out, value, valueType, (key == null ? null : session.toString(key)), null);

			if (mapEntries.hasNext())
				out.append(',').s();
		}

		out.cr(depth-1).append('}');

		return out;
	}

	@SuppressWarnings({ "rawtypes" })
	private SerializerWriter serializeCollectionMap(JsonSerializerSession session, JsonWriter out, Collection o, ClassMeta<?> type) throws Exception {
		int i = session.getIndent();
		out.append('{');
		out.cr(i).attr("_class").append(':').s().q().append(type.getInnerClass().getName()).q().append(',').s();
		out.cr(i).attr("items").append(':').s();
		session.indent++;
		serializeCollection(session, out, o, type);
		session.indent--;
		out.cr(i-1).append('}');
		return out;
	}

	@SuppressWarnings({ "rawtypes" })
	private SerializerWriter serializeBeanMap(JsonSerializerSession session, JsonWriter out, BeanMap m, boolean addClassAttr) throws Exception {
		int depth = session.getIndent();
		out.append('{');

		Iterator mapEntries = m.entrySet(session.isTrimNulls()).iterator();

		// Print out "_class" attribute on this bean if required.
		if (addClassAttr) {
			String attr = "_class";
			out.cr(depth).attr(attr).append(':').s().q().append(m.getClassMeta().getInnerClass().getName()).q();
			if (mapEntries.hasNext())
				out.append(',').s();
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
				out.append(',').s();

			out.cr(depth).attr(key).append(':').s();

			serializeAnything(session, out, value, pMeta.getClassMeta(), key, pMeta);

			addComma = true;
		}
		out.cr(depth-1).append('}');
		return out;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private SerializerWriter serializeCollection(JsonSerializerSession session, JsonWriter out, Collection c, ClassMeta<?> type) throws Exception {

		ClassMeta<?> elementType = type.getElementType();

		c = session.sort(c);

		out.append('[');
		int depth = session.getIndent();

		for (Iterator i = c.iterator(); i.hasNext();) {

			Object value = i.next();

			out.cr(depth);

			serializeAnything(session, out, value, elementType, "<iterator>", null);

			if (i.hasNext())
				out.append(',').s();
		}
		out.cr(depth-1).append(']');
		return out;
	}

	/**
	 * Returns the schema serializer based on the settings of this serializer.
	 * @return The schema serializer.
	 */
	public JsonSchemaSerializer getSchemaSerializer() {
		JsonSchemaSerializer s = new JsonSchemaSerializer(getContextFactory());
		return s;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public JsonSerializerSession createSession(Object output, ObjectMap properties, Method javaMethod) {
		return new JsonSerializerSession(getContext(JsonSerializerContext.class), getBeanContext(), output, properties, javaMethod);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		JsonSerializerSession s = (JsonSerializerSession)session;
		serializeAnything(s, s.getWriter(), o, null, "root", null);
	}

	@Override /* CoreApi */
	public JsonSerializer setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addTransforms(Class<?>...classes) throws LockedException {
		super.addTransforms(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> JsonSerializer addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public JsonSerializer lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public JsonSerializer clone() {
		try {
			JsonSerializer c = (JsonSerializer)super.clone();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
