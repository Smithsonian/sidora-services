
package com.asoroka.sidora.csvmetadata;

import java.util.List;

import com.asoroka.sidora.csvmetadata.datatype.DataType;
import com.google.common.collect.Range;

/**
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

    public List<String> headerNames() {
        return headerNames;
    }

    public List<DataType> columnTypes() {
        return columnTypes;
    }

    public List<Range<?>> minMaxes() {
        return minMaxes;
    }
}
