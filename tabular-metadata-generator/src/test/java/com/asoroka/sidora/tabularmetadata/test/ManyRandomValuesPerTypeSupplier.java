
package com.asoroka.sidora.tabularmetadata.test;

import static com.asoroka.sidora.tabularmetadata.test.RandomValuesPerTypeSupplier.randomValuesPerType;
import static com.asoroka.sidora.tabularmetadata.test.TestUtilities.toPotentialAssignment;
import static com.googlecode.totallylazy.Iterators.range;
import static com.googlecode.totallylazy.Maps.map;
import static com.googlecode.totallylazy.Sequences.forwardOnly;
import static com.googlecode.totallylazy.Sequences.sequence;

import java.util.List;
import java.util.Map;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.test.TestUtilities.RandomValuesForAType;
import com.googlecode.totallylazy.Callable1;
import com.googlecode.totallylazy.Pair;

public class ManyRandomValuesPerTypeSupplier extends ParameterSupplier {

    @Override
    public List<PotentialAssignment> getValueSources(final ParameterSignature sig) {

        final ManyRandomValuesPerType metadata = sig.getAnnotation(ManyRandomValuesPerType.class);
        final short valuesPerType = metadata.valuesPerType();
        final short numRuns = metadata.numRuns();

        return forwardOnly(range(0, numRuns)).map(randomValuesForAType(valuesPerType)).map(toPotentialAssignment())
                .toList();
    }

    private static Callable1<Number, Map<DataType, RandomValuesForAType>> randomValuesForAType(
            final short valuesPerType) {
        return new Callable1<Number, Map<DataType, RandomValuesForAType>>() {

            @Override
            public Map<DataType, RandomValuesForAType> call(final Number input) {
                return map(sequence(randomValuesPerType(valuesPerType)).map(
                        TestUtilities.<Pair<DataType, RandomValuesForAType>> fromPotentialAssignment()));
            }
        };
    }
}