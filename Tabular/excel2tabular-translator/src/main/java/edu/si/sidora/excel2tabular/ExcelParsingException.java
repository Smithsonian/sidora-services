
package edu.si.sidora.excel2tabular;

/**
 * @author ajs6f
 */
public class ExcelParsingException extends RuntimeException {

    /**
     * @param message
     * @param cause
     */
    public ExcelParsingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = 1L;

}
