
package com.asoroka.sidora.tabularmetadata.test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

import org.junit.experimental.theories.ParametersSuppliedBy;

/**
 * Machinery for generating random test values.
 * 
 * @author ajs6f
 */
@Retention(RUNTIME)
@ParametersSuppliedBy(RowsOfRandomValuesForAllTypesSupplier.class)
public @interface RowsOfRandomValuesForAllTypes {

    /**
     * Number of "rows" of test data to supply per type.
     */
    short numRowsPerType();

    /**
     * Number of of test values to supply per "row".
     */
    short valuesPerType();
}