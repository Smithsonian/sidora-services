
package com.asoroka.sidora.excel2tabular;

import static com.asoroka.sidora.excel2tabular.IsBlankRow.isBlankRow;
import static com.google.common.collect.Iterators.singletonIterator;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;

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
        when(rowWithDataCell.iterator()).thenReturn(singletonIterator(dataCell));
        when(blankCell.getCellType()).thenReturn(CELL_TYPE_BLANK);
        when(allBlankCellRow.iterator()).thenReturn(singletonIterator(blankCell));
        when(emptyRow.iterator()).thenReturn(Collections.<Cell> emptyIterator());
    }

    @Test
    public void testAllBlankCellRow() {
        assertTrue(isBlankRow.apply(allBlankCellRow));
    }

    @Test
    public void testEmptyRow() {
        assertTrue(isBlankRow.apply(emptyRow));
    }

    @Test
    public void testDataRow() {
        assertFalse(isBlankRow.apply(rowWithDataCell));
    }

}
