/*
 * Copyright 2015 Smithsonian Institution.
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

package edu.si.services.beans.excel;

import java.io.*;
import java.util.ArrayList;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Converts an Excel spreadsheet (workbook) into a CSV. This class makes the following assumptions;
 * <list>
 * <li>1. Where the Excel workbook contains more that one worksheet, then a single
 *    CSV file will contain the data from all of the worksheets.</li>
 * <li>2. The data matrix contained in the CSV file will be square. This means that
 *    the number of fields in each record of the CSV file will match the number
 *    of cells in the longest row found in the Excel workbook. Any short records
 *    will be 'padded' with empty fields - an empty field is represented in the
 *    the CSV file in this way - ,,.</li>
 * <li>3. Empty fields will represent missing cells.</li>
 * <li>4. A record consisting of empty fields will be used to represent an empty row
 *    in the Excel workbook.</li>
 * </list>
 * Therefore, if the worksheet looked like this;
 *
 * <pre>
 *  ___________________________________________
 *     |       |       |       |       |       |
 *     |   A   |   B   |   C   |   D   |   E   |
 *  ___|_______|_______|_______|_______|_______|
 *     |       |       |       |       |       |
 *   1 |   1   |   2   |   3   |   4   |   5   |
 *  ___|_______|_______|_______|_______|_______|
 *     |       |       |       |       |       |
 *   2 |       |       |       |       |       |
 *  ___|_______|_______|_______|_______|_______|
 *     |       |       |       |       |       |
 *   3 |       |   A   |       |   B   |       |
 *  ___|_______|_______|_______|_______|_______|
 *     |       |       |       |       |       |
 *   4 |       |       |       |       |   Z   |
 *  ___|_______|_______|_______|_______|_______|
 *     |       |       |       |       |       |
 *   5 | 1,400 |       |  250  |       |       |
 *  ___|_______|_______|_______|_______|_______|
 *
 * </pre>
 *
 * Then, the resulting CSV file will contain the following lines (records);
 * <pre>
 * 1,2,3,4,5
 * ,,,,
 * ,A,,B,
 * ,,,,Z
 * "1,400",,250,,
 * </pre><p>
 * Typically, the comma is used to separate each of the fields that, together,
 * constitute a single record or line within the CSV file. This is not however
 * a hard and fast rule and so this class allows the user to determine which
 * character is used as the field separator and assumes the comma if none other
 * is specified.
 * </p><p>
 * If a field contains the separator then it will be escaped. If the file should
 * obey Excel's CSV formatting rules, then the field will be surrounded with
 * speech marks while if it should obey UNIX conventions, each occurrence of
 * the separator will be preceded by the backslash character.
 * </p><p>
 * If a field contains an end of line (EOL) character then it too will be
 * escaped. If the file should obey Excel's CSV formatting rules then the field
 * will again be surrounded by speech marks. On the other hand, if the file
 * should follow UNIX conventions then a single backslash will precede the
 * EOL character. There is no single applicable standard for UNIX and some
 * appications replace the CR with \r and the LF with \n but this class will
 * not do so.
 * </p><p>
 * If the field contains double quotes then that character will be escaped. It
 * seems as though UNIX does not define a standard for this while Excel does.
 * Should the CSV file have to obey Excel's formatting rules then the speech
 * mark character will be escaped with a second set of speech marks. Finally, an
 * enclosing set of speach marks will also surround the entire field. Thus, if
 * the following line of text appeared in a cell - "Hello" he said - it would
 * look like this when converted into a field within a CSV file - """Hello"" he
 * said".
 * </p><p>
 * Finally, it is worth noting that talk of CSV 'standards' is really slightly
 * misleading as there is no such thing. It may well be that the code in this
 * class has to be modified to produce files to suit a specific application
 * or requirement.
 * </p>
 * @author Mark B
 * @author davsida
 *
 * Derived from: http://svn.apache.org/repos/asf/poi/trunk/src/examples/src/org/apache/poi/ss/examples/ToCSV.java
 */
public class ExcelToCSV
{
    private static final Logger logger = LoggerFactory.getLogger(ExcelToCSV.class);

    private Workbook workbook = null;
    private ArrayList<ArrayList<String>> csvData = null;
    private int maxRowWidth = 0;
    private String formattingConvention = EXCEL_STYLE_ESCAPING;
    private DataFormatter formatter = null;
    private FormulaEvaluator evaluator = null;
    private String separator = null;
    private static final String DEFAULT_SEPARATOR = ",";

