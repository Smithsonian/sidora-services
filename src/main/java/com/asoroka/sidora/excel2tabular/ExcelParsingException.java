
package com.asoroka.sidora.excel2tabular;

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

    /**
     * @param message
     */
    public ExcelParsingException(final String message) {
        super(message);
    }

    private static final long serialVersionUID = 1L;

}
