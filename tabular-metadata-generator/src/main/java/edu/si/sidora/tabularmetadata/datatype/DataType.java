/**
 * Copyright 2015 Smithsonian Institution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.You may obtain a copy of
 * the License at: http://www.apache.org/licenses/
 *
 * This software and accompanying documentation is supplied without
 * warranty of any kind. The copyright holder and the Smithsonian Institution:
 * (1) expressly disclaim any warranties, express or implied, including but not
 * limited to any implied warranties of merchantability, fitness for a
 * particular purpose, title or non-infringement; (2) do not assume any legal
 * liability or responsibility for the accuracy, completeness, or usefulness of
 * the software; (3) do not represent that use of the software would not
 * infringe privately owned rights; (4) do not warrant that the software
 * is error-free or will be maintained, supported, updated or enhanced;
 * (5) will not be liable for any indirect, incidental, consequential special
 * or punitive damages of any kind or nature, including but not limited to lost
 * profits or loss of data, on any basis arising from contract, tort or
 * otherwise, even if any of the parties has been warned of the possibility of
 * such loss or damage.
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */
package edu.si.sidora.tabularmetadata.datatype;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.copyOf;
import static java.util.EnumSet.noneOf;
import static java.util.EnumSet.of;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.joda.time.format.DateTimeFormat.forPattern;
import static org.joda.time.format.DateTimeFormat.fullDateTime;
import static org.joda.time.format.DateTimeFormat.mediumDateTime;
import static org.joda.time.format.DateTimeFormat.shortDateTime;
import static org.joda.time.format.ISODateTimeFormat.dateOptionalTimeParser;
import static org.joda.time.format.ISODateTimeFormat.dateTimeParser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

/**
 * This enumeration constructs
 * <ul>
 * <li>our "native" type system, as well as</li>
 * <li>maps from lexical spaces in our system to selected portions of selected value spaces in the Java type system, and
 * </li>
 * <li>maps from some of our types to types in the XSD type system.</li>
 * </ul>
 * It also offers convenience methods for working with our type system.
 * 
 * @author A. Soroka
 */
public enum DataType {

	String(W3C_XML_SCHEMA_NS_URI + "#string", null) {

		/*
		 * This override indicates that String is the top type.
		 */
		@Override
		public EnumSet<DataType> supertypes() {
			return of((DataType) this);
		}

		@SuppressWarnings("unchecked")
		@Override
		public String parse(final java.lang.String s) {
			return s;
		}
	},
	Decimal(W3C_XML_SCHEMA_NS_URI + "#decimal", String) {

		@SuppressWarnings("unchecked")
		@Override
		public Float parse(final java.lang.String s) throws ParsingException {
			try {
				return parseFloat(s.trim());
			} catch (final NumberFormatException e) {
				throw new ParsingException("Could not parse as Decimal!", e);
			}
		}
	},
	Integer(W3C_XML_SCHEMA_NS_URI + "#integer", Decimal) {

		@SuppressWarnings("unchecked")
		@Override
		public Integer parse(final java.lang.String s) throws ParsingException {
			try {
				return parseInt(s.trim());
			} catch (final NumberFormatException e) {
				throw new ParsingException("Could not parse as Integer!", e);
			}
		}
	},
	URI(W3C_XML_SCHEMA_NS_URI + "#anyURI", String) {

		@SuppressWarnings("unchecked")
		@Override
		public URI parse(final java.lang.String s) throws ParsingException {
			try {
				if (URI_REGEX.matcher(s).matches()) { return new URI(s); }
				throw new URISyntaxException(s, "Could not validate URI!");
			} catch (final URISyntaxException e) {
				throw new ParsingException("Could not parse as URI!", e);
			}
		}
	},
	NonNegativeInteger(W3C_XML_SCHEMA_NS_URI + "#nonNegativeInteger", Integer) {

		@SuppressWarnings("unchecked")
		@Override
		public Integer parse(final java.lang.String s) throws ParsingException {

			final Integer value = Integer.parse(s);
			if (value > -1) { return value; }
			throw new ParsingException("Value was negative!");
		}
	},
	PositiveInteger(W3C_XML_SCHEMA_NS_URI + "#positiveInteger", NonNegativeInteger) {

		@SuppressWarnings("unchecked")
		@Override
		public Integer parse(final java.lang.String s) throws ParsingException {
			final Integer value = NonNegativeInteger.parse(s);
			if (value > 0) { return value; }
			throw new ParsingException("Value was negative!");
		}
	},
	Geographic(null, String) {

		@SuppressWarnings("unchecked")
		@Override
		public GeographicValue parse(final String s) throws ParsingException {
			try {
				final List<Float> parts = stream(s.split(",")).map(Float::parseFloat).collect(toList());
				return new GeographicValue(parts);
			} catch (final NumberFormatException e) {
				throw new ParsingException("Could not parse as Geographic!", e);
			}
		}
	},
	Boolean(W3C_XML_SCHEMA_NS_URI + "#boolean", String) {

		@SuppressWarnings("unchecked")
		@Override
		public Boolean parse(final java.lang.String s) throws ParsingException {
			if (BOOLEAN_TRUE_REGEXP.matcher(s).matches()) { return true; }
			if (BOOLEAN_FALSE_REGEXP.matcher(s).matches()) { return false; }
			throw new ParsingException("Could not parse as Boolean!");
		}
	},
	DateTime(W3C_XML_SCHEMA_NS_URI + "#dateTime", String) {

		@SuppressWarnings("unchecked")
		@Override
		public DateTime parse(final java.lang.String s) throws ParsingException {
			for (final DateTimeFormatter format : dateTimeFormats) {
				try {
					return format.parseDateTime(s);
				} catch (final IllegalArgumentException e) {
					// log.trace("Could not parse date '{}' in form {}.");
				}
			}
			throw new ParsingException("Could not parse as DataTime!");
		}
	};

