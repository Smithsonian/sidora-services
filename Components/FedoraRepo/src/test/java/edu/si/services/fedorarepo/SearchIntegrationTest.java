/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
        String query = String.format("SELECT ?o FROM <#ri> WHERE {?o <fedora-model:label> '%s'}", searchLabel);

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
