/*
 * Copyright 2015-2016 Smithsonian Institution.
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

package edu.si.services.camel.reader;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Scanner;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author jshingler
 * @version 1.0
 */
public class ReaderStreamComponentTest extends ReaderComponentTest
{
    @Test
    public void testReader() throws Exception
    {
        Message msg = this.testReader("/wrongFile.txt");
        Object body = msg.getBody(InputStream.class);

        assertTrue("Output type should have been InputStream", body instanceof InputStream);

        String actualText = new Scanner((InputStream) body).useDelimiter("\\Z").next();

        File expected = new File(this.getClass().getResource("/test.txt").toURI());
        String expectedText = new Scanner(expected).useDelimiter("\\Z").next();

        assertEquals("File contents should have been the same", actualText, expectedText);

        Map<String, Object> expectedHeaders = createHeaders(expected);
        testHeaders(expectedHeaders, msg.getHeaders());
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure()
            {
                from("direct:start")
                        .transform().simple("test.txt")
                        .to("reader:file")
                        .to("mock:result");
            }
        };
    }//end createRouteBuilder
}//end ReaderStreamComponentTest.class
