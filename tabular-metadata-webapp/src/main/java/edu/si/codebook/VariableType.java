
package edu.si.codebook;

import static javax.xml.bind.annotation.XmlAccessType.NONE;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.Range;

@XmlAccessorType(NONE)
@XmlType(propOrder = { "range", "enumeration", })
public class VariableType {

    protected Range<?> range;

    protected Set<String> enumeration;

    @XmlAttribute(name = "name", required = true)
    protected String name;

    @XmlAttribute(name = "type")
    @XmlSchemaType(name = "anyURI")
    protected String type;

    @XmlElement
    public RangeType getRange() {
        if (range == null) {
            return null;
        }
        if (range.hasLowerBound() && range.hasLowerBound()) {
            return new RangeType().setRange(range);
        }
        return null;
    }

    public VariableType setRange(final Range<?> r) {
        this.range = r;
        return this;
    }

    @XmlElement
    public EnumerationType getEnumeration() {
        if (enumeration == null || enumeration.isEmpty()) {
            return null;
        }
        return new EnumerationType().setValues(enumeration);
    }

    public VariableType setEnumeration(final Set<String> e) {
        this.enumeration = e;
        return this;
    }

    public String getName() {
        return name;
    }

    public VariableType setName(final String n) {
        this.name = n;
        return this;
    }

    public String getType() {
        if (type == null) {
            return "http://www.w3.org/2001/XMLSchema#string";
        }
        return type;
    }

    public VariableType setType(final String t) {
        this.type = t;
        return this;
    }
}
