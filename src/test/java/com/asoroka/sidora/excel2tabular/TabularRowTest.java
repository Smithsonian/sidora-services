
package com.asoroka.sidora.excel2tabular;

import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TabularRowTest {

    private static final String TEST_STRING_VALUE = "Some string value.";

    private static final String TEST_STRING_PRINTED_VALUE = "\"" + TEST_STRING_VALUE + "\"";

    @Mock
    private Row mockRow;

    @Mock
    private Cell mockEmptyCell, stringCell;

    @Test
    public void testEmptyRow() {
        when(mockRow.getLastCellNum()).thenReturn((short) 0);
        assertEquals("", new TabularRow(mockRow).toString());
    }

    @Test
    public void testSingleEmptyCell() {
        when(mockRow.getLastCellNum()).thenReturn((short) 1);
        when(mockEmptyCell.getCellType()).thenReturn(CELL_TYPE_BLANK);
        when(mockRow.getCell(0)).thenReturn(mockEmptyCell);
        assertEquals("", new TabularRow(mockRow).toString());
    }

    public void testSingleNullCell() {
        when(mockRow.getLastCellNum()).thenReturn((short) 1);
        when(mockRow.getCell(0)).thenReturn(null);
        assertEquals("", new TabularRow(mockRow).toString());
    }

    @Test
    public void testSingleStringCell() {
        when(stringCell.getStringCellValue()).thenReturn(TEST_STRING_VALUE);
        when(stringCell.getCellType()).thenReturn(CELL_TYPE_STRING);
        when(mockRow.getLastCellNum()).thenReturn((short) 1);
        when(mockRow.getCell(0)).thenReturn(stringCell);
        assertEquals(TEST_STRING_PRINTED_VALUE, new TabularRow(mockRow).toString());
    }

    @Test
    public void testRemoveCell() {
        when(mockRow.getLastCellNum()).thenReturn((short) 1);
        when(mockRow.getCell(0)).thenReturn(stringCell);
        final Iterator<TabularCell> cellIterator = new TabularRow(mockRow).iterator();
        assertTrue(cellIterator.hasNext());
        cellIterator.next();
        cellIterator.remove();
        verify(mockRow).removeCell(stringCell);
    }
}
