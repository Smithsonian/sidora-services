
package com.asoroka.sidora.datatype;

import java.util.List;

/**
 * @author ajs6f
 */
public class GeographicValue {

    public Float firstCoordinate, secondCoordinate, thirdCoordinate;

    /**
     * @param coordinates
     */
    public GeographicValue(final Float... coordinates) {
        if (coordinates.length < 2 || coordinates.length > 3) {
            throw new IllegalArgumentException();
        }
        this.firstCoordinate = coordinates[0];
        this.secondCoordinate = coordinates[1];
        if (coordinates.length == 3) {
            this.thirdCoordinate = coordinates[2];
        }
    }

    /**
     * @param coordinates
     */
    public GeographicValue(final List<Float> coordinates) {
        this(coordinates.toArray(new Float[0]));
    }

}
