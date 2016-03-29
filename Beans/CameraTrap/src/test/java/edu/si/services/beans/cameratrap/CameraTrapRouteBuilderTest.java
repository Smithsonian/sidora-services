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

package edu.si.services.beans.cameratrap;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.JndiRegistry;
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

    @EndpointInject(uri = "mock:direct:validationErrorMessageAggregationStrategy")
    private MockEndpoint mockAggregationStrategyEndpoint;

    CameraTrapValidationMessage cameraTrapValidationMessage = new CameraTrapValidationMessage();
    /**
     * Testing post ingestion validation route where the RELS-EXT resource object reference count
     * compared to the expected ingested resource count.  When the validation fails, aggregation strategy endpoint
     * is expected to receive a message with Deployment Package ID, and the validation failed message
     *
     * @throws Exception
     */
    @Test
    public void testValidatePostResourceCountFailRoute() throws Exception {

        String camelFileParent = "10002000";
        int resourceCount = 100;
        int relsExtResourceCount = 103;

        //using adviceWith to mock the dependencies for testing purpose
        context.getRouteDefinition("CameraTrapValidatePostResourceCount").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip("direct:validationErrorMessageAggregationStrategy*");
            }
        });

        //creating a new messageBean that is expected from the test route
        CameraTrapValidationMessage.MessageBean messageBean = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                String.format("Post Resource Count validation failed. Expected %s but found %s", resourceCount, relsExtResourceCount), false);

        //expected message body response
        mockAggregationStrategyEndpoint.expectedBodiesReceived(messageBean);

        //expect the endpoint to get invoked once
        mockAggregationStrategyEndpoint.expectedMessageCount(1);

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
        context.getRouteDefinition("CameraTrapValidatePostResourceCount").adviceWith(context, new AdviceWithRouteBuilder() {
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
     * Registering the cameraTrapValidationMessage bean
     *
     * @return JndiRegistry
     * @throws Exception
     */
    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();
        jndi.bind("cameraTrapValidationMessage", cameraTrapValidationMessage);

        return jndi;
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