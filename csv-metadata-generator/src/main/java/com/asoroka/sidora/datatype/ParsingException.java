
package com.asoroka.sidora.datatype;

/**
 * Represents a failure to parse some string into an instance of some type.
 * 
 * @author ajs6f
 */
public class ParsingException extends Exception {

    public ParsingException(final Exception e) {
        super(e);
    }

    public ParsingException() {
        super();
    }

    public ParsingException(final java.lang.String msg) {
        super(msg);
    }

    private static final long serialVersionUID = 1L;

}
