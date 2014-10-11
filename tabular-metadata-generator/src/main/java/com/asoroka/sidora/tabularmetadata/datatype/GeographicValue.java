
package com.asoroka.sidora.tabularmetadata.datatype;

import java.util.List;

import com.google.common.collect.Ordering;

/**
 * A simple type for geographic coordinates.
 * 
 * @author ajs6f
 */
public class GeographicValue implements Comparable<GeographicValue> {

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
}
