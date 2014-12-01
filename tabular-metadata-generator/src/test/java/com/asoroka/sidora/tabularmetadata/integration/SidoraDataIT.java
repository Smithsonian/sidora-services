
package com.asoroka.sidora.tabularmetadata.integration;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.DateTime;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.PositiveInteger;
import static com.google.common.base.Predicates.contains;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Lists.transform;
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
import java.util.SortedSet;
import java.util.regex.Pattern;

import org.junit.Test;
import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.TabularMetadataGenerator;
import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.base.Function;

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
                all(result.headerNames, contains(DEFAULT_HEADER_NAME)));

        final List<DataType> mostLikelyTypes = getFirstElements(result.fieldTypes);
        assertEquals("Didn't get the expected type determinations!", expectedTypes, mostLikelyTypes);

        for (int i = 0; i < result.minMaxes.size(); i++) {
            final DataType mostLikelyType = mostLikelyTypes.get(i);
            log.debug("For most likely type {} got range: {}", mostLikelyType, result.minMaxes.get(i).get(
                    mostLikelyType));
        }
    }

    private static <T> List<T> getFirstElements(final List<SortedSet<T>> inputs) {
        return transform(inputs, SidoraDataIT.<T> firstOfIterable());
    }

    private static final <T> Function<SortedSet<T>, T> firstOfIterable() {
        return new Function<SortedSet<T>, T>() {

            @Override
            public T apply(final SortedSet<T> s) {
                return s.first();
            }
        };
    }
}
