
package edu.si.codebook;

import static javax.xml.bind.annotation.XmlAccessType.FIELD;

import javax.xml.bind.annotation.XmlAccessorType;

import com.google.common.collect.Range;

@XmlAccessorType(FIELD)
public class RangeType {

    protected String min;

    protected String max;

    public String getMin() {
        return min;
    }

    public RangeType setRange(final Range<?> r) {
        this.min = r.hasLowerBound() ? r.lowerEndpoint().toString() : null;
        this.max = r.hasUpperBound() ? r.upperEndpoint().toString() : null;
        return this;
    }

    public void setMin(final String m) {
        this.min = m;
    }

    public String getMax() {
        return max;
    }

    public void setMax(final String m) {
        this.max = m;
    }
}
