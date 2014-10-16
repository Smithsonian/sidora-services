
package com.asoroka.sidora.tabularmetadata.spring;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("/spring-xml/default-operation.xml")
public class DefaultOperationIT extends SpringITFramework {

    @Test
    public void testWithSimpleData() throws MalformedURLException, IOException {
        testFile(getTestFile(testFileSimple), SIMPLE_TYPES, getIntRange());
    }

    @Test
    public void testWithSlightlySimpleData() throws MalformedURLException, IOException {
        testFile(getTestFile(testFileSlightlyLessSimple), SLIGHTLY_SIMPLE_TYPES, getFloatRange());
    }

}
