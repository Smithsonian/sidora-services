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
package edu.smithsonian.services.fedorarepo;

import com.yourmediashelf.fedora.client.FedoraClient;
import java.io.InputStream;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author jshingler
 */
public class DatastreamIntegrationTest extends FedoraComponentIntegrationTest
{

    @Test
    public void testDatastream() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(2);

        template.sendBody("direct:testCreate", null);
        Message in = mock.getExchanges().get(0).getIn();

        InputStream input = this.getClass().getResourceAsStream("/test-image.jpg");
        template.sendBodyAndHeaders("direct:testDatastream", input, in.getHeaders());

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(1).getIn();

        assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));

        String pid = msg.getHeader("CamelFedoraPid", String.class);
        FedoraClient.purgeObject(pid).execute();
    }

    @Test
    public void testDataStreamNoPid() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(2);

        template.sendBody("direct:testCreate", null);
        Message in = mock.getExchanges().get(0).getIn();

        InputStream input = this.getClass().getResourceAsStream("/test-image.jpg");
        template.sendBodyAndHeaders("direct:testDatastreamNoPid", input, in.getHeaders());

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(1).getIn();

        assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));

        String pid = msg.getHeader("CamelFedoraPid", String.class);
        FedoraClient.purgeObject(pid).execute();
    }

    @Test
    public void testDataStreamString() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(2);

        template.sendBody("direct:testCreate", null);
        Message in = mock.getExchanges().get(0).getIn();

        String input = "Testing,input,CSV##More,test,";
        template.sendBodyAndHeaders("direct:testDatastreamCSV", input, in.getHeaders());

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(1).getIn();

        assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));

        String pid = msg.getHeader("CamelFedoraPid", String.class);

        FedoraClient.purgeObject(pid).execute();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure()
            {
                from("direct:testDatastream")
                        .recipientList(simple("fedora:datastream?pid=${header.CamelFedoraPid}&name=OBJ&type=image/jpeg&group=M"))
                        .to("mock:result");
                from("direct:testDatastreamNoPid")
                        .recipientList(simple("fedora:datastream?name=OBJ&type=image/jpeg&group=M"))
                        .to("mock:result");
                from("direct:testDatastreamCSV")
                        .recipientList(simple("fedora:datastream?name=OBJ&type=text/csv&group=M"))
                        .to("mock:result");

                //Helper route to set up object in add datastreams to
                from("direct:testCreate")
                        .to("fedora:create")
                        .to("mock:result");

            }
        };
    }
}
