
package com.asoroka.sidora.csvmetadata;

import static com.asoroka.sidora.csvmetadata.test.TestUtilities.cloneableMockStrategy;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.csv.CSVParser.parse;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.asoroka.sidora.csvmetadata.CsvScanner;
import com.asoroka.sidora.csvmetadata.datatype.DataType;
import com.asoroka.sidora.csvmetadata.heuristics.DataTypeHeuristic;
import com.google.common.base.Function;

@RunWith(MockitoJUnitRunner.class)
public class CsvScannerTest {

    @Mock
    DataType mockDataType;

    @Mock
    DataTypeHeuristic<?> mockStrategy;

    private static final File smalltestfile = new File("src/test/resources/test-data/small-test.csv");

    private ArrayList<DataType> expectedResults;

    private static final Logger log = getLogger(CsvScannerTest.class);

    @Before
    public void setUp() {
        expectedResults = newArrayList(mockDataType, mockDataType, mockDataType, mockDataType);
        when(mockStrategy.mostLikelyType()).thenReturn(mockDataType);
    }

    @Test
    public void testOperation() throws IOException {
        final CsvScanner testScanner;
        try (final CSVParser parser = parse(smalltestfile, UTF_8, DEFAULT.withHeader())) {
            log.debug("Found header map: {}", parser.getHeaderMap());
            testScanner = new CsvScanner(parser, cloneableMockStrategy(mockStrategy));
            testScanner.scan(0);
        }
        final List<DataType> guesses =
                transform(testScanner.getStrategies(), getMostLikelyType);
        assertEquals("Failed to find the correct column types!", expectedResults, guesses);
    }

    @Test
    public void testOperationWithLimitedScan() throws IOException {
        final CsvScanner testScanner;
        try (final CSVParser parser = parse(smalltestfile, UTF_8, DEFAULT.withHeader())) {
            log.debug("Found header map: {}", parser.getHeaderMap());
            testScanner = new CsvScanner(parser, cloneableMockStrategy(mockStrategy));
            testScanner.scan(2);
        }
        final List<DataType> guesses =
                transform(testScanner.getStrategies(), getMostLikelyType);
        assertEquals("Failed to find the correct column types!", expectedResults, guesses);
    }

    /**
     * Extracts the most likely type selection from a {@link DataTypeHeuristic}
     */
    private static final Function<DataTypeHeuristic<?>, DataType> getMostLikelyType =
            new Function<DataTypeHeuristic<?>, DataType>() {

                @Override
                public DataType apply(final DataTypeHeuristic<?> heuristic) {
                    return heuristic.mostLikelyType();
                }
            };
}
