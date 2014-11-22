
package com.asoroka.sidora.excel2tabular;

import static com.asoroka.sidora.excel2tabular.TestUtilities.iterateOver;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Before;
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

    @Mock
    private Row mockRow;

    @Mock
    private Cell mockCell;

    @Before
    public void setUp() {
        when(mockCell.getCellType()).thenReturn(CELL_TYPE_NUMERIC);
        when(mockRow.iterator()).thenAnswer(iterateOver(mockCell));
        when(mockRow.getRowNum()).thenReturn(0);
        when(mockRow.getLastCellNum()).thenReturn((short) 1);
    }

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

    @Test
    public void testWorkbookWithOneDataSheet() {
        when(mockSheet1.iterator()).thenAnswer(iterateOver(mockRow));
        when(mockSheet1.getRow(0)).thenReturn(mockRow);
        when(mockSheet1.getFirstRowNum()).thenReturn(0);
        when(mockSheet1.getLastRowNum()).thenReturn(0);
        when(mockWorkbook.getNumberOfSheets()).thenReturn(1);
        when(mockWorkbook.getSheetAt(0)).thenReturn(mockSheet1);
        // we want to see only one sheet with only one row in it
        assertEquals(mockRow, getOnlyElement(getOnlyElement(new Worksheets(mockWorkbook))));
    }

    @Test
    public void testWorkbookWithThreeDataSheet() {
        when(mockSheet1.iterator()).thenAnswer(iterateOver(mockRow));
        when(mockSheet1.getRow(0)).thenReturn(mockRow);
        when(mockSheet1.getFirstRowNum()).thenReturn(0);
        when(mockSheet1.getLastRowNum()).thenReturn(0);
        when(mockSheet2.iterator()).thenAnswer(iterateOver(mockRow));
        when(mockSheet2.getRow(0)).thenReturn(mockRow);
        when(mockSheet2.getFirstRowNum()).thenReturn(0);
        when(mockSheet2.getLastRowNum()).thenReturn(0);
        when(mockSheet3.iterator()).thenAnswer(iterateOver(mockRow));
        when(mockSheet3.getRow(0)).thenReturn(mockRow);
        when(mockSheet3.getFirstRowNum()).thenReturn(0);
        when(mockSheet3.getLastRowNum()).thenReturn(0);
        when(mockWorkbook.getNumberOfSheets()).thenReturn(3);
        when(mockWorkbook.getSheetAt(0)).thenReturn(mockSheet1);
        when(mockWorkbook.getSheetAt(1)).thenReturn(mockSheet2);
        when(mockWorkbook.getSheetAt(2)).thenReturn(mockSheet3);
        assertEquals(3, size(new Worksheets(mockWorkbook)));
    }
}
