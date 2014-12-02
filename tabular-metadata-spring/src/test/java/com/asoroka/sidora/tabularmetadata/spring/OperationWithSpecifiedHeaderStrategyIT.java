
package com.asoroka.sidora.tabularmetadata.spring;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.datatype.DataType;

@ContextConfiguration("/spring-xml/operation-with-specified-header-strategy.xml")
public class OperationWithSpecifiedHeaderStrategyIT extends SpringITFramework {

    private static String testFileSimpleWithCrazyHeaders = "simple-with-crazy-headers.csv";

    private static final Logger log = getLogger(OperationWithSpecifiedHeaderStrategyIT.class);

    @Test
    public void testWithSimpleData() throws MalformedURLException, IOException {
        log.trace("testWithSimpleData()...");
        final TabularMetadata result =
                testFile(getTestFile(testFileSimple), STRING_TYPES, getStringRange(), DataType.String);
        final List<String> headerNames = result.headerNames();
        final int numHeaders = headerNames.size();
        for (int i = 1; i < numHeaders; i++) {
            log.trace("Examining header #{}", i);
            assertEquals("Found wrong name for header!", "Variable " + i, headerNames.get(i - 1));

        }
        log.trace("End testWithSimpleData().");
    }

    @Test
    public void testWithDataWithHeadersMatching() throws MalformedURLException, IOException {
        log.trace("testWithDataWithHeadersMatching()...");
        final TabularMetadata result =
                testFile(getTestFile(testFileSimpleWithCrazyHeaders), SIMPLE_TYPES, getIntRange(), DataType.Integer);
        assertEquals("Didn't find header names when we should have!", asList("MATCH1", "MATCH2", "MATCH3"), result
                .headerNames());
        log.trace("End testWithDataWithHeadersMatching().");
    }
}
