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

package edu.si.services.camel.fcrepo;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Integration testing for the FcrepoComponent.
 *
 * @author parkjohn
 */
public class FcrepoIntegrationTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    private MockEndpoint mockEndpoint;
    private final FcrepoConfiguration fcrepoConfiguration = new FcrepoConfiguration();

    /**
     * Test calling datastreams api using the Fcrepo component.
     * This test currently assumes Fedora repository is running with test:deploymentObject object exists.
     *
     * Use the test_deploymentObject.xml FOXML 1.1 export found in the test/resources directory to create one if needed.
     *
     */
    @Test
    public void testGetDatastreamsFound()
    {
        String expectedStatus = "200";
        String expectedContentType = "text/xml";
        String deploymentPID = "test:deploymentObject";

        //setting up expected headers before sending message to test route
        Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, FcrepoHttpMethodEnum.GET);
        headers.put("SitePID", deploymentPID);
        template.sendBodyAndHeaders("direct:testFcrepoComponent", "foo", headers);

        //getting the response message
        Message message = this.mockEndpoint.getExchanges().get(0).getIn();

        String responseHttpCode = message.getHeader(Exchange.HTTP_RESPONSE_CODE, String.class);
        String responseContentType = message.getHeader(Exchange.CONTENT_TYPE, String.class);

        assertEquals(expectedStatus, responseHttpCode);
        assertEquals(expectedContentType, responseContentType);

        String body = message.getBody(String.class);
        assertNotNull(body);
    }


    /**
     * Test calling datastreams api and expecting it to return 404
     * This test currently assumes Fedora service is running.
     */
    @Test
    public void testGetDatastreamsNotFound()
    {
        String expectedStatus = "404";
        String expectedContentType = "text/plain";
        String deploymentPID = "test:notfound";

        //setting up expected headers before sending message to test route
        Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, FcrepoHttpMethodEnum.GET);
        headers.put("SitePID", deploymentPID);
        template.sendBodyAndHeaders("direct:testFcrepoComponent", "bar", headers);

        //getting the response message
        Message message = this.mockEndpoint.getExchanges().get(0).getIn();

        String responseHttpCode = message.getHeader(Exchange.HTTP_RESPONSE_CODE, String.class);
        String responseContentType = message.getHeader(Exchange.CONTENT_TYPE, String.class);

        assertEquals(expectedStatus, responseHttpCode);
        assertEquals(expectedContentType, responseContentType);
    }

    /**
     * Setting up the camel context with the fcrepo configuration
     *
     * @return CamelContext
     * @throws Exception
     */
    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();
        PropertiesComponent prop = context.getComponent("properties", PropertiesComponent.class);
        prop.setLocation("classpath:fcrepotest.properties");

        try {
            System.setProperty("si.fedora.host", context.resolvePropertyPlaceholders("{{si.fedora.host}}"));
            fcrepoConfiguration.setFedoraHost(context.resolvePropertyPlaceholders("{{si.fedora.host}}"));
            fcrepoConfiguration.setAuthUsername(context.resolvePropertyPlaceholders("{{si.fedora.user}}"));
            fcrepoConfiguration.setAuthPassword(context.resolvePropertyPlaceholders("{{si.fedora.password}}"));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return context;
    }

    /**
     * Registering the fcrepoConfiguration bean
     *
     * @return JndiRegistry
     * @throws Exception
     */
    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();
        jndi.bind("fcrepoConfiguration", fcrepoConfiguration);

        return jndi;
    }

    /**
     * Creating route builder for the test run
     *
     * @return RouteBuilder
     */
    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:testFcrepoComponent")
                        .routeId("testFcrepoComponentRoute")
                        .toD("fcrepo:objects/${header.SitePID}/datastreams?format=xml")
                        .to("mock:result")
                        .end();
            }
        };
    }
}
