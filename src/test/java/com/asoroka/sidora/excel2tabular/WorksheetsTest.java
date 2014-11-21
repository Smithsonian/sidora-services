
package com.asoroka.sidora.excel2tabular;

import static com.google.common.collect.Iterables.isEmpty;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WorksheetsTest {

    @Mock
    private Sheet mockSheet1, mockSheet2, mockSheet3;

    @Mock
    private Workbook mockWorkbook;

    @Test
    public void testEmptyWorkbook() {
        when(mockWorkbook.getNumberOfSheets()).thenReturn(0);
        assertTrue(isEmpty(new Worksheets(mockWorkbook)));
    }

    @Test
    public void testWorkbookWithOneEmptySheet() {
        when(mockSheet1.iterator()).thenReturn(Collections.<Row> emptyIterator());
        when(mockWorkbook.getNumberOfSheets()).thenReturn(1);
        when(mockWorkbook.getSheetAt(0)).thenReturn(mockSheet1);
        assertTrue(isEmpty(new Worksheets(mockWorkbook)));
    }

    @Test
    public void testWorkbookWithThreeEmptySheets() {
        when(mockSheet1.iterator()).thenReturn(Collections.<Row> emptyIterator());
        when(mockWorkbook.getNumberOfSheets()).thenReturn(3);
        when(mockWorkbook.getSheetAt(0)).thenReturn(mockSheet1);
        when(mockWorkbook.getSheetAt(1)).thenReturn(mockSheet1);
        when(mockWorkbook.getSheetAt(2)).thenReturn(mockSheet1);
        assertTrue(isEmpty(new Worksheets(mockWorkbook)));
    }
}
