
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
@ParametersSuppliedBy(RandomValuesPerTypeSupplier.class)
public @interface RandomValuesPerType {

    /**
     * Number of values to supply per type.
     */
    short numValues();

}