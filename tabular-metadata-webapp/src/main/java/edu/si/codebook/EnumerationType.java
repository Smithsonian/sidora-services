
package edu.si.codebook;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.xml.bind.annotation.XmlAccessType.FIELD;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(FIELD)
public class EnumerationType {

    private Set<String> values;

    public static EnumerationType enumerationType(final Set<String> vs) {
        final EnumerationType enumerationType = new EnumerationType();
        enumerationType.values = vs;
        return enumerationType;
    }

    public Set<String> getValue() {
        checkNotNull(values);
        return values;
    }
}
