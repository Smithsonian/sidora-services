
package com.asoroka.sidora.tabularmetadata.datatype;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Iterables.elementsEqual;

import java.io.Serializable;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Ordering;

/**
 * A simple type for geographic coordinates.
 * 
 * @author ajs6f
 */
public class GeographicValue implements Comparable<GeographicValue>, Serializable {

    private static final Joiner TO_STRING_HELPER = on(',');

    private static final long serialVersionUID = 1L;

    public static final byte MAXIMUM_DIMENSION = 3;

    public static final byte MINIMUM_DIMENSION = 2;

    private static final Ordering<Iterable<Float>> ordering = Ordering.<Float> natural().lexicographical();

    /**
     * The coordinates of this {@link GeographicValue}.
     */
    public final List<Float> coordinates;

    /**
     * @param coordinates
     */
    public GeographicValue(final List<Float> coordinates) {
        final int size = coordinates.size();
        if (size < MINIMUM_DIMENSION || size > MAXIMUM_DIMENSION) {
            throw new NumberFormatException("Unacceptable dimensionality " + size + " for coordinates!");
        }
        this.coordinates = coordinates;
    }

    /**
     * Lexicographical ordering.
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     **/
    @Override
    public int compareTo(final GeographicValue o) {
        return ordering.compare(this.coordinates, o.coordinates);
    }

    @Override
    public String toString() {
        return TO_STRING_HELPER.join(coordinates);
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof GeographicValue) {
            final GeographicValue other = (GeographicValue) o;
            return elementsEqual(coordinates, other.coordinates);
        }
        return false;
    }
}
