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

package edu.si.services.beans.edansidora;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.io.FileUtils.readFileToString;

/** Tests focusing on the JMS selector used to select only CT Ingest and CT Fedora object updates. The JMS Selector is used to only process messages that meet the following criteria.
 *
 * Fedora Related Messages must meet the following criteria to be selected for processing:
 * - user author name/origin not equal to the camel user name that the Camera Trap ingest uses
 * - and only OBJ datastreams
 * - and where dsLabel does not contain "Observations" filtering out OBJ datastreams for Researcher Observations, Volunteer Observations, and Image Observations
 * - and methodNames equal addDatastream, modifyDatastreamByValue, modifyDatastreamByReference, modifyObject, or ingest
 * - and where the pid contains "ct:"
 * Camera Trap Ingest related messages must meet the following criteria to be selected for processing:
 * - addEdanIds header present and equals true
 *
 * @author jbirkhimer
 */
public class EdanIdsJmsSelectorTest extends EDAN_CT_BlueprintTestSupport {

    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static String JMS_TEST_QUEUE;
    private static String TEST_PID;
    @Override
    protected String getBlueprintDescriptor() {
        return "OSGI-INF/blueprint/edan-ids-sidora-route.xml";
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        JMS_TEST_QUEUE = getExtra().getProperty("edanIds.queue");
        TEST_PID = getExtra().getProperty("si.ct.namespace") + ":test";
    }

    /**
     * A simple straight forward JMS selector test to make sure things are working
     *
     * @throws Exception
     */
    @Test
    public void testJmsSelectorSimple() throws Exception {
        String expectedBody = "<root><a key='first' num='1'/><b key='second' num='2'>b</b></root>";

        String edanIds_selector = "XPATH '/root/b=''b'' and /root/b[@key=''second'']'";

        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedBodiesReceived(expectedBody);
        resultEndpoint.setAssertPeriod(1500);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("activemq:queue:{{edanIds.queue}}?selector=" + edanIds_selector).routeId("simpleJMSSelectorTestRoute")
                        .log(LoggingLevel.INFO, "Message Selected:\n${body}")
                        .to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody(expectedBody);

        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test that the camel user name used for camel related processes does not get selected for processing
     * @throws Exception
     */
    @Test
    public void testJmsSelectorCamelUser() throws Exception {
        String expectedBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/camelUser-modifyDatastreamByValue.atom"));

        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(0);
        resultEndpoint.setAssertPeriod(1500);

        context.getRouteDefinition("EdanIdsStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("pid", TEST_PID);
        exchange.getIn().setBody(expectedBody, String.class);

        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test that non camel user names get selected for processing. i.e. the user is not related to camel processes
     * @throws Exception
     */
    @Test
    public void testJmsSelectorOtherUser() throws Exception {
        String expectedBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-modifyDatastreamByValue.atom"));

        context.getRouteDefinition("EdanIdsStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedBodiesReceived(expectedBody);
        resultEndpoint.expectedHeaderReceived("origin", "otherUser");
        resultEndpoint.expectedHeaderReceived("methodName", "modifyDatastreamByValue");
        resultEndpoint.expectedHeaderReceived("pid", TEST_PID);
        resultEndpoint.setAssertPeriod(1500);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("pid", TEST_PID);
        exchange.getIn().setBody(expectedBody, String.class);

        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test that only OBJ datastreams get selected for processing
     * @throws Exception
     */
    @Test
    public void testJmsSelectorNonOBJ() throws Exception {
        String expectedBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-non-OBJ.atom"));

        context.getRouteDefinition("EdanIdsStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(0);
        resultEndpoint.setAssertPeriod(1500);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("pid", TEST_PID);
        exchange.getIn().setBody(expectedBody, String.class);

        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test dsLabel contains Observations does not get selected for processing
     * @throws Exception
     */
    @Test
    public void testJmsSelectorObservations() throws Exception {
        String expectedBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-dsLabel-Observations.atom"));

        context.getRouteDefinition("EdanIdsStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(0);
        resultEndpoint.setAssertPeriod(1500);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("pid", TEST_PID);

        //test dsLabel containing Observations is not selected for processing
        exchange.getIn().setBody(expectedBody, String.class);
        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test only addDatastream, modifyDatastreamByValue, modifyDatastreamByReference, modifyObject, or ingest methodName header get selected for processing
     * @throws Exception
     */
    @Test
    public void testJmsSelectorMethodName() throws Exception {
        String expectedBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-modifyDatastreamByValue.atom"));

        context.getRouteDefinition("EdanIdsStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(5);
        resultEndpoint.setAssertPeriod(1500);


        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", TEST_PID);
        exchange.getIn().setBody(expectedBody, String.class);

        //Test non valid methodName header value
        exchange.getIn().setHeader("methodName", "testMethodName");
        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        //Test addDatastream methodName header value
        exchange.getIn().setHeader("methodName", "addDatastream");
        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        //Test modifyDatastreamByValue methodName header value
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        //Test modifyDatastreamByReference methodName header value
        exchange.getIn().setHeader("methodName", "modifyDatastreamByReference");
        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        //Test modifyObject methodName header value
        exchange.getIn().setHeader("methodName", "modifyObject");
        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        //Test ingest methodName header value
        exchange.getIn().setHeader("methodName", "ingest");
        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test only pids containing 'ct:' get selected for processing.
     * Sending everything that should be selected except for the pid.
     * The message should not be selected since the pid does not contain 'ct:'.
     * @throws Exception
     */
    @Test
    public void testJmsSelectorCtPidOnly() throws Exception {
        String expectedBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-modifyDatastreamByValue.atom"));

        context.getRouteDefinition("EdanIdsStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.setAssertPeriod(1500);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("pid",  "non-ct-namespace:1");
        exchange.getIn().setBody(expectedBody, String.class);

        //test with a non valid pid which should not be selected for processing
        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        //test with a valid pid which should be selected for processing
        exchange.getIn().setHeader("pid", getExtra().getProperty("si.ct.namespace") + ":1");
        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test messages selected for processing contain addEdanIds header and is equal to true
     * Sending everything that should be selected for fedora message processing and the addEdanIds header.
     * Having the addEdanIds header alone will start the camera trap message processing and not fedora message processing
     * @throws Exception
     */
    @Test
    public void testJmsSelectorAddEdanIdsHeader() throws Exception {
        String expectedBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-modifyDatastreamByValue.atom"));

        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived("addEdanIds", "true");
        resultEndpoint.expectedBodiesReceived(expectedBody);

        context.getRouteDefinition("EdanIdsStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("startProcessCtDeployment").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("pid", TEST_PID);
        exchange.getIn().setHeader("addEdanIds", "true");
        exchange.getIn().setHeader("ProjectId", "testProjectId");
        exchange.getIn().setHeader("SiteId", "testDeploymentId");
        exchange.getIn().setHeader("SitePID", "test:003");
        exchange.getIn().setHeader("PIDAggregation", "test:004,test:005,test:006,test:007,test:008,test:009,test:010,test:011,test:012");
        exchange.getIn().setHeader("ResearcherObservationPID", "test:010");
        exchange.getIn().setHeader("VolunteerObservationPID", "test:011");
        exchange.getIn().setHeader("ImageObservationPID", "test:012");
        exchange.getIn().setBody(expectedBody, String.class);

        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }
}
