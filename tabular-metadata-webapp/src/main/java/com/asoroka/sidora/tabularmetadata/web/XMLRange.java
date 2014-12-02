
package com.asoroka.sidora.tabularmetadata.web;

import static javax.xml.bind.annotation.XmlAccessType.PUBLIC_MEMBER;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Range;

@XmlRootElement
@XmlAccessorType(PUBLIC_MEMBER)
public class XMLRange {

    private Range<?> r;

    public XMLRange() {
    }

    public XMLRange(final Range<?> r) {
        this.r = r;
    }

    @XmlElement
    public String getMin() {
        return r.hasLowerBound() ? r.lowerEndpoint().toString() : null;

    }

    @XmlElement
    public String getMax() {
        return r.hasUpperBound() ? r.upperEndpoint().toString() : null;
    }

}
