
package com.asoroka.sidora.tabularmetadata;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;

/**
 * A container for the results of metadata extraction on a single data file.
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
     * @return A list of header names, empty if there was no header row found.
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
     *         maximum for each field, <i>if</i> that field is treated as correctly of that type. The values of the
     *         endpoints of the ranges, if they exist, are in the Java type value-space associated to the datatype
     *         considered most likely for that field. The idea here is that when a given field is finally determined
     *         to be of a given type (by user action after automatic action), the appropriate range can be looked up
     *         at that time. This is to ensure that in a situation where the type determination strategy employed gave
     *         a wrong answer, the correct answer for range can still be found after the type determination has been
     *         corrected.
     * @see com.google.common.collect.Range<?>
     */
    public List<Map<DataType, Range<?>>> minMaxes() {
        return minMaxes;
    }

    /**
     * @return a list (one element for each field in the input file) of maps from each possible datatype to an set of
     *         the lexes found in that field that were parseable into that data type.
     */
    public List<Map<DataType, Set<String>>> enumeratedValues() {
        return enumeratedValues;
    }
}
