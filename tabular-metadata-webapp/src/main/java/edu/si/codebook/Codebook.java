
package edu.si.codebook;

import static javax.xml.bind.annotation.XmlAccessType.NONE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.google.common.collect.Range;

@XmlAccessorType(NONE)
@XmlRootElement(name = "codebook")
public class Codebook {

    private TabularMetadata metadata;

    private static final Logger log = getLogger(Codebook.class);

    @XmlElementWrapper
    @XmlElement(name = "variable")
    public List<VariableType> getVariables() {
        final int numVariables = metadata.headerNames().size();
        final List<VariableType> variables = new ArrayList<>(numVariables);
        for (int i = 0; i < numVariables; i++) {
            final VariableType variable = new VariableType();
            final String name = metadata.headerNames().get(i);
            final String type = metadata.fieldTypes().get(i).first().uri.toString();
            final Range<?> range = metadata.minMaxes().get(i).firstEntry().getValue();
            final Set<String> enumeration = metadata.enumeratedValues().get(i).firstEntry().getValue();
            variable.setName(name).setType(type).setRange(range).setEnumeration(enumeration);
            variables.add(variable);
        }
        return variables;
    }

    public Codebook setMetadata(final TabularMetadata m) {
        this.metadata = m;
        return this;
    }

}
