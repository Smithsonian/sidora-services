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

import edu.si.sidora.tabularmetadata.TabularScanner;
import edu.si.sidora.tabularmetadata.datatype.DataType;
import edu.si.sidora.tabularmetadata.heuristics.enumerations.EnumeratedValuesHeuristic;
import edu.si.sidora.tabularmetadata.heuristics.ranges.RangeDeterminingHeuristic;
import edu.si.sidora.tabularmetadata.heuristics.types.TypeDeterminingHeuristic;
import edu.si.sidora.tabularmetadata.testframework.TestUtilities;

@RunWith(MockitoJUnitRunner.class)
public class TabularScannerTest extends TestUtilities {

    @Mock
    private DataType mockDataType;

    @Mock
    @SuppressWarnings("rawtypes")
    private RangeDeterminingHeuristic mockRangeStrategy;

    @Mock
    @SuppressWarnings("rawtypes")
    private TypeDeterminingHeuristic mockTypeStrategy;

    @Mock
    @SuppressWarnings("rawtypes")
    private EnumeratedValuesHeuristic mockEnumStrategy;

    private static final File smalltestfile = new File("src/test/resources/test-data/small-test.csv");

    private static ArrayList<DataType> expectedResults;

    private static final Logger log = getLogger(TabularScannerTest.class);

    @Before
    public void setUp() {
        expectedResults = newArrayList(mockDataType, mockDataType, mockDataType, mockDataType);
        when(mockTypeStrategy.results()).thenReturn(mockDataType);
        when(mockTypeStrategy.get()).thenReturn(mockTypeStrategy);
        when(mockEnumStrategy.get()).thenReturn(mockEnumStrategy);
        when(mockRangeStrategy.get()).thenReturn(mockRangeStrategy);
    }

    @Test
    public void testOperation() throws IOException {
        final TabularScanner testScanner;
        try (final CSVParser parser = parse(smalltestfile, UTF_8, DEFAULT.withHeader())) {
            log.debug("Found header map: {}", parser.getHeaderMap());
            testScanner =
                    new TabularScanner(parser.iterator(), mockTypeStrategy, mockRangeStrategy, mockEnumStrategy);
            testScanner.scan(0);
        }
        final List<DataType> guesses =
                transform(testScanner.getTypeStrategies(), getMostLikelyType);
        assertEquals("Failed to find the correct column types!", expectedResults, guesses);
    }

    @Test
    public void testOperationWithLimitedScan() throws IOException {
        final TabularScanner testScanner;
        try (final CSVParser parser = parse(smalltestfile, UTF_8, DEFAULT.withHeader())) {
            log.debug("Found header map: {}", parser.getHeaderMap());
            testScanner =
                    new TabularScanner(parser.iterator(), mockTypeStrategy, mockRangeStrategy, mockEnumStrategy);
            testScanner.scan(2);
        }
        final List<DataType> guesses =
                transform(testScanner.getTypeStrategies(), getMostLikelyType);
        assertEquals("Failed to find the correct column types!", expectedResults, guesses);
    }
}
