
package edu.si.codebook;

import static com.googlecode.totallylazy.Sequences.zip;
import static edu.si.codebook.VariableType.variableType;
import static javax.xml.bind.annotation.XmlAccessType.NONE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedSet;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;
import com.googlecode.totallylazy.Function1;
import com.googlecode.totallylazy.Quadruple;
import com.googlecode.totallylazy.Sequence;

@XmlAccessorType(NONE)
@XmlRootElement(name = "codebook")
public class Codebook
        extends
        Function1<Quadruple<String, SortedSet<DataType>, NavigableMap<DataType, Range<?>>, NavigableMap<DataType, Set<String>>>, VariableType> {

    private TabularMetadata metadata;

    private static final Logger log = getLogger(Codebook.class);

    @XmlElementWrapper
    @XmlElement(name = "variable")
    public Sequence<VariableType> getVariables() {
        return zip(metadata.headerNames(), metadata.fieldTypes(), metadata.minMaxes(), metadata.enumeratedValues())
                .map(this);
    }

    @Override
    public VariableType call(
            final Quadruple<String, SortedSet<DataType>, NavigableMap<DataType, Range<?>>, NavigableMap<DataType, Set<String>>> data) {
        final String name = data.first();
        final String type = data.second().first().uri.toString();
        final Range<?> range = data.third().firstEntry().getValue();
        final Set<String> enumeration = data.fourth().firstEntry().getValue();
        return variableType(name, type, range, enumeration);
    }

    public static Codebook codebook(final TabularMetadata m) {
        final Codebook codebook = new Codebook();
        codebook.metadata = m;
        return codebook;
    }
}
