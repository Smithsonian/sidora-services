
package com.asoroka.sidora.tabularmetadata;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Geographic;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.sortByHierarchy;
import static com.asoroka.sidora.tabularmetadata.test.TestUtilities.cloneableMockStrategy;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.concat;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.formats.TabularFormat;
import com.asoroka.sidora.tabularmetadata.heuristics.headers.HeaderHeuristic;
import com.asoroka.sidora.tabularmetadata.test.TestUtilities.MockedHeuristic;
import com.google.common.collect.Range;

/**
 * This relatively complicated test is so because we are trying to mock everything that can be mocked, as a means of
 * assurance that the basic workflow (instantiated in TabularMetadataParser) is as correct as we can test it to be.
 * 
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class TabularMetadataGeneratorTest {

    // TODO make test data more readable

    private static final String testHeaders = "NAME,RANK,SERIAL NUMBER";

    private static final String testRow1 = "Kirk,Captain,0033";

    private static final byte[] testCsv = (testHeaders + "\n" + testRow1).getBytes();

    private static final String MARKER_VALUE = "Picard";

    private static final String testRow2 = MARKER_VALUE + ",Redshirt,0456";

    private static final byte[] testCsvWithMarker = (testHeaders + "\n" + testRow1 + "\n" + testRow2).getBytes();

    private static final String testTsvHeaders = "NAME\tRANK\tSERIAL NUMBER";

    private static final String testTsvRow1 = "Kirk\tCaptain\t0033";

    private static final byte[] testTsv = (testTsvHeaders + "\n" + testTsvRow1).getBytes();

    @Mock
    private MockedHeuristic mockSimpleStrategy, mockStrategy;

    @Mock
    private HeaderHeuristic<?> mockHeaderHeuristic;

    @Mock
    private DataType mockDataType;

    @Mock
    private Map<DataType, Set<String>> mockEnumeratedValues;

    private Returns mockDataTypeAnswer = new Returns(mockDataType);

    static Map<DataType, Range<?>> testRanges() {
        return emptyMap();
    }

    private static final Logger log = getLogger(TabularMetadataGeneratorTest.class);

    @Before
    public void setUp() {
        when(mockSimpleStrategy.mostLikelyType()).thenAnswer(mockDataTypeAnswer);
        final Returns trueReturn = new Returns(true);
        when(mockSimpleStrategy.addValue(anyString())).thenAnswer(trueReturn);
        final Returns range = new Returns(testRanges());
        when(mockSimpleStrategy.getRanges()).thenAnswer(range);
        when(mockSimpleStrategy.getEnumeratedValues()).thenReturn(mockEnumeratedValues);
        mockStrategy = cloneableMockStrategy(mockSimpleStrategy);
    }

    private static TabularMetadataGenerator newGenerator(final MockedHeuristic strategy) {
        final TabularMetadataGenerator generator = new TabularMetadataGenerator();
        generator.setEnumStrategy(strategy);
        generator.setRangeStrategy(strategy);
        generator.setTypeStrategy(strategy);
        return generator;
    }

    @Test
    public void testOperationWithSetFormat() throws IOException {
        log.trace("Entering testOperationWithSetFormat()...");
        when(mockHeaderHeuristic.isHeader()).thenReturn(true);
        final URL mockURL = mockURL(testTsv);
        final TabularMetadataGenerator testParser = newGenerator(mockStrategy);
        testParser.setFormat(new TabularFormat.TabSeparated());
        testParser.setHeaderStrategy(mockHeaderHeuristic);
        final TabularMetadata results = testParser.getMetadata(mockURL);

        final List<String> headers = results.headerNames;
        final List<Map<DataType, Range<?>>> ranges = results.minMaxes;
        assertEquals(asList(testHeaders.split(",")), headers);
        final List<SortedSet<DataType>> types = results.fieldTypes;
        assertTrue("Found a data type that didn't originate from our mocking!", all(concat(types),
                equalTo(mockDataType)));
        final boolean rangeTest = all(ranges, equalTo(testRanges()));
        assertTrue(rangeTest);
    }

    @Test
    public void testOperationWithHeaders() throws IOException {
        log.trace("Entering testOperationWithHeaders()...");
        when(mockHeaderHeuristic.isHeader()).thenReturn(true);
        final URL mockURL = mockURL(testCsv);
        final TabularMetadataGenerator testParser = newGenerator(mockStrategy);
        testParser.setHeaderStrategy(mockHeaderHeuristic);
        final TabularMetadata results = testParser.getMetadata(mockURL);

        final List<SortedSet<DataType>> types = results.fieldTypes;
        assertTrue(all(concat(types), equalTo(mockDataType)));

        final List<Map<DataType, Range<?>>> ranges = results.minMaxes;
        assertTrue("Failed to find the right mock range results!", all(ranges, equalTo(testRanges())));
        final List<String> headers = results.headerNames;
        assertEquals("Got a bad list of header names!", asList(testHeaders.split(",")), headers);

        for (final Map<DataType, Set<String>> v : results.enumeratedValues) {
            assertEquals("Got a bad value as part of the enumerated list!", mockEnumeratedValues, v);
        }

    }

    @Test
    public void testOperationWithoutHeaders() throws IOException {
        log.trace("Entering testOperationWithoutHeaders()...");
        when(mockHeaderHeuristic.isHeader()).thenReturn(false);
        final URL mockURL = mockURL(testCsv);
        final TabularMetadataGenerator testParser = newGenerator(mockStrategy);
        testParser.setHeaderStrategy(mockHeaderHeuristic);
        final TabularMetadata results = testParser.getMetadata(mockURL);

        final List<SortedSet<DataType>> types = results.fieldTypes;
        assertTrue(all(concat(types), equalTo(mockDataType)));

        final List<Map<DataType, Range<?>>> ranges = results.minMaxes;
        assertTrue("Got a range that wasn't the one we inserted!", all(ranges, equalTo(testRanges())));

        final List<String> headers = results.headerNames;
        final int numHeaders = headers.size();
        for (int i = 1; i <= numHeaders; i++) {
            assertEquals("Variable " + i, headers.get(i));
        }
    }

    @Test
    public void testOperationWithoutHeadersWithScanLimit() throws IOException {
        log.trace("Entering testOperationWithoutHeadersWithScanLimit()...");
        when(mockHeaderHeuristic.isHeader()).thenReturn(false);
        final URL mockURL = mockURL(testCsvWithMarker);
        final MarkingMockDataTypeHeuristic testStrategy = new MarkingMockDataTypeHeuristic(MARKER_VALUE);
        final TabularMetadataGenerator testParser = newGenerator(testStrategy);
        testParser.setHeaderStrategy(mockHeaderHeuristic);
        testParser.setScanLimit(2);
        testParser.getMetadata(mockURL);
        assertFalse("Discovered the marker in a row we should not have been scanning!",
                testStrategy.hasMarkerBeenSeen);
        log.trace("Did not discover the marker in a row we should not have been scanning.");
        testParser.setScanLimit(3);
        testParser.getMetadata(mockURL);
        assertTrue("Failed to discover the marker in a row we should have been scanning!",
                testStrategy.hasMarkerBeenSeen);
        log.trace("Discovered the marker in a row we should have been scanning.");
        log.trace("Done with testOperationWithoutHeadersWithScanLimit().");
    }

    private static URL mockURL(final byte[] data) throws IOException {
        final URLConnection mockConnection = mock(URLConnection.class);
        when(mockConnection.getInputStream()).thenAnswer(makeStream(data));
        final URLStreamHandler handler = new URLStreamHandler() {

            @Override
            protected URLConnection openConnection(final URL whocares) {
                return mockConnection;
            }
        };
        return new URL("mock", "example.com", -1, "", handler);
    }

    private static final Answer<InputStream> makeStream(final byte[] bytes) {
        return new Answer<InputStream>() {

            @Override
            public InputStream answer(final InvocationOnMock invocation) {
                return new ByteArrayInputStream(copyOf(bytes, bytes.length));
            }
        };
    }

    private static class MarkingMockDataTypeHeuristic implements MockedHeuristic {

        private static final Logger markLog = getLogger(MarkingMockDataTypeHeuristic.class);

        /**
         * @param marker
         */
        public MarkingMockDataTypeHeuristic(final String marker) {
            this.marker = marker;
        }

        private String marker;

        public boolean hasMarkerBeenSeen = false;

        @Override
        public SortedSet<DataType> typesAsLikely() {
            return sortByHierarchy(singleton(Geographic));
        }

        @Override
        public MarkingMockDataTypeHeuristic clone() {
            return this;
        }

        @Override
        public boolean addValue(final String lex) {
            markLog.trace("Checking lex {} for marker {}", lex, marker);
            if (lex.equals(marker)) {
                hasMarkerBeenSeen = true;
            }
            return true;
        }

        @Override
        public MarkingMockDataTypeHeuristic get() {
            return this;
        }

        @Override
        public DataType mostLikelyType() {
            // NO OP
            return null;
        }

        @Override
        public Map<DataType, Range<?>> getRanges() {
            // NO OP
            return null;
        }

        @Override
        public void reset() {
            // NO OP
        }

        @Override
        public Map<DataType, Set<String>> getEnumeratedValues() {
            // NO OP
            return null;
        }
    }
}
