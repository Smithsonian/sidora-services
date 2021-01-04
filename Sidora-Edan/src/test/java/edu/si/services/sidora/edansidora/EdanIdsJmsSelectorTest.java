/*
 * Copyright 2018-2019 Smithsonian Institution.
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

package edu.si.services.sidora.edansidora;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.velocity.VelocityConstants;
import org.apache.camel.impl.DefaultExchange;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.DateTool;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

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
    private static String JMS_FEDORA_TEST_QUEUE;
    private static String JMS_CT_INGEST_TEST_QUEUE;
    private static String TEST_PID;
    private static String CT_PID_NS;
    private static String SI_FEDORA_USER;
    @Override
    protected String getBlueprintDescriptor() {
        return "OSGI-INF/blueprint/edan-ids-sidora-route.xml";
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        JMS_FEDORA_TEST_QUEUE = getExtra().getProperty("edanIds.queue");
        JMS_CT_INGEST_TEST_QUEUE = getExtra().getProperty("edanIds.ct.queue");
        TEST_PID = getExtra().getProperty("si.ct.namespace") + ":test";
        CT_PID_NS = context.resolvePropertyPlaceholders("{{si.ct.namespace}}") + ":";
        SI_FEDORA_USER = context.resolvePropertyPlaceholders("{{si.fedora.user}}");
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
//        resultEndpoint.setAssertPeriod(1500);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("activemq:queue:testSelector?selector=" + edanIds_selector).routeId("simpleJMSSelectorTestRoute")
                        .log(LoggingLevel.INFO, "Message Selected:\n${body}")
                        .to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody(expectedBody);

        template.send("activemq:queue:testSelector", exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test that the camel user name used for camel related processes does not get selected for processing
     * @throws Exception
     */
    @Test
    public void testJmsSelectorCamelUser() throws Exception {
        //String expectedBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/camelUser-modifyDatastreamByValue.atom"));
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("origin", SI_FEDORA_USER);
        headers.put("methodName", "modifyDatastreamByValue");
        headers.put("testPID", CT_PID_NS+"1");
        headers.put("testDsLabel", "testDeploymentIds1i1");
        headers.put("testDsId", "OBJ");
        headers.put("testObjMimeType", "image/jpg");
        headers.put("testFedoraModel", "info:fedora/si:generalImageCModel");

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("date", new DateTool());
        velocityContext.put("headers", headers);

        headers.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext);

        String jmsMsg = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_atom.vsl", "test body", headers, String.class);
