
package com.asoroka.sidora.statistics;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.csv.CSVParser.parse;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.Returns;
import org.slf4j.Logger;

import com.asoroka.sidora.datatype.DataType;
import com.asoroka.sidora.statistics.heuristics.TypeDeterminationHeuristic;
import com.google.common.base.Function;

public class CsvScannerTest {

    @Mock
    DataType mockDataType;

    @Mock
    TypeDeterminationHeuristic<?> mockStrategy;

    /**
     * The following peculiar locution arises from the need to provide "cloneability" while avoiding a recursive mock
     * 
     * @return a cloneable mock {@link TypeDeterminationHeuristic}
     */
    private <T extends TypeDeterminationHeuristic<T>> TypeDeterminationHeuristic<T> cloneableMockStrategy() {
        final TypeDeterminationHeuristic<T> mocked = mock(TypeDeterminationHeuristic.class);
        when(mocked.mostLikelyType()).thenReturn(mockDataType);
        final Returns cloner = new Returns(mockStrategy);
        when(mocked.clone()).thenAnswer(cloner);
        return mocked;
    }

    private static final File smalltestfile = new File("src/test/resources/test-data/small-test.csv");

    private ArrayList<DataType> expectedResults;

    private static final Logger log = getLogger(CsvScannerTest.class);

    /**
     * Extracts the most likely type selection from a {@link TypeDeterminationHeuristic}
     */
    private static final Function<TypeDeterminationHeuristic<?>, DataType> getMostLikelyType =
            new Function<TypeDeterminationHeuristic<?>, DataType>() {

                @Override
                public DataType apply(final TypeDeterminationHeuristic<?> heuristic) {
                    return heuristic.mostLikelyType();
                }
            };

    @Before
    public void setUp() {
        initMocks(this);
        expectedResults = newArrayList(mockDataType, mockDataType, mockDataType, mockDataType);
        when(mockStrategy.mostLikelyType()).thenReturn(mockDataType);
    }

    @Test
    public void testOperation() throws IOException {
        final CsvScanner testScanner;
        try (final CSVParser parser = parse(smalltestfile, UTF_8, DEFAULT.withHeader())) {
            log.debug("Found header map: {}", parser.getHeaderMap());
            testScanner = new CsvScanner(parser, cloneableMockStrategy());
            testScanner.scan();
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
            testScanner = new CsvScanner(parser, cloneableMockStrategy());
            testScanner.scan(2);
        }
        final List<DataType> guesses =
                transform(testScanner.getStrategies(), getMostLikelyType);
        assertEquals("Failed to find the correct column types!", expectedResults, guesses);
    }
}
