/*
 * Copyright 2015 Smithsonian Institution.
 *
 * Permission is granted to use, copy, modify,
 * and distribute this software and its documentation for educational, research
 * and non-profit purposes, without fee and without a signed licensing
 * agreement, provided that this notice, including the following two paragraphs,
 * appear in all copies, modifications and distributions.  For commercial
 * licensing, contact the Office of the Chief Information Officer, Smithsonian
 * Institution, 380 Herndon Parkway, MRC 1010, Herndon, VA. 20170, 202-633-5256.
 *
 * This software and accompanying documentation is supplied "as is" without
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

package edu.si.services.camel.reader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.apache.camel.Message;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;

/**
 *
 * @author jshingler
 * @version 1.0
 */
public abstract class ReaderComponentTest extends CamelTestSupport
{
    protected Map<String, Object> createHeaders(File file)
    {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("CamelFileAbsolute", false);
        headers.put("CamelFileRelativePath", file.getPath());
        headers.put("CamelFilePath", file.getPath());
        headers.put("CamelFileNameConsumed", file.getName());
        headers.put("CamelFileNameOnly", file.getName());
        headers.put("CamelFileParent", file.getParentFile().getName());
        headers.put("CamelFileLength", file.length());
        headers.put("CamelFileLastModified", file.lastModified());
        headers.put("CamelFileName", file.getName());
        headers.put("CamelFileAbsolutePath", file.getAbsolutePath());

        return headers;
    }//end createHeaders

    protected Message testReader(String from) throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);

        File wrongFile = new File(this.getClass().getResource(from).toURI());
        String text = new Scanner(wrongFile).useDelimiter("\\Z").next();

        template.sendBodyAndHeaders("direct:start", text, createHeaders(wrongFile));

        assertMockEndpointsSatisfied();

        return mock.getExchanges().get(0).getIn();
        
    }//end testReader

    protected void testHeaders(Map<String, Object> expectedHeaders, Map<String, Object> actualHeaders)
    {
        for (Map.Entry<String, Object> entrySet : expectedHeaders.entrySet())
        {
            String key = entrySet.getKey();
            assertEquals(String.format("%s should match", key), entrySet.getValue(), actualHeaders.get(key));
        }//end for
    }
}//end ReaderComponentTest