//        String dsXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_datastreams.vsl", "test body", headers, String.class);
//        String rels_extXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_RELS-EXT.vsl", "test body", headers, String.class);

        //log.debug("PID: {} | User: {} | Method: {} | Label: {}", pid, user, method, label);
        log.debug(jmsMsg);

        context.getRouteDefinition("EdanIdsStartProcessingFedoraMessage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
//                weaveById("processFedoraGetDatastreams").replace().setBody().simple(dsXML);
//                weaveById("processFedoraGetRELS-EXT").replace().setBody().simple(rels_extXML);
                weaveById("logFilteredMessage").after().to("mock:filter");
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        MockEndpoint mockresult = getMockEndpoint("mock:result");
        mockresult.expectedMessageCount(0);
        mockresult.expectedPropertyReceived(Exchange.FILTER_MATCHED, false);
//        mockresult.setAssertPeriod(1500);

        MockEndpoint mockFilter = getMockEndpoint("mock:filter");
        mockFilter.expectedMessageCount(1);
        mockFilter.expectedPropertyReceived(Exchange.FILTER_MATCHED, true);
//        mockFilter.setAssertPeriod(1500);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("pid", TEST_PID);
        exchange.getIn().setBody(jmsMsg, String.class);

        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test that non camel user names get selected for processing. i.e. the user is not related to camel processes
     * @throws Exception
     */
    @Test
    public void testJmsSelectorOtherUser() throws Exception {
        //String expectedBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-modifyDatastreamByValue.atom"));
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("origin", "otherUser");
        headers.put("methodName", "modifyDatastreamByValue");
        headers.put("testPID", CT_PID_NS+"1");
        headers.put("testDsLabel", "testDeploymentIds1i1");
        headers.put("testDsId", "OBJ");
        headers.put("testObjMimeType", "image/jpg");
        headers.put("testFedoraModel", "info:fedora/si:generalImageCModel");

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("date", new DateTool());
        velocityContext.put("headers", headers);

        headers.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext);

        String jmsMsg = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_atom.vsl", "test body", headers, String.class);
        String dsXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_datastreams.vsl", "test body", headers, String.class);
        String rels_extXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_RELS-EXT.vsl", "test body", headers, String.class);

        //log.debug("PID: {} | User: {} | Method: {} | Label: {}", pid, user, method, label);
        log.debug(jmsMsg);

        context.getRouteDefinition("EdanIdsStartProcessingFedoraMessage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraGetDatastreams").replace().setBody().simple(dsXML);
                weaveById("processFedoraGetRELS-EXT").replace().setBody().simple(rels_extXML);
                weaveById("logFilteredMessage").after().to("mock:filter");
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        //resultEndpoint.expectedBodiesReceived(jmsMsg);
        mockResult.expectedHeaderReceived("origin", "otherUser");
        mockResult.expectedHeaderReceived("methodName", "modifyDatastreamByValue");
        mockResult.expectedHeaderReceived("pid", CT_PID_NS+"1");
        mockResult.expectedPropertyReceived(Exchange.FILTER_MATCHED, false);
//        mockResult.setAssertPeriod(1500);

        MockEndpoint mockFilter = getMockEndpoint("mock:filter");
        mockFilter.expectedMessageCount(0);
        mockFilter.expectedPropertyReceived(Exchange.FILTER_MATCHED, true);
//        mockFilter.setAssertPeriod(1500);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("pid", CT_PID_NS+"1");
        exchange.getIn().setBody(jmsMsg, String.class);

        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test that only OBJ datastreams get selected for processing
     * @throws Exception
     */
    @Test
    public void testJmsSelectorNonOBJ() throws Exception {
        //String expectedBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-non-OBJ.atom"));
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("origin", "otherUser");
        headers.put("methodName", "modifyDatastreamByValue");
        headers.put("testPID", CT_PID_NS+"1");
        headers.put("testDsLabel", "testDeploymentIds1i1");
        headers.put("testDsId", "RELS-EXT");
        headers.put("testObjMimeType", "image/jpg");
        headers.put("testFedoraModel", "info:fedora/si:generalImageCModel");

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("date", new DateTool());
        velocityContext.put("headers", headers);

        headers.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext);

        String jmsMsg = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_atom.vsl", "test body", headers, String.class);
        String dsXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_datastreams.vsl", "test body", headers, String.class);
        String rels_extXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_RELS-EXT.vsl", "test body", headers, String.class);

        //log.debug("PID: {} | User: {} | Method: {} | Label: {}", pid, user, method, label);
        log.debug(jmsMsg);

        context.getRouteDefinition("EdanIdsStartProcessingFedoraMessage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraGetDatastreams").replace().setBody().simple(dsXML);
                weaveById("processFedoraGetRELS-EXT").replace().setBody().simple(rels_extXML);
                weaveById("logFilteredMessage").after().to("mock:filter");
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(0);
        resultEndpoint.expectedPropertyReceived(Exchange.FILTER_MATCHED, false);
//        resultEndpoint.setAssertPeriod(1500);

        MockEndpoint mockFilter = getMockEndpoint("mock:filter");
        mockFilter.expectedMessageCount(1);
        mockFilter.expectedPropertyReceived(Exchange.FILTER_MATCHED, true);
//        mockFilter.setAssertPeriod(1500);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("pid", TEST_PID);
        exchange.getIn().setBody(jmsMsg, String.class);

        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test dsLabel contains Observations does not get selected for processing
     * @throws Exception
     */
    @Test
    public void testJmsSelectorObservations() throws Exception {
        //String expectedBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-dsLabel-Observations.atom"));
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("origin", "otherUser");
        headers.put("methodName", "modifyDatastreamByValue");
        headers.put("testPID", CT_PID_NS+"1");
        headers.put("testDsLabel", "Researcher Observations");
        headers.put("testDsId", "OBJ");
        headers.put("testObjMimeType", "image/jpg");
        headers.put("testFedoraModel", "info:fedora/si:generalImageCModel");

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("date", new DateTool());
        velocityContext.put("headers", headers);

        headers.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext);

        String jmsMsg = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_atom.vsl", "test body", headers, String.class);
        String dsXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_datastreams.vsl", "test body", headers, String.class);
        String rels_extXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_RELS-EXT.vsl", "test body", headers, String.class);

        //log.debug("PID: {} | User: {} | Method: {} | Label: {}", pid, user, method, label);
        log.debug(jmsMsg);

        context.getRouteDefinition("EdanIdsStartProcessingFedoraMessage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraGetDatastreams").replace().setBody().simple(dsXML);
                weaveById("processFedoraGetRELS-EXT").replace().setBody().simple(rels_extXML);
                weaveById("logFilteredMessage").after().to("mock:filter");
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(0);
        mockResult.expectedPropertyReceived(Exchange.FILTER_MATCHED, false);
//        mockResult.setAssertPeriod(1500);

        MockEndpoint mockFilter = getMockEndpoint("mock:filter");
        mockFilter.expectedMessageCount(1);
        mockFilter.expectedPropertyReceived(Exchange.FILTER_MATCHED, true);
//        mockFilter.setAssertPeriod(1500);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("pid", TEST_PID);

        //test dsLabel containing Observations is not selected for processing
        exchange.getIn().setBody(jmsMsg, String.class);
        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test only addDatastream, modifyDatastreamByValue, modifyDatastreamByReference, modifyObject, or ingest methodName header get selected for processing
     * @throws Exception
     */
    @Test
    public void testJmsSelectorMethodName() throws Exception {
        //String expectedBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-modifyDatastreamByValue.atom"));
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("origin", "otherUser");
        headers.put("methodName", "testMethodName");
        headers.put("testPID", CT_PID_NS+"1");
        headers.put("testDsLabel", "testDeploymentIds1i1");
        headers.put("testDsId", "OBJ");
        headers.put("testObjMimeType", "image/jpg");
        headers.put("testFedoraModel", "info:fedora/si:generalImageCModel");

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("date", new DateTool());
        velocityContext.put("headers", headers);

        headers.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext);

        String jmsMsg = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_atom.vsl", "test body", headers, String.class);
        String dsXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_datastreams.vsl", "test body", headers, String.class);
        String rels_extXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_RELS-EXT.vsl", "test body", headers, String.class);

        //log.debug("PID: {} | User: {} | Method: {} | Label: {}", pid, user, method, label);
        log.debug(jmsMsg);

        HashMap<String, Object> headers2 = new HashMap<>();
        headers2.put("origin", "otherUser");
        headers2.put("methodName", "modifyDatastreamByValue");
        headers2.put("testPID", CT_PID_NS + "1");
        headers2.put("testDsLabel", "testDeploymentIds1i1");
        headers2.put("testDsId", "OBJ");
        headers2.put("testObjMimeType", "image/jpg");
        headers2.put("testFedoraModel", "info:fedora/si:generalImageCModel");

        VelocityContext velocityContext2 = new VelocityContext();
        velocityContext2.put("date", new DateTool());
        velocityContext2.put("headers", headers2);

        headers2.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext2);

        String jmsMsg2 = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_atom.vsl", "test body", headers2, String.class);
        String dsXML2 = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_datastreams.vsl", "test body", headers2, String.class);
        String rels_extXML2 = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_RELS-EXT.vsl", "test body", headers2, String.class);

        //log.debug("PID: {} | User: {} | Method: {} | Label: {}", pid, user, method, label);
        log.debug(jmsMsg2);

        context.getRouteDefinition("EdanIdsStartProcessingFedoraMessage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraGetDatastreams").replace()
                        .choice()
                        .when().simple("${header.test} == 1")
                        .setBody().simple(dsXML)
                        .endChoice()
                        .otherwise()
                        .setBody().simple(dsXML2)
                        .endChoice()
                        .end();
                weaveById("processFedoraGetRELS-EXT").replace()
                        .choice()
                        .when().simple("${header.test} == 1")
                        .setBody().simple(rels_extXML)
                        .endChoice()
                        .otherwise()
                        .setBody().simple(rels_extXML2)
                        .endChoice()
                        .end();
                weaveById("logFilteredMessage").after().to("mock:filter");
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(6);
        mockResult.expectedHeaderValuesReceivedInAnyOrder("test", 2,3,4,5,6,7);
        mockResult.expectedHeaderValuesReceivedInAnyOrder("methodName", "addDatastream","modifyDatastreamByValue","modifyDatastreamByReference","modifyObject","ingest","purgeDatastream");
        mockResult.expectedPropertyReceived(Exchange.FILTER_MATCHED, false);
