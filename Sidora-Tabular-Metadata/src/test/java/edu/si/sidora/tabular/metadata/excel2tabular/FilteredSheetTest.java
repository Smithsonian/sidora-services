/*
 * Copyright 2018-2019 Smithsonian Institution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.You may obtain a copy of
 * the License at: http://www.apache.org/licenses/
 *
 * This software and accompanying documentation is supplied without
 * warranty of any kind. The copyright holder and the Smithsonian Institution:
 * (1) expressly disclaim any warranties, express or implied, including but not
 * limited to any implied warranties of merchantability, fitness for a
 * particular purpose, title or non-infringement; (2) do not assume any legal
 * liability or responsibility for the accuracy, completeness, or usefulness of
 * the software; (3) do not represent that use of the software would not
 * infringe privately owned rights; (4) do not warrant that the software
 * is error-free or will be maintained, supported, updated or enhanced;
 * (5) will not be liable for any indirect, incidental, consequential special
 * or punitive damages of any kind or nature, including but not limited to lost
 * profits or loss of data, on any basis arising from contract, tort or
 * otherwise, even if any of the parties has been warned of the possibility of
 * such loss or damage.
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */

package edu.si.sidora.tabular.metadata.excel2tabular;

import static edu.si.sidora.tabular.metadata.excel2tabular.UnitTestUtilities.iterateOver;
import static com.google.common.collect.Iterables.elementsEqual;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Iterables.isEmpty;
import static java.util.Arrays.asList;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FilteredSheetTest {

    @Mock
    private Sheet mockSheet;

    @Mock
    private Row nullRow, emptyRow, blankRow, blankRow2, rowWithDataCell, rowWithDataCell1, rowWithDataCell2;

    @Mock
    private Cell blankCell, dataCell;

    @Before
    public void setUp() {
        when(emptyRow.iterator()).thenAnswer(iterateOver());
        when(nullRow.iterator()).thenAnswer(iterateOver());
        when(blankCell.getCellType()).thenReturn(CELL_TYPE_BLANK);
        when(blankRow.iterator()).thenAnswer(iterateOver(blankCell, blankCell, blankCell));
        when(blankRow2.iterator()).thenAnswer(iterateOver(blankCell, blankCell, blankCell));
        when(dataCell.getCellType()).thenReturn(CELL_TYPE_NUMERIC);
        when(rowWithDataCell.iterator()).thenAnswer(iterateOver(dataCell));
        when(rowWithDataCell.getLastCellNum()).thenReturn((short) 1);
        when(rowWithDataCell1.iterator()).thenAnswer(iterateOver(dataCell));
        when(rowWithDataCell1.getLastCellNum()).thenReturn((short) 1);
        when(rowWithDataCell2.iterator()).thenAnswer(iterateOver(dataCell));
        when(rowWithDataCell2.getLastCellNum()).thenReturn((short) 1);
    }

    @Test
    public void testEmptySheet() {
        when(mockSheet.iterator()).thenAnswer(iterateOver());
        assertTrue(isEmpty(new FilteredSheet(mockSheet)));
    }

    @Test
    public void testSheetWithAnEmptyRow() {
        when(emptyRow.getRowNum()).thenReturn(0);
        when(mockSheet.getFirstRowNum()).thenReturn(0);
        when(mockSheet.getLastRowNum()).thenReturn(0);
        when(mockSheet.iterator()).thenAnswer(iterateOver(emptyRow));
        when(mockSheet.getRow(0)).thenReturn(emptyRow);
        assertTrue(isEmpty(new FilteredSheet(mockSheet)));
    }

    @Test
    public void testSheetWithARowOfBlankCells() {
        when(blankRow.getRowNum()).thenReturn(0);
        when(mockSheet.getFirstRowNum()).thenReturn(0);
        when(mockSheet.getLastRowNum()).thenReturn(0);
        when(mockSheet.iterator()).thenAnswer(iterateOver(blankRow));
        when(mockSheet.getRow(0)).thenReturn(blankRow);
        assertTrue(isEmpty(new FilteredSheet(mockSheet)));
    }

    @Test
    public void testSheetWithThreeEmptyRows() {
        when(emptyRow.getRowNum()).thenReturn(0, 1, 2);
        when(mockSheet.getFirstRowNum()).thenReturn(0);
        when(mockSheet.getLastRowNum()).thenReturn(2);
        when(mockSheet.iterator()).thenAnswer(iterateOver(emptyRow, emptyRow, emptyRow));
        when(mockSheet.getRow(0)).thenReturn(emptyRow);
        when(mockSheet.getRow(1)).thenReturn(emptyRow);
        when(mockSheet.getRow(2)).thenReturn(emptyRow);
        assertTrue(isEmpty(new FilteredSheet(mockSheet)));
    }

    @Test
    public void testSheetWithThreeRowsOfBlankCells() {
        when(blankRow.getRowNum()).thenReturn(0, 1, 2);
        when(mockSheet.getFirstRowNum()).thenReturn(0);
        when(mockSheet.getLastRowNum()).thenReturn(2);
        when(mockSheet.iterator()).thenAnswer(iterateOver(blankRow, blankRow, blankRow));
        when(mockSheet.getRow(0)).thenReturn(blankRow);
        when(mockSheet.getRow(1)).thenReturn(blankRow);
        when(mockSheet.getRow(2)).thenReturn(blankRow);
        assertTrue(isEmpty(new FilteredSheet(mockSheet)));
    }

    @Test
    public void testSheetWithTwoRowsOfBlankCellsAroundAGap() {
        when(blankRow.getRowNum()).thenReturn(0, 2);
        when(mockSheet.getFirstRowNum()).thenReturn(0);
        when(mockSheet.getLastRowNum()).thenReturn(2);
        when(mockSheet.iterator()).thenAnswer(iterateOver(blankRow, null, blankRow));
        when(mockSheet.getRow(0)).thenReturn(blankRow);
        when(mockSheet.createRow(1)).thenReturn(nullRow);
        when(mockSheet.getRow(2)).thenReturn(blankRow);
        assertTrue(isEmpty(new FilteredSheet(mockSheet)));
    }

    @Test
    public void testSheetWithTwoBlankRowsAroundADataRow() {
        when(blankRow.getRowNum()).thenReturn(0);
        when(blankRow2.getRowNum()).thenReturn(2);
        when(rowWithDataCell.getRowNum()).thenReturn(1);
        when(mockSheet.getFirstRowNum()).thenReturn(0);
        when(mockSheet.getLastRowNum()).thenReturn(2);
        when(mockSheet.iterator()).thenAnswer(iterateOver(blankRow, rowWithDataCell, blankRow));
        when(mockSheet.getRow(0)).thenReturn(blankRow);
        when(mockSheet.getRow(1)).thenReturn(rowWithDataCell);
        when(mockSheet.getRow(2)).thenReturn(blankRow2);
        assertEquals(rowWithDataCell, getOnlyElement(new FilteredSheet(mockSheet)));
    }

    @Test
    public void testSheetWithBlankRowAndGapAroundADataRow() {
        when(blankRow.getRowNum()).thenReturn(0);
        when(rowWithDataCell.getRowNum()).thenReturn(1);
        when(nullRow.getRowNum()).thenReturn(2);
        when(mockSheet.getFirstRowNum()).thenReturn(0);
        when(mockSheet.getLastRowNum()).thenReturn(2);
        when(mockSheet.iterator()).thenAnswer(iterateOver(blankRow, rowWithDataCell, null));
        when(mockSheet.getRow(0)).thenReturn(blankRow);
        when(mockSheet.getRow(1)).thenReturn(rowWithDataCell);
        when(mockSheet.createRow(2)).thenReturn(nullRow);
        assertEquals(rowWithDataCell, getOnlyElement(new FilteredSheet(mockSheet)));
    }

    @Test
    public void testSheetWithGapAndBlankRowAroundADataRow() {
        when(nullRow.getRowNum()).thenReturn(0);
        when(rowWithDataCell.getRowNum()).thenReturn(1);
        when(blankRow.getRowNum()).thenReturn(2);
        when(mockSheet.getFirstRowNum()).thenReturn(0);
        when(mockSheet.getLastRowNum()).thenReturn(2);
        when(mockSheet.iterator()).thenAnswer(iterateOver(null, rowWithDataCell, blankRow));
        when(mockSheet.createRow(0)).thenReturn(nullRow);
        when(mockSheet.getRow(1)).thenReturn(rowWithDataCell);
        when(mockSheet.getRow(2)).thenReturn(blankRow);
        assertEquals(rowWithDataCell, getOnlyElement(new FilteredSheet(mockSheet)));
    }

    @Test
    public void testSheetWithThreeRowsOfDataCells() {
        when(rowWithDataCell.getRowNum()).thenReturn(0);
        when(rowWithDataCell1.getRowNum()).thenReturn(1);
        when(rowWithDataCell2.getRowNum()).thenReturn(2);
        when(mockSheet.getFirstRowNum()).thenReturn(0);
        when(mockSheet.getLastRowNum()).thenReturn(2);
        when(mockSheet.iterator()).thenAnswer(iterateOver(rowWithDataCell, rowWithDataCell1, rowWithDataCell2));
        when(mockSheet.getRow(0)).thenReturn(rowWithDataCell);
        when(mockSheet.getRow(1)).thenReturn(rowWithDataCell1);
        when(mockSheet.getRow(2)).thenReturn(rowWithDataCell2);
        assertTrue(elementsEqual(
                asList(rowWithDataCell, rowWithDataCell1, rowWithDataCell2),
                new FilteredSheet(mockSheet)));
    }
}
