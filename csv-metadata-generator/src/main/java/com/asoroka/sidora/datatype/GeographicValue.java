
package com.asoroka.sidora.datatype;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Ordering;

/**
 * @author ajs6f
 */
public class GeographicValue implements Comparable<GeographicValue> {

    private static final Ordering<Iterable<Float>> ordering = Ordering.<Float> natural().lexicographical();

    public List<Float> coordinates = new ArrayList<>();

    /**
     * @param coordinates
     */
    public GeographicValue(final List<Float> coordinates) {
        if (coordinates.size() < 2 || coordinates.size() > 3) {
            throw new IllegalArgumentException();
        }
        this.coordinates = coordinates;
    }

    @Override
    public int compareTo(final GeographicValue o) {
        return ordering.compare(this.coordinates, o.coordinates);
    }
}