    /**
     * Identifies that the CSV file should obey Excel's formatting conventions
     * with regard to escaping certain embedded characters - the field separator,
     * speech mark and end of line (EOL) character
     */
    public static final String EXCEL_STYLE_ESCAPING = "EXCEL_STYLE_ESCAPING";

    /**
     * Identifies that the CSV file should obey UNIX formatting conventions
     * with regard to escaping certain embedded characters - the field separator
     * and end of line (EOL) character
     */
    public static final String UNIX_STYLE_ESCAPING = "UNIX_STYLE_ESCAPING";


    /**
     * Process the contents of a stream, convert the contents of the Excel
     * workbook into CSV format and write the result to the specified stream.
     * Workbooks with the .xls or * .xlsx formats are supported. This method
     * will ensure that the CSV file created contains the comma field separator
     * and that embedded characters such as the field separator, the EOL and
     * double quotes are escaped in accordance with Excel's convention.
     *
     * @param inStream An instance of the InputStream class that encapsulates the
     *        Excel workbook that is to be converted.
     * @param separator A String that contains the value, usually one character,
     *        that is used to separate the cells on a row.  Can be null.
     * @throws java.io.IOException Thrown if the stream handling encounters any
     *         problems during processing.
     * @throws org.apache.poi.openxml4j.exceptions.InvalidFormatException Thrown
     *         if the spreadsheet format cannot be processed
     *         to the arguments cannot be used.
     * @throws org.apache.poi.openxml4j.exceptions Thrown if the input is not a
     *         supported Excel format.
     */
    public OutputStream convertExcelToCSV(InputStream inStream,
                                          String separator)
        throws IOException, InvalidFormatException
    {
        // Simply chain the call to the overloaded convertExcelToCSV(String,
        // String, String, int) method, pass the default separator and ensure
        // that certain embedded characters are escaped in accordance with
        // Excel's formatting conventions
        if (separator == null) { separator = ExcelToCSV.DEFAULT_SEPARATOR; }

        return this.convertExcelToCSV(inStream,
                                      separator,
                                      ExcelToCSV.EXCEL_STYLE_ESCAPING);
    }


    /**
     * Process the contents of a stream, convert the contents of the Excel
     * workbook into CSV format and write the result to the specified stream.
     * Workbooks with the .xls or * .xlsx formats are supported. This method
     * will ensure that the CSV file created contains the comma field separator
     * and that embedded characters such as the field separator, the EOL and
     * double quotes are escaped in accordance with Excel's convention.
     *
     * @param inStream An instance of the InputStream class that encapsulates the
     *        Excel workbook that is to be converted.
     * @param separator A String that contains the value, usually one character,
     *        that is used to separate the cells on a row.  Can be null.
     * @param formattingConvention An int that determines if Excel style escaping
     *        or Unix style escaping should be used.
     * @throws java.io.IOException Thrown if the stream handling encounters any
     *         problems during processing.
     * @throws org.apache.poi.openxml4j.exceptions.InvalidFormatException Thrown
     *         if the spreadsheet format cannot be processed
     * @throws org.apache.poi.openxml4j.exceptions Thrown if the input is not a
     *         supported Excel format.
     */
    public OutputStream convertExcelToCSV(InputStream inStream,
                                          String separator,
                                          String formattingConvention)
        throws IOException, InvalidFormatException
    {
        OutputStream outStream = new ByteArrayOutputStream();
        if (separator == null) { separator = ExcelToCSV.DEFAULT_SEPARATOR; }

        // Ensure the value passed to the formattingConvention parameter is
        // within range.
        if (formattingConvention == null)
        {
            formattingConvention = ExcelToCSV.EXCEL_STYLE_ESCAPING;
        }
        else if (!formattingConvention.equals(ExcelToCSV.EXCEL_STYLE_ESCAPING) &&
                 !formattingConvention.equals(ExcelToCSV.UNIX_STYLE_ESCAPING))
        {
            logger.warn("ExcelToCSV: Improper formatting convention provided");
        }

        // Copy the seperator character and formatting convention into local
        // variables for use in other methods.
        this.separator = separator;
        this.formattingConvention = formattingConvention;
        workbook = WorkbookFactory.create(inStream);
        this.evaluator = this.workbook.getCreationHelper().createFormulaEvaluator();
        this.formatter = new DataFormatter(true);

        // Convert its contents into a CSV file.
        this.convertToCSV();

        // Return the converted CSV.
        return this.getCSVStream(outStream);
    }


