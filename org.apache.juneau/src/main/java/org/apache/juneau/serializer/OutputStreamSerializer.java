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
package org.apache.juneau.serializer;

import java.io.*;

import org.apache.juneau.annotation.*;

/**
 * Subclass of {@link Serializer} for byte-based serializers.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	This class is typically the parent class of all byte-based serializers.
 * 	It has 1 abstract method to implement...
 * <ul>
 * 	<li>{@link #doSerialize(SerializerSession, Object)}
 * </ul>
 *
 *
 * <h6 class='topic'>@Produces annotation</h6>
 * <p>
 * 	The media types that this serializer can produce is specified through the {@link Produces @Produces} annotation.
 * <p>
 * 	However, the media types can also be specified programmatically by overriding the {@link #getMediaTypes()}
 * 		and {@link #getResponseContentType()} methods.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public abstract class OutputStreamSerializer extends Serializer {

	@Override /* Serializer */
	public boolean isWriterSerializer() {
		return false;
	}

	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Convenience method for serializing an object to a <code><jk>byte</jk></code>.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@Override
	public final byte[] serialize(Object o) throws SerializeException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serialize(createSession(baos), o);
		return baos.toByteArray();
	}
}