//        mockResult.setAssertPeriod(1500);

        MockEndpoint mockFilter = getMockEndpoint("mock:filter");
        mockFilter.expectedMessageCount(1);
        mockFilter.expectedHeaderReceived("test", 1);
        mockFilter.expectedHeaderReceived("methodName", "testMethodName");
        mockFilter.expectedPropertyReceived(Exchange.FILTER_MATCHED, true);
//        mockFilter.setAssertPeriod(1500);


        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", CT_PID_NS+"1");
        exchange.getIn().setBody(jmsMsg, String.class);

        //Test non valid methodName header value
        exchange.getIn().setHeader("methodName", "testMethodName");
        exchange.getIn().setHeader("test", 1);
        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        //Test addDatastream methodName header value
        exchange.getIn().setHeader("methodName", "addDatastream");
        exchange.getIn().setHeader("test", 2);
        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        //Test modifyDatastreamByValue methodName header value
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("test", 3);
        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        //Test modifyDatastreamByReference methodName header value
        exchange.getIn().setHeader("methodName", "modifyDatastreamByReference");
        exchange.getIn().setHeader("test", 4);
        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        //Test modifyObject methodName header value
        exchange.getIn().setHeader("methodName", "modifyObject");
        exchange.getIn().setHeader("test", 5);
        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        //Test ingest methodName header value
        exchange.getIn().setHeader("methodName", "ingest");
        exchange.getIn().setHeader("test", 6);
        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        //Test purgeDatastream methodName header value
        exchange.getIn().setHeader("methodName", "purgeDatastream");
        exchange.getIn().setHeader("test", 7);
        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

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
        //String expectedBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-modifyDatastreamByValue.atom"));
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("origin", "otherUser");
        headers.put("methodName", "modifyDatastreamByValue");
        headers.put("testPID", "non-ct-namespace:1");
        headers.put("testDsLabel", "testDeploymentIds1i1");
        headers.put("testDsId", "OBJ");
        headers.put("testObjMimeType", "image/jpg");
        headers.put("testFedoraModel", "info:fedora/si:generalImageCModel");

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("date", new DateTool());
        velocityContext.put("headers", headers);

        headers.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext);

        String jmsMsg = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_atom.vsl", "test body", headers, String.class);
        String dsXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_datastreams.vsl", "test body", headers, String.class);
        String rels_extXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_RELS-EXT.vsl", "test body", headers, String.class);

        //log.debug("PID: {} | User: {} | Method: {} | Label: {}", pid, user, method, label);
        log.debug(jmsMsg);

        HashMap<String, Object> headers2 = new HashMap<>();
        headers2.put("origin", "otherUser");
        headers2.put("methodName", "modifyDatastreamByValue");
        headers2.put("testPID", CT_PID_NS + "1");
        headers2.put("testDsLabel", "testDeploymentIds1i1");
        headers2.put("testDsId", "OBJ");
        headers2.put("testObjMimeType", "image/jpg");
        headers2.put("testFedoraModel", "info:fedora/si:generalImageCModel");

        VelocityContext velocityContext2 = new VelocityContext();
        velocityContext2.put("date", new DateTool());
        velocityContext2.put("headers", headers2);

        headers2.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext2);

        String jmsMsg2 = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_atom.vsl", "test body", headers2, String.class);
        String dsXML2 = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_datastreams.vsl", "test body", headers2, String.class);
        String rels_extXML2 = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_RELS-EXT.vsl", "test body", headers2, String.class);

        //log.debug("PID: {} | User: {} | Met//log.debug("PID: {} | User: {} | Method: {} | Label: {}", pid, user, method, label);hod: {} | Label: {}", pid, user, method, label);
        log.debug(jmsMsg2);

        context.getRouteDefinition("EdanIdsStartProcessingFedoraMessage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraGetDatastreams").replace()
                        .choice()
                            .when().simple("${header.test} == 1")
                                .setBody().simple(dsXML)
                            .endChoice()
                            .otherwise()
                                .setBody().simple(dsXML2)
                            .endChoice()
                        .end();
                weaveById("processFedoraGetRELS-EXT").replace()
                        .choice()
                            .when().simple("${header.test} == 1")
                                .setBody().simple(rels_extXML)
                            .endChoice()
                            .otherwise()
                                .setBody().simple(rels_extXML2)
                            .endChoice()
                        .end();
                weaveById("logFilteredMessage").after().to("mock:filter");
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("test", 2);
        mockResult.expectedPropertyReceived(Exchange.FILTER_MATCHED, false);
