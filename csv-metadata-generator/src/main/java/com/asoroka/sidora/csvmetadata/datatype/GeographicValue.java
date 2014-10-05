
package com.asoroka.sidora.csvmetadata.datatype;

import java.util.ArrayList;
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

    public List<Float> coordinates = new ArrayList<>();

    /**
     * @param coordinates
     */
    public GeographicValue(final List<Float> coordinates) {
        if (coordinates.size() < MINIMUM_DIMENSION || coordinates.size() > MAXIMUM_DIMENSION) {
            throw new IllegalArgumentException("Unacceptable dimensionality for coordinates!");
        }
        this.coordinates = coordinates;
    }

    @Override
    public int compareTo(final GeographicValue o) {
        return ordering.compare(this.coordinates, o.coordinates);
    }
}
