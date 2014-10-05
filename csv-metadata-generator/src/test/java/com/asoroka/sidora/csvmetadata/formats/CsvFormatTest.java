
package com.asoroka.sidora.csvmetadata.formats;

import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.csv.CSVFormat.TDF;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.asoroka.sidora.csvmetadata.formats.CsvFormat.Default;
import com.asoroka.sidora.csvmetadata.formats.CsvFormat.TabSeparated;

public class CsvFormatTest {

    @Test
    public void testDefault() {
        assertEquals(DEFAULT, new Default().get());
    }

    @Test
    public void testTabFormat() {
        assertEquals(TDF, new TabSeparated().get());
    }

}
