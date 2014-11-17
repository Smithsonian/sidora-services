
package com.asoroka.sidora.excel2tabular;

import static com.google.common.math.DoubleMath.isMathematicalInteger;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BOOLEAN;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_ERROR;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING;
import static org.apache.poi.ss.usermodel.DateUtil.getJavaDate;
import static org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Date;

import org.apache.poi.ss.format.CellDateFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;

public class TabularCell {

    private final Cell cell;

    public static final String defaultQuote = "\"";

    private String quote = defaultQuote;

    private static final String EMPTY_STRING = "";

    private static final Logger log = getLogger(TabularCell.class);

    public TabularCell(final Cell c, final String q) {
        this.cell = c;
        this.quote = q;
    }

    public TabularCell(final Cell c) {
        this(c, defaultQuote);
    }

    @Override
    public String toString() {
        if (cell == null) {
            return EMPTY_STRING;
        }
        switch (cell.getCellType()) {
        case CELL_TYPE_NUMERIC: {
            if (isCellDateFormatted(cell)) {
                log.trace("Found date-formatted cell: {}", cell);
                final Date date = getJavaDate(cell.getNumericCellValue());
                final String dateFmt = cell.getCellStyle().getDataFormatString();
                return new CellDateFormatter(dateFmt).format(date);
            }
            final Double numericCellValue = cell.getNumericCellValue();
            if (isMathematicalInteger(numericCellValue)) {
                return Integer.toString(numericCellValue.intValue());
            }
            return Double.toString(numericCellValue);
        }
        case CELL_TYPE_BLANK:
            return EMPTY_STRING;

        case CELL_TYPE_BOOLEAN:
            return Boolean.toString(cell.getBooleanCellValue());

        case Cell.CELL_TYPE_FORMULA: {
            cell.setCellType(cell.getCachedFormulaResultType());
            return toString();
        }
        case CELL_TYPE_STRING:
            final String stringCellValue = cell.getStringCellValue();
            try {
                return Integer.toString(parseInt(stringCellValue));
            } catch (final NumberFormatException e) {
                try {
                    return Double.toString(parseDouble(stringCellValue));
                } catch (final NumberFormatException e1) {
                    return quote(stringCellValue);
                }
            }
        case CELL_TYPE_ERROR:
            return FormulaError.forInt(cell.getErrorCellValue()).getString();
        default:
            throw new ExcelParsingException("Unintelligible cell type: " + cell.getCellType() + " at " +
                    new CellReference(cell.getRowIndex(), cell.getColumnIndex()).formatAsString() + "!");
        }
    }

    private String quote(final String quotable) {
        return quote + quotable.replace(quote, quote + quote) + quote;
    }
}
