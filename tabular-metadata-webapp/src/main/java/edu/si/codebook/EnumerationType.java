
package edu.si.codebook;

import static java.util.Collections.emptySet;
import static javax.xml.bind.annotation.XmlAccessType.FIELD;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(FIELD)
@XmlType(name = "enumerationType")
public class EnumerationType {

    protected Set<String> values;

    public Set<String> getValue() {
        if (values == null) {
            return emptySet();
        }
        return values;
    }

    public EnumerationType setValues(final Set<String> v) {
        this.values = v;
        return this;
    }
}
