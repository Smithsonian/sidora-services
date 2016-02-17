
package edu.si.sidora.excel2tabular;

import static java.lang.Integer.MAX_VALUE;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BOOLEAN;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_ERROR;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_FORMULA;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DateUtil.class)
public class TabularCellTest {

    private static final boolean TEST_BOOLEAN_VALUE = true;

    private static final String TEST_BOOLEAN_PRINTED_VALUE = Boolean.toString(TEST_BOOLEAN_VALUE);

    private static final double TEST_DATE_VALUE = 33993;

    private static final String TEST_DATE_FORMAT = "d-mmm-yyyy";

    private static final String TEST_DATE_PRINTED_VALUE = "24-Jan-1993";

    private static final double TEST_INTEGER_VALUE = 56;

    private static final String TEST_INTEGER_PRINTED_VALUE = "56";

    private static final double TEST_DECIMAL_VALUE = 23.1;

    private static final String TEST_DECIMAL_PRINTED_VALUE = "23.1";

    private static final String TEST_STRING_VALUE = "A string value.";

    private static final String TEST_STRING_PRINTED_VALUE = "\"" + TEST_STRING_VALUE + "\"";

    @Mock
    private Cell stringCell, integerCell, decimalCell, blankCell, booleanCell, funkyCell, errorCell, formulaCell,
            integerStringCell, decimalStringCell, dateCell;

    private final Cell nullCell = null;

    @Mock
    private CellStyle mockDateCellStyle;

    @Before
    public void setUp() {
        when(stringCell.getStringCellValue()).thenReturn(TEST_STRING_VALUE);
        when(stringCell.getCellType()).thenReturn(CELL_TYPE_STRING);

        // Excel data persistence is a nightmare
        when(dateCell.getNumericCellValue()).thenReturn(TEST_DATE_VALUE);
        when(dateCell.getCellType()).thenReturn(CELL_TYPE_NUMERIC);
        when(dateCell.getCellStyle()).thenReturn(mockDateCellStyle);
        when(mockDateCellStyle.getDataFormatString()).thenReturn(TEST_DATE_FORMAT);
        spy(DateUtil.class);
        when(DateUtil.isCellDateFormatted(dateCell)).thenReturn(true);

        when(decimalCell.getNumericCellValue()).thenReturn(TEST_DECIMAL_VALUE);
        when(decimalCell.getCellType()).thenReturn(CELL_TYPE_NUMERIC);
        when(integerCell.getNumericCellValue()).thenReturn(TEST_INTEGER_VALUE);
        when(integerCell.getCellType()).thenReturn(CELL_TYPE_NUMERIC);
        when(blankCell.getCellType()).thenReturn(CELL_TYPE_BLANK);
        when(booleanCell.getBooleanCellValue()).thenReturn(TEST_BOOLEAN_VALUE);
        when(booleanCell.getCellType()).thenReturn(CELL_TYPE_BOOLEAN);
        when(funkyCell.getCellType()).thenReturn(/* Not a good value for a POI/Excel cell type */MAX_VALUE);
        when(formulaCell.getCachedFormulaResultType()).thenReturn(CELL_TYPE_NUMERIC);
        when(formulaCell.getNumericCellValue()).thenReturn(TEST_INTEGER_VALUE);
        // TODO account for TabularCell's action on a changed cell type in a more robust manner
        when(formulaCell.getCellType()).thenReturn(CELL_TYPE_FORMULA, CELL_TYPE_NUMERIC);
    }

    @Test
    public void testStringValuedCell() {
        assertEquals(TEST_STRING_PRINTED_VALUE, new TabularCell(stringCell).toString());
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
        assertTrue(new TabularCell(blankCell).toString().isEmpty());
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

    @Test
    public void testDateValuedCell() {
        assertEquals(TEST_DATE_PRINTED_VALUE, new TabularCell(dateCell).toString());
    }

}
