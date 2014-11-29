
package com.asoroka.sidora.tabularmetadata.test;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Decimal;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Integer;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.NonNegativeInteger;
import static com.google.common.collect.Maps.toMap;
import static java.lang.Math.abs;
import static java.lang.Math.random;
import static java.lang.Math.round;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.joda.time.DateTime.now;
import static org.junit.experimental.theories.PotentialAssignment.forValue;

import java.lang.annotation.Retention;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.ParametersSuppliedBy;
import org.junit.experimental.theories.PotentialAssignment;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.datatype.GeographicValue;
import com.google.common.base.Function;

/**
 * Machinery for generating random test values.
 * 
 * @author ajs6f
 */
@Retention(RUNTIME)
@ParametersSuppliedBy(RandomValues.RandomValuesSupplier.class)
public @interface RandomValues {

    /**
     * Number of "rows" of test data to supply. A "row" is a {@link Map}s from {@link DataType}s to
     * {@link #valuesPerType()} random values for that type.
     */
    short numRuns();

    /**
     * Number of values to supply per {@link DataType}.
     */
    short valuesPerType();

    public static class RandomValuesSupplier extends ParameterSupplier {

        @Override
        public List<PotentialAssignment> getValueSources(final ParameterSignature sig) {

            final RandomValues range = sig.getAnnotation(RandomValues.class);
            final List<PotentialAssignment> results = new ArrayList<>(range.numRuns());

            for (short runIndex = 0; runIndex < range.numRuns(); runIndex++) {
                final Map<DataType, List<Comparable<?>>> resultsPerRun =
                        randomValuesForAllTypes(range.valuesPerType());
                results.add(forValue(null, resultsPerRun));
            }
            return results;
        }

        private static Map<DataType, List<Comparable<?>>> randomValuesForAllTypes(final short numValues) {
            return toMap(DataType.valuesSet(), new Function<DataType, List<Comparable<?>>>() {

                @Override
                public List<Comparable<?>> apply(final DataType type) {
                    return randomValues(numValues, type);
                }
            });
        }

        public static List<Comparable<?>> randomValues(final short numValues, final DataType type) {
            final List<Comparable<?>> values = new ArrayList<>(numValues);
            for (short valueIndex = 0; valueIndex < numValues; valueIndex++) {
                values.add(generateRandomValue(type));
            }
            return values;
        }

        private static Comparable<?> generateRandomValue(final DataType type) {
            switch (type) {
            case Boolean:
                return random() > 0.5;
            case DateTime:
                return now().plus(round(random() * 1000000000000F));
            case Decimal:
                return (float) (random() - 0.5) * 10;
            case Geographic:
                if ((Boolean) generateRandomValue(DataType.Boolean)) {
                    return new GeographicValue(asList(
                            (Float) generateRandomValue(Decimal),
                            (Float) generateRandomValue(Decimal)));
                }
                return new GeographicValue(asList(
                        (Float) generateRandomValue(Decimal),
                        (Float) generateRandomValue(Decimal),
                        (Float) generateRandomValue(Decimal)));
            case Integer:
                return (int) round((Float) generateRandomValue(Decimal));
            case NonNegativeInteger:
                return abs((Integer) generateRandomValue(Integer));
            case PositiveInteger:
                return (Integer) generateRandomValue(NonNegativeInteger) + 1;
            case String:
                return randomUUID().toString();
            case URI:
                return URI.create("info:" + generateRandomValue(DataType.String));
            default:
                throw new AssertionError("A DataType of an un-enumerated kind should never exist!");
            }
        }
    }
}