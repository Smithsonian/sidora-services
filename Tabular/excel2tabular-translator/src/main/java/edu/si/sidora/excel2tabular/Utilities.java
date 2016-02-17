
package edu.si.sidora.excel2tabular;

import static java.util.UUID.randomUUID;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class Utilities {

    public static File createTempFile(final Object forObject) {
        try {
            final String filePrefix = forObject.getClass().getName();
            final String fileSuffix = randomUUID().toString();
            final File tempFile = Files.createTempFile(filePrefix, fileSuffix).toFile();
            tempFile.deleteOnExit();
            return tempFile;
        } catch (final IOException e) {
            throw new AssertionError("Could not create temp file!", e);
        }
    }
}
