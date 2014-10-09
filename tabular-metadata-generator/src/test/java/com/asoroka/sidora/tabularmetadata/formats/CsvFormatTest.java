
package com.asoroka.sidora.tabularmetadata.formats;

import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.csv.CSVFormat.TDF;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.asoroka.sidora.tabularmetadata.formats.CsvFormat.Default;
import com.asoroka.sidora.tabularmetadata.formats.CsvFormat.TabSeparated;

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
