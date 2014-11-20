
package com.asoroka.sidora.excel2tabular;

import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BOOLEAN;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_ERROR;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_FORMULA;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

// TODO develop test for a date-valued cell
@RunWith(MockitoJUnitRunner.class)
public class TabularCellTest {

    private static final boolean TEST_BOOLEAN_VALUE = true;

    private static final String TEST_BOOLEAN_PRINTED_VALUE = Boolean.toString(TEST_BOOLEAN_VALUE);

    private static final double TEST_INTEGER_VALUE = 56;

    private static final String TEST_INTEGER_PRINTED_VALUE = "56";

    private static final double TEST_DECIMAL_VALUE = 23.1;

    private static final String TEST_DECIMAL_PRINTED_VALUE = "23.1";

    private static final String TEST_STRING_VALUE = "A string value.";

    private static final String TEST_STRING_PRINTED_VALUE = "\"" + TEST_STRING_VALUE + "\"";

    private static final String TEST_INTEGER_STRING_VALUE = "945";

    private static final String TEST_INTEGER_STRING_PRINTED_VALUE = "945";

    private static final String TEST_DECIMAL_STRING_VALUE = "3456.23";

    private static final String TEST_DECIMAL_STRING_PRINTED_VALUE = "3456.23";

    @Mock
    private Cell stringCell, integerCell, decimalCell, emptyCell, booleanCell, funkyCell, errorCell, formulaCell,
            integerStringCell, decimalStringCell;

    private final Cell nullCell = null;

    @Before
    public void setUp() {
        when(stringCell.getStringCellValue()).thenReturn(TEST_STRING_VALUE);
        when(stringCell.getCellType()).thenReturn(CELL_TYPE_STRING);
        when(integerStringCell.getStringCellValue()).thenReturn(TEST_INTEGER_STRING_VALUE);
        when(integerStringCell.getCellType()).thenReturn(CELL_TYPE_STRING);
        when(decimalStringCell.getStringCellValue()).thenReturn(TEST_DECIMAL_STRING_VALUE);
        when(decimalStringCell.getCellType()).thenReturn(CELL_TYPE_STRING);
        when(decimalCell.getNumericCellValue()).thenReturn(TEST_DECIMAL_VALUE);
        when(decimalCell.getCellType()).thenReturn(CELL_TYPE_NUMERIC);
        when(integerCell.getNumericCellValue()).thenReturn(TEST_INTEGER_VALUE);
        when(integerCell.getCellType()).thenReturn(CELL_TYPE_NUMERIC);
        when(emptyCell.getCellType()).thenReturn(CELL_TYPE_BLANK);
        when(booleanCell.getBooleanCellValue()).thenReturn(TEST_BOOLEAN_VALUE);
        when(booleanCell.getCellType()).thenReturn(CELL_TYPE_BOOLEAN);
        when(funkyCell.getCellType()).thenReturn(Integer.MAX_VALUE);
        when(formulaCell.getCachedFormulaResultType()).thenReturn(CELL_TYPE_NUMERIC);
        when(formulaCell.getNumericCellValue()).thenReturn(TEST_INTEGER_VALUE);
        // TODO account for TabularCell's action of a changed cell type in a more robust manner
        when(formulaCell.getCellType()).thenReturn(CELL_TYPE_FORMULA, CELL_TYPE_NUMERIC);
    }

    @Test
    public void testStringValuedCell() {
        assertEquals(TEST_STRING_PRINTED_VALUE, new TabularCell(stringCell).toString());
    }

    @Test
    public void testIntegerStringValuedCell() {
        assertEquals(TEST_INTEGER_STRING_PRINTED_VALUE, new TabularCell(integerStringCell).toString());
    }

    @Test
    public void testDecimalStringValuedCell() {
        assertEquals(TEST_DECIMAL_STRING_PRINTED_VALUE, new TabularCell(decimalStringCell).toString());
    }

    @Test
    public void testIntegerValuedCell() {
        assertEquals(TEST_INTEGER_PRINTED_VALUE, new TabularCell(integerCell).toString());
    }

    @Test
    public void testDecimalValuedCell() {
        assertEquals(TEST_DECIMAL_PRINTED_VALUE, new TabularCell(decimalCell).toString());
    }

    @Test
    public void testEmptyCell() {
        assertTrue(new TabularCell(emptyCell).toString().isEmpty());
    }

    @Test
    public void testBooleanValuedCell() {
        assertEquals(TEST_BOOLEAN_PRINTED_VALUE, new TabularCell(booleanCell).toString());
    }

    @Test(expected = ExcelParsingException.class)
    public void testFunkyValuedCell() {
        new TabularCell(funkyCell).toString();
    }

    @Test
    public void testErrorValuedCell() {
        for (final FormulaError error : FormulaError.values()) {
            when(errorCell.getCellType()).thenReturn(CELL_TYPE_ERROR);
            when(errorCell.getErrorCellValue()).thenReturn(error.getCode());
            assertEquals(error.getString(), new TabularCell(errorCell).toString());
        }
    }

    @Test
    public void testFormulaValuedCell() {
        assertEquals(TEST_INTEGER_PRINTED_VALUE, new TabularCell(formulaCell).toString());
    }

    @Test
    public void testNullCell() {
        assertTrue(new TabularCell(nullCell).toString().isEmpty());
    }

}
