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

package edu.si.services.fedorarepo;

import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;

/**
 * Tests the SIdora RISearch function.
 *
 * @author davisda
 * @author jshingler
 */
public class SearchIntegrationTest extends FedoraComponentIntegrationTest
{
    @Test
    public void testSearch() throws Exception
    {
         String expectedBody = "o : <info:fedora/fedora-system:ContentModel-3.0>\n\n";

        // Use a system object.
        String searchLabel = "Content Model Object for Content Model Objects";
        String query = String.format("SELECT ?o FROM <info:edu.si.fedora#ri> WHERE {?o <info:fedora/fedora-system:def/model#label> '%s'}", searchLabel);

        template.sendBody("direct:testSearch", query);

        // Verify Search results.
        Message msg = this.getMockMessage();
        String body = msg.getBody(String.class);
        assertEquals(expectedBody, body);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure()
            {
                from("direct:testSearch")
                        .to("fedora:search?lang=sparql&format=simple")
                        .to("mock:result")
                        .end();
            }
        };
    }
}
