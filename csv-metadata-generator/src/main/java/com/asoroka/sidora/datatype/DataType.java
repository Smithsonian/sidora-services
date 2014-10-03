/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.datatype;

import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.filter;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Collections.max;
import static java.util.EnumSet.allOf;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.joda.time.format.ISODateTimeFormat.dateTimeParser;

import java.net.URI;
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
 * @author ajs6f
 */
public enum DataType {

    String(W3C_XML_SCHEMA_NS_URI + "string", null) {

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
    Decimal(W3C_XML_SCHEMA_NS_URI + "decimal", String) {

        @SuppressWarnings("unchecked")
        @Override
        public Float parse(final java.lang.String s) throws ParsingException {
            try {
                return parseFloat(s);
            } catch (final NumberFormatException e) {
                throw new ParsingException(e);
            }
        }
    },
    Integer(W3C_XML_SCHEMA_NS_URI + "integer", Decimal) {

        @SuppressWarnings("unchecked")
        @Override
        public Integer parse(final java.lang.String s) throws ParsingException {
            try {
                return parseInt(s);
            } catch (final NumberFormatException e) {
                throw new ParsingException(e);
            }
        }
    },
    NonNegativeInteger(W3C_XML_SCHEMA_NS_URI + "nonNegativeInteger", Integer) {

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
    PositiveInteger(W3C_XML_SCHEMA_NS_URI + "positiveInteger", NonNegativeInteger) {

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
                throw new ParsingException(e);
            }
        }
    },
    Boolean(W3C_XML_SCHEMA_NS_URI + "boolean", String) {

        @SuppressWarnings("unchecked")
        @Override
        public Boolean parse(final java.lang.String s) throws ParsingException {
            if (BOOLEAN_TRUE.matcher(s).matches()) {
                return true;
            }
            if (BOOLEAN_FALSE.matcher(s).matches()) {
                return false;
            }
            throw new ParsingException();
        }
    },
    DateTime(W3C_XML_SCHEMA_NS_URI + "dateTime", String) {

        @SuppressWarnings("unchecked")
        @Override
        public DateTime parse(final java.lang.String s) throws ParsingException {
            try {
                return dateTimeParser().parseDateTime(s);
            } catch (final IllegalArgumentException e) {
                throw new ParsingException(e);
            }
        }
    };

    private DataType(final String uri, final DataType supertype) {
        this.xsdType = uri == null ? null : URI.create(uri);
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
     * A memoized form of the supertypes of this DataType. We memoize this to avoid redoing this recursive calculation
     * in operation.
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
     * @param s
     * @return s parsed into this DataType
     * @throws ParsingException
     */
    abstract public <T> T parse(final String s) throws ParsingException;

    /**
     * @param s A value to parse
     * @return a {@link Set} of those DataTypes into which s can be parsed
     */
    public static Set<DataType> parseableAs(final String s) {
        return filter(allOf(DataType.class), new Predicate<DataType>() {

            @Override
            public boolean apply(final DataType t) {
                try {
                    t.parse(s.trim());
                    return true;
                } catch (final ParsingException e) {
                    return false;
                }
            }
        });
    }

    /**
     * An ordering by type hierarchy. Those types with more supertypes are considered "lower" than those with fewer.
     */
    public static final Comparator<DataType> orderingByHierarchy = new Comparator<DataType>() {

        @Override
        public int compare(final DataType left, final DataType right) {
            return left.supertypes().size() - right.supertypes().size();
        }
    };

    /**
     * @param types The types to compare.
     * @return The first type with as low as position in the hierarchy as any other.
     */
    public static DataType firstMostRestrictiveType(final Collection<DataType> types) {
        return max(types, orderingByHierarchy);
    }

    /**
     * @return Can this be considered a numeric type?
     */
    public boolean isComparable() {
        return supertypes().contains(Decimal);
    }

    // private static final Logger log = getLogger(DataType.class);

    static final Pattern BOOLEAN_TRUE = compile("true|t", CASE_INSENSITIVE);

    static final Pattern BOOLEAN_FALSE = compile("false|f", CASE_INSENSITIVE);

    static final Function<java.lang.String, Float> string2float = new Function<String, Float>() {

        @Override
        public Float apply(final java.lang.String seg) {
            return Float.parseFloat(seg);
        }
    };

}
