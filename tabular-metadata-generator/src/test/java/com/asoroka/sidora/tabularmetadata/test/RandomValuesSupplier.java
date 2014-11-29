
package com.asoroka.sidora.tabularmetadata.test;

import static com.asoroka.sidora.tabularmetadata.test.TestUtilities.randomValues;
import static com.asoroka.sidora.tabularmetadata.test.TestUtilities.toPotentialAssignment;
import static com.googlecode.totallylazy.Sequences.sequence;

import java.util.List;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

public class RandomValuesSupplier extends ParameterSupplier {

    @Override
    public List<PotentialAssignment> getValueSources(final ParameterSignature sig) {

        final RandomValues metadata = sig.getAnnotation(RandomValues.class);
        return sequence(randomValues(metadata.type(), metadata.numValues())).map(toPotentialAssignment()).toList();
    }
}
