
package com.asoroka.sidora.tabularmetadata.web;

import static com.google.common.collect.Lists.transform;
import static javax.xml.bind.annotation.XmlAccessType.PUBLIC_MEMBER;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.base.Function;
import com.google.common.collect.Range;

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
        return metadata.headerNames();
    }

    @XmlElementWrapper
    @XmlElement(name = "mostLikelyType")
    public List<DataType> getMostLikelyTypes() {
        return transform(metadata.fieldTypes(), TabularMetadataPrecis.<DataType> first());
    }

    @XmlElementWrapper
    @XmlElement(name = "mostLikelyRange")
    public List<XMLRange> getMostLikelyRanges() {
        return transform(metadata.minMaxes(), toXMLRange);
    }

    private static final <T> Function<SortedSet<T>, T> first() {
        return new Function<SortedSet<T>, T>() {

            @Override
            public T apply(final SortedSet<T> s) {
                return s.first();
            }
        };
    }

    private static final Function<SortedMap<DataType, Range<?>>, XMLRange> toXMLRange =
            new Function<SortedMap<DataType, Range<?>>, XMLRange>() {

                @Override
                public XMLRange apply(final SortedMap<DataType, Range<?>> minMaxForField) {
                    return new XMLRange(minMaxForField.get(minMaxForField.firstKey()));
                }
            };
}
