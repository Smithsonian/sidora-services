
package com.asoroka.sidora.tabularmetadata;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Geographic;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.sortByHierarchy;
import static com.asoroka.sidora.tabularmetadata.test.TestUtilities.cloneableMockStrategy;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.all;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

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
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.formats.TabularFormat;
import com.asoroka.sidora.tabularmetadata.heuristics.HeaderHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.ValueHeuristic;
import com.google.common.collect.Range;

/**
 * This relatively complicated test is so because we are trying to mock everything that can be mocked, as a means of
 * assurance that the basic workflow (instantiated in TabularMetadataParser) is as correct as we can test it to be.
 * 
 * @author ajs6f
 */
public class TabularMetadataGeneratorTest {

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
    private ValueHeuristic<?> mockSimpleStrategy, mockStrategy;

    @Mock
    private HeaderHeuristic<?> mockHeaderHeuristic;

    @Mock
    private DataType mockDataType;

    private Returns mockDataTypeAnswer = new Returns(mockDataType);

    static <MinMax extends Comparable<?>> Range<MinMax> testRange() {
        return Range.all();
    }

    // private static final Logger log = getLogger(TabularMetadataGeneratorTest.class);

    @Before
    public void setUp() {
        initMocks(this);
        when(mockSimpleStrategy.mostLikelyType()).thenAnswer(mockDataTypeAnswer);
        final Returns range = new Returns(testRange());
        when(mockSimpleStrategy.getRange()).thenAnswer(range);
        mockStrategy = cloneableMockStrategy(mockSimpleStrategy);
    }

    @Test
    public void testOperationWithSetFormat() throws IOException {
        when(mockHeaderHeuristic.isHeader()).thenReturn(true);
        final URL mockURL = mockURL(testTsv);
        final TabularMetadataGenerator testParser = new TabularMetadataGenerator();
        testParser.setStrategy(mockStrategy);
        testParser.setFormat(new TabularFormat.TabSeparated());
        testParser.setHeaderStrategy(mockHeaderHeuristic);
        final TabularMetadata results = testParser.getMetadata(mockURL);

        final List<String> headers = results.headerNames();
        final List<SortedSet<DataType>> types = results.fieldTypes();
        @SuppressWarnings("rawtypes")
        // we ignore type-safety here for a simpler unit test
        final List ranges = results.minMaxes();

        assertEquals(asList(testHeaders.split(",")), headers);
        for (final Set<DataType> eachType : types) {
            assertTrue(all(eachType, equalTo(mockDataType)));
        }
        @SuppressWarnings("unchecked")
        // we ignore type-safety here for a simpler unit test
        final boolean rangeTest = all(ranges, equalTo(testRange()));
        assertTrue(rangeTest);
    }

    @Test
    public void testOperationWithHeaders() throws IOException {
        when(mockHeaderHeuristic.isHeader()).thenReturn(true);
        final URL mockURL = mockURL(testCsv);
        final TabularMetadataGenerator testParser = new TabularMetadataGenerator();
        testParser.setStrategy(mockStrategy);
        testParser.setHeaderStrategy(mockHeaderHeuristic);
        final TabularMetadata results = testParser.getMetadata(mockURL);

        final List<SortedSet<DataType>> types = results.fieldTypes();
        for (final Set<DataType> eachType : types) {
            assertTrue(all(eachType, equalTo(mockDataType)));
        }
        @SuppressWarnings("rawtypes")
        // we ignore type-safety here for a simpler unit test
        final List ranges = results.minMaxes();
        @SuppressWarnings("unchecked")
        // we ignore type-safety here for a simpler unit test
        final boolean rangeTest = all(ranges, equalTo(testRange()));
        assertTrue(rangeTest);
        final List<String> headers = results.headerNames();
        assertEquals(asList(testHeaders.split(",")), headers);

    }

    @Test
    public void testOperationWithoutHeaders() throws IOException {
        when(mockHeaderHeuristic.isHeader()).thenReturn(false);
        final URL mockURL = mockURL(testCsv);
        final TabularMetadataGenerator testParser = new TabularMetadataGenerator();
        testParser.setStrategy(mockStrategy);
        testParser.setHeaderStrategy(mockHeaderHeuristic);
        final TabularMetadata results = testParser.getMetadata(mockURL);

        final List<SortedSet<DataType>> types = results.fieldTypes();
        for (final Set<DataType> eachType : types) {
            assertTrue(all(eachType, equalTo(mockDataType)));
        }
        @SuppressWarnings("rawtypes")
        // we ignore type-safety here for a simpler unit test
        final List ranges = results.minMaxes();
        @SuppressWarnings("unchecked")
        // we ignore type-safety here for a simpler unit test
        final boolean rangeTest = all(ranges, equalTo(testRange()));
        assertTrue(rangeTest);

        final List<String> headers = results.headerNames();
        assertEquals(emptyList(), headers);
    }

    @Test
    public <T extends ValueHeuristic<T>> void testOperationWithoutHeadersWithScanLimit() throws IOException {
        when(mockHeaderHeuristic.isHeader()).thenReturn(false);
        final URL mockURL = mockURL(testCsvWithMarker);
        final MarkingMockDataTypeHeuristic testStrategy = new MarkingMockDataTypeHeuristic(MARKER_VALUE);
        final TabularMetadataGenerator testParser = new TabularMetadataGenerator();
        testParser.setStrategy(testStrategy);
        testParser.setHeaderStrategy(mockHeaderHeuristic);
        testParser.setScanLimit(2);
        testParser.getMetadata(mockURL);
        assertFalse("Discovered a marker in a row we should not have been scanning!", testStrategy.failure);
        testParser.setScanLimit(3);
        testParser.getMetadata(mockURL);
        assertTrue("Failed to discover a marker in a row we should have been scanning!", testStrategy.failure);
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

    private static class MarkingMockDataTypeHeuristic implements ValueHeuristic<MarkingMockDataTypeHeuristic> {

        /**
         * @param marker
         */
        public MarkingMockDataTypeHeuristic(final String marker) {
            this.marker = marker;
        }

        private String marker;

        public boolean failure = false;

        @Override
        public SortedSet<DataType> typesAsLikely() {
            return sortByHierarchy(singleton(Geographic));
        }

        @Override
        public MarkingMockDataTypeHeuristic clone() {
            return this;
        }

        @Override
        public void addValue(final String value) {
            if (value.equals(marker)) {
                failure = true;
            }

        }

        @Override
        public <MinMax extends Comparable<MinMax>> Range<MinMax> getRange() {
            return testRange();
        }

        @Override
        public MarkingMockDataTypeHeuristic get() {
            return this;
        }

        @Override
        public DataType mostLikelyType() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<DataType, Range<?>> getRanges() {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
