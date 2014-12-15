
package com.asoroka.sidora.tabularmetadata;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.googlecode.totallylazy.Sequences.sequence;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;
import com.googlecode.totallylazy.Function1;
import com.googlecode.totallylazy.Pair;

/**
 * A container for the results of metadata extraction on a single data file, with machinery to present those results
 * in appropriate orders.
 * 
 * @author ajs6f
 */
public class TabularMetadata {

    private final List<String> headerNames;

    private final List<SortedSet<DataType>> fieldTypes;

    private final List<Map<DataType, Range<?>>> minMaxes;

    private final List<Map<DataType, Set<String>>> enumeratedValues;

    /**
     * @param headerNames
     * @param fieldTypes
     * @param minMaxes
     * @param enumeratedValues
     */
    public TabularMetadata(final List<String> headerNames, final List<SortedSet<DataType>> fieldTypes,
            final List<Map<DataType, Range<?>>> minMaxes, final List<Map<DataType, Set<String>>> enumeratedValues) {
        this.headerNames = headerNames;
        this.fieldTypes = fieldTypes;
        this.minMaxes = minMaxes;
        this.enumeratedValues = enumeratedValues;
    }

    /**
     * Sorts an indexed {@link Map<DataType,T>} by the {@link Comparator} that is used for the correspondingly-indexed
     * field in {@link #fieldTypes()}. WARNING: Must not be used before setting {@link #fieldTypes}!
     */
    private <T> Function1<Pair<Number, Map<DataType, T>>, NavigableMap<DataType, T>> sortByLikelihood() {
        return new Function1<Pair<Number, Map<DataType, T>>, NavigableMap<DataType, T>>() {

            @Override
            public NavigableMap<DataType, T> call(final Pair<Number, Map<DataType, T>> minMaxMapWithIndex) {
                final int fieldIndex = minMaxMapWithIndex.getKey().intValue();
                final Map<DataType, T> minMaxMap = minMaxMapWithIndex.getValue();

                final SortedSet<DataType> typesForThisField = fieldTypes().get(fieldIndex);
                final TreeMap<DataType, T> sortedByLikelihood = new TreeMap<>(typesForThisField.comparator());
                for (final DataType type : typesForThisField) {
                    sortedByLikelihood.put(type, minMaxMap.get(type));
                }
                return sortedByLikelihood;
            }
        };
    }

    /**
     * @return A list of prospective header names.
     */
    public List<String> headerNames() {
        return headerNames;
    }

    /**
     * @return A list of candidates for field datatypes, one candidate set per field. Each candidate set is sorted
     *         according to a notion of decreasing plausibility that is specific to the type-determination strategy
     *         used.
     */
    public List<SortedSet<DataType>> fieldTypes() {
        return fieldTypes;
    }

    /**
     * @return A list of maps from {@link DataType}s to {@link Range}s, with each data type mapped to the minimum and
     *         maximum for each field, <i>if</i> that field is treated as of that type. The maps are sorted in the
     *         same manner as is {@link #fieldTypes()}. The values of the endpoints of the ranges, if they exist, are
     *         in the Java type value-space associated to the datatype considered most likely for that field. The idea
     *         here is that when a given field is finally determined to be of a given type (by user action after
     *         automatic action), the appropriate range can be looked up at that time. This is to ensure that in a
     *         situation where the type determination strategy employed gave a wrong answer, the correct answer for
     *         range can still be found after the type determination has been corrected.
     * @see com.google.common.collect.Range
     */
    public List<NavigableMap<DataType, Range<?>>> minMaxes() {
        return sequence(minMaxes).zipWithIndex().map(this.<Range<?>> sortByLikelihood()).toList();
    }

    /**
     * @return A list (one element for each field in the input file) of maps from each possible datatype to an set of
     *         the lexes found in that field that were parseable into that data type. The maps are sorted in the same
     *         manner as is {@link #fieldTypes()}.
     */
    public List<NavigableMap<DataType, Set<String>>> enumeratedValues() {
        return sequence(enumeratedValues).zipWithIndex().map(this.<Set<String>> sortByLikelihood()).toList();
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("headerNames", headerNames()).add("fieldTypes", fieldTypes())
                .add("enumeratedValues", enumeratedValues()).add("minMaxes", minMaxes()).toString();
    }
}
