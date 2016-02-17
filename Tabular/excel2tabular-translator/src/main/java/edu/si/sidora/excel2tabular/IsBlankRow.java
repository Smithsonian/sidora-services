
package edu.si.sidora.excel2tabular;

import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.isEmpty;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.google.common.base.Predicate;

/**
 * A {@link Row} that is blank is either empty or contains only blank {@link Cell}s.
 * 
 * @author ajs6f
 */
public class IsBlankRow implements Predicate<Row> {

    public static final IsBlankRow isBlankRow = new IsBlankRow();

    public static final boolean isBlankRow(final Row row) {
        return isBlankRow.apply(row);
    }

    private IsBlankRow() {
    }

    @Override
    public boolean apply(final Row row) {
        return isEmpty(row) || all(row, isBlankCell);
    }

    private static final Predicate<Cell> isBlankCell = new Predicate<Cell>() {

        @Override
        public boolean apply(final Cell cell) {
            return cell.getCellType() == CELL_TYPE_BLANK;
        }
    };
}
