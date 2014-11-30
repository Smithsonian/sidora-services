
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.NonNegativeInteger;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.PositiveInteger;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.String;
import static com.asoroka.sidora.tabularmetadata.testframework.TestUtilities.addValues;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.testframework.RowsOfRandomValuesForAllTypes;
import com.asoroka.sidora.tabularmetadata.testframework.TestUtilities.RandomValuesForAType;

@RunWith(Theories.class)
public class FractionHeuristicTest extends PerTypeHeuristicTestFrame<FractionHeuristic> {

    @DataPoints
    public static DataType[] datatypes = DataType.values();

    private static final Logger log = getLogger(FractionHeuristicTest.class);

    @Override
    protected FractionHeuristic newTestHeuristic() {
        return new FractionHeuristic(0.2F);
    }

    @Theory
    public void inputsWithNoUnparseableValuesShouldBeRecognizedAsTheirTrueType(@RowsOfRandomValuesForAllTypes(
            numRowsPerType = 5, valuesPerType = 50) final RandomValuesForAType values) {
        final FractionHeuristic testHeuristic = newTestHeuristic();
        addValues(testHeuristic, values);
        final DataType type = values.type;
        if (type.equals(NonNegativeInteger)) {
            // NonNegativeInteger and PositiveInteger differ by only one value (0); it's difficult to tell them apart
            assertTrue(type.equals(NonNegativeInteger) || type.equals(PositiveInteger));
        } else {
            assertEquals(type, testHeuristic.mostLikelyType());
        }
    }

    @Theory
    public void testThatInputsWithOnlyOneUnparseableValueShouldBeRecognizedAsTheirTrueType(
            @RowsOfRandomValuesForAllTypes(
                    numRowsPerType = 5, valuesPerType = 50) final RandomValuesForAType values) {
        final FractionHeuristic testHeuristic = newTestHeuristic();
        // A UUID could only be recognized as a String
        values.add(randomUUID());
        addValues(testHeuristic, values);
        final DataType type = values.type;
        if (type.equals(NonNegativeInteger)) {
            // NonNegativeInteger and PositiveInteger differ by only one value (0); it's difficult to tell them apart
            assertTrue(type.equals(NonNegativeInteger) || type.equals(PositiveInteger));
        } else {
            assertEquals(type, testHeuristic.mostLikelyType());
        }
    }

    @Theory
    public void testThatInputsWithHalfUnparseableValuesShouldBeNotRecognizedAsTheirTrueType(
            @RowsOfRandomValuesForAllTypes(
                    numRowsPerType = 5, valuesPerType = 50) final RandomValuesForAType values) {
        // nothing cannot be recognized as a String
        assumeThat(values.type, not(is(String)));

        final FractionHeuristic testHeuristic = newTestHeuristic();
        // A UUID could only be recognized as a String
        final int numValues = values.size();
        for (byte counter = 0; counter < numValues; counter++) {
            values.add(randomUUID());
        }
        addValues(testHeuristic, values);
        assertNotEquals(values.type, testHeuristic.mostLikelyType());

    }

    @Theory
    public void testAcceptAllTypesInTheAbsenceOfData(final DataType type) {
        final FractionHeuristic testHeuristic = newTestHeuristic();
        assertTrue(testHeuristic.typesAsLikely().contains(type));
    }
}
