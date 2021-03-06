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
package org.apache.juneau.html;

import static org.apache.juneau.serializer.SerializerContext.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Serializes POJO models to HTML.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/html</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/html</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	The conversion is as follows...
 * 	<ul class='spaced-list'>
 * 		<li>{@link Map Maps} (e.g. {@link HashMap}, {@link TreeMap}) and beans are converted to HTML tables with 'key' and 'value' columns.
 * 		<li>{@link Collection Collections} (e.g. {@link HashSet}, {@link LinkedList}) and Java arrays are converted to HTML ordered lists.
 * 		<li>{@code Collections} of {@code Maps} and beans are converted to HTML tables with keys as headers.
 * 		<li>Everything else is converted to text.
 * 	</ul>
 * <p>
 * 	This serializer provides several serialization options.  Typically, one of the predefined <jsf>DEFAULT</jsf> serializers will be sufficient.
 * 	However, custom serializers can be constructed to fine-tune behavior.
 * <p>
 * 	The {@link HtmlLink} annotation can be used on beans to add hyperlinks to the output.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul class='spaced-list'>
 * 	<li>{@link HtmlSerializerContext}
 * </ul>
 *
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * 	The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>{@link Sq} - Default serializer, single quotes.
 * 	<li>{@link SqReadable} - Default serializer, single quotes, whitespace added.
 * </ul>
 *
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 		String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(someObject);
 *
 * 		<jc>// Create a custom serializer that doesn't use whitespace and newlines</jc>
 * 		HtmlSerializer serializer = <jk>new</jk> HtmlSerializer()
 * 			.setProperty(SerializerContext.<jsf>SERIALIZER_useIndentation</jsf>, <jk>false</jk>);
 *
 * 		<jc>// Same as above, except uses cloning</jc>
 * 		HtmlSerializer serializer = HtmlSerializer.<jsf>DEFAULT</jsf>.clone()
 * 			.setProperty(SerializerContext.<jsf>SERIALIZER_useIndentation</jsf>, <jk>false</jk>);
 *
 * 		<jc>// Serialize POJOs to HTML</jc>
 *
 * 		<jc>// Produces: </jc>
 * 		<jc>// &lt;ul&gt;&lt;li&gt;1&lt;li&gt;2&lt;li&gt;3&lt;/ul&gt;</jc>
 * 		List l = new ObjectList(1, 2, 3);
 * 		String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(l);
 *
 * 		<jc>// Produces: </jc>
 * 		<jc>//    &lt;table&gt; </jc>
 * 		<jc>//       &lt;tr&gt;&lt;th&gt;firstName&lt;/th&gt;&lt;th&gt;lastName&lt;/th&gt;&lt;/tr&gt; </jc>
 * 		<jc>//       &lt;tr&gt;&lt;td&gt;Bob&lt;/td&gt;&lt;td&gt;Costas&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//       &lt;tr&gt;&lt;td&gt;Billy&lt;/td&gt;&lt;td&gt;TheKid&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//       &lt;tr&gt;&lt;td&gt;Barney&lt;/td&gt;&lt;td&gt;Miller&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//    &lt;/table&gt; </jc>
 * 		l = <jk>new</jk> ObjectList();
 * 		l.add(<jk>new</jk> ObjectMap(<js>"{firstName:'Bob',lastName:'Costas'}"</js>));
 * 		l.add(<jk>new</jk> ObjectMap(<js>"{firstName:'Billy',lastName:'TheKid'}"</js>));
 * 		l.add(<jk>new</jk> ObjectMap(<js>"{firstName:'Barney',lastName:'Miller'}"</js>));
 * 		String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(l);
 *
 * 		<jc>// Produces: </jc>
 * 		<jc>//    &lt;table&gt; </jc>
 * 		<jc>//       &lt;tr&gt;&lt;th&gt;key&lt;/th&gt;&lt;th&gt;value&lt;/th&gt;&lt;/tr&gt; </jc>
 * 		<jc>//       &lt;tr&gt;&lt;td&gt;foo&lt;/td&gt;&lt;td&gt;bar&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//       &lt;tr&gt;&lt;td&gt;baz&lt;/td&gt;&lt;td&gt;123&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//    &lt;/table&gt; </jc>
 * 		Map m = <jk>new</jk> ObjectMap(<js>"{foo:'bar',baz:123}"</js>);
 * 		String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(m);
 *
 * 		<jc>// HTML elements can be nested arbitrarily deep</jc>
 * 		<jc>// Produces: </jc>
 * 		<jc>//	&lt;table&gt; </jc>
 * 		<jc>//		&lt;tr&gt;&lt;th&gt;key&lt;/th&gt;&lt;th&gt;value&lt;/th&gt;&lt;/tr&gt; </jc>
 * 		<jc>//		&lt;tr&gt;&lt;td&gt;foo&lt;/td&gt;&lt;td&gt;bar&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//		&lt;tr&gt;&lt;td&gt;baz&lt;/td&gt;&lt;td&gt;123&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//		&lt;tr&gt;&lt;td&gt;someNumbers&lt;/td&gt;&lt;td&gt;&lt;ul&gt;&lt;li&gt;1&lt;li&gt;2&lt;li&gt;3&lt;/ul&gt;&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//		&lt;tr&gt;&lt;td&gt;someSubMap&lt;/td&gt;&lt;td&gt; </jc>
 * 		<jc>//			&lt;table&gt; </jc>
 * 		<jc>//				&lt;tr&gt;&lt;th&gt;key&lt;/th&gt;&lt;th&gt;value&lt;/th&gt;&lt;/tr&gt; </jc>
 * 		<jc>//				&lt;tr&gt;&lt;td&gt;a&lt;/td&gt;&lt;td&gt;b&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//			&lt;/table&gt; </jc>
 * 		<jc>//		&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//	&lt;/table&gt; </jc>
 * 		Map m = <jk>new</jk> ObjectMap(<js>"{foo:'bar',baz:123}"</js>);
 * 		m.put("someNumbers", new ObjectList(1, 2, 3));
 * 		m.put(<js>"someSubMap"</js>, new ObjectMap(<js>"{a:'b'}"</js>));
 * 		String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(m);
 * </p>
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Produces("text/html")
@SuppressWarnings("hiding")
public class HtmlSerializer extends XmlSerializer {

