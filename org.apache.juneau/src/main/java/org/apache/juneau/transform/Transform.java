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

import org.apache.juneau.*;

/**
 * Parent class for all bean and POJO transforms.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Transforms are used to alter how POJOs are handled by bean contexts (and subsequently serializers and parsers).
 * 	The are a very powerful feature of the Juneau framework that allows virtually any POJO to be serialized and parsed.
 * 	For example, they can be used to...
 * <ul class='spaced-list'>
 * 	<li>Convert a non-serializable POJO into a serializable POJO during serialization (and optionally vis-versa during parsing).
 * 	<li>Control various aspects of beans, such as what properties are visible, bean subclasses, etc...
 * </ul>
 * <p>
 * 	There are 2 subclasses of transforms:
 * <ul class='spaced-list'>
 * 	<li>{@link PojoTransform} - Non-bean transforms for converting POJOs into serializable equivalents.
 * 	<li>{@link BeanTransform} - Bean transforms for configuring how beans are handled.
 * </ul>
 * <p>
 * 	Transforms are associated with bean contexts (and serializers/parsers) through the {@link ContextFactory#addToProperty(String,Object)}
 * 		and {@link CoreApi#addTransforms(Class[])} methods.
 *
 *
 * <h6 class='topic'>Additional information</h6>
 * 	See {@link org.apache.juneau.transform} for more information.
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class Transform {

	/** Represents no transform. */
	public static class NULL extends Transform {}

	/** The transform subtype */
	public static enum TransformType {
		/** PojoTransform */
		POJO,
		/** BeanTransform */
		BEAN
	}

	/** The class that this transform applies to. */
	protected Class<?> forClass;

	/** The bean context that this transform instance belongs to. */
	protected BeanContext beanContext;

	/** Whether this is a BeanTransform or PojoTransform. */
	protected TransformType type = TransformType.POJO;

	Transform() {}

	Transform(Class<?> forClass) {
		this.forClass = forClass;
	}


	/**
	 * Returns the class that this transform applies to.
	 *
	 * @return The class that this transform applies to.
	 */
	public Class<?> forClass() {
		return forClass;
	}

	/**
	 * Returns the implementation class.
	 * Useful for debugging when calling {@link BeanContext#toString()}.
	 *
	 * @return The implementation class of this transform.
	 */
	public Class<?> getImplClass() {
		return this.getClass();
	}

	/**
	 * Returns whether this is an instance of {@link PojoTransform} or {@link BeanTransform}.
	 *
	 * @return The transform type.
	 */
	public TransformType getType() {
		return type;
	}

	/**
	 * Returns the {@link BeanContext} that created this transform.
	 *
	 * @return The bean context that created this transform.
	 */
	protected BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Sets the bean context that this transform instance was created by.
	 *
	 * @param beanContext The bean context that created this transform.
	 * @return This object (for method chaining).
	 */
	public Transform setBeanContext(BeanContext beanContext) {
		this.beanContext = beanContext;
		return this;
	}

	@Override /* Object */
	public int hashCode() {
		return getClass().getName().hashCode() + forClass().getName().hashCode();
	}

	/**
	 * Checks if the specified transform class is the same as this one.
	 *
	 * @param f The transform to check.
	 * @return <jk>true</jk> if the specified transform is equivalent to this one.
	 */
	public boolean isSameAs(Transform f) {
		return f.getClass().equals(getClass()) && f.forClass().equals(forClass());
	}
}
