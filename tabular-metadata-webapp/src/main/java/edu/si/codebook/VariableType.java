
package edu.si.codebook;

import static edu.si.codebook.EnumerationType.enumerationType;
import static edu.si.codebook.RangeType.rangeType;
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

    private Range<?> range;

    private Set<String> enumeration;

    private String name;

    private String type;

    public static VariableType variableType(final String name, final String type, final Range<?> range,
            final Set<String> enumeration) {
        final VariableType variableType = new VariableType();
        variableType.name = name;
        variableType.type = type;
        variableType.range = range;
        variableType.enumeration = enumeration;
        return variableType;
    }

    @XmlElement
    public RangeType getRange() {
        return range == null ? null : rangeType(range);
    }

    @XmlElement
    public EnumerationType getEnumeration() {
        if (enumeration == null || enumeration.isEmpty()) {
            return null;
        }
        return enumerationType(enumeration);
    }

    @XmlAttribute
    public String getName() {
        return name;
    }

    @XmlAttribute
    @XmlSchemaType(name = "anyURI")
    public String getType() {
        if (type == null) {
            return "http://www.w3.org/2001/XMLSchema#string";
        }
        return type;
    }
}
