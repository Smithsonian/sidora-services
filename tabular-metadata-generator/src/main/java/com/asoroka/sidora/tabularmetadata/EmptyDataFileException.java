
package com.asoroka.sidora.tabularmetadata;

/**
 * Thrown when an empty data file is offered for examination.
 * 
 * @author ajs6f
 */
public class EmptyDataFileException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    /**
     * @param url the URL which resolved to an empty data file
     */
    public EmptyDataFileException(final String url) {
        super(url);
        // TODO Auto-generated constructor stub
    }

}
