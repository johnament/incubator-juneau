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
package org.apache.juneau.xml;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Metadata on beans specific to the XML serializers and parsers pulled from the {@link Xml @Xml} annotation on the class.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 * @param <T> The bean class type.
 */
public class XmlBeanMeta<T> {

	// XML related fields
	private final Map<String,BeanPropertyMeta<T>> xmlAttrs;                        // Map of bean properties that are represented as XML attributes.
	private final BeanPropertyMeta<T> xmlContent;                                  // Bean property that is represented as XML content within the bean element.
	private final XmlContentHandler<T> xmlContentHandler;                          // Class used to convert bean to XML content.
	private final Map<String,BeanPropertyMeta<T>> childElementProperties;          // Properties defined with @Xml.childName annotation.
	private final BeanMeta<T> beanMeta;

	/**
	 * Constructor.
	 *
	 * @param beanMeta The metadata on the bean that this metadata applies to.
	 * @param pNames Only look at these property names.  If <jk>null</jk>, apply to all bean properties.
	 */
	public XmlBeanMeta(BeanMeta<T> beanMeta, String[] pNames) {
		this.beanMeta = beanMeta;
		Class<T> c = beanMeta.getClassMeta().getInnerClass();

		Map<String,BeanPropertyMeta<T>> tXmlAttrs = new LinkedHashMap<String,BeanPropertyMeta<T>>();
		BeanPropertyMeta<T> tXmlContent = null;
		XmlContentHandler<T> tXmlContentHandler = null;
		Map<String,BeanPropertyMeta<T>> tChildElementProperties = new LinkedHashMap<String,BeanPropertyMeta<T>>();

		for (BeanPropertyMeta<T> p : beanMeta.getPropertyMetas(pNames)) {
			XmlFormat xf = p.getXmlMeta().getXmlFormat();
			if (xf == XmlFormat.ATTR)
				tXmlAttrs.put(p.getName(), p);
			else if (xf == XmlFormat.CONTENT) {
				if (tXmlContent != null)
					throw new BeanRuntimeException(c, "Multiple instances of CONTENT properties defined on class.  Only one property can be designated as such.");
				tXmlContent = p;
				tXmlContentHandler = p.getXmlMeta().getXmlContentHandler();
			}
			// Look for any properties that are collections with @Xml.childName specified.
			String n = p.getXmlMeta().getChildName();
			if (n != null) {
				if (tChildElementProperties.containsKey(n))
					throw new BeanRuntimeException(c, "Multiple properties found with the name ''{0}''.", n);
				tChildElementProperties.put(n, p);
			}
		}

		xmlAttrs = Collections.unmodifiableMap(tXmlAttrs);
		xmlContent = tXmlContent;
		xmlContentHandler = tXmlContentHandler;
		childElementProperties = (tChildElementProperties.isEmpty() ? null : Collections.unmodifiableMap(tChildElementProperties));
	}

	/**
	 * Returns the list of properties annotated with an {@link Xml#format()} of {@link XmlFormat#ATTR}.
	 * In other words, the list of properties that should be rendered as XML attributes instead of child elements.
	 *
	 * @return Metadata on the XML attribute properties of the bean.
	 */
	protected Map<String,BeanPropertyMeta<T>> getXmlAttrProperties() {
		return xmlAttrs;
	}

	/**
	 * Returns the bean property annotated with an {@link Xml#format()} value of {@link XmlFormat#CONTENT}
	 *
	 * @return The bean property, or <jk>null</jk> if annotation is not specified.
	 */
	protected BeanPropertyMeta<T> getXmlContentProperty() {
		return xmlContent;
	}

	/**
	 * Return the XML content handler for this bean.
	 *
	 * @return The XML content handler for this bean, or <jk>null</jk> if no content handler is defined.
	 */
	protected XmlContentHandler<T> getXmlContentHandler() {
		return xmlContentHandler;
	}

	/**
	 * Returns the child element properties for this bean.
	 * See {@link Xml#childName()}
	 *
	 * @return The child element properties for this bean, or <jk>null</jk> if no child element properties are defined.
	 */
	protected Map<String,BeanPropertyMeta<T>> getChildElementProperties() {
		return childElementProperties;
	}

	/**
	 * Returns bean property meta with the specified name.
	 * This is identical to calling {@link BeanMeta#getPropertyMeta(String)} except it first retrieves
	 * 	the bean property meta based on the child name (e.g. a property whose name is "people", but whose child name is "person").
	 *
	 * @param fieldName The bean property name.
	 * @return The property metadata.
	 */
	protected BeanPropertyMeta<T> getPropertyMeta(String fieldName) {
		if (childElementProperties != null) {
			BeanPropertyMeta<T> bpm = childElementProperties.get(fieldName);
			if (bpm != null)
				return bpm;
		}
		return beanMeta.getPropertyMeta(fieldName);
	}
}
