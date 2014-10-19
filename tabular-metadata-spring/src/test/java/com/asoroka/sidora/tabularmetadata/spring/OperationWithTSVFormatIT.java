
package com.asoroka.sidora.tabularmetadata.spring;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

@ContextConfiguration("/spring-xml/operation-with-tsv-format.xml")
public class OperationWithTSVFormatIT extends SpringITFramework {

    private static final String testTSVFile = "simple.tsv";

    @Test
    public void testWithTSVFormat() throws MalformedURLException, IOException {
        testFile(getTestFile(testTSVFile), SIMPLE_TYPES, getIntRange(), DataType.Integer);
    }
}
