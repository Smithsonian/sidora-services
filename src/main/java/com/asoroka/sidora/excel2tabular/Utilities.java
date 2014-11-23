
package com.asoroka.sidora.excel2tabular;

import static java.util.UUID.randomUUID;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class Utilities {

    public static File createTempFile(final Object forObject) {
        try {
            return Files.createTempFile(forObject.getClass().getName(), randomUUID().toString()).toFile();
        } catch (final IOException e) {
            throw new AssertionError("Could not create temp file!", e);
        }
    }
}