//        mockResult.setAssertPeriod(1500);

        MockEndpoint mockFilter = getMockEndpoint("mock:filter");
        mockFilter.expectedMessageCount(1);
        mockFilter.expectedHeaderReceived("test", 1);
        mockFilter.expectedPropertyReceived(Exchange.FILTER_MATCHED, true);
//        mockFilter.setAssertPeriod(1500);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("pid",  "non-ct-namespace:1");
        exchange.getIn().setHeader("test", 1);
        exchange.getIn().setBody(jmsMsg, String.class);

        //test with a non valid pid which should not be selected for processing
        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        //test with a valid pid which should be selected for processing
        exchange.getIn().setHeader("pid", CT_PID_NS + "1");
        exchange.getIn().setHeader("test", 2);
        exchange.getIn().setBody(jmsMsg, String.class);

        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test that non camel user names get selected for processing. i.e. the user is not related to camel processes
     * @throws Exception
     */
    @Test
    public void testDeleteJmsSelectorOtherUser() throws Exception {
        //String expectedBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-purgeDatastream.atom"));
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("origin", "otherUser");
        headers.put("methodName", "purgeDatastream");
        headers.put("testPID", CT_PID_NS +"test");
        headers.put("testDsLabel", "test_label");
        headers.put("testDsId", "OBJ");
        headers.put("testObjMimeType", "image/jpg");
        headers.put("testFedoraModel", "info:fedora/si:generalImageCModel");

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("date", new DateTool());
        velocityContext.put("headers", headers);

        headers.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext);

        String jmsMsg = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_atom.vsl", "test body", headers, String.class);
        String dsXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_datastreams.vsl", "test body", headers, String.class);
        String rels_extXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_RELS-EXT.vsl", "test body", headers, String.class);

        //log.debug("PID: {} | User: {} | Method: {} | Label: {}", pid, user, method, label);
        log.debug(jmsMsg);

        context.getRouteDefinition("EdanIdsStartProcessingFedoraMessage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraGetDatastreams").replace().setBody().simple(dsXML);
                weaveById("processFedoraGetRELS-EXT").replace().setBody().simple(rels_extXML);
                weaveById("logFilteredMessage").after().to("mock:filter");
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(1);
        //resultEndpoint.expectedBodiesReceived(jmsMsg);
        resultEndpoint.expectedHeaderReceived("origin", "otherUser");
        resultEndpoint.expectedHeaderReceived("methodName", "purgeDatastream");
        resultEndpoint.expectedHeaderReceived("pid", CT_PID_NS +"test");
        resultEndpoint.expectedPropertyReceived(Exchange.FILTER_MATCHED, false);
