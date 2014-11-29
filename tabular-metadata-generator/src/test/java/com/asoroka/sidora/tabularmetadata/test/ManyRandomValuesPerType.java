
package com.asoroka.sidora.tabularmetadata.test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.util.Map;

import org.junit.experimental.theories.ParametersSuppliedBy;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

/**
 * Machinery for generating random test values.
 * 
 * @author ajs6f
 */
@Retention(RUNTIME)
@ParametersSuppliedBy(ManyRandomValuesPerTypeSupplier.class)
public @interface ManyRandomValuesPerType {

    /**
     * Number of "rows" of test data to supply. A "row" is a {@link Map}s from {@link DataType}s to
     * {@link #valuesPerType()} random values for that type.
     */
    short numRuns();

    /**
     * Number of values to supply per {@link DataType}.
     */
    short valuesPerType();
}