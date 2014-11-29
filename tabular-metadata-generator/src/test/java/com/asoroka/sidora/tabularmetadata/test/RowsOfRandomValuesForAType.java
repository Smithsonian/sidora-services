
package com.asoroka.sidora.tabularmetadata.test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

import org.junit.experimental.theories.ParametersSuppliedBy;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

/**
 * Machinery for generating random test values.
 * 
 * @author ajs6f
 */
@Retention(RUNTIME)
@ParametersSuppliedBy(RowsOfRandomValuesForATypeSupplier.class)
public @interface RowsOfRandomValuesForAType {

    /**
     * Number of "rows" of test data to supply.
     */
    short numRows();

    /**
     * {@link DataType} of values to supply.
     */
    DataType type();

    /**
     * Number of of test values to supply per "row".
     */
    short valuesPerType();
}