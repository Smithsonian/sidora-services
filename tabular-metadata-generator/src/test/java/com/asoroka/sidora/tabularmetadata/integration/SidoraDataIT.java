
package com.asoroka.sidora.tabularmetadata.integration;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.DateTime;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.PositiveInteger;
import static com.google.common.base.Predicates.contains;
import static com.google.common.collect.Iterables.all;
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

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.TabularMetadataGenerator;
import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.enumerations.LimitedEnumeratedValuesHeuristic;
import com.google.common.collect.Range;

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

    private static final List<DataType> expectedTypes =
            asList(DataType.String, DataType.String, DataType.String, DateTime,
                    DateTime, DataType.String, DataType.String, DataType.String, DataType.String,
                    DataType.String, PositiveInteger, DataType.String, DataType.String, DataType.String);

    @Test
    public void testSIfiles() throws IOException {
        for (final URL testFile : testFiles) {
            testFile(testFile);
        }
    }

    private static void testFile(final URL testFile) throws IOException {
        final TabularMetadataGenerator testGenerator = new TabularMetadataGenerator();
        final TabularMetadata result = testGenerator.getMetadata(testFile);
        log.debug("Got results: {}", result);
        assertTrue("Should have found all header names matching against '" + DEFAULT_HEADER_NAME + "'!",
                all(result.headerNames(), contains(DEFAULT_HEADER_NAME)));

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
    }
}
