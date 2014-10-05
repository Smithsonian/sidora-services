
package com.asoroka.sidora.csvmetadata.formats;

import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.csv.CSVFormat.TDF;

import org.apache.commons.csv.CSVFormat;

import com.google.common.base.Supplier;

public interface CsvFormat extends Supplier<CSVFormat> {

    public static class Default implements CsvFormat {

        @Override
        public CSVFormat get() {
            return DEFAULT;
        }

    }

    public static class TabSeparated implements CsvFormat {

        @Override
        public CSVFormat get() {
            return TDF;
        }

    }

}
