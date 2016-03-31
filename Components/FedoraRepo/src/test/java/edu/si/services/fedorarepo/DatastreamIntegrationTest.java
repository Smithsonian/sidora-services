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

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.response.GetDatastreamResponse;
import java.io.InputStream;

import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests SIdora datastream operations.
 *
 * @author davisda
 * @author jshingler
 */
public class DatastreamIntegrationTest extends FedoraComponentIntegrationTest
{
    @Test
    public void testDatastream() throws Exception
    {
        mockEnpoint.expectedMinimumMessageCount(2);
        Message in = this.getMockMessage();

        InputStream input = this.getClass().getResourceAsStream("/test-image.jpg");
        template.sendBodyAndHeaders("direct:testDatastream", input, in.getHeaders());

        this.validate();
    }

    @Test
    public void testDataStreamNoPid() throws Exception
    {
        mockEnpoint.expectedMinimumMessageCount(2);
        Message in = this.getMockMessage();

        InputStream input = this.getClass().getResourceAsStream("/test-image.jpg");
        template.sendBodyAndHeaders("direct:testDatastreamNoPid", input, in.getHeaders());

        this.validate();
    }

    @Test
    public void testDataStreamString() throws Exception
    {
        mockEnpoint.expectedMinimumMessageCount(2);
        Message in = this.getMockMessage();

        String input = "Testing,input,CSV##More,test,";
        template.sendBodyAndHeaders("direct:testDatastreamCSV", input, in.getHeaders());

        this.validate("false");
    }

    @Test
    public void testGetDataStreamDisseminationString() throws Exception
    {
        //mockEnpoint.expectedMinimumMessageCount(2);
        Message in = this.getMockMessage();

        String input = "Testing,input,CSV##More,test,";
        template.sendBodyAndHeaders("direct:testDatastreamCSV", input, in.getHeaders());

        String dsOut = 
            template.requestBodyAndHeaders("direct:testDatastreamDisseminationString", null, in.getHeaders(), String.class);
        assertEquals(input, dsOut);

        // TODO - Move this out to an after method in case an exception is thrown.
        FedoraClient.purgeObject("test:dsIT").execute();
    }
    
    @Before
    public void initialize()
    {
        // We need an FDO on which to perform datastream operations.
        template.sendBody("direct:testCreate", null);
    }
    
    private void validate() throws Exception
    {
        this.validate("true");
    }

    private void validate(String expectedVersionable) throws Exception
    {

        String pid = null;

        // TODO - We know the PID, its fixed, so this can be simplified.
        
        try
        {
            assertMockEndpointsSatisfied();

            Message msg = this.getMockMessage(1);
            pid = msg.getHeader(Headers.PID, String.class);

            assertEquals("Ingest Status should have been 201", 201, msg.getHeader(Headers.STATUS));
            GetDatastreamResponse ds = FedoraClient.getDatastream(pid, "OBJ").execute();
            String versionable = ds.getDatastreamProfile().getDsVersionable();
            assertEquals("Datastream should not be versionable.", expectedVersionable, versionable);
        }
        finally
        {
            if (pid == null)
            {
                Message msg = this.getMockMessage();
                if (msg != null)
                {
                    pid = msg.getHeader(Headers.PID, String.class);
                }
            }
            if (pid != null)
            {
                FedoraClient.purgeObject(pid).execute();
            }
        }
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
                        .recipientList(simple("fedora://addDatastream?pid=${header.CamelFedoraPid}&name=OBJ&type=image/jpeg&group=M"))
                        .to("mock:result");
                
                from("direct:testDatastreamNoPid")
                        .to("fedora://addDatastream?name=OBJ&type=image/jpeg&group=M")
                        .to("mock:result");
                
                from("direct:testDatastreamCSV")
                        .to("fedora://addDatastream?name=OBJ&type=text/csv&group=M&versionable=false")
                        .to("mock:result");
                
                from("direct:testDatastreamDisseminationString")
                        .to("fedora://getDatastreamDissemination?dsId=OBJ")
                        .to("mock:result");
                
                // Helper route to set up an object for doing datastream operations.
                from("direct:testCreate")
                    .to("fedora:create?pid=test:dsIT")
                    .to("mock:result");
            }
        };
    }
}
