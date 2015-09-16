/**
 * Copyright 2015 Smithsonian Institution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.You may obtain a copy of
 * the License at: http://www.apache.org/licenses/
 *
 * This software and accompanying documentation is supplied without
 * warranty of any kind. The copyright holder and the Smithsonian Institution:
 * (1) expressly disclaim any warranties, express or implied, including but not
 * limited to any implied warranties of merchantability, fitness for a
 * particular purpose, title or non-infringement; (2) do not assume any legal
 * liability or responsibility for the accuracy, completeness, or usefulness of
 * the software; (3) do not represent that use of the software would not
 * infringe privately owned rights; (4) do not warrant that the software
 * is error-free or will be maintained, supported, updated or enhanced;
 * (5) will not be liable for any indirect, incidental, consequential special
 * or punitive damages of any kind or nature, including but not limited to lost
 * profits or loss of data, on any basis arising from contract, tort or
 * otherwise, even if any of the parties has been warned of the possibility of
 * such loss or damage.
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */


package edu.si.sidora.tabularmetadata;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
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
import java.util.NavigableMap;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.collect.Range;

import edu.si.sidora.tabularmetadata.EmptyDataFileException;
import edu.si.sidora.tabularmetadata.TabularMetadata;
import edu.si.sidora.tabularmetadata.TabularMetadataGenerator;
import edu.si.sidora.tabularmetadata.datatype.DataType;
import edu.si.sidora.tabularmetadata.formats.TabularFormat;
import edu.si.sidora.tabularmetadata.heuristics.enumerations.EnumeratedValuesHeuristic;
import edu.si.sidora.tabularmetadata.heuristics.headers.HeaderHeuristic;
import edu.si.sidora.tabularmetadata.heuristics.ranges.RangeDeterminingHeuristic;
import edu.si.sidora.tabularmetadata.heuristics.types.TypeDeterminingHeuristic;

@RunWith(MockitoJUnitRunner.class)
public class TabularMetadataGeneratorTest {

    @SuppressWarnings("rawtypes")
    static final Range TEST_RANGE = Range.closed(0, 1);

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
    @SuppressWarnings("rawtypes")
    private RangeDeterminingHeuristic mockRangeStrategy;

    @Mock
    @SuppressWarnings("rawtypes")
    private TypeDeterminingHeuristic mockTypeStrategy;

    @Mock
    @SuppressWarnings("rawtypes")
    private EnumeratedValuesHeuristic mockEnumStrategy;

    @Mock
    @SuppressWarnings("rawtypes")
    private HeaderHeuristic mockHeaderHeuristic;

    @Before
    public void setUpStrategies() {

        when(mockTypeStrategy.addValue(anyString())).thenReturn(true);
        when(mockEnumStrategy.addValue(anyString())).thenReturn(true);
        when(mockRangeStrategy.addValue(anyString())).thenReturn(true);

        when(mockTypeStrategy.results()).thenReturn(mockDataType);
        when(mockRangeStrategy.results()).thenReturn(testRanges);
        when(mockEnumStrategy.results()).thenReturn(mockEnumeratedValues);

        when(mockTypeStrategy.get()).thenReturn(mockTypeStrategy);
        when(mockEnumStrategy.get()).thenReturn(mockEnumStrategy);
        when(mockRangeStrategy.get()).thenReturn(mockRangeStrategy);
    }

    @Mock
    DataType mockDataType;

    @Mock
    private Map<DataType, Set<String>> mockEnumeratedValues;

    @Before
    public void setUpMockEnumeratedValues() {
        when(mockEnumeratedValues.get(mockDataType)).thenReturn(newHashSet(MARKER_VALUE));
    }

    private Predicate<Map<DataType, Set<String>>> matchesMockEnumeratedValues =
            new Predicate<Map<DataType, Set<String>>>() {

                @Override
                public boolean apply(final Map<DataType, Set<String>> map) {
                    return map.get(mockDataType).equals(newHashSet(MARKER_VALUE));
                }
            };

    @Mock
    private NavigableMap<DataType, Range<?>> testRanges;

    @SuppressWarnings("unchecked")
    @Before
    public void setUpTestRanges() {
        when(testRanges.get(mockDataType)).thenReturn(TEST_RANGE);
    }

    private Predicate<Map<DataType, Range<?>>> matchesTestRanges =
            new Predicate<Map<DataType, Range<?>>>() {

                @Override
                public boolean apply(final Map<DataType, Range<?>> map) {
                    return map.get(mockDataType).equals(TEST_RANGE);
                }
            };

    private static final Logger log = getLogger(TabularMetadataGeneratorTest.class);

    private static TabularMetadataGenerator newGenerator(final RangeDeterminingHeuristic<?> rangeStrategy,
            final EnumeratedValuesHeuristic<?> enumStrategy, final TypeDeterminingHeuristic<?> typeStrategy) {
        final TabularMetadataGenerator generator = new TabularMetadataGenerator();
        generator.setEnumStrategy(enumStrategy);
        generator.setRangeStrategy(rangeStrategy);
        generator.setTypeStrategy(typeStrategy);
        return generator;
    }

    @Test(expected = EmptyDataFileException.class)
    public void testEmptyDataFile() throws IOException {
        final URL mockURL = mockURL("".getBytes());
        final TabularMetadataGenerator testParser =
                newGenerator(mockRangeStrategy, mockEnumStrategy, mockTypeStrategy);
        testParser.getMetadata(mockURL);
    }

