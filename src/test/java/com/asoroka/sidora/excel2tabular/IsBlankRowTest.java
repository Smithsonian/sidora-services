
package com.asoroka.sidora.excel2tabular;

import static com.asoroka.sidora.excel2tabular.IsBlankRow.isBlankRow;
import static com.asoroka.sidora.excel2tabular.UnitTestUtilities.iterateOver;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IsBlankRowTest {

    @Mock
    private Row allBlankCellRow, emptyRow, rowWithDataCell;

    @Mock
    private Cell blankCell, dataCell;

    @Before
    public void setUp() {
        when(dataCell.getCellType()).thenReturn(CELL_TYPE_NUMERIC);
        when(rowWithDataCell.iterator()).thenAnswer(iterateOver(dataCell));
        when(blankCell.getCellType()).thenReturn(CELL_TYPE_BLANK);
        when(allBlankCellRow.iterator()).thenAnswer(iterateOver(blankCell));
        when(emptyRow.iterator()).thenAnswer(iterateOver());
    }

    @Test
    public void testAllBlankCellRow() {
        assertTrue(isBlankRow(allBlankCellRow));
    }

    @Test
    public void testEmptyRow() {
        assertTrue(isBlankRow(emptyRow));
    }

    @Test
    public void testDataRow() {
        assertFalse(isBlankRow(rowWithDataCell));
    }

}
