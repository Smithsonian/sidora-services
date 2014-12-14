
package edu.si.codebook;

import static com.googlecode.totallylazy.Sequences.zip;
import static edu.si.codebook.Codebook.VariableType.variableType;
import static edu.si.codebook.Codebook.VariableType.RangeType.rangeType;
import static javax.xml.bind.annotation.XmlAccessType.FIELD;
import static javax.xml.bind.annotation.XmlAccessType.NONE;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedSet;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;
import com.googlecode.totallylazy.Function1;
import com.googlecode.totallylazy.Quadruple;
import com.googlecode.totallylazy.Sequence;

import edu.si.codebook.Codebook.VariableType;

@XmlAccessorType(NONE)
@XmlRootElement(name = "codebook")
public class Codebook
        extends
        Function1<Quadruple<String, SortedSet<DataType>, NavigableMap<DataType, Range<?>>, NavigableMap<DataType, Set<String>>>, VariableType> {

    private TabularMetadata metadata;

    private static final Logger log = getLogger(Codebook.class);

    @XmlElementWrapper
    @XmlElement(name = "variable")
    public Sequence<edu.si.codebook.Codebook.VariableType> getVariables() {
        return zip(metadata.headerNames(), metadata.fieldTypes(), metadata.minMaxes(), metadata.enumeratedValues())
                .map(this);
    }

    @Override
    public VariableType call(
            final Quadruple<String, SortedSet<DataType>, NavigableMap<DataType, Range<?>>, NavigableMap<DataType, Set<String>>> data) {
        final String name = data.first();
        final URI type = data.second().first().uri;
        final Range<?> range = data.third().firstEntry().getValue();
        final Set<String> enumeration = data.fourth().firstEntry().getValue();
        return variableType(name, type, range, enumeration);
    }

    public static Codebook codebook(final TabularMetadata m) {
        final Codebook codebook = new Codebook();
        codebook.metadata = m;
        return codebook;
    }

    @XmlAccessorType(NONE)
    @XmlType(propOrder = { "range", "enumeration", })
    public static class VariableType {

        private Range<?> range;

        @XmlElementWrapper
        @XmlElement(name = "value")
        public Set<String> enumeration;

        @XmlAttribute
        public String name;

        @XmlAttribute
        public URI type;

        protected static VariableType variableType(final String name, final URI type, final Range<?> range,
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

        @XmlAccessorType(FIELD)
        public static class RangeType {

            public String min, max;

            protected static RangeType rangeType(final Range<?> r) {
                final RangeType rangeType = new RangeType();
                rangeType.min = r.hasLowerBound() ? r.lowerEndpoint().toString() : null;
                rangeType.max = r.hasUpperBound() ? r.upperEndpoint().toString() : null;
                return rangeType;
            }
        }
    }
}
