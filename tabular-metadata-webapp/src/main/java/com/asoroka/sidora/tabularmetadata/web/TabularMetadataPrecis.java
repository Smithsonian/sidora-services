
package com.asoroka.sidora.tabularmetadata.web;

import static com.google.common.collect.ImmutableList.builder;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.SortedSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class TabularMetadataPrecis {

    private TabularMetadata metadata;

    private static final Logger log = getLogger(TabularMetadataPrecis.class);

    /**
     * Constructor for JAX-RS
     */
    public TabularMetadataPrecis() {
    }

    /**
     * Real default constructor.
     * 
     * @param m
     */
    public TabularMetadataPrecis(final TabularMetadata m) {
        this.metadata = m;
    }

    @XmlList
    public List<String> getHeaders() {
        return metadata.headerNames;
    }

    @XmlList
    public List<DataType> getMostLikelyTypes() {
        return getFirstElements(metadata.fieldTypes);
    }

    @XmlList
    public List<String> getMostLikelyRanges() {
        final ImmutableList.Builder<String> b = builder();
        for (int i = 0; i < metadata.minMaxes.size(); i++) {
            final DataType mostLikelyType = getMostLikelyTypes().get(i);
            log.trace("Found most likely type {} for field number {}", mostLikelyType, i);
            b.add(rangeToString(metadata.minMaxes.get(i).get(mostLikelyType)));
        }
        return b.build();
    }

    private static String rangeToString(final Range<?> r) {
        return r.toString();
    }

    private static <T> List<T> getFirstElements(final Iterable<SortedSet<T>> inputs) {
        final ImmutableList.Builder<T> b = builder();
        for (final SortedSet<T> s : inputs) {
            b.add(s.first());
        }
        return b.build();
    }

}
