/*
 * Copyright 2018-2019 Smithsonian Institution.
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

package edu.si.sidora.tabular.metadata.generator;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import edu.si.sidora.tabular.metadata.generator.datatype.DataType;
import edu.si.sidora.tabular.metadata.generator.heuristics.Heuristic;
import edu.si.sidora.tabular.metadata.generator.heuristics.enumerations.EnumeratedValuesHeuristic;
import edu.si.sidora.tabular.metadata.generator.heuristics.ranges.RangeDeterminingHeuristic;
import edu.si.sidora.tabular.metadata.generator.heuristics.types.TypeDeterminingHeuristic;
import edu.si.sidora.tabular.metadata.generator.testframework.TestUtilities;
import org.apache.commons.csv.CSVParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("rawtypes")
public class TabularScannerTest extends TestUtilities {

	@Mock private DataType mockDataType;

	@Mock private RangeDeterminingHeuristic mockRangeStrategy;

	@Mock private TypeDeterminingHeuristic mockTypeStrategy;

	@Mock private EnumeratedValuesHeuristic mockEnumStrategy;

	private static final File smalltestfile = new File("src/test/resources/generator/test-data/small-test.csv");

	private static List<DataType> expectedResults;

	private static final Logger log = getLogger(TabularScannerTest.class);

	@Before
	public void setUp() {
		expectedResults = asList(mockDataType, mockDataType, mockDataType, mockDataType);
		when(mockTypeStrategy.results()).thenReturn(mockDataType);
		when(mockTypeStrategy.get()).thenReturn(mockTypeStrategy);
		when(mockEnumStrategy.get()).thenReturn(mockEnumStrategy);
		when(mockRangeStrategy.get()).thenReturn(mockRangeStrategy);
	}

	@Test
	public void testOperation() throws IOException {
		try (Reader reader = new FileReader(smalltestfile);
				final CSVParser parser = new CSVParser(reader, DEFAULT.withHeader())) {
			log.debug("Found header map: {}", parser.getHeaderMap());
			final TabularScanner testScanner = new TabularScanner(parser.iterator(), mockTypeStrategy,
					mockRangeStrategy, mockEnumStrategy);
			testScanner.scan(0);
			final List<DataType> guesses = testScanner.getTypeStrategies().stream().map(Heuristic::results)
					.collect(toList());
			assertEquals("Failed to find the correct column types!", expectedResults, guesses);
		}
	}

	@Test
	public void testOperationWithLimitedScan() throws IOException {
		try (Reader reader = new FileReader(smalltestfile);
				final CSVParser parser = new CSVParser(reader, DEFAULT.withHeader())) {
			log.debug("Found header map: {}", parser.getHeaderMap());
			final TabularScanner testScanner = new TabularScanner(parser.iterator(), mockTypeStrategy,
					mockRangeStrategy, mockEnumStrategy);
			testScanner.scan(2);
			final List<DataType> guesses = testScanner.getTypeStrategies().stream().map(Heuristic::results)
					.collect(toList());
			assertEquals("Failed to find the correct column types!", expectedResults, guesses);
		}
	}
}