	public static final EnumSet<DataType> valuesSet() {
		return allOf(DataType.class);
	}

	private DataType(final String uri, final DataType supertype) {
		this.uri = uri == null ? null : java.net.URI.create(uri);
		this.supertype = supertype;
	}

	/**
	 * An URI, if any, with which this DataType is associated
	 */
	public final URI uri;

	/**
	 * The immediate supertype of this DataType.
	 */
	private final DataType supertype;

	/**
	 * We memoize the calculation of supertypes to avoid redoing this recursion during operation.
	 */
	private static final EnumMap<DataType, EnumSet<DataType>> supertypesMemo = new EnumMap<>(DataType.class);

	/**
	 * @return The supertypes of this type, including itself.
	 */
	public EnumSet<DataType> supertypes() {
		return supertypesMemo.computeIfAbsent(this, type -> {
			final EnumSet<DataType> types = copyOf(supertype.supertypes());
			types.add(type);
			return types;
		});
	}

	/**
	 * Map from lex to Java value.
	 * 
	 * @param s
	 * @return s parsed into an instance of a Java type representing this {@link DataType}
	 * @throws ParsingException
	 */
	abstract public <T extends Comparable<T>> T parse(final String s) throws ParsingException;

	/**
	 * @param lex
	 * @return true iff {@code lex} is parseable into this data type
	 */
	public boolean parseable(String lex) {
		try {
			parse(lex);
		} catch (ParsingException e) {
			return false;
		}
		return true;
	}

	/**
	 * @param s A value to parse
	 * @return an {@link EnumSet} of those DataTypes into which s can be parsed
	 */
	public static EnumSet<DataType> parseableAs(final String s) {
		return valuesSet().stream().filter(t -> t.parseable(s)).collect(enumSetCollector);
	}

	/**
	 * A simple {@link Collector} into an {@link EnumSet}.
	 */
	private static final Collector<DataType, EnumSet<DataType>, EnumSet<DataType>> enumSetCollector = Collector
			.<DataType, EnumSet<DataType>> of(() -> noneOf(DataType.class), EnumSet::add, (l, r) -> {
				l.addAll(r);
				return l;
			});

	/**
	 * A simple ordering by hierarchy. Those types with more supertypes are considered "smaller" than those with fewer.
	 * In case of a tie, natural enum order prevails.
	 */
	public static final Comparator<DataType> orderingByHierarchy = comparing(
			(DataType type) -> type.supertypes().size()).reversed().thenComparing(naturalOrder());

	// private static final Logger log = getLogger(DataType.class);

	/**
	 * How to recognize a Boolean lex for true.
	 */
	public static final Pattern BOOLEAN_TRUE_REGEXP = compile("^true|t$", CASE_INSENSITIVE);

	/**
	 * How to recognize a Boolean lex for false.
	 */
	public static final Pattern BOOLEAN_FALSE_REGEXP = compile("^false|f$", CASE_INSENSITIVE);

	/**
	 * We use the well-known regex from <a href="http://tools.ietf.org/html/rfc3986#appendix-B">the standard</a> but we
	 * disallow relative URIs.
	 */
	static Pattern URI_REGEX = compile("^(([^:/?#]+):)(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");

	static List<DateTimeFormatter> dateTimeFormats = asList(shortDateTime(), mediumDateTime(), fullDateTime(),
			dateOptionalTimeParser(), dateTimeParser(), forPattern("yyyy-MM-dd HH:mm:ss"));
}
