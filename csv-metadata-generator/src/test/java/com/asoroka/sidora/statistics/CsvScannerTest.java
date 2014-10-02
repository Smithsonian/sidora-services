
package com.asoroka.sidora.statistics;

import static com.asoroka.sidora.datatype.DataType.Boolean;
import static com.asoroka.sidora.datatype.DataType.DateTime;
import static com.asoroka.sidora.datatype.DataType.PositiveInteger;
import static com.asoroka.sidora.datatype.DataType.String;
import static com.asoroka.sidora.datatype.DataType.isNumeric;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.csv.CSVParser.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.junit.Test;
import org.slf4j.Logger;

import com.asoroka.sidora.datatype.DataType;
import com.asoroka.sidora.statistics.heuristics.NotANumericFieldException;
import com.asoroka.sidora.statistics.heuristics.RunningMinMaxHeuristic;
import com.asoroka.sidora.statistics.heuristics.StrictlySatisfactoryType;
import com.asoroka.sidora.statistics.heuristics.TypeDeterminationHeuristic;

public class CsvScannerTest {

    private static final File smalltestfile = new File("src/test/resources/test-data/small-test.csv");

    private static final Logger log = getLogger(CsvScannerTest.class);

    @Test
    public void testBasicOperation() throws IOException {
        final ArrayList<DataType> expectedResults = newArrayList(PositiveInteger, String, Boolean, DateTime);
        final CsvScanner testScanner;
        try (final CSVParser parser = parse(smalltestfile, UTF_8, DEFAULT.withHeader())) {
            log.debug("Found header map: {}", parser.getHeaderMap());
            testScanner = new CsvScanner(parser, new StrictlySatisfactoryType());
            testScanner.scan();
        }
        final List<DataType> guesses = new ArrayList<>();
        int i = 0;
        for (final TypeDeterminationHeuristic<?> strategy : testScanner.getStrategies()) {
            final DataType guess = strategy.mostLikelyType();
            log.debug("I think column {} is of type: {}", i++, guess);
            guesses.add(guess);
        }
        assertEquals("Failed to find the correct column types!", expectedResults, guesses);
        assertEquals("Didn't find the right number of numeric datatypes!", 1, size(filter(guesses,
                isNumeric)));
        final RunningMinMaxHeuristic<?> firstColumnStrategy =
                (RunningMinMaxHeuristic<?>) testScanner.getStrategies().get(0);
        final RunningMinMaxHeuristic<?> secondColumnStrategy =
                (RunningMinMaxHeuristic<?>) testScanner.getStrategies().get(1);
        assertTrue("Didn't find first column to be numeric!", firstColumnStrategy.mostLikelyType().isNumeric());
        assertEquals("Didn't find the proper maximum in the first column!", (Float) 4F, firstColumnStrategy
                .getNumericMaximum());
        assertEquals("Didn't find the proper minimum in the first column!", (Float) 1F, firstColumnStrategy
                .getNumericMinimum());
        assertFalse("Found second column to be numeric when it should not be!", secondColumnStrategy.mostLikelyType()
                .isNumeric());
        try {
            secondColumnStrategy.getNumericMaximum();
            fail();
        } catch (final NotANumericFieldException e) {
            // expected
        }
        try {
            secondColumnStrategy.getNumericMinimum();
            fail();
        } catch (final NotANumericFieldException e) {
            // expected
        }
    }
}
