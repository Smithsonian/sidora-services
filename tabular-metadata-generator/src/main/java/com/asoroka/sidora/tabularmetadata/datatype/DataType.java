/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.tabularmetadata.datatype;

import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Ordering.natural;
import static com.google.common.collect.Sets.filter;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.copyOf;
import static java.util.EnumSet.of;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
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
import java.util.EnumSet;
import java.util.List;
import java.util.SortedSet;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

/**
 * This enumeration constructs
 * <ul>
 * <li>our "native" type system, as well as</li>
 * <li>maps from lexical spaces in our system to selected portions of selected value spaces in the Java type system,
 * and</li>
 * <li>maps from our types to types in the XSD type system.</li>
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
                if (URI_REGEX.matcher(s).matches()) {
                    return new URI(s);
                }
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
            if (value > -1) {
                return value;
            }
            throw new ParsingException("Value was negative!");
        }
    },
    PositiveInteger(W3C_XML_SCHEMA_NS_URI + "#positiveInteger", NonNegativeInteger) {

        @SuppressWarnings("unchecked")
        @Override
        public Integer parse(final java.lang.String s) throws ParsingException {
            final Integer value = NonNegativeInteger.parse(s);
            if (value > 0) {
                return value;
            }
            throw new ParsingException("Value was negative!");
        }
    },
    Geographic(null, String) {

        @SuppressWarnings("unchecked")
        @Override
        public GeographicValue parse(final String s) throws ParsingException {
            try {
                final List<Float> parts = transform(asList(s.split(",")), string2float);
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
            if (BOOLEAN_TRUE_REGEXP.matcher(s).matches()) {
                return true;
            }
            if (BOOLEAN_FALSE_REGEXP.matcher(s).matches()) {
                return false;
            }
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
                }
                catch (final IllegalArgumentException e) {
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
     * This DataType, recorded because Java doesn't handle lexical "this" as we might like.
     */
    final DataType self = this;

    /**
     * The immediate supertype of this DataType.
     */
    final DataType supertype;

    /**
     * We memoize the calculation of supertypes to avoid redoing this recursion during operation.
     */
    private final Supplier<EnumSet<DataType>> supertypesMemo = memoize(new Supplier<EnumSet<DataType>>() {

        @Override
        public EnumSet<DataType> get() {
            final EnumSet<DataType> types = copyOf(supertype.supertypes());
            types.add(self);
            return types;
        }
    });

    /**
     * @return The supertypes of this type, including itself.
     */
    public EnumSet<DataType> supertypes() {
        return supertypesMemo.get();
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
     * @param s A value to parse
     * @return an {@link EnumSet} of those DataTypes into which s can be parsed
     */
    public static EnumSet<DataType> parseableAs(final String s) {
        return copyOf(filter(DataType.valuesSet(), new Predicate<DataType>() {

            @Override
            public boolean apply(final DataType t) {
                try {
                    t.parse(s);
                    return true;
                } catch (final ParsingException e) {
                    return false;
                }
            }
        }));
    }

    private static Function<DataType, Integer> numSuperTypes = new Function<DataType, Integer>() {

        @Override
        public Integer apply(final DataType type) {
            return type.supertypes().size();
        }
    };

    /**
     * A simple ordering by hierarchy. Those types with more supertypes are considered "smaller" than those with
     * fewer. In case of a tie, natural enum order prevails.
     */
    private static final Comparator<DataType> orderingByHierarchy = natural().reverse()
            .onResultOf(numSuperTypes).compound(natural());

    public static SortedSet<DataType> sortByHierarchy(final Iterable<DataType> types) {
        return from(types).toSortedSet(orderingByHierarchy);
    }

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
     * We use the well-known regex from <a href="http://tools.ietf.org/html/rfc3986#appendix-B">the standard</a> but
     * we disallow relative URIs.
     */
    static Pattern URI_REGEX = compile("^(([^:/?#]+):)(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");

    static final Function<java.lang.String, Float> string2float = new Function<String, Float>() {

        @Override
        public Float apply(final java.lang.String seg) {
            return parseFloat(seg);
        }
    };

    static List<DateTimeFormatter> dateTimeFormats = ImmutableList.of(shortDateTime(), mediumDateTime(),
            fullDateTime(), dateOptionalTimeParser(), dateTimeParser(), forPattern("yyyy-MM-dd HH:mm:ss"));

}
