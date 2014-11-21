
package com.asoroka.sidora.excel2tabular;

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterators.forArray;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Iterators;

@RunWith(MockitoJUnitRunner.class)
public class FilteredSheetTest {

    @Mock
    private Sheet mockSheet;

    @Mock
    private Row blankRow;

    @Mock
    private Cell blankCell;

    @Before
    public void setUp() {
        when(blankCell.getCellType()).thenReturn(CELL_TYPE_BLANK);
    }

    @Test
    public void testEmptySheet() {
        when(mockSheet.iterator()).thenReturn(Collections.<Row> emptyIterator());
        assertTrue(isEmpty(new FilteredSheet(mockSheet)));
    }

    @Test
    public void testSheetWithABlankRow() {
        when(blankRow.iterator()).thenReturn(Collections.<Cell> emptyIterator());
        when(blankRow.getRowNum()).thenReturn(0);
        when(blankRow.getLastCellNum()).thenReturn((short) 0);
        when(mockSheet.getFirstRowNum()).thenReturn(0);
        when(mockSheet.getLastRowNum()).thenReturn(0);
        when(mockSheet.iterator()).thenReturn(Iterators.<Row> singletonIterator(blankRow));
        when(mockSheet.getRow(0)).thenReturn(blankRow);
        assertTrue(isEmpty(new FilteredSheet(mockSheet)));
    }

    @Test
    public void testSheetWithARowOfBlankCells() {
        when(blankRow.iterator()).thenReturn(forArray(blankCell, blankCell, blankCell));
        when(blankRow.getRowNum()).thenReturn(0);
        when(mockSheet.getFirstRowNum()).thenReturn(0);
        when(mockSheet.getLastRowNum()).thenReturn(0);
        when(mockSheet.iterator()).thenReturn(Iterators.<Row> singletonIterator(blankRow));
        when(mockSheet.getRow(0)).thenReturn(blankRow);
        assertTrue(isEmpty(new FilteredSheet(mockSheet)));
    }

    @Test
    public void testSheetWithThreeBlankRows() {
        when(blankRow.iterator()).thenReturn(Collections.<Cell> emptyIterator());
        when(blankRow.getRowNum()).thenReturn(0, 1, 2);
        when(mockSheet.getFirstRowNum()).thenReturn(0);
        when(mockSheet.getLastRowNum()).thenReturn(2);
        when(mockSheet.iterator()).thenReturn(forArray(blankRow, blankRow, blankRow));
        when(mockSheet.getRow(0)).thenReturn(blankRow);
        when(mockSheet.getRow(1)).thenReturn(blankRow);
        when(mockSheet.getRow(2)).thenReturn(blankRow);
        assertTrue(isEmpty(new FilteredSheet(mockSheet)));
    }

    @Test
    public void testSheetWithThreeRowsOfBlankCells() {
        when(blankRow.iterator()).thenReturn(forArray(blankCell, blankCell, blankCell));
        when(blankRow.getRowNum()).thenReturn(0, 1, 2);
        when(mockSheet.getFirstRowNum()).thenReturn(0);
        when(mockSheet.getLastRowNum()).thenReturn(2);
        when(mockSheet.iterator()).thenReturn(forArray(blankRow, blankRow, blankRow));
        when(mockSheet.getRow(0)).thenReturn(blankRow);
        when(mockSheet.getRow(1)).thenReturn(blankRow);
        when(mockSheet.getRow(2)).thenReturn(blankRow);
        assertTrue(isEmpty(new FilteredSheet(mockSheet)));
    }
}
