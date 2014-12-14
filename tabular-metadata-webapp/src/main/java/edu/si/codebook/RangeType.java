
package edu.si.codebook;

import static javax.xml.bind.annotation.XmlAccessType.FIELD;

import javax.xml.bind.annotation.XmlAccessorType;

import com.google.common.collect.Range;

@XmlAccessorType(FIELD)
public class RangeType {

    private String min;

    private String max;

    public static RangeType rangeType(final Range<?> r) {
        final RangeType rangeType = new RangeType();
        rangeType.min = r.hasLowerBound() ? r.lowerEndpoint().toString() : null;
        rangeType.max = r.hasUpperBound() ? r.upperEndpoint().toString() : null;
        return rangeType;
    }

    public String getMin() {
        return min;
    }

    public String getMax() {
        return max;
    }

}
