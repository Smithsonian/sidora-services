
package com.asoroka.sidora.csvmetadata.spring;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("/spring-xml/default-operation.xml")
public class DefaultOperationIT extends IT {

    private static File testFile = new File("src/test/resources/test-data/simple.csv");

    @Test
    public void testWithSimpleData() throws MalformedURLException, IOException {
        testInputs(testFile);
    }

}
