/* 
 * Copyright 2014 Smithsonian Institution.  
 *
 * Permission is granted to use, copy, modify,
 * and distribute this software and its documentation for educational, research
 * and non-profit purposes, without fee and without a signed licensing
 * agreement, provided that this notice, including the following two paragraphs,
 * appear in all copies, modifications and distributions.  For commercial
 * licensing, contact the Office of the Chief Information Officer, Smithsonian
 * Institution, 380 Herndon Parkway, MRC 1010, Herndon, VA. 20170, 202-633-5256.
 *  
 * This software and accompanying documentation is supplied â€œas isâ€� without
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
 */
package edu.smithsonian.services.camel.reader;

import java.io.File;
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
public class ReaderStringComponentTest extends ReaderComponentTest
{

    @Test
    public void testReader() throws Exception
    {
        Message msg = this.testReader("/wrongFile.txt");
        Object body = msg.getBody();

        assertTrue("Output type should have been String", body instanceof String);

        String actualText = (String) body;

        File expected = new File(this.getClass().getResource("/folder/test2.txt").toURI());
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
                        .transform().simple("/folder/test2.txt")
                        .to("reader:file?type=text")
                        .to("mock:result");
            }
        };
    }//end createRouteBuilder
}//end ReaderStringComponentTest.class
