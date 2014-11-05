
package com.asoroka.sidora.tabularmetadata.integration;

import static com.google.common.collect.Lists.transform;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.TabularMetadataGenerator;
import com.asoroka.sidora.tabularmetadata.test.TestUtilities;
import com.google.common.base.Function;

public class IT extends TestUtilities {

    private static final String ITEST_DATA_DIR = "src/test/resources/itest-data";

    private URL testFile1, testFile2;

    private static final Logger log = getLogger(IT.class);

    @Before
    public void setUp() throws MalformedURLException {
        final File testFileDir = new File(ITEST_DATA_DIR);
        testFile1 = new File(testFileDir, "Thompson-WMA-10B-researcher_observation.csv").toURI().toURL();
        testFile2 = new File(testFileDir, "Thompson-WMA-10C-researcher_observation.csv").toURI().toURL();
    }

    @Test
    public void testFirstFile() throws IOException {
        final TabularMetadataGenerator testGenerator = new TabularMetadataGenerator();
        final TabularMetadata result = testGenerator.getMetadata(testFile1);
        log.debug("Got results: {}", result);
        log.debug("Got likely types: {}", getFirstElements(result.fieldTypes));
    }

    private static <T> List<T> getFirstElements(final List<SortedSet<T>> inputs) {
        return transform(inputs, IT.<T> firstOfIterable());
    }

    private static final <T> Function<Iterable<T>, T> firstOfIterable() {
        return new Function<Iterable<T>, T>() {

            @Override
            public T apply(final Iterable<T> s) {
                final Iterator<T> iterator = s.iterator();
                return iterator.hasNext() ? iterator.next() : null;
            }
        };
    }
}
