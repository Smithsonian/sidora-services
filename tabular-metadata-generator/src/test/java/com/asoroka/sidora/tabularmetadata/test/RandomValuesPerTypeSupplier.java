
package com.asoroka.sidora.tabularmetadata.test;

import static com.asoroka.sidora.tabularmetadata.test.TestUtilities.randomValues;
import static com.asoroka.sidora.tabularmetadata.test.TestUtilities.toPotentialAssignment;
import static com.googlecode.totallylazy.Pair.pair;
import static com.googlecode.totallylazy.Sequences.sequence;

import java.util.List;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.test.TestUtilities.RandomValuesForAType;
import com.googlecode.totallylazy.Callable1;
import com.googlecode.totallylazy.Pair;

public class RandomValuesPerTypeSupplier extends ParameterSupplier {

    @Override
    public List<PotentialAssignment> getValueSources(final ParameterSignature sig) {

        final RandomValuesPerType metadata = sig.getAnnotation(RandomValuesPerType.class);
        final short numValues = metadata.numValues();

        return randomValuesPerType(numValues);
    }

    static List<PotentialAssignment> randomValuesPerType(final short numValues) {
        final Callable1<DataType, Pair<DataType, RandomValuesForAType>> randomValuesPerType =
                new Callable1<DataType, Pair<DataType, RandomValuesForAType>>() {

                    @Override
                    public Pair<DataType, RandomValuesForAType> call(final DataType type) {
                        return pair(type, randomValues(type, numValues));
                    }
                };

        return sequence(DataType.valuesSet()).map(randomValuesPerType).map(
                toPotentialAssignment()).toList();
    }
}
