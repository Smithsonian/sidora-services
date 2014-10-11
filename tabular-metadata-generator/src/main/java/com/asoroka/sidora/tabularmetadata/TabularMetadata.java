
package com.asoroka.sidora.tabularmetadata;

import java.util.List;
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

    private final List<Range<?>> minMaxes;

    /**
     * @param headerNames
     * @param fieldTypes
     * @param minMaxes
     */
    public TabularMetadata(final List<String> headerNames, final List<SortedSet<DataType>> fieldTypes,
            final List<Range<?>> minMaxes) {
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
     * @return A list of closed {@link Range}s, with the minimum and maximum for each field. The values of the
     *         endpoints of the ranges are in the Java type value-space associated to the datatype considered most
     *         likely for that field.
     * @see com.google.common.collect.Range<?>
     */
    public List<Range<?>> minMaxes() {
        return minMaxes;
    }
}
