/**
 * Copyright 2015 Smithsonian Institution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.You may obtain a copy of
 * the License at: http://www.apache.org/licenses/
 *
 * This software and accompanying documentation is supplied without
 * warranty of any kind. The copyright holder and the Smithsonian Institution:
 * (1) expressly disclaim any warranties, express or implied, including but not
 * limited to any implied warranties of merchantability, fitness for a
 * particular purpose, title or non-infringement; (2) do not assume any legal
 * liability or responsibility for the accuracy, completeness, or usefulness of
 * the software; (3) do not represent that use of the software would not
 * infringe privately owned rights; (4) do not warrant that the software
 * is error-free or will be maintained, supported, updated or enhanced;
 * (5) will not be liable for any indirect, incidental, consequential special
 * or punitive damages of any kind or nature, including but not limited to lost
 * profits or loss of data, on any basis arising from contract, tort or
 * otherwise, even if any of the parties has been warned of the possibility of
 * such loss or damage.
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */


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
