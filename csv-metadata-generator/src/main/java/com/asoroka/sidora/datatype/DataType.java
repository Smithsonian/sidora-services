/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.datatype;

import static com.asoroka.sidora.datatype.Namespaces.XML_DATATYPES_NAMESPACE;
import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Sets.filter;
import static java.util.Arrays.asList;
import static java.util.Collections.max;
import static java.util.EnumSet.allOf;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static org.joda.time.format.ISODateTimeFormat.dateParser;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;

/**
 * @author ajs6f
 */
public enum DataType {

    String(XML_DATATYPES_NAMESPACE + "string", null) {

        /*
         * Anything can be parsed as merely a String.
         */
        @Override
        public boolean canParse(final String s) {
            return true;
        }

        /*
         * This override indicates that String is the top type.
         */
        @Override
        public Set<DataType> supertypes() {
            return EnumSet.of((DataType) this);
        }
    },
    Decimal(XML_DATATYPES_NAMESPACE + "decimal", String) {

        @Override
        public boolean canParse(final String s) {
            final java.lang.String[] parts = s.split("\\.");
            if (parts.length > 2) return false;
            if (parts.length == 0) return Integer.canParse(s);
            return Integer.canParse(parts[0]) && Integer.canParse(parts[0]);
        }
    },
    Integer(XML_DATATYPES_NAMESPACE + "integer", Decimal) {

        @Override
        public boolean canParse(final String s) {
            try {
                java.lang.Integer.parseInt(s);
                return true;
            } catch (final NumberFormatException e) {
                return false;
            }
        }
    },
    NonNegativeInteger(XML_DATATYPES_NAMESPACE + "nonNegativeInteger", Integer) {

        @Override
        public boolean canParse(final String s) {
            return Integer.canParse(s) && (java.lang.Integer.parseInt(s) > -1);
        }
    },
    PositiveInteger(XML_DATATYPES_NAMESPACE + "positiveInteger", NonNegativeInteger) {

        @Override
        public boolean canParse(final String s) {
            return NonNegativeInteger.canParse(s) && (java.lang.Integer.parseInt(s) > 0);
        }
    },
    Geographic(null, String) {

        @Override
        public boolean canParse(final String s) {
            final List<java.lang.String> parts = asList(s.split(","));
            // we consider only geographic data in two or three coordinates
            if (parts.size() > 3 || parts.size() < 2) return false;
            // are all coordinates proper decimals?
            return all(parts,
                    new Predicate<String>() {

                        @Override
                        public boolean apply(final java.lang.String seg) {
                            return Decimal.canParse(seg);
                        }
                    });
        }
    },
    Boolean(XML_DATATYPES_NAMESPACE + "boolean", String) {

        @Override
        public boolean canParse(final String s) {
            return BOOLEAN_TRUE.matcher(s).matches() || BOOLEAN_FALSE.matcher(s).matches();
        }
    },
    DateTime(XML_DATATYPES_NAMESPACE + "dateTime", String) {

        @Override
        public boolean canParse(final String s) {
            try {
                dateParser().parseDateTime(s);
                return true;
            }
            catch (final IllegalArgumentException e) {
                return false;
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
     * The self-type of this DataType. We must record it because Java does not handle self-types well.
     */
    final DataType selftype = this;

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
            return EnumSet.of(selftype, EnumSet.of(supertype, supertype.supertypes().toArray(new DataType[0]))
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
     * @return whether s can be parsed into this DataType
     */
    abstract public boolean canParse(final String s);

    /**
     * @param s A value to parse
     * @return a {@link Set} of those DataTypes into which s can be parsed
     */
    public static Set<DataType> parseableAs(final String s) {
        return filter(allOf(DataType.class), new Predicate<DataType>() {

            @Override
            public boolean apply(final DataType t) {
                return t.canParse(s.trim());
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
    public boolean isNumeric() {
        return supertypes().contains(Decimal);
    }

    private static final Logger log = getLogger(DataType.class);

    static final Pattern BOOLEAN_TRUE = compile("true", CASE_INSENSITIVE);

    static final Pattern BOOLEAN_FALSE = compile("false", CASE_INSENSITIVE);

}
