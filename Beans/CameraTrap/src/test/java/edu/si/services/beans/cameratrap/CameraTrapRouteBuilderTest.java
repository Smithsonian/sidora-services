/*
 * Copyright 2015 Smithsonian Institution.
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
package edu.si.services.beans.cameratrap;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Integration testing for the CameraTrapRouteBuilder class.
 *
 * @author parkjohn
 */
public class CameraTrapRouteBuilderTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    private MockEndpoint mockEndpoint;

    @EndpointInject(uri = "mock:activemq:queue:ct.post.validation.error")
    private MockEndpoint mockActiveMQEndpoint;

    /**
     * Testing post ingestion validation route where the RELS-EXT resource object reference count
     * compared to the expected ingested resource count.  When the validation fails, activemq is expected to receive a message
     * with Deployment Package ID, and the validation failed message
     *
     * @throws Exception
     */
    @Test
    public void testValidatePostResourceCountFailRoute() throws Exception {

        int camelFileParent = 10002000;
        int resourceCount = 100;
        int relsExtResourceCount = 103;

        //using adviceWith to mock the activemq component for testing purpose
        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("activemq:queue*");
            }
        });

        //expected message body response
        mockActiveMQEndpoint.expectedBodiesReceived(String.format("Deployment Package ID - %s, Message - " +
                "Post Resource Count validation failed.  Expected %s but found %s", camelFileParent, resourceCount, relsExtResourceCount));

        //expect the queue to get invoked once
        mockActiveMQEndpoint.expectedMessageCount(1);

        //setting up expected headers before sending message to test route
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFileParent", camelFileParent);
        headers.put("ResourceCount", resourceCount);
        headers.put("RelsExtResourceCount", relsExtResourceCount);
        template.sendBodyAndHeaders("direct:validatePostResourceCount", "body text", headers);

        assertMockEndpointsSatisfied();
    }

    /**
     * Testing post ingestion validation route where the RELS-EXT resource object reference count
     * compared to the expected ingested resource count.  When the validation passes, the current route logic logs the result.
     * In this test, we are using an anchor (id) in the route to create a mock end point to see if the message was received.
     *
     * @throws Exception
     */
    @Test
    public void testValidatePostResourceCountSuccessRoute() throws Exception {

        int camelFileParent = 10002001;
        int matchingCount = 10;
        String toSendBody = "some message";

        //using adviceWith to added mock endpoint with the id anchor for testing purpose
        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("ValidatePostResourceCountWhenBlock").after().to("mock:result");
            }
        });

        //setting up expected headers before sending message to test route
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFileParent", camelFileParent);
        headers.put("ResourceCount", matchingCount);
        headers.put("RelsExtResourceCount", matchingCount);
        template.sendBodyAndHeaders("direct:validatePostResourceCount", toSendBody, headers);

        mockEndpoint.expectedMessageCount(1);
        assertEquals(mockEndpoint.getReceivedExchanges().get(0).getIn().getBody().toString(), toSendBody);
        mockEndpoint.expectedHeaderReceived("CamelFileParent", camelFileParent);
        mockEndpoint.assertIsSatisfied();
    }

    /**
     * Testing post ingestion validation route to check whether the resource object is found in the Fedora RI.
     * If the object is not found then the message gets routed back to try redelivery based on the configurable values.
     * When the redelivery exhausts, the message gets sent to activemq queue for reporting purpose.  This test method
     * utilizes mock endpoints and mock behaviors to simulate external system like fedora repo.
     *
     * @throws Exception
     */
    @Test
    public void testValidateFedoraResourceRedeliverAndFailRoute() throws Exception {

        int camelFileParent = 10002002;
        int validationRedeliveryCounter = 1;
        int validationMaxRedeliveryAttempt = 10;
        String validationPID = "ct:100";

        //using adviceWith to mock the activemq component for testing purpose
        context.getRouteDefinitions().get(1).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("activemq:queue*");
                weaveByType(ToDefinition.class).selectFirst().replace().to("mock:result");
            }
        });
        //mocking the fedora search response and always returning as false
        mockEndpoint.returnReplyBody(new Expression() {
            @Override
            public <T> T evaluate(Exchange exchange, Class<T> type) {
                String body = "false";
                return (T) body;
            }
        });

        //expected message body response
        mockActiveMQEndpoint.expectedBodiesReceived(String.format("Deployment Package ID - %s, Resource PID - %s, " +
                "Message - Fedora RI Search validation failed", camelFileParent, validationPID));

        //expect the queue to get invoked once
        mockActiveMQEndpoint.expectedMessageCount(1);

        //setting up expected headers before sending message to test route
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFileParent", camelFileParent);
        headers.put("ValidationRedeliveryCounter", validationRedeliveryCounter);
        headers.put("ValidationMaxRedeliveryAttempt", validationMaxRedeliveryAttempt);
        headers.put("ValidationPID", validationPID);
        template.sendBodyAndHeaders("direct:validateFedoraResource", "body text", headers);

        //queue should be triggered when the maxRedeliveryAttempt is at max since
        //the fedora search response is always returning false in this scenario
        mockActiveMQEndpoint.expectedHeaderReceived("ValidationRedeliveryCounter", validationMaxRedeliveryAttempt);

        assertMockEndpointsSatisfied();
    }

    /**
     * Configure the route definition for the test class
     *
     * @return Returns CameraTrapRouteBuilder
     */
    @Override
    protected RouteBuilder createRouteBuilder() {
        return new CameraTrapRouteBuilder();
    }

}