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
package edu.si.services.fedorarepo;

import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;

/**
 * Tests the nextPID functions.
 *
 * @author davisda
 * @author jshingler
 */
public class NextPidIntegrationTest extends FedoraComponentIntegrationTest
{
    @Test
    public void testGetNextPid() throws Exception
    {
        mockEnpoint.expectedMinimumMessageCount(1);

        template.sendBody("direct:testPid", "Integration Test");

        assertMockEndpointsSatisfied();

        Message msg = this.getMockMessage();
        String pid = msg.getHeader("CamelFedoraPid", String.class);

        assertNotNull("PID Should not be null", pid);

        String body = msg.getBody(String.class);
        assertEquals("Body should be the same", "Integration Test", body);
    }

    @Test
    public void testGetNextPidWithNamespace() throws Exception
    {
        mockEnpoint.expectedMinimumMessageCount(1);

        template.sendBody("direct:testPidWithNamespace", "Integration Test");

        assertMockEndpointsSatisfied();

        Message msg = this.getMockMessage();
        String pid = msg.getHeader("CamelFedoraPid", String.class);

        assertNotNull("PID should not be null", pid);
        assertTrue("PID namespace should start with 'namespaceTest'.", pid.startsWith("namespaceTest"));

        String body = msg.getBody(String.class);
        assertEquals("Body should be the same", "Integration Test", body);

    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure()
            {
                from("direct:testPid")
                        .to("fedora:nextPid")
                        .to("mock:result");
                from("direct:testPidWithNamespace")
                        .to("fedora:nextPid?namespace=namespaceTest")
                        .to("mock:result");
            }
        };
    }
}