	/** Default serializer, all default settings. */
	public static final HtmlSerializer DEFAULT = new HtmlSerializer().lock();

	/** Default serializer, single quotes. */
	public static final HtmlSerializer DEFAULT_SQ = new HtmlSerializer.Sq().lock();

	/** Default serializer, single quotes, whitespace added. */
	public static final HtmlSerializer DEFAULT_SQ_READABLE = new HtmlSerializer.SqReadable().lock();

	/** Default serializer, single quotes. */
	public static class Sq extends HtmlSerializer {
		/** Constructor */
		public Sq() {
			setProperty(SERIALIZER_quoteChar, '\'');
		}
	}

	/** Default serializer, single quotes, whitespace added. */
	public static class SqReadable extends Sq {
		/** Constructor */
		public SqReadable() {
			setProperty(SERIALIZER_useIndentation, true);
		}
	}

	/**
	 * Main serialization routine.
	 * @param session The serialization context object.
	 * @param o The object being serialized.
	 * @param w The writer to serialize to.
	 *
	 * @return The same writer passed in.
	 * @throws IOException If a problem occurred trying to send output to the writer.
	 */
	private HtmlWriter doSerialize(HtmlSerializerSession session, Object o, HtmlWriter w) throws Exception {
		serializeAnything(session, w, o, null, null, session.getInitialDepth()-1, null);
		return w;
	}