//        resultEndpoint.setAssertPeriod(1500);

        MockEndpoint mockFilter = getMockEndpoint("mock:filter");
        mockFilter.expectedMessageCount(0);
        mockFilter.expectedPropertyReceived(Exchange.FILTER_MATCHED, true);
//        mockFilter.setAssertPeriod(1500);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "purgeDatastream");
        exchange.getIn().setHeader("pid", CT_PID_NS +"test");
        exchange.getIn().setBody(jmsMsg, String.class);

        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test that non camel user names, only OBJ, not Observations, pid not ct pid get selected for processing. i.e. the user is not related to camel processes
     * @throws Exception
     */
    @Test
    public void testFilterFedoraMessages() throws Exception {
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("origin", "ForresterT");
        headers.put("methodName", "modifyDatastreamByReference");
        headers.put("testPID", "ct:2625763");
        //headers.put("testDsLabel", "Researcher Observations (35).csv");
        headers.put("testDsLabel", "null");
        headers.put("testDsId", "OBJ");

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("date", new DateTool());
        velocityContext.put("headers", headers);

        headers.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext);

        String jmsMsg = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_atom.vsl", "test body", headers, String.class);

        //log.debug("PID: {} | User: {} | Method: {} | Label: {}", pid, user, method, label);
        log.debug(jmsMsg);

        context.getRouteDefinition("EdanIdsStartProcessingFedoraMessage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
                weaveById("logFilteredMessage").after().to("mock:filter");

                weaveById("processFedoraGetDatastreams").replace().setBody().simple("\n" +
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><objectDatastreams  xmlns=\"http://www.fedora.info/definitions/1/0/access/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/access/ http://www.fedora-commons.org/definitions/1/0/listDatastreams.xsd\" pid=\"ct:2625763\" baseURL=\"http://localhost:8080/fedora/\" >    <datastream dsid=\"DC\" label=\"Dublin Core Record for this object\" mimeType=\"text/xml\" />    <datastream dsid=\"FGDC\" label=\"FGDC Record\" mimeType=\"text/xml\" />    <datastream dsid=\"OBJ\" label=\"Researcher Observations (35).csv\" mimeType=\"text/csv\" />    <datastream dsid=\"RELS-EXT\" label=\"RDF Statements about this object\" mimeType=\"application/rdf+xml\" />    <datastream dsid=\"CSV\" label=\"CSV\" mimeType=\"text/csv\" /></objectDatastreams>");
                weaveById("processFedoraGetRELS-EXT").replace().setBody().simple("<rdf:RDF xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:default=\"http://islandora.org/ontologies/metadata#\" xmlns:fedora=\"info:fedora/fedora-system:def/relations-external#\" xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\" xmlns:islandora=\"http://islandora.ca/ontology/relsext#\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\n" +
                        "    <rdf:Description rdf:about=\"info:fedora/ct:2625760\">\n" +
                        //"        <fedora-model:hasModel rdf:resource=\"info:fedora/si:generalImageCModel\"></fedora-model:hasModel>\n" +
                        //"        <fedora-model:hasModel rdf:resource=\"info:fedora/si:imageCModel\"></fedora-model:hasModel>\n" +
                        "        <fedora-model:hasModel rdf:resource=\"info:fedora/si:resourceCModel\"></fedora-model:hasModel>\n" +
                        "        <default:orginal_metadata xmlns=\"http://islandora.org/ontologies/metadata#\">TRUE</default:orginal_metadata>\n" +
                        "    </rdf:Description>\n" +
                        "</rdf:RDF>");
            }
        });

        MockEndpoint resultEndpoint = getMockEndpoint("mock:filter");
        resultEndpoint.expectedMessageCount(1);
        //resultEndpoint.expectedBodiesReceived(jmsMsg);
        resultEndpoint.expectedHeaderReceived("origin", "ForresterT");
        resultEndpoint.expectedHeaderReceived("methodName", "modifyDatastreamByReference");
        resultEndpoint.expectedHeaderReceived("pid", "ct:2625763");
        resultEndpoint.expectedPropertyReceived(Exchange.FILTER_MATCHED, true);