    @Test
    public void testOperationWithSetFormat() throws IOException {
        log.trace("Entering testOperationWithSetFormat()...");
        when(mockHeaderHeuristic.results()).thenReturn(true);
        final URL mockURL = mockURL(testTsv);
        final TabularMetadataGenerator testParser =
                newGenerator(mockRangeStrategy, mockEnumStrategy, mockTypeStrategy);
        testParser.setFormat(new TabularFormat.TabSeparated());
        testParser.setHeaderStrategy(mockHeaderHeuristic);
        final TabularMetadata results = testParser.getMetadata(mockURL);

        final List<String> headers = results.headerNames();
        final List<Map<DataType, Range<?>>> ranges = results.minMaxes();
        assertEquals(asList(testHeaders.split(",")), headers);
        final List<DataType> types = results.fieldTypes();
        assertTrue("Found a data type that didn't originate from our mocking!", all(types, equalTo(mockDataType)));
        assertTrue("Failed to find the right mock range results!", all(ranges, matchesTestRanges));
    }
    
    @Test
    public void testOperationWithDeclaredHeaders() throws IOException {
        log.trace("Entering testOperationWithDeclaredHeaders()...");
        when(mockHeaderHeuristic.results()).thenReturn(false);
        final URL mockURL = mockURL(testCsv);
        final TabularMetadataGenerator testParser =
                newGenerator(mockRangeStrategy, mockEnumStrategy, mockTypeStrategy);
        testParser.setHeaderStrategy(mockHeaderHeuristic);
        final TabularMetadata results = testParser.getMetadata(mockURL, true);

        final List<DataType> types = results.fieldTypes();
        assertTrue(all(types, equalTo(mockDataType)));

        final List<Map<DataType, Range<?>>> ranges = results.minMaxes();
        assertTrue("Failed to find the right mock range results!", all(ranges, matchesTestRanges));
        final List<String> headers = results.headerNames();
        assertEquals("Got a bad list of header names!", asList(testHeaders.split(",")), headers);
        assertTrue(all(results.enumeratedValues(), matchesMockEnumeratedValues));
    }

    @Test
    public void testOperationWithHeaders() throws IOException {
        log.trace("Entering testOperationWithHeaders()...");
        when(mockHeaderHeuristic.results()).thenReturn(true);
        final URL mockURL = mockURL(testCsv);
        final TabularMetadataGenerator testParser =
                newGenerator(mockRangeStrategy, mockEnumStrategy, mockTypeStrategy);
        testParser.setHeaderStrategy(mockHeaderHeuristic);
        final TabularMetadata results = testParser.getMetadata(mockURL);

        final List<DataType> types = results.fieldTypes();
        assertTrue(all(types, equalTo(mockDataType)));

        final List<Map<DataType, Range<?>>> ranges = results.minMaxes();
        assertTrue("Failed to find the right mock range results!", all(ranges, matchesTestRanges));
        final List<String> headers = results.headerNames();
        assertEquals("Got a bad list of header names!", asList(testHeaders.split(",")), headers);
        assertTrue(all(results.enumeratedValues(), matchesMockEnumeratedValues));
    }

    @Test
    public void testOperationWithoutHeaders() throws IOException {
        log.trace("Entering testOperationWithoutHeaders()...");
        when(mockHeaderHeuristic.results()).thenReturn(false);
        final URL mockURL = mockURL(testCsv);
        final TabularMetadataGenerator testParser =
                newGenerator(mockRangeStrategy, mockEnumStrategy, mockTypeStrategy);
        testParser.setHeaderStrategy(mockHeaderHeuristic);
        final TabularMetadata results = testParser.getMetadata(mockURL);

        final List<DataType> types = results.fieldTypes();
        assertTrue(all(types, equalTo(mockDataType)));

        final List<Map<DataType, Range<?>>> ranges = results.minMaxes();
        assertTrue("Got a range that wasn't the one we inserted!", all(ranges, matchesTestRanges));

        final List<String> headers = results.headerNames();
        final int numHeaders = headers.size();
        for (int i = 0; i < numHeaders; i++) {
            assertEquals("Variable " + (i + 1), headers.get(i));
        }
    }

    @Test
    public void testOperationWithoutHeadersWithScanLimit() throws IOException {
        log.trace("Entering testOperationWithoutHeadersWithScanLimit()...");
        when(mockHeaderHeuristic.results()).thenReturn(false);
        final URL mockURL = mockURL(testCsvWithMarker);
        final MarkingMockDataTypeHeuristic testStrategy = new MarkingMockDataTypeHeuristic(MARKER_VALUE);
        final TabularMetadataGenerator testParser =
                newGenerator(mockRangeStrategy, mockEnumStrategy, testStrategy);
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

    private static class MarkingMockDataTypeHeuristic implements
            TypeDeterminingHeuristic<MarkingMockDataTypeHeuristic> {

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
        public DataType results() {
            return DataType.Boolean;
        }

        @Override
        public MarkingMockDataTypeHeuristic get() {
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
        public void reset() {
            // NO OP
        }

        @Override
        public int valuesSeen() {
            // NO OP
            return 0;
        }

        @Override
        public int parseableValuesSeen() {
            // NO OP
            return 0;
        }
    }
}
