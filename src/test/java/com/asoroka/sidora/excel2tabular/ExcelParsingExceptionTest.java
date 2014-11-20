
package com.asoroka.sidora.excel2tabular;

import org.junit.Test;

public class ExcelParsingExceptionTest {

    @Test(expected = ExcelParsingException.class)
    public void testException() {
        throw new ExcelParsingException("Expected.", new RuntimeException());
    }
}
