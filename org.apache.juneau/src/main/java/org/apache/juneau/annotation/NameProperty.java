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

import org.apache.juneau.ini.*;

/**
 * Identifies a setter as a method for setting the name of a POJO as it's known by
 * its parent object.
 * <p>
 * For example, the {@link Section} class must know the name it's known by it's parent
 * {@link ConfigFileImpl} class, so parsers will call this method with the sectio name
 * using the {@link Section#setName(String)} method.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Target({METHOD})
@Retention(RUNTIME)
@Inherited
public @interface NameProperty {}
