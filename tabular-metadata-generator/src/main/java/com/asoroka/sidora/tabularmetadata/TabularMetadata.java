
package com.asoroka.sidora.tabularmetadata;

import java.util.List;
import java.util.Map;
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

    /**
     * @param headerNames
     * @param fieldTypes
     * @param minMaxes
     */
    public TabularMetadata(final List<String> headerNames, final List<SortedSet<DataType>> fieldTypes,
            final List<Map<DataType, Range<?>>> minMaxes) {
        this.headerNames = headerNames;
        this.fieldTypes = fieldTypes;
        this.minMaxes = minMaxes;
    }

    /**
     * @return A list of header names, empty if there was no header row found.
     */
    public List<String> headerNames() {
        return headerNames;
    }

    /**
     * @return A list of field datatypes.
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
}
