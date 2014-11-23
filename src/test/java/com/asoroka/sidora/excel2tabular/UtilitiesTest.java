
package com.asoroka.sidora.excel2tabular;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Files.readFirstLine;
import static com.google.common.io.Files.write;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;

public class UtilitiesTest {

    private static final String TEST_DATA = "Test data";

    private static final Logger log = getLogger(UtilitiesTest.class);

    @Test
    public void testTempFileCreation() {
        final File tempFile = Utilities.createTempFile(this);
        try {
            write(TEST_DATA.getBytes(), tempFile);
        } catch (final IOException e) {
            log.error(e.getLocalizedMessage());
            fail("Failed to write to temp file!");
        }
        try {
            final String firstLine = readFirstLine(tempFile, UTF_8);
            assertEquals(TEST_DATA, firstLine);
        } catch (final IOException e) {
            log.error(e.getLocalizedMessage());
            fail("Failed to read from temp file!");
        }
    }
}