    /**
     * Called to convert the contents of the currently opened workbook into
     * a CSV file.
     */
    private void convertToCSV()
    {
        Sheet sheet = null;
        Row row = null;
        int lastRowNum = 0;
        this.csvData = new ArrayList<ArrayList<String>>();

        logger.debug("Converting stream content to CSV format.");

        // Discover how many sheets there are in the workbook....
        int numSheets = this.workbook.getNumberOfSheets();

        // and then iterate through them.
        for (int i = 0; i < numSheets; i++)
        {
            // Get a reference to a sheet and check to see if it contains
            // any rows.
            sheet = this.workbook.getSheetAt(i);
            if (sheet.getPhysicalNumberOfRows() > 0)
            {
                // Note down the index number of the bottom-most row and
                // then iterate through all of the rows on the sheet starting
                // from the very first row - number 1 - even if it is missing.
                // Recover a reference to the row and then call another method
                // which will strip the data from the cells and build lines
                // for inclusion in the resylting CSV file.
                lastRowNum = sheet.getLastRowNum();
                for (int j = 0; j <= lastRowNum; j++)
                {
                    row = sheet.getRow(j);
                    this.rowToCSV(row);
                }
            }
        }
    }


    /**
     * Called to convert a row of cells into a line of data that can later be
     * output to the CSV file.
     *
     * @param row An instance of either the HSSFRow or XSSFRow classes that
     *            encapsulates information about a row of cells recovered from
     *            an Excel workbook.
     */
    private void rowToCSV(Row row)
    {
        Cell cell = null;
        int lastCellNum = 0;
        ArrayList<String> csvLine = new ArrayList<String>();

        logger.debug("Converting row content to CSV format.");

        // Check to ensure that a row was recovered from the sheet as it is
        // possible that one or more rows between other populated rows could be
        // missing - blank. If the row does contain cells then...
        if (row != null)
        {
            // Get the index for the right most cell on the row and then
            // step along the row from left to right recovering the contents
            // of each cell, converting that into a formatted String and
            // then storing the String into the csvLine ArrayList.
            lastCellNum = row.getLastCellNum();
            for (int i = 0; i <= lastCellNum; i++)
            {
                cell = row.getCell(i);
                if (cell == null) {
                    csvLine.add("");
                }
                else
                {
                    if(cell.getCellType() != Cell.CELL_TYPE_FORMULA)
                    {
                        csvLine.add(this.formatter.formatCellValue(cell));
                    }
                    else
                    {
                        csvLine.add(this.formatter.formatCellValue(cell, this.evaluator));
                    }
                }
            }

            // Make a note of the index number of the right most cell. This value
            // will later be used to ensure that the matrix of data in the CSV file
            // is square.
            if (lastCellNum > this.maxRowWidth)
            {
                this.maxRowWidth = lastCellNum;
            }
        }
        this.csvData.add(csvLine);
    }


    /**
     * Called to actually get the data recovered from the Excel workbook as in CSV format.
     *
     * @returns An output stream containing the CSV.
     * @throws java.io.IOException Thrown to indicate and error occurred in the
     *                             underylying file system.
     */
    private OutputStream getCSVStream(OutputStream outStream) throws IOException
    {
        OutputStreamWriter sw = null;
        BufferedWriter bw = null;
        ArrayList<String> line = null;
        StringBuffer buffer = null;
        String csvLineElement = null;

        try
        {
            logger.debug("Writing the CSV format.");

            // Open a writer for the CSV output.
            sw = new OutputStreamWriter(outStream);
            bw = new BufferedWriter(sw);

            // Step through the elements of the ArrayList that was used to hold
            // all of the data recovered from the Excel workbooks' sheets, rows
            // and cells.
            for (int i = 0; i < this.csvData.size(); i++)
            {
                buffer = new StringBuffer();

                // Get an element from the ArrayList that contains the data for
                // the workbook. This element will itself be an ArrayList
                // containing Strings and each String will hold the data recovered
                // from a single cell. The for() loop is used to recover elements
                // from this 'row' ArrayList one at a time and to write the Strings
                // away to a StringBuffer thus assembling a single line for inclusion
                // in the CSV file. If a row was empty or if it was short, then
                // the ArrayList that contains it's data will also be shorter than
                // some of the others. Therefore, it is necessary to check within
                // the for loop to ensure that the ArrayList contains data to be
                // processed. If it does, then an element will be recovered and
                // appended to the StringBuffer.
                line = this.csvData.get(i);
                for (int j = 0; j < this.maxRowWidth; j++)
                {
                    if (line.size() > j)
                    {
                        csvLineElement = line.get(j);
                        if (csvLineElement != null)
                        {
                            buffer.append(this.escapeEmbeddedCharacters(csvLineElement));
                        }
                    }
                    if (j < (this.maxRowWidth - 1))
                    {
                        buffer.append(this.separator);
                    }
                }

                // Once the line is built, output it to the CSV stream.
                logger.debug(buffer.toString().trim());
                bw.write(buffer.toString().trim());

                // Condition the inclusion of new line characters so as to
                // avoid an additional, superfluous, new line at the end of
                // the file.
                if (i < (this.csvData.size() - 1))
                {
                    bw.newLine();
                }
            }
        }
        finally
        {
            if (bw != null)
            {
                bw.flush();
                bw.close();
            }
        }

        return outStream;
    }


