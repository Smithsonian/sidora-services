/*
 * Copyright 2015-2016 Smithsonian Institution.
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
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */

package edu.si.codebook;

import edu.si.sidora.tabularmetadata.TabularMetadata;
import edu.si.sidora.tabularmetadata.TabularMetadata.Ratio;
import edu.si.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;


import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static edu.si.codebook.Codebook.VariableType.EnumerationList.enumerationList;
import static edu.si.codebook.Codebook.VariableType.RangeType.rangeType;
import static edu.si.codebook.Codebook.VariableType.variableType;
import static javax.xml.bind.annotation.XmlAccessType.FIELD;
import static javax.xml.bind.annotation.XmlAccessType.NONE;

/**
 * Constructs an SI-schema XML serialization of a precis of metadata results.
 *
 * @author A. Soroka
 * @author Jason Birkhimer
 *
 */
@XmlAccessorType(NONE)
@XmlRootElement
@XmlType(propOrder = {"title", "description", "creator", "date", "identifier", "variables"})
public class Codebook {

    private TabularMetadata metadata;

    @XmlElement(namespace = "http://purl.org/dc/terms/", nillable = true)
    public String title, description, creator, date, identifier;

    @XmlElementWrapper
    @XmlElement(name = "variable")
    public List<VariableType> getVariables() {
        List<VariableType> output = new ArrayList<>();
        for (int i = 0; i < metadata.headerNames().size(); i++) {
            final String name = metadata.headerNames().get(i);
            final Ratio unparseableOverTotal = metadata.unparseablesOverTotals().get(i);
            final DataType type = metadata.fieldTypes().get(i);
            final Range<?> range = metadata.minMaxes().get(i).get(type);
            final Set<String> enumeration = metadata.enumeratedValues().get(i).get(type);
            output.add(variableType(name, unparseableOverTotal, type, range, enumeration));
        }
        return output;
    }

    public static Codebook codebook(final TabularMetadata m) {
        final Codebook codebook = new Codebook();
        codebook.metadata = m;
        return codebook;
    }

    /**
     * Serializes a single variable description.
     */
    @XmlAccessorType(NONE)
    @XmlType(propOrder = {
            "label",
            "description",
            "enumeration",
            "projection",
            "datum",
            "format",
            "range",
            "uom",
            "valueForMissingValue",
            "dateFormat"})
    public static class VariableType {

        private Range<?> range;

        protected Set<String> enumeration;

        public String vocabulary;

        @XmlAttribute
        public String name, type;

        private Ratio unparseableOverTotal;

        @XmlElement(nillable = true)
        private String label, description, projection, datum, format, uom, dateFormat;

        @XmlElement(defaultValue = "--", nillable = true)
        public String valueForMissingValue;

        @XmlElement
        public EnumerationList getEnumeration() {
            return enumerationList(enumeration, vocabulary);
        }

        @XmlElement
        public RangeType getRange() {
            return range == null ? null : rangeType(range);
        }

        protected static VariableType variableType(final String name, final Ratio unparseableOverTotal,
                                                   final DataType type, final Range<?> range,
                                                   final Set<String> enumeration) {
            final VariableType variableType = new VariableType();
            variableType.name = name;
            variableType.unparseableOverTotal = unparseableOverTotal;
            variableType.type = getType(type.name().toLowerCase());
            variableType.range = range;
            variableType.enumeration = enumeration;
            variableType.label = "label of " + name;
            variableType.description = "desc of " + name;
            variableType.projection = null;
            variableType.datum = null;
            variableType.format = null;
            variableType.uom = null;
            variableType.valueForMissingValue = "--";
            variableType.dateFormat = null;
            variableType.vocabulary = name;
            return variableType;
        }

        private static String getType(String type) {

            switch (type) {
                case "decimal":
                    type = "numeric";
                case "integer":
                    type = "numeric";
                case "positiveinteger":
                    type = "numeric";
                case "nonNegativeinteger":
                    type = "numeric";
            }
            return type;
        }

        /**
         * Serializes the list of enumeration values.
         */
        @XmlAccessorType(FIELD)
        public static class EnumerationList {

            @XmlAttribute
            public String vocabulary;


            @XmlElement(name = "value")
            public Set<String> enumeration;

            protected static EnumerationList enumerationList(final Set<String> enumeration, final String vocabulary) {
                final EnumerationList enumerationList = new EnumerationList();
                enumerationList.enumeration = enumeration;
                enumerationList.vocabulary = vocabulary;
                return enumerationList;
            }
        }

        /**
         * Serializes the range of a single variable.
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
