/**
 * 
 */

package edu.si.sidora.excel2tabular;

import static com.google.common.collect.Iterables.isEmpty;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.google.common.collect.AbstractIterator;

/**
 * @author ajs6f
 */
public class Worksheets implements Iterable<FilteredSheet> {

    final Workbook workbook;

    final int numberOfSheets;

    /**
     * @param workbook provides the sheets over which to iterate
     */
    public Worksheets(final Workbook wb) {
        this.workbook = wb;
        this.numberOfSheets = workbook.getNumberOfSheets();
    }

    @Override
    public AbstractIterator<FilteredSheet> iterator() {
        return new AbstractIterator<FilteredSheet>() {

            private int sheetIndex = 0;

            @Override
            protected FilteredSheet computeNext() {
                if (sheetIndex >= numberOfSheets) {
                    return endOfData();
                }
                final Sheet currentSheet = workbook.getSheetAt(sheetIndex++);
                final FilteredSheet filteredSheet = new FilteredSheet(currentSheet);
                return isEmpty(filteredSheet) ? computeNext() : filteredSheet;
            }
        };
    }
}
