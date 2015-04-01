package edu.si.services.beans.excel;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.*;

/**
 * Tests conversions of Excel (xls and xlsx) streams to csv.
 *
 * @author davisda
 */
public class ExcelToCSVTest
{
    @Test
    public void testXLSToCSV() throws Exception
    {
        InputStream input = this.getClass().getResourceAsStream("/BDD-SP-08.xls");
        //OutputStream os = new ByteArrayOutputStream();

        ExcelToCSV e2csv= new ExcelToCSV();
        OutputStream outStream = e2csv.convertExcelToCSV(input, null);

        String csv = outStream.toString();
        assertEquals("Año,Autor,", csv.substring(0, 10));
        assertEquals(97053, csv.length());
    }

    @Test
    public void testXLSXToCSV() throws Exception
    {
        InputStream input = this.getClass().getResourceAsStream("/BDD-SP-08.xlsx");
        //OutputStream os = new ByteArrayOutputStream();

        ExcelToCSV e2csv= new ExcelToCSV();
        OutputStream outStream = e2csv.convertExcelToCSV(input, null);

        String csv = outStream.toString();
        assertEquals("Año,Autor,", csv.substring(0, 10));
        System.out.println(csv.length());
        assertEquals(97053, csv.length());
    }
}