//        resultEndpoint.setAssertPeriod(1500);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByReference");
        exchange.getIn().setHeader("pid", "ct:2625763");
        exchange.getIn().setBody(jmsMsg, String.class);

        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFilter() throws Exception {
        MockEndpoint mockfilter = getMockEndpoint("mock:filter");
        mockfilter.expectedPropertyReceived(Exchange.FILTER_MATCHED, true);
        mockfilter.expectedMessageCount(1);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedPropertyReceived(Exchange.FILTER_MATCHED, false);
        mockResult.expectedMessageCount(0);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:testFilter")
                        .setHeader("origin_b").simple("${header.origin} == '{{si.fedora.user}}'", boolean.class)
                        .setHeader("dsID_b").simple("${header.dsID} != 'OBJ'", boolean.class)
                        .setHeader("dsLabel_b").simple("${header.dsLabel} contains 'Observations'", boolean.class)
                        .setHeader("objLabel_b").simple("${header.objLabel} contains 'Observations'", boolean.class)
                        .setHeader("dsLabel_b2").simple("${header.dsLabel} == null", boolean.class)
                        .setHeader("objLabel_b2").simple("${header.objLabel} == null", boolean.class)
                        .setHeader("dsLabel_b3").simple("${header.dsLabel} in ',null'", boolean.class)
                        .setHeader("objLabel_b3").simple("${header.objLabel}  in ',null'", boolean.class)
                        .setHeader("methodName_b").simple("${header.methodName} not in 'addDatastream,modifyDatastreamByValue,modifyDatastreamByReference,modifyObject,ingest,purgeDatastream'", boolean.class)
                        .setHeader("pid_b").simple("${header.pid} not contains '{{si.ct.namespace}}:'", boolean.class)
                        .setHeader("mimeType_b").simple("${header.mimeType} not contains 'image'", boolean.class)
                        .setHeader("hasModel_b").simple("${header.hasModel?.toLowerCase()} not contains 'image'", boolean.class)

                        .setHeader("filterValue").simple("${header.origin} == '{{si.fedora.user}}' || " +
                                    " ${header.dsID} != 'OBJ' ${header.origin_b} || " +
                                    " ${header.dsLabel} contains 'Observations' || " +
                                    " ${header.objLabel} contains 'Observations' || " +
                                    " ${header.dsLabel} == null || " +
                                    " ${header.objLabel} == null || " +
                                    " ${header.dsLabel} in ',null' || " +
                                    " ${header.objLabel} in ',null' || " +
                                    " ${header.methodName} not in 'addDatastream,modifyDatastreamByValue,modifyDatastreamByReference,modifyObject,ingest,purgeDatastream' || " +
                                    " ${header.pid} not contains '{{si.ct.namespace}}:' || " +
                                    " ${header.mimeType} not contains 'image' || " +
                                    " ${header.hasModel?.toLowerCase()} not contains 'image'", String.class)

                        .setHeader("filterValue").simple("${header.filterValue}", boolean.class)

                        .setHeader("filter").simple("${header.origin} == '{{si.fedora.user}}' || -> [${header.origin_b}]\n" +
                        "${header.dsID} != 'OBJ' ${header.origin_b} || -> [${header.dsID_b}]\n" +
                        "${header.dsLabel} contains 'Observations' || -> [${header.dsLabel_b}]\n" +
                        "${header.objLabel} contains 'Observations' || -> [${header.objLabel_b}]\n" +
                        "${header.dsLabel} == null || -> [${header.dsLabel_b2}]\n" +
                        "${header.objLabel} == null || -> [${header.objLabel_b2}]\n" +
                        "${header.dsLabel} in ',null' || -> [${header.dsLabel_b3}]\n" +
                        "${header.objLabel} in ',null' || -> [${header.objLabel_b3}]\n" +
                        "${header.methodName} not in 'addDatastream,modifyDatastreamByValue,modifyDatastreamByReference,modifyObject,ingest,purgeDatastream' || -> [${header.methodName_b}]\n" +
                        "${header.pid} not contains '{{si.ct.namespace}}:' || -> [${header.pid_b}]\n" +
                        "${header.mimeType} not contains 'image' || -> [${header.mimeType_b}]\n" +
                        "${header.hasModel?.toLowerCase()} not contains 'image' -> [${header.hasModel_b}]", String.class)

                        .setHeader("logMsg").simple("${header.filter}\n=============\n${header.origin_b} || ${header.dsID_b} || ${header.dsLabel_b} || ${header.objLabel_b} || ${header.dsLabel_b2} || ${header.objLabel_b2} || ${header.dsLabel_b3} || ${header.objLabel_b3} ||${header.methodName_b} || ${header.pid_b} || ${header.mimeType_b} || ${header.hasModel_b} --> ${header.filterValue}\n=============")
                        .log(LoggingLevel.INFO, "\n${header.logMsg}")

                        .filter()
                            .simple("${header.origin} == '{{si.fedora.user}}' || " +
                                    " ${header.dsID} != 'OBJ' || " +
                                    " ${header.dsLabel} contains 'Observations' || " +
                                    " ${header.objLabel} contains 'Observations' || " +
                                    " ${header.dsLabel} == null || " +
                                    " ${header.objLabel} == null || " +
                                    " ${header.dsLabel} in ',null' || " +
                                    " ${header.objLabel} in ',null' || " +
                                    " ${header.methodName} not in 'addDatastream,modifyDatastreamByValue,modifyDatastreamByReference,modifyObject,ingest,purgeDatastream' || " +
                                    " ${header.pid} not contains '{{si.ct.namespace}}:' || " +
                                    " ${header.mimeType} not contains 'image' || " +
                                    " ${header.hasModel?.toLowerCase()} not contains 'image'")

                            .log(LoggingLevel.INFO, "<<<<<<<<<<[ ( filter = ${header.filterValue} ) No message processing required. ]>>>>>>>>>>")
                            .to("mock:filter")
                            .stop()
                        .end()

                        .log(LoggingLevel.INFO, "************* NOT FILTERED **************")
                        .log(LoggingLevel.INFO, "\n${header.logMsg}")
                .to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("origin", "ForresterT");
        //exchange.getIn().setHeader("origin", context.resolvePropertyPlaceholders("{{si.fedora.user}}"));
        exchange.getIn().setHeader("dsID", "OBJ");
        exchange.getIn().setHeader("dsLabel", "null");
        //exchange.getIn().setHeader("dsLabel", "Researcher Observations (35).csv");
        exchange.getIn().setHeader("objLabel", "null");
        //exchange.getIn().setHeader("objLabel", "Researcher Observations (35).csv");
        exchange.getIn().setHeader("methodName", "modifyDatastreamByReference");
        exchange.getIn().setHeader("pid", "ct:123456");
        exchange.getIn().setHeader("mimeType", "image/csv");
        //exchange.getIn().setHeader("hasModel", "info:fedora/si:resourceCModel");
        exchange.getIn().setHeader("hasModel", "info:fedora/si:generalImageCModel,info:fedora/si:imageCModel,info:fedora/si:resourceCModel");

        template.send("direct:testFilter", exchange);

        assertMockEndpointsSatisfied();
    }
}
