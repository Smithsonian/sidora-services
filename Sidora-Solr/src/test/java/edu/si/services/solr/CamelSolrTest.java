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

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.velocity.VelocityConstants;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.DateTool;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author jbirkhimer
 */
public class CamelSolrTest extends Solr_CT_BlueprintTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(CamelSolrTest.class);

    private static final String KARAF_HOME = System.getProperty("karaf.home");

    private static Integer MAX_DOC;
    private static Integer COMPLETION_SIZE;
    private static Integer TEST_COMPLETION_TIMEOUT = 1000;
    private static Integer TOTAL_BATCH_COUNT;

    private static final String FEDORA_NAMESPACE = "namespaceTest";
    private static String CT_NAMESPACE;

    private static String CT_OWNER;

    private EmbeddedDatabase db;
    private JdbcTemplate jdbcTemplate;

    @Override
    protected String getBlueprintDescriptor() {
        return "OSGI-INF/blueprint/sidora-solr-route.xml";
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        CT_NAMESPACE = context.resolvePropertyPlaceholders("{{si.ct.namespace}}");
        CT_OWNER = context.resolvePropertyPlaceholders("{{si.fedora.user}}");
        MAX_DOC = Integer.valueOf(context.resolvePropertyPlaceholders("{{sidora.solr.batch.size}}"));
        db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.DERBY).setName("derbyTestDB").addScripts("sql/createAndPopulateDatabase.sql").build();
        jdbcTemplate = new JdbcTemplate(db);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        db.shutdown();
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry reg = super.createRegistry();
        reg.bind("dataSource", db);
        reg.bind("dataSourceReIndex", db);
        return reg;
    }

    private HashMap<String, List<String>> buildJMSBatch(boolean filterCT) {

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

            if (filterCT) {
                if (!user.equals(CT_OWNER)) {
                    log.info("Adding NON CT INGEST job: user = {} | pid = {} | label: {}", user, pid, label);
                    TOTAL_BATCH_COUNT = (label.equals(labelList[1]) ? TOTAL_BATCH_COUNT + 2 : ++TOTAL_BATCH_COUNT);
                }
            } else {
                //add extra because we have both index for
                TOTAL_BATCH_COUNT = (label.equals(labelList[1]) ? TOTAL_BATCH_COUNT + 2 : ++TOTAL_BATCH_COUNT);
            }

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

        log.info("Test TOTAL_BATCH_COUNT = {}", TOTAL_BATCH_COUNT);

        return answer;
    }

    @Test
    public void testBuildJMSBatch() {
        HashMap<String, List<String>> batchJMS = buildJMSBatch(false);
        assertTrue(batchJMS.size() == MAX_DOC);
    }

    @Test
    public void testFedoraMessages() throws Exception {

        COMPLETION_SIZE = Integer.valueOf(context.resolvePropertyPlaceholders("{{sidora.solr.batch.size}}"));

        HashMap<String, List<String>> batchJMS = buildJMSBatch(true);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(TOTAL_BATCH_COUNT/COMPLETION_SIZE+1);

        MockEndpoint mockCreateDocResult = getMockEndpoint("mock:createDocResult");
        mockCreateDocResult.expectedMessageCount(TOTAL_BATCH_COUNT);


        MockEndpoint mockEnd = getMockEndpoint("mock:end");
        mockEnd.expectedMessageCount(1);

        context.getRouteDefinition("fedoraApimUpdateSolrJob").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("fedoraApimGetFoxml").replace().to("velocity:file:{{karaf.home}}/fedora/foxml/test_foxml.vsl");
            }
        });

        context.getRouteDefinition("createBatchSolrJob").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("updateSianctReindexJob").replace().log(LoggingLevel.INFO, "Skipp updating db");
            }
        });

        context.getRouteDefinition("createSolrDoc").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("createSolrDocChoice").replace().to("mock:createDocResult");
            }
        });

        context.getRouteDefinition("sidoraSolrUpdate").adviceWith(context, new AdviceWithRouteBuilder() {
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
            exchange.getIn().setHeader("testTimeout", TEST_COMPLETION_TIMEOUT);

            template.send("activemq:queue:{{solr.apim.update.queue}}", exchange);
        }

        log.info("Test TOTAL_BATCH_COUNT = {}", TOTAL_BATCH_COUNT);

        assertMockEndpointsSatisfied(6000, TimeUnit.MILLISECONDS);
    }

    /**
     * Test to simulate messages from the CI Ingest route for indexing
     * @throws Exception
     */
    @Test
    public void ctIngestMessageTest() throws Exception {

        COMPLETION_SIZE = Integer.valueOf(context.resolvePropertyPlaceholders("{{sidora.solr.batch.size}}"));

        List<String> pidAgg = new ArrayList<>();
        for (int i = 1; i <= MAX_DOC; i++) {
            pidAgg.add(CT_NAMESPACE + ":" + i);
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
        mockCreateDocResult.expectedMessageCount(TOTAL_BATCH_COUNT+1);

        MockEndpoint mockEnd = getMockEndpoint("mock:end");
        mockEnd.expectedMessageCount(1);

        context.getRouteDefinition("cameraTrapSolrJob").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("ctJobGetFoxml").replace()
                        .setHeader("origin").simple(CT_OWNER)
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String pid = out.getHeader("pid", String.class);
                                if (observationResources.contains(pid)) {
                                    out.setHeader("dsLabel", "Observations");
                                } else {
                                    out.setHeader("dsLabel", "testLabel");
                                }
                            }
                        })
                        .to("velocity:file:{{karaf.home}}/fedora/foxml/test_foxml.vsl");
            }
        });

        context.getRouteDefinition("createBatchSolrJob").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("updateSianctReindexJob").replace().log(LoggingLevel.INFO, "Skipp updating db");
            }
        });

        context.getRouteDefinition("createSolrDoc").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("createSolrDocChoice").replace().to("mock:createDocResult");
            }
        });

        context.getRouteDefinition("sidoraSolrUpdate").adviceWith(context, new AdviceWithRouteBuilder() {
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
        exchange.getIn().setHeader("testTimeout", TEST_COMPLETION_TIMEOUT);

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
    @Test
    public void testGroovy() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:unauthorized");
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived("You Are Not Authorized To Preform This Operation!!!");

        MockEndpoint endpoint = getMockEndpoint("mock:end");
        endpoint.expectedMessageCount(1);
        endpoint.expectedBodiesReceived("SUCCESS");

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:groovy")
                        .filter().groovy("request.headers.auth != camelContext.resolvePropertyPlaceholders('{{si.fedora.password}}')")
                            .setBody().simple("You Are Not Authorized To Preform This Operation!!!")
                            .to("mock:unauthorized")
                            .stop()
                        .end()
                        .setBody().simple("SUCCESS")
                        .to("mock:end");

            }
        });

        template.sendBodyAndHeader("direct:groovy", null, "auth", context.resolvePropertyPlaceholders("{{si.fedora.password}}"));
        template.sendBodyAndHeader("direct:groovy", null, "auth", "some_other_password");
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFedoraSqlPaginationTest() throws Exception {
        TOTAL_BATCH_COUNT = Integer.valueOf(context.resolvePropertyPlaceholders("{{sidora.solr.page.limit}}")) * MAX_DOC;
        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived("totalBatch", TOTAL_BATCH_COUNT/MAX_DOC);
        resultEndpoint.expectedHeaderReceived("reindexCount", TOTAL_BATCH_COUNT);
        resultEndpoint.expectedHeaderReceived("pidCount", TOTAL_BATCH_COUNT);
        resultEndpoint.expectedBodyReceived().body().contains("Total inactive records solr: 0, Total inactive records sianct: 0\n" +
                "Total active records solr index: " + TOTAL_BATCH_COUNT + ", Total active records sianct index: 0\n" +
                "Combined Records Indexed: " + TOTAL_BATCH_COUNT);

        context.getRouteDefinition("solrReindexAll").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("clearSianctReindexTable").remove();
                weaveById("insertSianctReindexJob").remove();
                weaveById("insertSolrReindexJob").remove();
                weaveById("solrReindexAllPidCount").replace().setHeader("pidCount").simple(TOTAL_BATCH_COUNT + "");
                weaveById("solrReindexAllPidBatch").replace().process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        ArrayList<LinkedHashMap<String,String>> testSqlResultBody = new ArrayList<>();
                        for (int i = 1; i < (Integer.valueOf(exchange.getContext().resolvePropertyPlaceholders("{{sidora.solr.page.limit}}"))+1); i++) {
                            LinkedHashMap<String, String> row = new LinkedHashMap<>();
                            row.put("pid", "testPid:" + i);
                            testSqlResultBody.add(row);
                        }

                        exchange.getIn().setBody(testSqlResultBody);
                    }
                });
                weaveById("reindexGetFoxml").replace()
                        .setHeader("origin").simple("testUser")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                exchange.getIn().setHeader("dsLabel", "testLabel");
                            }
                        })
                        .to("velocity:file:{{karaf.home}}/fedora/foxml/test_foxml.vsl");

                weaveById("reindexCreateSianctJob").remove();
                weaveById("reindexCreateSolrJob").remove();
                weaveById("printReindexJobs").after().log("${body}").to("mock:result");
            }
        });

        context.getRouteDefinition("createBatchSolrJob").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("updateSianctReindexJob").replace().log(LoggingLevel.INFO, "Skipp updating db");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("gsearch_solr", true);
        exchange.getIn().setHeader("gsearch_sianct", true);
        exchange.getIn().setHeader("operationName", "solrReindexAll");
        exchange.getIn().setHeader("TOTAL_BATCH_COUNT", TOTAL_BATCH_COUNT);
        exchange.getIn().setHeader("testLimit", context.resolvePropertyPlaceholders("{{sidora.solr.page.limit}}"));
        exchange.getIn().setHeader("auth", context.resolvePropertyPlaceholders("{{si.fedora.password}}"));
        exchange.getIn().setHeader("testTimeout", TEST_COMPLETION_TIMEOUT);

        template.send("direct:solrReindexAll", exchange);

        String resultBody = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);
//        assertStringContains(resultBody, "Total inactive records solr: 0, Total inactive records sianct: 0\n" +
//                "Total active records solr index: " + TOTAL_BATCH_COUNT + ", Total active records sianct index: 0\n" +
//                "Combined Records Indexed: " + TOTAL_BATCH_COUNT);

        log.info("===============[ Fedora DB Requests ]===============\n{}", jdbcTemplate.queryForList("select * from fedora3.doRegistry"));

        log.info("===============[ Sidora DB Resource ]===============\n{}", jdbcTemplate.queryForList("select * from sidora.camelSolrReindexing"));

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