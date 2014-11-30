
package com.asoroka.sidora.tabularmetadata.testframework;

import static com.asoroka.sidora.tabularmetadata.testframework.TestUtilities.randomValues;
import static com.asoroka.sidora.tabularmetadata.testframework.TestUtilities.toPotentialAssignment;
import static com.googlecode.totallylazy.Iterators.range;
import static com.googlecode.totallylazy.Sequences.forwardOnly;
import static com.googlecode.totallylazy.Sequences.sequence;

import java.util.List;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.testframework.TestUtilities.RandomValuesForAType;
import com.googlecode.totallylazy.Callable1;
import com.googlecode.totallylazy.Sequence;

public class RowsOfRandomValuesForAllTypesSupplier extends ParameterSupplier {

    @Override
    public List<PotentialAssignment> getValueSources(final ParameterSignature sig) {

        final RowsOfRandomValuesForAllTypes metadata = sig.getAnnotation(RowsOfRandomValuesForAllTypes.class);
        final short valuesPerType = metadata.valuesPerType();
        final short numRowsPerType = metadata.numRowsPerType();

        return sequence(DataType.values()).flatMap(rowsOfRandomValuesPerType(numRowsPerType, valuesPerType))
                .map(toPotentialAssignment()).toList();

    }

    private static Callable1<DataType, Sequence<RandomValuesForAType>> rowsOfRandomValuesPerType(
            final short numRowsPerType, final short valuesPerType) {
        return new Callable1<DataType, Sequence<RandomValuesForAType>>() {

            @Override
            public Sequence<RandomValuesForAType> call(final DataType type) {
                return forwardOnly(range(0, numRowsPerType)).map(randomValuesForAType(valuesPerType, type));
            }
        };
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
