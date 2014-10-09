
package com.asoroka.sidora.tabularmetadata;

import java.util.List;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;

/**
 * A container for the results of metadata extraction on a single data file.
 * 
 * @author ajs6f
 */
public class CsvMetadata {

    private final List<String> headerNames;

    private final List<DataType> columnTypes;

    private final List<Range<?>> minMaxes;

    /**
     * @param headerNames
     * @param columnTypes
     * @param minMaxes
     */
    public CsvMetadata(final List<String> headerNames, final List<DataType> columnTypes,
            final List<Range<?>> minMaxes) {
        this.headerNames = headerNames;
        this.columnTypes = columnTypes;
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
    public List<DataType> fieldTypes() {
        return columnTypes;
    }

    /**
     * @return A list of closed {@link Range}s, with the minimum and maximum for each field. The values of the
     *         endpoints of the ranges are in the Java type value-space associated to the datatype considered most
     *         likely for that field.
     */
    public List<Range<?>> minMaxes() {
        return minMaxes;
    }
}
