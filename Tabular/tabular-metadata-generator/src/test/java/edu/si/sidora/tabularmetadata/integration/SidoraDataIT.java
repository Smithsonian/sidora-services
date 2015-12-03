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

package edu.si.sidora.tabularmetadata.integration;

import static edu.si.sidora.tabularmetadata.datatype.DataType.DateTime;
import static edu.si.sidora.tabularmetadata.datatype.DataType.PositiveInteger;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;
import org.slf4j.Logger;

import com.google.common.collect.Range;

import edu.si.sidora.tabularmetadata.TabularMetadata;
import edu.si.sidora.tabularmetadata.TabularMetadataGenerator;
import edu.si.sidora.tabularmetadata.datatype.DataType;
import edu.si.sidora.tabularmetadata.heuristics.enumerations.LimitedEnumeratedValuesHeuristic;

public class SidoraDataIT {

	private static final Pattern DEFAULT_HEADER_NAME = compile("^Variable");

	private static final String ITEST_DATA_DIR = "src/test/resources/itest-data";

	private static final URL testSIFile1, testSIFile2;

	private static final List<URL> testFiles;

	static {
		final File testFileDir = new File(ITEST_DATA_DIR);
		try {
			testSIFile1 = new File(testFileDir, "Thompson-WMA-10B-researcher_observation.csv").toURI().toURL();
			testSIFile2 = new File(testFileDir, "Thompson-WMA-16C-researcher_observation.csv").toURI().toURL();
			testFiles = asList(testSIFile1, testSIFile2);
		} catch (final MalformedURLException e) {
			throw new AssertionError("Couldn't find test files!");
		}

	}

	private static final Logger log = getLogger(SidoraDataIT.class);

	private static final List<DataType> expectedTypes = asList(DataType.String, DataType.String, DataType.String,
			DateTime, DateTime, DataType.String, DataType.String, DataType.String, DataType.String, DataType.String,
			PositiveInteger, DataType.String, DataType.String, DataType.String);

	@Test
	public void testSIfiles() throws IOException {
		testFiles.forEach(SidoraDataIT::testFile);
	}

	private static void testFile(final URL testFile) {
		final TabularMetadataGenerator testGenerator = new TabularMetadataGenerator();
		try {
			final TabularMetadata result = testGenerator.getMetadata(testFile);
			log.debug("Got results: {}", result);
			assertTrue("Should have found all header names matching against '" + DEFAULT_HEADER_NAME + "'!",
					result.headerNames().stream().allMatch(DEFAULT_HEADER_NAME.asPredicate()));

			final List<DataType> mostLikelyTypes = result.fieldTypes();
			assertEquals("Didn't get the expected type determinations!", expectedTypes, mostLikelyTypes);

			final List<Map<DataType, Range<?>>> minMaxes = result.minMaxes();
			for (int i = 0; i < minMaxes.size(); i++) {
				final DataType mostLikelyType = mostLikelyTypes.get(i);
				log.debug("For most likely type {} got range: {}", mostLikelyType, minMaxes.get(i).get(mostLikelyType));
			}
			final List<Map<DataType, Set<String>>> enumerations = result.enumeratedValues();
			for (int i = 0; i < enumerations.size(); i++) {
				final DataType mostLikelyType = mostLikelyTypes.get(i);
				final Set<String> enumeration = enumerations.get(i).get(mostLikelyType);
				log.debug("For most likely type {} got enumeration: {}", mostLikelyType, enumeration);
				// Default operation is to limit to 10 enumerated values recorded
				assertTrue(LimitedEnumeratedValuesHeuristic.DEFAULT_LIMIT >= enumeration.size());
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
}
