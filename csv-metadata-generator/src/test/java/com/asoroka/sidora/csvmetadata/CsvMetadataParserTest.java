
package com.asoroka.sidora.csvmetadata;

import static com.asoroka.sidora.csvmetadata.datatype.DataType.Geographic;
import static com.asoroka.sidora.csvmetadata.test.TestUtilities.cloneableMockStrategy;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.all;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import com.asoroka.sidora.csvmetadata.datatype.DataType;
import com.asoroka.sidora.csvmetadata.heuristics.DataTypeHeuristic;
import com.asoroka.sidora.csvmetadata.heuristics.HeaderHeuristic;
import com.google.common.collect.Range;

@RunWith(PowerMockRunner.class)
@PrepareOnlyThisForTest(CSVParser.class)
public class CsvMetadataParserTest {

    private static final String testHeaders = "NAME,RANK,SERIAL NUMBER";

    private static final String testRow1 = "\"Kirk\",\"Captain\",0033";

    private static final byte[] testCsv = (testHeaders + "\n" + testRow1).getBytes();

    private static final String MARKER_VALUE = "Picard";

    private static final String testRow2 = "\"" + MARKER_VALUE + "\",\"Captain\",0456";

    private static final byte[] testCsvWithMarker = (testHeaders + "\n" + testRow1 + "\n" + testRow2).getBytes();

    private static final Answer<InputStream> makeStream(final byte[] bytes) {
        return new Answer<InputStream>() {

            @Override
            public InputStream answer(final InvocationOnMock invocation) {
                return new ByteArrayInputStream(copyOf(bytes, bytes.length));
            }
        };

    }

    @Mock
    private DataTypeHeuristic<?> mockSimpleStrategy, mockStrategy;

    @Mock
    private HeaderHeuristic mockHeaderHeuristic;

    @Mock
    private DataType mockDataType;

    static <C extends Comparable<C>> Range<C> mockRange() {
        return Range.all();
    }

    private static final Logger log = getLogger(CsvMetadataParserTest.class);

    @Before
    public void setUp() {
        when(mockSimpleStrategy.mostLikelyType()).thenReturn(mockDataType);
        final Returns range = new Returns(mockRange());
        when(mockSimpleStrategy.getRange()).thenAnswer(range);
        mockStrategy = cloneableMockStrategy(mockSimpleStrategy);
    }

    @Test
    public void testOperationWithHeaders() throws IOException {
        when(mockHeaderHeuristic.apply(anyList())).thenReturn(true);
        final URL mockURL = mock(URL.class);

        when(mockURL.openStream()).thenAnswer(makeStream(testCsv));
        final CsvMetadataGenerator testParser = new CsvMetadataGenerator();
        testParser.setStrategy(mockStrategy);
        testParser.setHeaderStrategy(mockHeaderHeuristic);
        final CsvMetadata results = testParser.getMetadata(mockURL);

        final List<String> headers = results.headerNames();
        final List<DataType> types = results.columnTypes();
        @SuppressWarnings("rawtypes")
        // we ignore type-safety here for a simpler unit test
        final List ranges = results.minMaxes();

        assertEquals(asList(testHeaders.split(",")), headers);
        assertTrue(all(types, equalTo(mockDataType)));
        @SuppressWarnings("unchecked")
        // we ignore type-safety here for a simpler unit test
        final boolean rangeTest = all(ranges, equalTo(mockRange()));
        assertTrue(rangeTest);

    }

    @Test
    public void testOperationWithoutHeaders() throws IOException {
        when(mockHeaderHeuristic.apply(anyList())).thenReturn(false);
        final URL mockURL = mock(URL.class);
        when(mockURL.openStream()).thenAnswer(makeStream(testCsv));
        final CsvMetadataGenerator testParser = new CsvMetadataGenerator();
        testParser.setStrategy(mockStrategy);
        testParser.setHeaderStrategy(mockHeaderHeuristic);

        final CsvMetadata results = testParser.getMetadata(mockURL);
        final List<String> headers = results.headerNames();
        assertEquals(emptyList(), headers);
    }

    @Test
    public <T extends DataTypeHeuristic<T>> void testOperationWithoutHeadersWithScanLimit() throws IOException {
        when(mockHeaderHeuristic.apply(anyList())).thenReturn(false);
        final URL mockURL = mock(URL.class);
        when(mockURL.openStream()).thenAnswer(makeStream(testCsvWithMarker));
        final MarkingMockDataTypeHeuristic<T> testStrategy = new MarkingMockDataTypeHeuristic<>(MARKER_VALUE);
        final CsvMetadataGenerator testParser = new CsvMetadataGenerator();
        testParser.setStrategy(testStrategy);
        testParser.setHeaderStrategy(mockHeaderHeuristic);
        testParser.setScanLimit(2);
        testParser.getMetadata(mockURL);
        assertFalse("Discovered a marker in a row we should not have been scanning!", testStrategy.failure());
        testParser.setScanLimit(3);
        testParser.getMetadata(mockURL);
        assertTrue("Failed to discover a marker in a row we should have been scanning!", testStrategy.failure());
    }

    private static class MarkingMockDataTypeHeuristic<T extends DataTypeHeuristic<T>> implements DataTypeHeuristic<T> {

        /**
         * @param marker
         */
        public MarkingMockDataTypeHeuristic(final String marker) {
            this.marker = marker;
        }

        private String marker;

        public boolean failure = false;

        @Override
        public DataType mostLikelyType() {
            return Geographic;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T clone() {
            return (T) this;
        }

        @Override
        public void addValue(final String value) {
            if (value.equals(marker)) {
                failure = true;
            }

        }

        @Override
        public Range<?> getRange() {
            return mockRange();
        }

        public boolean failure() {
            return failure;
        }

        @Override
        public DataTypeHeuristic<T> get() {
            return this;
        }

    }
}
