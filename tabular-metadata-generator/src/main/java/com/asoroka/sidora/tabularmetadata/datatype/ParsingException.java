
package com.asoroka.sidora.tabularmetadata.datatype;

/**
 * Represents a failure to parse some string into an instance of some type.
 * 
 * @author A. Soroka
 */
public class ParsingException extends Exception {

    public ParsingException(final String msg) {
        super(msg);
    }

    public ParsingException(final String msg, final Exception e) {
        super(msg, e);
    }

    private static final long serialVersionUID = 1L;

}
