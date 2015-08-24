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

package edu.si.services.fedorarepo;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraClientException;
import com.yourmediashelf.fedora.client.response.GetObjectProfileResponse;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

import org.junit.Test;

/**
 * Tests SIdora services ingest functions.
 *
 * @author davisda
 * @author jshingler
 */
public class CreateIntegrationTest extends FedoraComponentIntegrationTest
{
    @Test
    public void testCreate() throws Exception
    {
        
        mockEnpoint.expectedMinimumMessageCount(1);

        String expectedBody = "Test Create Body";
        template.sendBody("direct:testCreate", expectedBody);

        assertMockEndpointsSatisfied();

        Message msg = this.getMockMessage();
        String pid = msg.getHeader("CamelFedoraPid", String.class);

        try
        {
            assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));
            assertEquals("Message Body should be", expectedBody, msg.getBody(String.class));
        }
        finally
        {
            if (pid != null)
            {
                FedoraClient.purgeObject(pid).execute();
            }
        }
    }

    @Test
    public void testCreateWithParams() throws Exception
    {
        mockEnpoint.expectedMinimumMessageCount(1);

        template.sendBody("direct:testCreateParams", null);

        assertMockEndpointsSatisfied();

        Message msg = this.getMockMessage();
        String pid = msg.getHeader("CamelFedoraPid", String.class);

        try
        {
            assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));
            assertTrue("PID should have namespace 'namespaceTest'", pid.startsWith("namespaceTest:"));

            GetObjectProfileResponse profile = FedoraClient.getObjectProfile(pid).execute();
            assertEquals("Owner wasn't properly set", "Test Owner", profile.getOwnerId());
            assertEquals("Label wasn't set properly", "This is a label 2 test", profile.getLabel());

            String foxml = FedoraClient.getObjectXML(pid).execute().getEntity(String.class);
            String logMsg = "Logging action 4 create/ingest";
            assertStringContains(foxml, logMsg);
        }
        finally
        {
            if (pid != null)
            {
                FedoraClient.purgeObject(pid).execute();
            }
        }

    }

    @Test
    public void testCreateHeaders() throws Exception
    {
        mockEnpoint.expectedMessageCount(2);

        String expectedOwner = "ActualOwner";

        template.sendBodyAndHeader("direct:testCreateHeaders", "Test", "CamelFedoraOwner", expectedOwner);

        assertMockEndpointsSatisfied();

        String pid = getMockMessage().getHeader("CamelFedoraPid", String.class);

        try
        {
            GetObjectProfileResponse profile = FedoraClient.getObjectProfile(pid).execute();
            assertEquals("Owner wasn't properly set", expectedOwner, profile.getOwnerId());
        }
        catch (FedoraClientException fedoraClientException)
        {
            fail("PID was not set by header");
        }
        finally
        {
            if (pid != null)
            {
                FedoraClient.purgeObject(pid);
            }
        }
    }

    @Test
    public void testCreateOverwriteHeaders() throws Exception
    {
        mockEnpoint.expectedMessageCount(1);

        String badOwner = "Incorrect Owner";
        String badPid = "bad:pid";

        Map<String, Object> inHeaders = new HashMap<String, Object>();
        inHeaders.put("CamelFedoraOwner", badOwner);
        inHeaders.put("CamelFedoraPid", badPid);

        template.sendBodyAndHeaders("direct:testCreateOverwriteHeaders", null, inHeaders);

        assertMockEndpointsSatisfied();

        String pid = getMockMessage().getHeader("CamelFedoraPid", String.class);

        try
        {
            assertNotNull("PID should have been set", pid);
            assertNotEquals("PID was not overwritten", badPid, pid);
            GetObjectProfileResponse profile = FedoraClient.getObjectProfile(pid).execute();
            assertEquals("Owner wasn't properly set", "URI Set Owner", profile.getOwnerId());
        }
        catch (FedoraClientException fedoraClientException)
        {
            fail("Could not find Fedora object from PID");
        }
        finally
        {
            if (pid != null)
            {
                FedoraClient.purgeObject(pid);
            }
        }

    }

    @Test
    public void testCreateWithPid() throws Exception
    {
        mockEnpoint.expectedMinimumMessageCount(1);

        template.sendBody("direct:testCreateWithPid", null);

        assertMockEndpointsSatisfied();

        Message msg = this.getMockMessage();
        String pid = msg.getHeader("CamelFedoraPid", String.class);

        try
        {
            assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));
        }
        finally
        {
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
                from("direct:testCreateParams")
                        .to("fedora:create?label=This is a label 2 test&owner=Test Owner&namespace=namespaceTest&log=Logging action 4 create/ingest")
                        .to("mock:result");
                from("direct:testCreate")
                        .to("fedora:create")
                        .to("mock:result");
                from("direct:testCreateHeaders")
                        .to("fedora:nextPid")
                        .to("mock:result")
                        .to("fedora:create")
                        .to("mock:result");
                from("direct:testCreateOverwriteHeaders")
                        .to("fedora:create?owner=URI Set Owner&pid=null")
                        .to("mock:result");
                from("direct:testCreateWithPid")
                        .to("fedora:nextPID")
                        .recipientList(simple("fedora:create?pid=${header.CamelFedoraPid}"))
                        .to("mock:result");
            }
        };
    }
}