	/**
	 * Serialize the specified object to the specified writer.
	 *
	 * @param session The context object that lives for the duration of this serialization.
	 * @param out The writer.
	 * @param o The object to serialize.
	 * @param eType The expected type of the object if this is a bean property.
	 * @param name The attribute name of this object if this object was a field in a JSON object (i.e. key of a {@link java.util.Map.Entry} or property name of a bean).
	 * @param indent The current indentation value.
	 * @param pMeta The bean property being serialized, or <jk>null</jk> if we're not serializing a bean property.
	 *
	 * @throws Exception If a problem occurred trying to convert the output.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void serializeAnything(HtmlSerializerSession session, HtmlWriter out, Object o, ClassMeta<?> eType, String name, int indent, BeanPropertyMeta pMeta) throws Exception {

		BeanContext bc = session.getBeanContext();
		ClassMeta<?> aType = null;       // The actual type
		ClassMeta<?> gType = object();   // The generic type

		if (eType == null)
			eType = object();

		aType = session.push(name, o, eType);

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

		session.indent += indent;
		int i = session.indent;

		// Determine the type.
		if (o == null || (aType.isChar() && ((Character)o).charValue() == 0))
			out.tag(i, "null").nl();
		else {

			gType = aType.getTransformedClassMeta();
			String classAttr = null;
			if (session.isAddClassAttrs() && ! eType.equals(aType))
				classAttr = aType.toString();

			// Transform if necessary
			PojoTransform transform = aType.getPojoTransform();
			if (transform != null) {
				o = transform.transform(o);

				// If the transforms getTransformedClass() method returns Object, we need to figure out
				// the actual type now.
				if (gType.isObject())
					gType = bc.getClassMetaForObject(o);
			}

			HtmlClassMeta html = gType.getHtmlMeta();

			if (html.isAsXml() || (pMeta != null && pMeta.getHtmlMeta().isAsXml()))
				super.serializeAnything(session, out, o, null, null, null, false, XmlFormat.NORMAL, null);
			else if (html.isAsPlainText() || (pMeta != null && pMeta.getHtmlMeta().isAsPlainText()))
				out.write(o == null ? "null" : o.toString());
			else if (o == null || (gType.isChar() && ((Character)o).charValue() == 0))
				out.tag(i, "null").nl();
			else if (gType.hasToObjectMapMethod())
				serializeMap(session, out, gType.toObjectMap(o), eType, classAttr, pMeta);
			else if (gType.isBean())
				serializeBeanMap(session, out, bc.forBean(o), classAttr, pMeta);
			else if (gType.isNumber())
				out.sTag(i, "number").append(o).eTag("number").nl();
			else if (gType.isBoolean())
				out.sTag(i, "boolean").append(o).eTag("boolean").nl();
			else if (gType.isMap()) {
				if (o instanceof BeanMap)
					serializeBeanMap(session, out, (BeanMap)o, classAttr, pMeta);
				else
					serializeMap(session, out, (Map)o, eType, classAttr, pMeta);
			}
			else if (gType.isCollection()) {
				if (classAttr != null)
					serializeCollection(session, out, (Collection)o, gType, name, classAttr, pMeta);
				else
					serializeCollection(session, out, (Collection)o, eType, name, null, pMeta);
			}
			else if (gType.isArray()) {
				if (classAttr != null)
					serializeCollection(session, out, toList(gType.getInnerClass(), o), gType, name, classAttr, pMeta);
				else
					serializeCollection(session, out, toList(gType.getInnerClass(), o), eType, name, null, pMeta);
			}
			else if (session.isUri(gType, pMeta, o)) {
				String label = session.getAnchorText(pMeta, o);
				out.oTag(i, "a").attrUri("href", o).append('>');
				out.append(label);
				out.eTag("a").nl();
			}
			else
				out.sTag(i, "string").encodeText(session.toString(o)).eTag("string").nl();
		}
		session.pop();
		session.indent -= indent;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void serializeMap(HtmlSerializerSession session, HtmlWriter out, Map m, ClassMeta<?> type, String classAttr, BeanPropertyMeta<?> ppMeta) throws Exception {
		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();
		ClassMeta<?> aType = session.getBeanContext().getClassMetaForObject(m);       // The actual type

		int i = session.getIndent();
		out.oTag(i, "table").attr("type", "object");
		if (classAttr != null)
			out.attr("class", classAttr);
		out.appendln(">");
		if (! (aType.getHtmlMeta().isNoTableHeaders() || (ppMeta != null && ppMeta.getHtmlMeta().isNoTableHeaders()))) {
			out.sTag(i+1, "tr").nl();
			out.sTag(i+2, "th").nl().appendln(i+3, "<string>key</string>").eTag(i+2, "th").nl();
			out.sTag(i+2, "th").nl().appendln(i+3, "<string>value</string>").eTag(i+2, "th").nl();
			out.eTag(i+1, "tr").nl();
		}
		for (Map.Entry e : (Set<Map.Entry>)m.entrySet()) {

			Object key = session.generalize(e.getKey(), keyType);
			Object value = null;
			try {
				value = e.getValue();
			} catch (StackOverflowError t) {
				throw t;
			} catch (Throwable t) {
				session.addWarning("Could not call getValue() on property ''{0}'', {1}", e.getKey(), t.getLocalizedMessage());
			}

			out.sTag(i+1, "tr").nl();
			out.sTag(i+2, "td").nl();
			serializeAnything(session, out, key, keyType, null, 2, null);
			out.eTag(i+2, "td").nl();
			out.sTag(i+2, "td").nl();
			serializeAnything(session, out, value, valueType, (key == null ? "_x0000_" : key.toString()), 2, null);
			out.eTag(i+2, "td").nl();
			out.eTag(i+1, "tr").nl();
		}
		out.eTag(i, "table").nl();
	}

	@SuppressWarnings({ "rawtypes" })
	private void serializeBeanMap(HtmlSerializerSession session, HtmlWriter out, BeanMap m, String classAttr, BeanPropertyMeta<?> ppMeta) throws Exception {
		int i = session.getIndent();

		Object o = m.getBean();

		Class<?> c = o.getClass();
		if (c.isAnnotationPresent(HtmlLink.class)) {
			HtmlLink h = o.getClass().getAnnotation(HtmlLink.class);
			Object urlProp = m.get(h.hrefProperty());
			Object nameProp = m.get(h.nameProperty());
			out.oTag(i, "a").attrUri("href", urlProp).append('>').encodeText(nameProp).eTag("a").nl();
			return;
		}

		out.oTag(i, "table").attr("type", "object");
		if (classAttr != null)
			out.attr("_class", classAttr);
		out.append('>').nl();
		if (! (m.getClassMeta().getHtmlMeta().isNoTableHeaders() || (ppMeta != null && ppMeta.getHtmlMeta().isNoTableHeaders()))) {
			out.sTag(i+1, "tr").nl();
			out.sTag(i+2, "th").nl().appendln(i+3, "<string>key</string>").eTag(i+2, "th").nl();
			out.sTag(i+2, "th").nl().appendln(i+3, "<string>value</string>").eTag(i+2, "th").nl();
			out.eTag(i+1, "tr").nl();
		}

		Iterator mapEntries = m.entrySet(session.isTrimNulls()).iterator();

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

			out.sTag(i+1, "tr").nl();
			out.sTag(i+2, "td").nl();
			out.sTag(i+3, "string").encodeText(key).eTag("string").nl();
			out.eTag(i+2, "td").nl();
			out.sTag(i+2, "td").nl();
			try {
				serializeAnything(session, out, value, p.getMeta().getClassMeta(), key, 2, pMeta);
			} catch (SerializeException t) {
				throw t;
			} catch (StackOverflowError t) {
				throw t;
			} catch (Throwable t) {
				session.addBeanGetterWarning(pMeta, t);
			}
			out.eTag(i+2, "td").nl();
			out.eTag(i+1, "tr").nl();
		}
		out.eTag(i, "table").nl();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void serializeCollection(HtmlSerializerSession session, HtmlWriter out, Collection c, ClassMeta<?> type, String name, String classAttr, BeanPropertyMeta<?> ppMeta) throws Exception {

		BeanContext bc = session.getBeanContext();
		ClassMeta<?> elementType = type.getElementType();

		int i = session.getIndent();
		if (c.isEmpty()) {
			out.appendln(i, "<ul></ul>");
			return;
		}

		c = session.sort(c);

		// Look at the objects to see how we're going to handle them.  Check the first object to see how we're going to handle this.
		// If it's a map or bean, then we'll create a table.
		// Otherwise, we'll create a list.
		String[] th = getTableHeaders(session, c, ppMeta);

		if (th != null) {

			out.oTag(i, "table").attr("type", "array");
			if (classAttr != null)
				out.attr("_class", classAttr);
			out.append('>').nl();
			out.sTag(i+1, "tr").nl();
			for (String key : th)
				out.sTag(i+2, "th").append(key).eTag("th").nl();
			out.eTag(i+1, "tr").nl();

			for (Object o : c) {
				ClassMeta<?> cm = bc.getClassMetaForObject(o);

				if (cm != null && cm.getPojoTransform() != null) {
					PojoTransform f = cm.getPojoTransform();
					o = f.transform(o);
					cm = cm.getTransformedClassMeta();
				}

				if (cm != null && session.isAddClassAttrs() && elementType.getInnerClass() != o.getClass())
					out.oTag(i+1, "tr").attr("_class", o.getClass().getName()).append('>').nl();
				else
					out.sTag(i+1, "tr").nl();

				if (cm == null) {
					serializeAnything(session, out, o, null, null, 1, null);

				} else if (cm.isMap() && ! (cm.isBeanMap())) {
					Map m2 = session.sort((Map)o);

					Iterator mapEntries = m2.entrySet().iterator();
					while (mapEntries.hasNext()) {
						Map.Entry e = (Map.Entry)mapEntries.next();
						out.sTag(i+2, "td").nl();
						serializeAnything(session, out, e.getValue(), elementType, e.getKey().toString(), 2, null);
						out.eTag(i+2, "td").nl();
					}
				} else {
					BeanMap m2 = null;
					if (o instanceof BeanMap)
						m2 = (BeanMap)o;
					else
						m2 = bc.forBean(o);

					Iterator mapEntries = m2.entrySet(session.isTrimNulls()).iterator();
					while (mapEntries.hasNext()) {
						BeanMapEntry p = (BeanMapEntry)mapEntries.next();
						BeanPropertyMeta pMeta = p.getMeta();
						out.sTag(i+2, "td").nl();
						serializeAnything(session, out, p.getValue(), pMeta.getClassMeta(), p.getKey().toString(), 2, pMeta);
						out.eTag(i+2, "td").nl();
					}
				}
				out.eTag(i+1, "tr").nl();
			}
			out.eTag(i, "table").nl();

		} else {
			out.sTag(i, "ul").nl();
			for (Object o : c) {
				out.sTag(i+1, "li").nl();
				serializeAnything(session, out, o, elementType, name, 1, null);
				out.eTag(i+1, "li").nl();
			}
			out.eTag(i, "ul").nl();
		}
	}

	/*
	 * Returns the table column headers for the specified collection of objects.
	 * Returns null if collection should not be serialized as a 2-dimensional table.
	 * 2-dimensional tables are used for collections of objects that all have the same set of property names.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String[] getTableHeaders(SerializerSession session, Collection c, BeanPropertyMeta<?> pMeta) throws Exception {
		BeanContext bc = session.getBeanContext();
		if (c.size() == 0)
			return null;
		c = session.sort(c);
		String[] th;
		Set<String> s = new TreeSet<String>();
		Set<ClassMeta> prevC = new HashSet<ClassMeta>();
		Object o1 = null;
		for (Object o : c)
			if (o != null) {
				o1 = o;
				break;
			}
		if (o1 == null)
			return null;
		ClassMeta cm = bc.getClassMetaForObject(o1);
		if (cm.getPojoTransform() != null) {
			PojoTransform f = cm.getPojoTransform();
			o1 = f.transform(o1);
			cm = cm.getTransformedClassMeta();
		}
		if (cm == null || ! (cm.isMap() || cm.isBean()))
			return null;
		if (cm.getInnerClass().isAnnotationPresent(HtmlLink.class))
			return null;
		HtmlClassMeta h = cm.getHtmlMeta();
		if (h.isNoTables() || (pMeta != null && pMeta.getHtmlMeta().isNoTables()))
			return null;
		if (h.isNoTableHeaders() || (pMeta != null && pMeta.getHtmlMeta().isNoTableHeaders()))
			return new String[0];
		if (session.canIgnoreValue(cm, null, o1))
			return null;
		if (cm.isMap() && ! cm.isBeanMap()) {
			Map m = (Map)o1;
			th = new String[m.size()];
			int i = 0;
			for (Object k : m.keySet())
				th[i++] = (k == null ? null : k.toString());
		} else {
			BeanMap<?> bm = (o1 instanceof BeanMap ? (BeanMap)o1 : bc.forBean(o1));
			List<String> l = new LinkedList<String>();
			for (String k : bm.keySet())
				l.add(k);
			th = l.toArray(new String[l.size()]);
		}
		prevC.add(cm);
		s.addAll(Arrays.asList(th));

		for (Object o : c) {
			if (o == null)
				continue;
			cm = bc.getClassMetaForObject(o);
			if (cm != null && cm.getPojoTransform() != null) {
				PojoTransform f = cm.getPojoTransform();
				o = f.transform(o);
				cm = cm.getTransformedClassMeta();
			}
			if (prevC.contains(cm))
				continue;
			if (cm == null || ! (cm.isMap() || cm.isBean()))
				return null;
			if (cm.getInnerClass().isAnnotationPresent(HtmlLink.class))
				return null;
			if (session.canIgnoreValue(cm, null, o))
				return null;
			if (cm.isMap() && ! cm.isBeanMap()) {
				Map m = (Map)o;
				if (th.length != m.keySet().size())
					return null;
				for (Object k : m.keySet())
					if (! s.contains(k.toString()))
						return null;
			} else {
				BeanMap<?> bm = (o instanceof BeanMap ? (BeanMap)o : bc.forBean(o));
				int l = 0;
				for (String k : bm.keySet()) {
					if (! s.contains(k))
						return null;
					l++;
				}
				if (s.size() != l)
					return null;
			}
		}
		return th;
	}

	/**
	 * Returns the schema serializer based on the settings of this serializer.
	 * @return The schema serializer.
	 */
	@Override /* XmlSerializer */
	public HtmlSerializer getSchemaSerializer() {
		try {
			return new HtmlSchemaDocSerializer(getContextFactory().clone());
		} catch (CloneNotSupportedException e) {
			// Should never happen.
			throw new RuntimeException(e);
		}
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public HtmlSerializerSession createSession(Object output, ObjectMap properties, Method javaMethod) {
		return new HtmlSerializerSession(getContext(HtmlSerializerContext.class), getBeanContext(), output, properties, javaMethod);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		HtmlSerializerSession s = (HtmlSerializerSession)session;
		doSerialize(s, o, s.getWriter());
	}

	@Override /* CoreApi */
	public HtmlSerializer setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlSerializer setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public HtmlSerializer addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public HtmlSerializer addTransforms(Class<?>...classes) throws LockedException {
		super.addTransforms(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> HtmlSerializer addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public HtmlSerializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public HtmlSerializer lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public HtmlSerializer clone() {
		HtmlSerializer c = (HtmlSerializer)super.clone();
		return c;
	}
}
