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
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
    public void testOtherKindOfEmptyRow() {
        when(mockRow.iterator()).thenAnswer(iterateOver());
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
    public void testTwoStringCells() {
        when(stringCell.getStringCellValue()).thenReturn(TEST_STRING_VALUE);
        when(stringCell.getCellType()).thenReturn(CELL_TYPE_STRING);
        when(mockRow.getLastCellNum()).thenReturn((short) 2);
        when(mockRow.getCell(0)).thenReturn(stringCell);
        when(mockRow.getCell(1)).thenReturn(stringCell);
        final String testValue = TEST_STRING_PRINTED_VALUE + "," + TEST_STRING_PRINTED_VALUE;
        assertEquals(testValue, new TabularRow(mockRow).toString());
    }
}
