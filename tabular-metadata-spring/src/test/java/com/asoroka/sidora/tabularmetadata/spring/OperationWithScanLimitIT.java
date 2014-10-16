
package com.asoroka.sidora.tabularmetadata.spring;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("/spring-xml/scan-limit-operation.xml")
public class OperationWithScanLimitIT extends SpringITFramework {

    private static final String testFileWithMarkerRow = "simplewithmarkerrow.csv";

    /**
     * Scan a file with a very high maximum value in the last column and check that we didn't get a minMax range
     * including that value.
     * 
     * @throws MalformedURLException
     * @throws IOException
     */
    @Test
    public void testWithScanLimit() throws MalformedURLException, IOException {
        testFile(getTestFile(testFileWithMarkerRow), SLIGHTLY_SIMPLE_TYPES, getFloatRange());
    }

}
