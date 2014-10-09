
package com.asoroka.sidora.tabularmetadata.formats;

import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.csv.CSVFormat.TDF;

import javax.inject.Provider;

import org.apache.commons.csv.CSVFormat;

/**
 * Represents a format for tabular data.
 * 
 * @author ajs6f
 */
public interface CsvFormat extends Provider<CSVFormat> {

    /**
     * The default format.
     * 
     * @author ajs6f
     * @see org.apache.commons.csv.CSVFormat.DEFAULT
     */
    public static class Default implements CsvFormat {

        @Override
        public CSVFormat get() {
            return DEFAULT;
        }

    }

    /**
     * A tabular data format.
     * 
     * @author ajs6f
     * @see org.apache.commons.csv.CSVFormat.TDF
     */
    public static class TabSeparated implements CsvFormat {

        @Override
        public CSVFormat get() {
            return TDF;
        }

    }

}
