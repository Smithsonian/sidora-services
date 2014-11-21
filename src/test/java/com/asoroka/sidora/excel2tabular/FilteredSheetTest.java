
package com.asoroka.sidora.excel2tabular;

import static com.google.common.collect.Iterables.isEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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

    @Test
    public void testEmptySheet() {
        when(mockSheet.iterator()).thenReturn(Collections.<Row> emptyIterator());
        assertTrue(isEmpty(new FilteredSheet(mockSheet)));
    }

    @Test
    // TODO this test should fail-- the filtered sheet should be empty
            public
            void testSheetWithABlankRow() {
        when(blankRow.iterator()).thenReturn(Collections.<Cell> emptyIterator());
        when(blankRow.getRowNum()).thenReturn(0);
        when(blankRow.getLastCellNum()).thenReturn((short) 0);
        when(mockSheet.iterator()).thenReturn(Iterators.<Row> singletonIterator(blankRow));
        when(mockSheet.createRow(0)).thenReturn(blankRow);
        final Iterator<Row> testResult = new FilteredSheet(mockSheet).iterator();
        assertEquals(blankRow, testResult.next());
        assertFalse(testResult.hasNext());
    }
}
