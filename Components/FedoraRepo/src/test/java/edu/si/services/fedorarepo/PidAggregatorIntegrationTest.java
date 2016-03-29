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

import edu.si.services.fedorarepo.aggregators.PidAggregationStrategy;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

import org.junit.Test;

/**
 * Tests the FedoraRepo Camel component PID aggregation strategy.
 *
 * @author davisda
 * @author jshingler
 */
public class PidAggregatorIntegrationTest extends FedoraComponentIntegrationTest
{
    @Test
    public void testPidAggregator() throws Exception
    {
        mockEnpoint.expectedMinimumMessageCount(4);

        StringBuilder sb = new StringBuilder("<testing>");
        sb.append("\t<test>test1</test>");
        sb.append("\t<test>test2</test>");
        sb.append("\t<test>test3</test>");
        sb.append("</testing>");

        template.sendBody("direct:testAggregator", sb.toString());

        assertMockEndpointsSatisfied();

        String expectedPids = null;

        for (int i = 0; i < 3; i++)
        {
            Message in = this.getMockMessage(i);
            String pid = in.getHeader("CamelFedoraPid", String.class);
            assertNotNull("PID should not be null", pid);

            if (expectedPids == null)
            {
                expectedPids = pid.trim();
            }//end if
            else
            {
                expectedPids += "," + pid.trim();
            }//end else

            String body = in.getBody(String.class);
            assertEquals("Test body should equal test#", body, String.format("test%d", i + 1));
            
        }//end for

        Message in = this.getMockMessage(3);
        String actualPids = in.getHeader("PIDAggregation", String.class);
        
        assertNotNull("Aggregator body shouldn't be null", actualPids);
        assertArrayEquals("PIDs should be equal", expectedPids.split(","), actualPids.split(","));
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure()
            {
                from("direct:testAggregator")
                        .split(xpath("//test/text()"), new PidAggregationStrategy())
                        .to("fedora:nextPid")
                        .to("mock:result")
                        .end()
                        .to("mock:result");

            }
        };
    }
}
