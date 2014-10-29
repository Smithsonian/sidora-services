
package com.asoroka.sidora.tabularmetadata.web;

import static com.google.common.collect.ImmutableList.builder;
import static com.google.common.collect.Lists.transform;
import static javax.xml.bind.annotation.XmlAccessType.PUBLIC_MEMBER;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

/**
 * A JAXB-supported class to serialize a precis of tabular data metadata.
 * 
 * @author ajs6f
 */
@XmlRootElement
@XmlAccessorType(PUBLIC_MEMBER)
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

    @XmlElementWrapper
    @XmlElement(name = "mostLikelyHeader")
    public List<String> getLikelyHeaders() {
        return metadata.headerNames;
    }

    @XmlElementWrapper
    @XmlElement(name = "mostLikelyType")
    public List<DataType> getMostLikelyTypes() {
        return getFirstElements(metadata.fieldTypes);
    }

    @XmlElementWrapper
    @XmlElement(name = "mostLikelyRange")
    public List<XMLRange> getMostLikelyRanges() {
        final ImmutableList.Builder<XMLRange> b = builder();
        for (int i = 0; i < metadata.minMaxes.size(); i++) {
            final DataType mostLikelyType = getMostLikelyTypes().get(i);
            log.trace("Found most likely type {} for field number {}", mostLikelyType, i);
            b.add(new XMLRange(metadata.minMaxes.get(i).get(mostLikelyType)));
        }
        return b.build();
    }

    private static <T> List<T> getFirstElements(final List<SortedSet<T>> inputs) {
        return transform(inputs, TabularMetadataPrecis.<T> firstOfIterable());
    }

    private static final <T> Function<Iterable<T>, T> firstOfIterable() {
        return new Function<Iterable<T>, T>() {

            @Override
            public T apply(final Iterable<T> s) {
                final Iterator<T> iterator = s.iterator();
                return iterator.hasNext() ? iterator.next() : null;
            }
        };
    }
}
