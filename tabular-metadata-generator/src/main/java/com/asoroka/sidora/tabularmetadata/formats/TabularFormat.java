
package com.asoroka.sidora.tabularmetadata.formats;

import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.csv.CSVFormat.TDF;

import javax.inject.Provider;

import org.apache.commons.csv.CSVFormat;

/**
 * Represents a format for tabular data.
 * 
 * @author A. Soroka
 */
public interface TabularFormat extends Provider<CSVFormat> {

    /**
     * The default format.
     * 
     * @author A. Soroka
     * @see org.apache.commons.csv.CSVFormat#DEFAULT
     */
    public static class Default implements TabularFormat {

        @Override
        public CSVFormat get() {
            return DEFAULT;
        }

    }

    /**
     * A tabular data format.
     * 
     * @author A. Soroka
     * @see org.apache.commons.csv.CSVFormat#TDF
     */
    public static class TabSeparated implements TabularFormat {

        @Override
        public CSVFormat get() {
            return TDF;
        }

    }

}
