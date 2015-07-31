/*
 * Copyright 2015 Smithsonian Institution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.You may obtain a copy of
 * the License at: http://www.apache.org/licenses/
 *
 * This software and accompanying documentation is supplied without
 * warranty of any kind. The copyright holder and the Smithsonian Institution:
 * (1) expressly disclaim any warranties, express or implied, including but not
 * limited to any implied warranties of merchantability, fitness for a
 * particular purpose, title or non-infringement; (2) do not assume any legal
 * liability or responsibility for the accuracy, completeness, or usefulness of
 * the software; (3) do not represent that use of the software would not
 * infringe privately owned rights; (4) do not warrant that the software
 * is error-free or will be maintained, supported, updated or enhanced;
 * (5) will not be liable for any indirect, incidental, consequential special
 * or punitive damages of any kind or nature, including but not limited to lost
 * profits or loss of data, on any basis arising from contract, tort or
 * otherwise, even if any of the parties has been warned of the possibility of
 * such loss or damage.
 *
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */

package edu.si.codebook;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;
import com.googlecode.totallylazy.Function1;
import com.googlecode.totallylazy.Quintuple;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.numbers.Ratio;
import edu.si.codebook.Codebook.VariableType;

import javax.xml.bind.annotation.*;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import static com.googlecode.totallylazy.Sequences.zip;
import static edu.si.codebook.Codebook.VariableType.RangeType.rangeType;
import static edu.si.codebook.Codebook.VariableType.variableType;
import static javax.xml.bind.annotation.XmlAccessType.FIELD;
import static javax.xml.bind.annotation.XmlAccessType.NONE;

/**
 * Constructs an SI-schema XML serialization of a precis of metadata results.
 *
 * @author A. Soroka
 */
@XmlAccessorType(NONE)
@XmlRootElement
public class Codebook
        extends
        Function1<Quintuple<String, Ratio, DataType, Map<DataType, Range<?>>, Map<DataType, Set<String>>>, VariableType> {

    private TabularMetadata metadata;

    @XmlElementWrapper
    @XmlElement(name = "variable")
    public Sequence<VariableType> getVariables() {
        return zip(metadata.headerNames(), metadata.unparseablesOverTotals(), metadata.fieldTypes(),
                metadata.minMaxes(), metadata.enumeratedValues()).map(this);
    }

    /**
     * Constructs a single variable description.
     */
    @Override
    public VariableType call(
            final Quintuple<String, Ratio, DataType, Map<DataType, Range<?>>, Map<DataType, Set<String>>> data) {
        final String name = data.first();
        final Ratio unparseableOverTotal = data.second();
        final DataType type = data.third();
        final Range<?> range = data.fourth().get(type);
        final Set<String> enumeration = data.fifth().get(type);
        return variableType(name, unparseableOverTotal, type, range, enumeration);
    }

    public static Codebook codebook(final TabularMetadata m) {
        final Codebook codebook = new Codebook();
        codebook.metadata = m;
        return codebook;
    }

    /**
     * Serializes a single variable description.
     *
     * @author A. Soroka
     */
    @XmlAccessorType(NONE)
    public static class VariableType {

        private Range<?> range;

        @XmlElementWrapper
        @XmlElement(name = "value")
        public Set<String> enumeration;

        @XmlAttribute
        public String name;

        @XmlAttribute
        public URI type;

        private Ratio unparseableOverTotal;

        protected static VariableType variableType(final String name, final Ratio unparseableOverTotal,
                                                   final DataType type, final Range<?> range,
                                                   final Set<String> enumeration) {
            final VariableType variableType = new VariableType();
            variableType.name = name;
            variableType.unparseableOverTotal = unparseableOverTotal;
            variableType.type = type.uri;
            variableType.range = range;
            variableType.enumeration = enumeration;
            return variableType;
        }

        @XmlElement
        String getDescription() {
            return "[Found " + unparseableOverTotal.numerator + " values that failed to parse as " + type +
                    " out of " + unparseableOverTotal.denominator + " values examined by automatic process.]";
        }

        @XmlElement
        public RangeType getRange() {
            return range == null ? null : rangeType(range);
        }

        /**
         * Serializes the range of a single variable.
         *
         * @author A. Soroka
         */
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