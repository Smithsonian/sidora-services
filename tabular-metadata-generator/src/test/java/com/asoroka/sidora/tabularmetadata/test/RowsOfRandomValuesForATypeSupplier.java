
package com.asoroka.sidora.tabularmetadata.test;

import static com.asoroka.sidora.tabularmetadata.test.TestUtilities.randomValues;
import static com.asoroka.sidora.tabularmetadata.test.TestUtilities.toPotentialAssignment;
import static com.googlecode.totallylazy.Iterators.range;
import static com.googlecode.totallylazy.Sequences.forwardOnly;

import java.util.List;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.test.TestUtilities.RandomValuesForAType;
import com.googlecode.totallylazy.Callable1;

public class RowsOfRandomValuesForATypeSupplier extends ParameterSupplier {

    @Override
    public List<PotentialAssignment> getValueSources(final ParameterSignature sig) {

        final RowsOfRandomValuesForAType metadata = sig.getAnnotation(RowsOfRandomValuesForAType.class);
        final DataType type = metadata.type();
        final short numRows = metadata.numRows();
        final short valuesPerType = metadata.valuesPerType();

        return forwardOnly(range(0, numRows)).map(randomValuesForAType(valuesPerType, type)).map(
                toPotentialAssignment()).toList();
    }

    static Callable1<Number, RandomValuesForAType> randomValuesForAType(
            final short valuesPerType, final DataType type) {
        return new Callable1<Number, RandomValuesForAType>() {

            @Override
            public RandomValuesForAType call(final Number input) {
                return randomValues(type, valuesPerType);
            }
        };
    }
}