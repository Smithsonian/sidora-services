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

package edu.si.services.solr;

import edu.si.services.solr.aggregationStrategy.MySolrUpdateStrategy;
import net.sf.saxon.functions.False;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.solr.SolrConstants;
import org.apache.camel.component.velocity.VelocityConstants;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.DateTool;
import org.junit.Ignore;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * @author jbirkhimer
 * @author jbirkhimer
 */
public class CamelSolrTest extends Solr_CT_BlueprintTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(CamelSolrTest.class);

    private static final String KARAF_HOME = System.getProperty("karaf.home");

    private static final Integer MAX_DOC = 10;
    private static final Integer COMPLETION_SIZE = 3;
    private static Integer TOTAL_BATCH_COUNT;

    private static final String FEDORA_NAMESPACE = "namespaceTest";
    private static String CT_NAMESPACE = "si.ct.namespace";

    private static String CT_OWNER = "testCtUser";

    private static boolean USE_CT_USER = false;
    private static boolean USE_CT_OBSERVATION = false;

    @Override
    protected String getBlueprintDescriptor() {
        return "OSGI-INF/blueprint/sidora-solr-route.xml";
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        CT_NAMESPACE = getExtra().getProperty("si.ct.namespace");
    }

    private HashMap<String, List<String>> buildJMSBatch() {

        TOTAL_BATCH_COUNT = 0;

        String[] methodList = {"addDatastream", "modifyDatastreamByValue", "modifyDatastreamByReference", "modifyObject", "ingest", "purgeDatastream"};

        String[] labelList = {"testLabel", "Observations"};
        String[] userList = {"testUser", CT_OWNER};

        HashMap<String, List<String>> answer = new HashMap<>();
        for (int i = 1; i <= MAX_DOC;) {
            List<String> methodAndBody = new ArrayList<>();

            Random rand = new Random();
            int ranUser = rand.nextInt(userList.length);
            int ranMethod = rand.nextInt(methodList.length);
            int ranCtMethod = rand.nextInt(methodList.length - 1); //ctUser no purge
            int ranLabel = rand.nextInt(labelList.length);

            String user = userList[ranUser];

            String pid, method, label;
            //if ctIngest
            if (user.equals(userList[1])) {
                label = labelList[ranLabel];
                method = methodList[ranCtMethod];
                pid = CT_NAMESPACE + ":" + i;
                //if lablel is not observation make unique label
                label = (label.equals(labelList[1])) ? label : label + rand.nextInt(10); //make unique label
            } else {
                user = user + rand.nextInt(10);  //make unique user
                label = labelList[ranLabel];
                method = methodList[ranMethod];
                pid = ((label.equals(labelList[1])) ? CT_NAMESPACE : FEDORA_NAMESPACE) + ":" + i;
                label = (label.equals(labelList[1])) ? label : label + rand.nextInt(10); //make unique label
            }

            //add extra because we have both index for
            TOTAL_BATCH_COUNT = ((label.equals(labelList[1])) ? TOTAL_BATCH_COUNT + 2 : TOTAL_BATCH_COUNT++);

            HashMap<String, Object> headers = new HashMap<>();
            headers.put("origin", user);
            headers.put("methodName", method);
            headers.put("testPID", pid);
            headers.put("testDsLabel", label);
            headers.put("testDsId", "TEST");

            VelocityContext velocityContext = new VelocityContext();
            velocityContext.put("date", new DateTool());
            velocityContext.put("headers", headers);

            headers.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext);

            String jmsMsg = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/fedora/atom.vsl", "test body", headers, String.class);

            LOG.debug("PID: {} | User: {} | Method: {} | Label: {}", pid, user, method, label);
            LOG.debug(jmsMsg);

            methodAndBody.add(user);
            methodAndBody.add(method);
            methodAndBody.add(jmsMsg);
            answer.put(pid, methodAndBody);

            i++;
        }

        for (Map.Entry e : answer.entrySet()) {
            LOG.debug("JMSBatch created :\n{}", e.getValue());
        }

        return answer;
    }

    @Test
    public void testBuildJMSBatch() {
        HashMap<String, List<String>> batchJMS = buildJMSBatch();
        assertTrue(batchJMS.size() == MAX_DOC);
    }

    @Test
    public void testFedoraMessages() throws Exception {

        USE_CT_USER = true;
        USE_CT_OBSERVATION = true;

        HashMap<String, List<String>> batchJMS = buildJMSBatch();

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(TOTAL_BATCH_COUNT/COMPLETION_SIZE+1);

        MockEndpoint mockCreateDocResult = getMockEndpoint("mock:createDocResult");
        mockCreateDocResult.expectedMessageCount(TOTAL_BATCH_COUNT);

        MockEndpoint mockEnd = getMockEndpoint("mock:end");
        mockEnd.expectedMessageCount(1);

        context.getRouteDefinition("createDoc").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(ChoiceDefinition.class).replace().to("mock:createDocResult");
            }
        });

        context.getRouteDefinition("solr").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("sendToSolr").replace().to("mock:result");
            }
        });

        context.getRouteDefinition("storeReceivedJobs").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("printStoreReceivedJobs").replace().to("mock:end");
            }
        });

        for (Map.Entry<String, List<String>> msg : batchJMS.entrySet()) {

            LOG.debug("Sending pid: {} | methodName: {} | Body: {}", msg.getKey(), msg.getValue().get(0), msg.getValue().get(1));
            Exchange exchange = new DefaultExchange(context);
            exchange.getIn().setHeader("pid", msg.getKey());
            exchange.getIn().setHeader("methodName", msg.getValue().get(1));
            exchange.getIn().setBody(msg.getValue().get(2));
            exchange.getIn().setHeader("TOTAL_BATCH_COUNT", TOTAL_BATCH_COUNT);

            template.send("activemq:queue:{{solr.apim.update.queue}}", exchange);
        }

        assertMockEndpointsSatisfied();
    }

    /**
     * Test to simulate messages from the CI Ingest route for indexing
     * @throws Exception
     */
    @Test
    public void ctIngestMessageTest() throws Exception {

        //Flag for if we want to test against real fedora/fuseki/solr
        boolean LIVE_TEST = false;
        final boolean I_KNOW_WHAT_I_AM_DOING = false;

        CT_NAMESPACE = getExtra().getProperty("si.ct.namespace");

        int startPid = 12;
        int endPid = 23; //subproject, plot, site, images, observations

        List<String> pidAgg = new ArrayList<>();
        if (!LIVE_TEST || I_KNOW_WHAT_I_AM_DOING) {
            for (int i = startPid; i <= endPid; i++) {
                pidAgg.add(CT_NAMESPACE + ":" + i);
            }
        } else {
            pidAgg.addAll(Arrays.asList(new String[]{}));
            if(pidAgg.isEmpty()) {
                throw new SidoraSolrException("For Live Test You must provide the pids!!!");
            }
        }

        LOG.info("PIDAggregation list = {}", pidAgg);

        List<String> projectStructure = pidAgg.subList(pidAgg.size()-4, pidAgg.size());
        List<String> imageResources = pidAgg.subList(pidAgg.size()-6, pidAgg.size());
        List<String> observationResources = pidAgg.subList(pidAgg.size()-2, pidAgg.size());

        //2 indexes for observations (3 if we include image observation)
        TOTAL_BATCH_COUNT = pidAgg.size() + 2;

        LOG.debug("Total batch count = {}", TOTAL_BATCH_COUNT);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(TOTAL_BATCH_COUNT/COMPLETION_SIZE+1);

        MockEndpoint mockCreateDocResult = getMockEndpoint("mock:createDocResult");
        mockCreateDocResult.expectedMessageCount(TOTAL_BATCH_COUNT);

        MockEndpoint mockEnd = getMockEndpoint("mock:end");
        mockEnd.expectedMessageCount(1);

        context.getRouteDefinition("cameraTrapJob").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                if (!LIVE_TEST) {
                    weaveById("cameraTrapJobFedoraCall").replace().to("velocity:file:{{karaf.home}}/fedora/fedora-search-response.vsl");
                }
            }
        });

        context.getRouteDefinition("createDoc").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                if (!LIVE_TEST) {
                    weaveByType(ChoiceDefinition.class).replace().to("mock:createDocResult");
                } else {
                    weaveByType(ChoiceDefinition.class).after().to("mock:createDocResult");
                }
            }
        });

        context.getRouteDefinition("solr").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                if (!LIVE_TEST) {
                    weaveById("sendToSolr").replace().to("mock:result");
                } else {
                    weaveAddLast().to("mock:result");
                }
            }
        });

        context.getRouteDefinition("storeReceivedJobs").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                if (!LIVE_TEST) {
                    weaveById("printStoreReceivedJobs").replace().to("mock:end");
                } else {
                    weaveById("printStoreReceivedJobs").after().to("mock:end");
                }
            }
        });

        Exchange exchange = new DefaultExchange(context);

        //Project (projectPID)
        exchange.getIn().setHeader("ProjectId", "testDeploymentId");
        exchange.getIn().setHeader("ProjectName", "testDeploymentId");
        exchange.getIn().setHeader("ProjectPID", projectStructure.get(0));

        //SubProject (parkPID)
        exchange.getIn().setHeader("SubProjectId", "testDeploymentId");
        exchange.getIn().setHeader("SubProjectName", "testDeploymentId");
        exchange.getIn().setHeader("SubProjectPID", projectStructure.get(1));

        //Plot (sitePID)
        exchange.getIn().setHeader("PlotId", "testDeploymentId");
        exchange.getIn().setHeader("PlotName", "testDeploymentId");
        exchange.getIn().setHeader("PlotPID", projectStructure.get(2));

        //Site (ctPID)
        exchange.getIn().setHeader("SiteId", "testDeploymentId");
        exchange.getIn().setHeader("SiteName", "testDeploymentId");
        exchange.getIn().setHeader("SitePID", projectStructure.get(3));


        //Observations
        exchange.getIn().setHeader("ResearcherObservationPID", observationResources.get(0));
        exchange.getIn().setHeader("VolunteerObservationPID", observationResources.get(1));
        //exchange.getIn().setHeader("ImageObservationPID", observations.get(2));

        //just for fun mix things up
        Collections.shuffle(pidAgg);
        exchange.getIn().setHeader("PIDAggregation", Arrays.toString(pidAgg.toArray()).replaceAll("[\\[\\ \\]]", ""));

        exchange.getIn().setHeader("TOTAL_BATCH_COUNT", TOTAL_BATCH_COUNT);

        template.send("activemq:queue:{{sidoraCTSolr.queue}}", exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Testing split with filter, should continue routing body after filter similar to wiretap
     * @throws Exception
     */
    @Test
    public void splitAndFilterObservationsTest() throws Exception {

        CT_NAMESPACE = getExtra().getProperty("si.ct.namespace");

        int startPid = 12;
        int endPid = 23; //subproject, plot, site, images, observations

        List<String> pidAgg = new ArrayList<>();
        for (int i = startPid; i <= endPid; i++) {
            pidAgg.add(CT_NAMESPACE + ":" + i);
        }
        LOG.info("PIDAggregation list = {}", pidAgg);

        List<String> projectStructure = pidAgg.subList(pidAgg.size()-4, pidAgg.size());
        List<String> imageResources = pidAgg.subList(pidAgg.size()-6, pidAgg.size());
        List<String> observationResources = pidAgg.subList(pidAgg.size()-2, pidAgg.size());

        MockEndpoint filtered = getMockEndpoint("mock:observations");
        filtered.expectedMessageCount(observationResources.size());
        MockEndpoint mockAll = getMockEndpoint("mock:all");
        mockAll.expectedMessageCount(pidAgg.size());

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:filterTest")
                        .log(LoggingLevel.INFO, "PIDAggregation: ${header.PIDAggregation}")
                        .split().tokenize(",", "PIDAggregation")
                            .filter().simple("${body} in '${header.ResearcherObservationPID},${header.VolunteerObservationPID},${header.ImageObservationPID}'")
                                .log(LoggingLevel.INFO, "FOUND OBSERVATION pid = ${body}")
                                .to("seda:sedaFiltered")
                            .end()
                        .to("seda:sedaAll");

                from("seda:sedaFiltered")
                        .to("mock:observations");
                from("seda:sedaAll")
                        .to("mock:all");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        //Observations
        exchange.getIn().setHeader("ResearcherObservationPID", observationResources.get(0));
        exchange.getIn().setHeader("VolunteerObservationPID", observationResources.get(1));
        //just for fun mix things up
        Collections.shuffle(pidAgg);
        exchange.getIn().setHeader("PIDAggregation", Arrays.toString(pidAgg.toArray()).replaceAll("[\\[\\ \\]]", ""));

        template.send("direct:filterTest", exchange);

        assertMockEndpointsSatisfied();
    }


    /**
     * TODO: finish test
     * @throws Exception
     */
    @Ignore
    @Test
    public void testAggregateAndOnException() throws Exception {
        context.addRoutes(new RouteBuilder() {
            int count = 0;
            @Override
            public void configure() throws Exception {
                errorHandler(deadLetterChannel("mock:error")
                                //.useOriginalMessage()
                                .maximumRedeliveries(3)
                                .redeliveryDelay(0)
                                .retryAttemptedLogLevel(LoggingLevel.ERROR)
                                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                                .logRetryStackTrace(true)
                                .logExhausted(false)
                                .logExhaustedMessageHistory(false)
//                        .logRetryStackTrace(false)
                                .log("${routeId} ERROR_HANDLER Body:${body}")
                );

                onException(IllegalArgumentException.class)
//                        .maximumRedeliveries(3)
//                        .redeliveryDelay(0)
//                        .logRetryAttempted(true)
//                        .retryAttemptedLogLevel(LoggingLevel.WARN)
//                        .retriesExhaustedLogLevel(LoggingLevel.WARN)
//                        .logExhausted(false)
//                        .logExhaustedMessageHistory(false)
                        //.logStackTrace(false)
                        .log(LoggingLevel.WARN, "ON_EXCEPTION Body: ${body}")
                        .to("mock:onException");

                from("seda:start").routeId("start")
                        .aggregate(header("id"),
                                new AggregationStrategy() {
                                    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
                                        count++;
                                        if (count == 3) {
                                            throw new IllegalArgumentException("Forced oldBody: " + oldExchange.getIn().getBody() + ", newBody: " + newExchange.getIn().getBody());
                                        }
                                        if (oldExchange == null) {
                                            return newExchange;
                                        } else {
                                            String oldBody = oldExchange.getIn().getBody(String.class);
                                            String newBody = newExchange.getIn().getBody(String.class);

                                            oldExchange.getIn().setBody(oldBody + ", " + newBody);
                                            return oldExchange;
                                        }
                                    }
                                }).completionSize(4).completionTimeout(1000)
                        .log(LoggingLevel.INFO, "${routeId} AGGREGATED Body:${body}")
                        .to("mock:result");
            }
        });

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived("Hello World_1, Hello World_2, Hello World_3");

        MockEndpoint mockError = getMockEndpoint("mock:error");
        mockError.expectedMessageCount(0);

        MockEndpoint mockOnException = getMockEndpoint("mock:onException");
        mockOnException.expectedMessageCount(1);

        template.sendBodyAndHeader("seda:start", "Hello World_1", "id", 123);
        template.sendBodyAndHeader("seda:start", "Hello World_2", "id", 123);
        template.sendBodyAndHeader("seda:start", "Bye World", "id", 123);
        template.sendBodyAndHeader("seda:start", "Hello World_3", "id", 123);

        assertMockEndpointsSatisfied();
    }
}