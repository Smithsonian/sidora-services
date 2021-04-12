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

import static com.google.common.math.DoubleMath.isMathematicalInteger;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BOOLEAN;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_ERROR;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING;
import static org.apache.poi.ss.usermodel.DateUtil.getJavaDate;
import static org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted;

import java.util.Date;

import org.apache.poi.ss.format.CellDateFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.util.CellReference;

public class TabularCell {

    private final Cell cell;

    public static final String defaultQuote = "\"";

    private String quote = defaultQuote;

    private static final String EMPTY_STRING = "";

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
        final int cellType = cell.getCellType();
        switch (cellType) {
        case CELL_TYPE_NUMERIC: {
            if (isCellDateFormatted(cell)) {
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
            return quote(stringCellValue);
        case CELL_TYPE_ERROR:
            return FormulaError.forInt(cell.getErrorCellValue()).getString();
        default:
            final CellReference cellReference = new CellReference(cell.getRowIndex(), cell.getColumnIndex());
            throw new ExcelParsingException("Unregistered cell type: " + cellType + " at " +
                    cellReference.formatAsString() + "!",
                    new IllegalArgumentException("Not a registered POI cell type: " + cellType));
        }
    }

    private String quote(final String quotable) {
        return quote + quotable.replace(quote, quote + quote) + quote;
    }
}
