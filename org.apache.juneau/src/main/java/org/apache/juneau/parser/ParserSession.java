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
package org.apache.juneau.parser;

import static org.apache.juneau.parser.ParserContext.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Session object that lives for the duration of a single use of {@link Parser}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class ParserSession extends Session {

	private static JuneauLogger logger = JuneauLogger.getLogger(ParserSession.class);

	private final boolean debug, trimStrings;
	private boolean closed;
	private final BeanContext beanContext;
	private final List<String> warnings = new LinkedList<String>();

	private final ObjectMap properties;
	private final Method javaMethod;
	private final Object outer;
	private final Object input;
	private InputStream inputStream;
	private Reader reader, noCloseReader;
	private BeanPropertyMeta<?> currentProperty;
	private ClassMeta<?> currentClass;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param beanContext The bean context being used.
	 * @param input The input.
	 * 	<br>For character-based parsers, this can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text.
	 * 		<li>{@link File} containing system encoded text.
	 * 	</ul>
	 * 	<br>For byte-based parsers, this can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 	</ul>
	 * @param op The override properties.
	 * 	These override any context properties defined in the context.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 */
	public ParserSession(ParserContext ctx, BeanContext beanContext, Object input, ObjectMap op, Method javaMethod, Object outer) {
		super(ctx);
		if (op == null || op.isEmpty()) {
			debug = ctx.debug;
			trimStrings = ctx.trimStrings;
		} else {
			debug = op.getBoolean(PARSER_debug, ctx.debug);
			trimStrings = op.getBoolean(PARSER_trimStrings, ctx.trimStrings);
		}
		this.beanContext = beanContext;
		this.input = input;
		this.properties = op;
		this.javaMethod = javaMethod;
		this.outer = outer;
	}

	/**
	 * Wraps the specified input object inside an input stream.
	 * Subclasses can override this method to implement their own input streams.
	 *
	 * @return The input object wrapped in an input stream, or <jk>null</jk> if the object is null.
	 * @throws ParseException If object could not be converted to an input stream.
	 */
	public InputStream getInputStream() throws ParseException {
		if (input == null)
			return null;
		if (input instanceof InputStream)
			return (InputStream)input;
		if (input instanceof byte[])
			return new ByteArrayInputStream((byte[])input);
		if (input instanceof File)
			try {
				inputStream = new FileInputStream((File)input);
				return inputStream;
			} catch (FileNotFoundException e) {
				throw new ParseException(e);
			}
		throw new ParseException("Cannot convert object of type {0} to an InputStream.", input.getClass().getName());
	}


	/**
	 * Wraps the specified input object inside a reader.
	 * Subclasses can override this method to implement their own readers.
	 *
	 * @return The input object wrapped in a Reader, or <jk>null</jk> if the object is null.
	 * @throws Exception If object could not be converted to a reader.
	 */
	public Reader getReader() throws Exception {
		if (input == null)
			return null;
		if (input instanceof Reader)
			return (Reader)input;
		if (input instanceof CharSequence) {
			if (reader == null)
				reader = new ParserReader((CharSequence)input);
			return reader;
		}
		if (input instanceof InputStream) {
			if (noCloseReader == null)
				noCloseReader = new InputStreamReader((InputStream)input, IOUtils.UTF8);
			return noCloseReader;
		}
		if (input instanceof File) {
			if (reader == null)
				reader = new FileReader((File)input);
			return reader;
		}
		throw new ParseException("Cannot convert object of type {0} to a Reader.", input.getClass().getName());
	}

	/**
	 * Returns information used to determine at what location in the parse a failure occurred.
	 *
	 * @return A map, typically containing something like <code>{line:123,column:456,currentProperty:"foobar"}</code>
	 */
	public Map<String,Object> getLastLocation() {
		Map<String,Object> m = new LinkedHashMap<String,Object>();
		if (currentClass != null)
			m.put("currentClass", currentClass.toString(true));
		if (currentProperty != null)
			m.put("currentProperty", currentProperty);
		return m;
	}

	/**
	 * Returns the raw input object passed into this session.
	 *
	 * @return The raw input object passed into this session.
	 */
	protected Object getInput() {
		return input;
	}

	/**
	 * Returns the bean context in use for this session.
	 *
	 * @return The bean context in use for this session.
	 */
	public final BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Returns the Java method that invoked this parser.
	 * <p>
	 * When using the REST API, this is the Java method invoked by the REST call.
	 * Can be used to access annotations defined on the method or class.
	 *
	 * @return The Java method that invoked this parser.
	*/
	public final Method getJavaMethod() {
		return javaMethod;
	}

	/**
	 * Returns the outer object used for instantiating top-level non-static member classes.
	 * When using the REST API, this is the servlet object.
	 *
	 * @return The outer object.
	*/
	public final Object getOuter() {
		return outer;
	}

	/**
	 * Sets the current bean property being parsed for proper error messages.
	 * @param currentProperty The current property being parsed.
	 */
	public void setCurrentProperty(BeanPropertyMeta<?> currentProperty) {
		this.currentProperty = currentProperty;
	}

	/**
	 * Sets the current class being parsed for proper error messages.
	 * @param currentClass The current class being parsed.
	 */
	public void setCurrentClass(ClassMeta<?> currentClass) {
		this.currentClass = currentClass;
	}

	/**
	 * Returns the {@link ParserContext#PARSER_debug} setting value for this session.
	 *
	 * @return The {@link ParserContext#PARSER_debug} setting value for this session.
	 */
	public final boolean isDebug() {
		return debug;
	}

	/**
	 * Returns the {@link ParserContext#PARSER_trimStrings} setting value for this session.
	 *
	 * @return The {@link ParserContext#PARSER_trimStrings} setting value for this session.
	 */
	public final boolean isTrimStrings() {
		return trimStrings;
	}

	/**
	 * Returns the runtime properties associated with this context.
	 *
	 * @return The runtime properties associated with this context.
	 */
	public final ObjectMap getProperties() {
		return properties;
	}

	/**
	 * Logs a warning message.
	 *
	 * @param msg The warning message.
	 * @param args Optional printf arguments to replace in the error message.
	 */
	public void addWarning(String msg, Object... args) {
		logger.warning(msg, args);
		msg = args.length == 0 ? msg : String.format(msg, args);
		warnings.add((warnings.size() + 1) + ": " + msg);
	}

	/**
	 * Trims the specified object if it's a <code>String</code> and {@link #isTrimStrings()} returns <jk>true</jk>.
	 *
	 * @param o The object to trim.
	 * @return The trimmmed string if it's a string.
	 */
	@SuppressWarnings("unchecked")
	public final <K> K trim(K o) {
		if (trimStrings && o instanceof String)
			return (K)o.toString().trim();
		return o;

	}

	/**
	 * Trims the specified string if {@link ParserSession#isTrimStrings()} returns <jk>true</jk>.
	 *
	 * @param s The input string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public final String trim(String s) {
		if (trimStrings && s != null)
			return s.trim();
		return s;
	}

	/**
	 * Perform cleanup on this context object if necessary.
	 *
	 * @throws ParseException If called more than once, or in debug mode and warnings occurred.
	 */
	public void close() throws ParseException {
		if (closed)
			throw new ParseException("Attempt to close ParserSession more than once.");

		try {
			if (inputStream != null)
				inputStream.close();
			if (reader != null)
				reader.close();
		} catch (IOException e) {
			throw new ParseException(e);
		}

		if (debug && warnings.size() > 0)
			throw new ParseException("Warnings occurred during parsing: \n" + StringUtils.join(warnings, "\n"));
		closed = true;
	}

	@Override /* Object */
	protected void finalize() throws Throwable {
		if (! closed)
			throw new RuntimeException("ParserSession was not closed.");
	}
}
