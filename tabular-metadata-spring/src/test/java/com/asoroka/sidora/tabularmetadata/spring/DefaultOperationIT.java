
package com.asoroka.sidora.tabularmetadata.spring;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.datatype.DataType;

@ContextConfiguration("/spring-xml/default-operation.xml")
public class DefaultOperationIT extends SpringITFramework {

    @Test
    public void testWithSimpleData() throws MalformedURLException, IOException {
        final TabularMetadata results =
                testFile(getTestFile(testFileSimple), SIMPLE_TYPES, getIntRange(), DataType.Integer);
        final Set<String> enumeratedValuesForFirstField = results.enumeratedValues.get(0).get(DataType.String);
        assertEquals("Found wrong enumerated values in first field!", newHashSet("Kirk", "Chekov", "Scott", "Uhuru"),
                enumeratedValuesForFirstField);
    }

    @Test
    public void testWithSlightlySimpleData() throws MalformedURLException, IOException {
        testFile(getTestFile(testFileSlightlyLessSimple), SLIGHTLY_SIMPLE_TYPES, getFloatRange(), DataType.Decimal);
    }

}
