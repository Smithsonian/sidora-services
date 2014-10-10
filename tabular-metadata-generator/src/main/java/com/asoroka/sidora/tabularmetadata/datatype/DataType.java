/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.tabularmetadata.datatype;

import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.filter;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Collections.max;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.complementOf;
import static java.util.EnumSet.copyOf;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.joda.time.format.ISODateTimeFormat.dateTimeParser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.joda.time.DateTime;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;

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
 * @author ajs6f
 */
public enum DataType {

    String(W3C_XML_SCHEMA_NS_URI + "#string", null) {

        /*
         * This override indicates that String is the top type.
         */
        @Override
        public Set<DataType> supertypes() {
            return EnumSet.of((DataType) this);
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
                if (!URI_REGEX.matcher(s).matches()) {
                    throw new URISyntaxException(s, "Could not validate URI!");
                }
                return new URI(s);
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
                final List<Float> parts =
                        transform(asList(s.split(",")), string2float);
                return new GeographicValue(parts);
            } catch (final IllegalArgumentException e) {
                throw new ParsingException("Could not parse as Geographic!", e);
            }
        }
    },
    Boolean(W3C_XML_SCHEMA_NS_URI + "#boolean", String) {

        @SuppressWarnings("unchecked")
        @Override
        public Boolean parse(final java.lang.String s) throws ParsingException {
            if (BOOLEAN_TRUE.matcher(s).matches()) {
                return true;
            }
            if (BOOLEAN_FALSE.matcher(s).matches()) {
                return false;
            }
            throw new ParsingException("Could not parse as Boolean!");
        }
    },
    DateTime(W3C_XML_SCHEMA_NS_URI + "#dateTime", String) {

        @SuppressWarnings("unchecked")
        @Override
        public DateTime parse(final java.lang.String s) throws ParsingException {
            try {
                return dateTimeParser().parseDateTime(s);
            } catch (final IllegalArgumentException e) {
                throw new ParsingException("Could not parse as DataTime!", e);
            }
        }
    };

    public static final EnumSet<DataType> setValues() {
        return allOf(DataType.class);
    }

    private DataType(final String uri, final DataType supertype) {
        this.xsdType = uri == null ? null : java.net.URI.create(uri);
        this.supertype = supertype;
    }

    /**
     * The XSD type, if any, with which this DataType is associated
     */
    public final URI xsdType;

    /**
     * This DataType, recorded because Java doesn't handle lexical "this" as we might like.
     */
    final DataType self = this;

    /**
     * The immediate supertype of this DataType.
     */
    final DataType supertype;

    /**
     * A memoized form of the supertypes of this DataType, used to avoid redoing this recursive calculation during
     * operation.
     */
    private final Supplier<Set<DataType>> supertypesSupplier = memoize(new Supplier<Set<DataType>>() {

        @Override
        public Set<DataType> get() {
            return EnumSet.of(self, EnumSet.of(supertype, supertype.supertypes().toArray(new DataType[0]))
                    .toArray(
                            new DataType[0]));
        }
    });

    /**
     * @return The supertypes of this type, including itself.
     */
    public Set<DataType> supertypes() {
        return supertypesSupplier.get();
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
        return copyOf(filter(DataType.setValues(), new Predicate<DataType>() {

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

    /**
     * @param value
     * @return an {@link EnumSet} of those DataTypes into which s cannot be parsed
     */
    public static EnumSet<DataType> notParseableAs(final String value) {
        return complementOf(parseableAs(value));
    }

    /**
     * An ordering by type hierarchy. Those types with more supertypes are considered "larger" than those with fewer.
     */
    public static final Comparator<DataType> orderingByHierarchy = new Comparator<DataType>() {

        @Override
        public int compare(final DataType left, final DataType right) {
            return left.supertypes().size() - right.supertypes().size();
        }
    };

    /**
     * @param types The types to compare.
     * @return The first type with as "bottom-most" as position in the hierarchy as any other.
     */
    public static DataType firstMostRestrictiveType(final Collection<DataType> types) {
        return max(types, orderingByHierarchy);
    }

    // private static final Logger log = getLogger(DataType.class);

    /**
     * How to recognize a Boolean lex for true.
     */
    public static final Pattern BOOLEAN_TRUE = compile("^true|t$", CASE_INSENSITIVE);

    /**
     * How to recognize a Boolean lex for false.
     */
    public static final Pattern BOOLEAN_FALSE = compile("^false|f$", CASE_INSENSITIVE);

    static final Function<java.lang.String, Float> string2float = new Function<String, Float>() {

        @Override
        public Float apply(final java.lang.String seg) {
            return parseFloat(seg);
        }
    };

    /**
     * We use the well-known regex from <a href="http://tools.ietf.org/html/rfc3986#appendix-B">the standard</a> but
     * we disallow relative URIs.
     */
    static Pattern URI_REGEX = compile("^(([^:/?#]+):)(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");

}