    /**
     * Checks to see whether the field - which consists of the formatted
     * contents of an Excel worksheet cell encapsulated within a String - contains
     * any embedded characters that must be escaped. The method is able to
     * comply with either Excel's or UNIX formatting conventions in the
     * following manner;
     *
     * With regard to UNIX conventions, if the field contains any embedded
     * field separator or EOL characters they will each be escaped by prefixing
     * a leading backspace character. These are the only changes that have yet
     * emerged following some research as being required.
     *
     * Excel has other embedded character escaping requirements, some that emerged
     * from empirical testing, other through research. Firstly, with regards to
     * any embedded speech marks ("), each occurrence should be escaped with
     * another speech mark and the whole field then surrounded with speech marks.
     * Thus if a field holds <em>"Hello" he said</em> then it should be modified
     * to appear as <em>"""Hello"" he said"</em>. Furthermore, if the field
     * contains either embedded separator or EOL characters, it should also
     * be surrounded with speech marks. As a result <em>1,400</em> would become
     * <em>"1,400"</em> assuming that the comma is the required field separator.
     * This has one consequence in, if a field contains embedded speech marks
     * and embedded separator characters, checks for both are not required as the
     * additional set of speech marks that should be placed around ay field
     * containing embedded speech marks will also account for the embedded
     * separator.
     *
     * It is worth making one further note with regard to embedded EOL
     * characters. If the data in a worksheet is exported as a CSV file using
     * Excel itself, then the field will be surrounded with speech marks. If the
     * resulting CSV file is then re-imports into another worksheet, the EOL
     * character will result in the original single field occupying more than
     * one cell. This same 'feature' is replicated in this classes behavior.
     *
     * @param field An instance of the String class encapsulating the formatted
     *        contents of a cell on an Excel worksheet.
     * @return A String that encapsulates the formatted contents of that
     *         Excel worksheet cell but with any embedded separator, EOL or
     *         speech mark characters correctly escaped.
     */
    private String escapeEmbeddedCharacters(String field)
    {
        StringBuffer buffer = null;

        // If the fields contents should be formatted to confrom with Excel's
        // convention....
        if (this.formattingConvention.equals(ExcelToCSV.EXCEL_STYLE_ESCAPING))
        {
            // Firstly, check if there are any speech marks (") in the field;
            // each occurrence must be escaped with another set of speech marks
            // and then the entire field should be enclosed within another
            // set of speech marks. Thus, "Yes" he said would become
            // """Yes"" he said"
            if (field.contains("\""))
            {
                buffer = new StringBuffer(field.replaceAll("\"", "\\\"\\\""));
                buffer.insert(0, "\"");
                buffer.append("\"");
            }
            else
            {
                // If the field contains either embedded separator or EOL
                // characters, then escape the whole field by surrounding it
                // with speech marks.
                buffer = new StringBuffer(field);
                if((buffer.indexOf(this.separator)) > -1 ||
                    (buffer.indexOf("\n")) > -1) {
                    buffer.insert(0, "\"");
                    buffer.append("\"");
                }
            }
            return(buffer.toString().trim());
        }
        // The only other formatting convention this class obeys is the UNIX one
        // where any occurrence of the field separator or EOL character will
        // be escaped by preceding it with a backslash.
        else
        {
            if( field.contains(this.separator))
            {
                field = field.replaceAll(this.separator, ("\\\\" + this.separator));
            }
            if (field.contains("\n"))
            {
                field = field.replaceAll("\n", "\\\\\n");
            }
            return(field);
        }
    }
}