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
package org.apache.juneau.transforms;

import java.text.*;
import java.util.*;

import javax.xml.bind.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.transform.*;

/**
 * Transforms {@link Date Dates} to {@link String Strings}.
 *
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * 	The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>{@link ToString} - Transforms to {@link String Strings} using the {@code Date.toString()} method.
 * 	<li>{@link ISO8601DT} - Transforms to ISO8601 date-time strings.
 * 	<li>{@link ISO8601DTP} - Transforms to ISO8601 date-time strings with millisecond precision.
 * 	<li>{@link ISO8601DTZ} - Same as {@link ISO8601DT}, except always serializes in GMT.
 * 	<li>{@link ISO8601DTZP} - Same as {@link ISO8601DTZ}, except with millisecond precision.
 * 	<li>{@link RFC2822DT} - Transforms to RFC2822 date-time strings.
 * 	<li>{@link RFC2822DTZ} - Same as {@link RFC2822DT}, except always serializes in GMT.
 * 	<li>{@link RFC2822D} - Transforms to RFC2822 date strings.
 * 	<li>{@link SimpleDT} - Transforms to simple <js>"yyyy/MM/dd HH:mm:ss"</js> strings.
 * 	<li>{@link SimpleT} - Transforms to simple <js>"yyyy/MM/dd HH:mm:ss"</js> strings.
 * 	<li>{@link Medium} - Transforms to {@link DateFormat#MEDIUM} strings.
 * </ul>
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class DateTransform extends PojoTransform<Date,String> {

	/**
	 * Transforms {@link Date Dates} to {@link String Strings} using the {@code Date.toString()} method.
	 * <p>
	 * <dl>
	 * 	<dt>Example output:</dt>
	 * 	<dd>
	 * <ul>
	 * 	<li><js>"Wed Jul 04 15:30:45 EST 2001"</js>
	 * </ul>
	 * 	</dd>
	 * </dl>
	 */
	public static class ToString extends DateTransform {
		/** Constructor */
		public ToString() {
			super("EEE MMM dd HH:mm:ss zzz yyyy");
		}
	}

	/**
	 * Transforms {@link Date Dates} to ISO8601 date-time strings.
	 *
	 * <dl>
	 * 	<dt>Example output:</dt>
	 * 	<dd>
	 * <ul>
	 * 	<li><js>"2001-07-04T15:30:45-05:00"</js>
	 * 	<li><js>"2001-07-04T15:30:45Z"</js>
	 * </ul>
	 * 	</dd>
	 * 	<dt>Example input:</dt>
	 * 	<dd>
	 * <ul>
	 * 	<li><js>"2001-07-04T15:30:45-05:00"</js>
	 * 	<li><js>"2001-07-04T15:30:45Z"</js>
	 * 	<li><js>"2001-07-04T15:30:45.1Z"</js>
	 * 	<li><js>"2001-07-04T15:30Z"</js>
	 * 	<li><js>"2001-07-04"</js>
	 * 	<li><js>"2001-07"</js>
	 * 	<li><js>"2001"</js>
	 * </ul>
	 * 	</dd>
	 * </dl>
	 */
	public static class ISO8601DT extends DateTransform {
		private SimpleDateFormat tzFormat = new SimpleDateFormat("Z");

		/** Constructor */
		public ISO8601DT() {
			this("yyyy-MM-dd'T'HH:mm:ss");
		}

		/**
		 * Constructor with specific pattern.
		 *
		 * @param pattern The {@link MessageFormat}-style format string.
		 */
		protected ISO8601DT(String pattern) {
			super(pattern);
		}

		@Override /* PojoTransform */
		public Date normalize(String o, ClassMeta<?> hint) throws ParseException {
			try {
				if (StringUtils.isEmpty(o))
					return null;
				return convert(DatatypeConverter.parseDateTime(o).getTime(), hint);
			} catch (ParseException e) {
				throw e;
			} catch (Exception e) {
				throw new ParseException(e);
			}
		}

		@Override /* PojoTransform */
		public String transform(Date o) {
			String s = super.transform(o);
			String tz = tzFormat.format(o);
			if (tz.equals("+0000"))
				return s + "Z";
			return s + tz.substring(0,3) + ':' + tz.substring(3);
		}
	}

	/**
	 * Same as {@link ISO8601DT} except serializes to millisecond precision.
	 * <p>
	 * Example output: <js>"2001-07-04T15:30:45.123-05:00"</js>
	 */
	public static class ISO8601DTP extends ISO8601DT {

		/** Constructor */
		public ISO8601DTP() {
			super("yyyy-MM-dd'T'HH:mm:ss.SSS");
		}
	}

	/**
	 * Same as {@link ISO8601DT} except serializes to millisecond precision and doesn't include timezone.
	 * <p>
	 * Example output: <js>"2001-07-04T15:30:45.123"</js>
	 */
	public static class ISO8601DTPNZ extends DateTransform {

		/** Constructor */
		public ISO8601DTPNZ() {
			super("yyyy-MM-dd'T'HH:mm:ss.SSS");
		}
	}

	/**
	 * Same as {@link ISO8601DT}, except always serializes in GMT.
	 * <p>
	 * Example output:  <js>"2001-07-04T15:30:45Z"</js>
	 */
	public static class ISO8601DTZ extends DateTransform {

		/** Constructor */
		public ISO8601DTZ() {
			this("yyyy-MM-dd'T'HH:mm:ss'Z'");
		}

		/**
		 * Constructor with specific pattern.
		 *
		 * @param pattern The {@link MessageFormat}-style format string.
		 */
		protected ISO8601DTZ(String pattern) {
			super(pattern, "GMT");
		}

		@Override /* PojoTransform */
		public Date normalize(String o, ClassMeta<?> hint) throws ParseException {
			try {
				if (StringUtils.isEmpty(o))
					return null;
				return convert(DatatypeConverter.parseDateTime(o).getTime(), hint);
			} catch (ParseException e) {
				throw e;
			} catch (Exception e) {
				throw new ParseException(e);
			}
		}
	}

	/**
	 * Same as {@link ISO8601DTZ} except serializes to millisecond precision.
	 * <p>
	 * Example output:  <js>"2001-07-04T15:30:45.123Z"</js>
	 */
	public static class ISO8601DTZP extends ISO8601DT {

		/** Constructor */
		public ISO8601DTZP() {
			super("yyyy-MM-dd'T'HH:mm:ss.SSS");
		}
	}

	/**
	 * Transforms {@link Date Dates} to RFC2822 date-time strings.
	 */
	public static class RFC2822DT extends DateTransform {
		/** Constructor */
		public RFC2822DT() {
			super("EEE, dd MMM yyyy HH:mm:ss z");
		}
	}

	/**
	 * Same as {@link RFC2822DT}, except always serializes in GMT.
	 * <p>
	 * Example output:  <js>"2001-07-04T15:30:45Z"</js>
	 */
	public static class RFC2822DTZ extends DateTransform {
		/** Constructor */
		public RFC2822DTZ() {
			super("EEE, dd MMM yyyy HH:mm:ss z", "GMT");
		}
	}

	/**
	 * Transforms {@link Date Dates} to RFC2822 date strings.
	 */
	public static class RFC2822D extends DateTransform {
		/** Constructor */
		public RFC2822D() {
			super("dd MMM yyyy");
		}
	}

	/**
	 * Transforms {@link Date Dates} to simple <js>"yyyy/MM/dd HH:mm:ss"</js> strings.
	 */
	public static class SimpleDT extends DateTransform {
		/** Constructor */
		public SimpleDT() {
			super("yyyy/MM/dd HH:mm:ss");
		}
	}

	/**
	 * Transforms {@link Date Dates} to simple <js>"yyyy/MM/dd HH:mm:ss.SSS"</js> strings.
	 */
	public static class SimpleDTP extends DateTransform {
		/** Constructor */
		public SimpleDTP() {
			super("yyyy/MM/dd HH:mm:ss.SSS");
		}
	}

	/**
	 * Transforms {@link Date Dates} to simple <js>"HH:mm:ss"</js> strings.
	 */
	public static class SimpleT extends DateTransform {
		/** Constructor */
		public SimpleT() {
			super("HH:mm:ss");
		}
	}

	/**
	 * Transforms {@link Date Dates} to simple <js>"HH:mm:ss.SSS"</js> strings.
	 */
	public static class SimpleTP extends DateTransform {
		/** Constructor */
		public SimpleTP() {
			super("HH:mm:ss.SSS");
		}
	}

	/**
	 * Transforms {@link Date Dates} to {@link DateFormat#MEDIUM} strings.
	 */
	public static class Medium extends DateTransform {
		/** Constructor */
		public Medium() {
			super(DateFormat.getDateInstance(DateFormat.MEDIUM));
		}
	}

	/** The formatter to convert dates to Strings. */
	private DateFormat format;

	/**
	 * Construct a transform using the specified date format string that will be
	 * 	used to construct a {@link SimpleDateFormat} that will be used to convert
	 * 	dates to strings.
	 *
	 * @param simpleDateFormat The {@link SimpleDateFormat} pattern.
	 */
	public DateTransform(String simpleDateFormat) {
		this(new SimpleDateFormat(simpleDateFormat));
	}

	/**
	 * Construct a transform using the specified date format string that will be
	 * 	used to construct a {@link SimpleDateFormat} that will be used to convert
	 * 	dates to strings.
	 *
	 * @param simpleDateFormat The {@link SimpleDateFormat} pattern.
	 * @param timeZone The time zone to associate with the date pattern.
	 */
	public DateTransform(String simpleDateFormat, String timeZone) {
		this(new SimpleDateFormat(simpleDateFormat));
		format.setTimeZone(TimeZone.getTimeZone(timeZone));
	}

	/**
	 * Construct a transform using the specified {@link DateFormat} that will be used to convert
	 * 	dates to strings.
	 *
	 * @param format The format to use to convert dates to strings.
	 */
	public DateTransform(DateFormat format) {
		super();
		this.format = format;
	}

	/**
	 * Converts the specified {@link Date} to a {@link String}.
	 */
	@Override /* PojoTransform */
	public String transform(Date o) {
		return format.format(o);
	}

	/**
	 * Converts the specified {@link String} to a {@link Date}.
	 */
	@Override /* PojoTransform */
	public Date normalize(String o, ClassMeta<?> hint) throws ParseException {
		try {
			if (StringUtils.isEmpty(o))
				return null;
			Date d = format.parse(o);
			return convert(d, hint);
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}

	private static Date convert(Date in, ClassMeta<?> hint) throws Exception {
		if (in == null)
			return null;
		if (hint == null || hint.isInstance(in))
			return in;
		Class<?> c = hint.getInnerClass();
		if (c == java.util.Date.class)
			return in;
		if (c == java.sql.Date.class)
			return new java.sql.Date(in.getTime());
		if (c == java.sql.Time.class)
			return new java.sql.Time(in.getTime());
		if (c == java.sql.Timestamp.class)
			return new java.sql.Timestamp(in.getTime());
		throw new ParseException("DateTransform is unable to narrow object of type ''{0}''", c);
	}
}
