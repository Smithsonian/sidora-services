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
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.processor.DefaultExchangeFormatter;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import org.apache.camel.util.MessageHelper;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

/**
 * @author jbirkhimer
 */
public class SidoraSolrRouteBuilder extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(SidoraSolrRouteBuilder.class);


    @PropertyInject(value = "edu.si.solr")
    static private String LOG_NAME;
    static Marker logMarker = MarkerFactory.getMarker("edu.si.solr");

    @PropertyInject(value = "si.fedora.user")
    private String fedoraUser;

    @PropertyInject(value = "si.fedora.password")
    private String fedoraPasword;

    @PropertyInject(value = "sidora.solr.batch.size", defaultValue ="1000")
    private String BATCH_SIZE;

    @PropertyInject(value = "sidora.solr.batch.completionTimeout", defaultValue = "1000")
    private String COMPLETION_TIMEOUT;

    @PropertyInject(value = "sidora.solr.createDoc.parallelProcessing", defaultValue = "true")
    private String PARALLEL_PROCESSING;

    @PropertyInject(value = "sidoraSolr.redeliveryDelay", defaultValue = "0")
    private String redeliveryDelay;

    @PropertyInject(value = "sidoraSolr.maximumRedeliveries", defaultValue = "5")
    private String maximumRedeliveries;

    @PropertyInject(value = "sidoraSolr.backOffMultiplier", defaultValue = "2")
    private String backOffMultiplier;

    @PropertyInject(value = "sidora.solr.default.index", defaultValue = "gsearch_solr")
    private static String DEFAULT_SOLR_INDEX;

    @PropertyInject(value = "sidora.sianct.default.index", defaultValue = "gsearch_sianct")
    private static String DEFAULT_SIANCT_INDEX;

    @PropertyInject(value = "sidora.solr.endpoint")
    private static String solrHost;

    @PropertyInject(value = "sidora.solr.commitWithin", defaultValue = "300000")
    private static String commitWithin;

    @PropertyInject(value = "sidora.solr.concurrentConsumers", defaultValue = "20")
    private static String concurrentConsumers;

    //private static Integer sqlOffset = 9500000;
    private static Integer sqlOffset;

    //CloseableHttpClient client; // = HttpClientBuilder.create().build();
    //private static SolrClient client = new HttpSolrClient.Builder(solrHost).build();

    //TODO: used by .loopDoWhile(stopLoopPredicate()) which is only in camel 2.17 and up :(
    /*public Predicate stopLoopPredicate() {
        Predicate stopLoop = new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                return exchange.getIn().getHeader("offset") == null || !(exchange.getIn().getHeader("offset", Integer.class) >= exchange.getIn().getHeader("pidCount", Integer.class));
            }
        };
        return stopLoop;
    }*/

    public Processor offsetLoopProcessor() {
        Processor offset = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                //Integer offset = (Integer) exchange.getIn().getHeader("offset");
                Integer limit = Integer.valueOf(exchange.getContext().resolvePropertyPlaceholders("{{sidora.solr.page.limit}}"));
                if (sqlOffset == null) {
                    sqlOffset = 0;
                } else {
                    sqlOffset += limit;
                }
                exchange.getIn().setHeader("offset", sqlOffset);
            }
        };
        return offset;
    }

    public static Processor createSolrJob() {
        Processor createSolrJobProcessor = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Message out = exchange.getIn();
                String pid = out.getHeader("pid", String.class);
                String index = out.getHeader("solrIndex", String.class);
                String method = out.getHeader("method", String.class);
                if (index != null) {
                    MySolrJob solrJob = new MySolrJob();
                    solrJob.setPid(pid);
                    solrJob.setOrigin(out.getHeader("origin", String.class));
                    solrJob.setMethodName(out.getHeader("methodName", String.class));
                    solrJob.setDsLabel(out.getHeader("dsLabel", String.class));
                    solrJob.setState(out.getHeader("state", String.class));
                    solrJob.setSolrOperation(method);
                    solrJob.setIndex(index);
                    solrJob.indexes.add(index);
                    solrJob.setFoxml(out.getBody(String.class));

                    LOG.debug(logMarker, "******[ NEW solrJob ]******* {}", solrJob);

                    out.setHeader("solrJob", solrJob);
                } else {
                    throw new SidoraSolrException("Cannot create Solr Job!!! solrIndex is null for pid: " + pid + "!!!");
                }
            }
        };

        return createSolrJobProcessor;

    }

    public Processor createSolrInputDocumentProcessor() {
        Processor createSolrInputDocumentProcessor = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Message out = exchange.getIn();
                String body = out.getBody(String.class);
                MySolrJob solrJob = out.getHeader("solrjob", MySolrJob.class);

                if (!body.startsWith("<doc>")) {
                    LOG.error(logMarker, "Found something other than a solr doc");
                    throw new SidoraSolrException("Found something other than a solr doc:\n" + body);
                }

                SolrInputDocument solrInputDoc = new SolrInputDocument();
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(new InputSource(new StringReader(body)));
                NodeList docList = doc.getElementsByTagName("doc");
                for (int docIdx = 0; docIdx < docList.getLength(); docIdx++) {
                    Node docNode = docList.item(docIdx);
                    if (docNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element docElement = (Element) docNode;
                        NodeList fieldsList = docElement.getChildNodes();
                        for (int fieldIdx = 0; fieldIdx < fieldsList.getLength(); fieldIdx++) {
                            Node fieldNode = fieldsList.item(fieldIdx);
                            if (fieldNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element fieldElement = (Element) fieldNode;
                                String fieldName = fieldElement.getAttribute("name");
                                String fieldValue = fieldElement.getTextContent();

                                //TODO: check cameraDeploymentBeginDate and cameraLongitude values
                                //HttpSolrClient$RemoteSolrException: Error from server at http://localhost:8090/solr: ERROR: [doc=si:2413718] Error adding field 'cameraLongitude'='' msg=empty String
                                //HttpSolrClient$RemoteSolrException: Error from server at http://localhost:8090/solr: ERROR: [doc=si:242440] Error adding field 'cameraDeploymentBeginDate'='NaN' msg=For input string: "NaN"
                                //.HttpSolrClient$RemoteSolrException: Error from server at http://localhost:8090/solr: ERROR: [doc=si:2746530] Error adding field 'cameraDeploymentBeginDate'='2014-08-09' msg=For input string: "2014-08-09"

                                solrInputDoc.addField(fieldName, fieldValue);
                            }
                        }
                    }
                }

                solrJob.setSolrdoc(solrInputDoc);
                out.removeHeader("solrJob");
                out.setBody(solrJob);
            }
        };

        return createSolrInputDocumentProcessor;
    }

    @Override
    public void configure() throws Exception {

        PoolingHttpClientConnectionManager httpConnectionManager = new PoolingHttpClientConnectionManager();
        httpConnectionManager.setDefaultMaxPerRoute(50);
        httpConnectionManager.setMaxTotal(200);
        getContext().getComponent("http4", HttpComponent.class).setClientConnectionManager(httpConnectionManager);
        //client = HttpClients.custom().setConnectionManager(httpConnectionManager).build();

        errorHandler(deadLetterChannel("direct:deadLetterChannel")
                //.useOriginalMessage()
                .maximumRedeliveries(Integer.parseInt(maximumRedeliveries))
                .redeliveryDelay(Integer.parseInt(redeliveryDelay))
                .backOffMultiplier(Integer.parseInt(backOffMultiplier))
                .useExponentialBackOff()
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                //.logRetryStackTrace(true)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .logExhausted(true)
                .logExhaustedMessageHistory(true)
                .logStackTrace(true)
                .logHandled(true)
        );

        Namespaces ns = new Namespaces("atom", "http://www.w3.org/2005/Atom");
        ns.add("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        ns.add("fs", "info:fedora/fedora-system:def/model#");
        ns.add("fedora-types", "http://www.fedora.info/definitions/1/0/types/");
        ns.add("objDatastreams", "http://www.fedora.info/definitions/1/0/access/");
        ns.add("ri", "http://www.w3.org/2005/sparql-results#");
        ns.add("findObjects", "http://www.fedora.info/definitions/1/0/types/");
        ns.add("fits", "http://hul.harvard.edu/ois/xml/ns/fits/fits_output");
        ns.add("fedora", "info:fedora/fedora-system:def/relations-external#");
        ns.add("eac", "urn:isbn:1-931666-33-4");
        ns.add("mods", "http://www.loc.gov/mods/v3");
        ns.add("atom", "http://www.w3.org/2005/Atom");
        ns.add("sidora", "http://oris.si.edu/2017/01/relations#");
        ns.add("foxml", "info:fedora/fedora-system:def/foxml#");

        from("direct:deadLetterChannel").routeId("solrDeadLetter")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        StringBuilder sb = new StringBuilder();
                        String pid = exchange.getIn().getHeader("pid", String.class);
                        String routeID = exchange.getFromRouteId();

                        sb.append("Failed PID: "+pid);
                        sb.append("\nFailureRouteId: " + exchange.getProperty(Exchange.FAILURE_ROUTE_ID, String.class));
                        sb.append("\nToEndpoint: " + exchange.getProperty(Exchange.TO_ENDPOINT, String.class));
                        sb.append("\nFailureEndpoint: " + exchange.getProperty(Exchange.FAILURE_ENDPOINT, String.class));

                        // setup exchange formatter to be used for message history dump
                        DefaultExchangeFormatter formatter = new DefaultExchangeFormatter();
                        formatter.setShowExchangeId(true);
                        formatter.setMultiline(true);
                        formatter.setShowHeaders(true);
                        formatter.setStyle(DefaultExchangeFormatter.OutputStyle.Fixed);
                        formatter.setMaxChars(1000000);
                        formatter.setShowProperties(true);

                        String routeStackTrace = MessageHelper.dumpMessageHistoryStacktrace(exchange, formatter, true);
                        sb.append("\n" + routeStackTrace);

                        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                        StringWriter sw = new StringWriter();
                        exception.printStackTrace(new PrintWriter(sw));
                        sb.append("\n"+sw.toString());

                        sb.append("\n=======================================================================================================================================\n");

                        //getContext().createProducerTemplate().sendBody("file:{{karaf.home}}/deadLetter?fileName="+pid+"_error-"+routeID+"&fileExist=append", sb.toString());

                        exchange.getIn().setHeader(Exchange.FILE_NAME, pid+"_error-"+routeID);
                        exchange.getIn().setBody(sb.toString());

                        //Sometimes in exchange body does not get set ??? weird
                        exchange.getOut().setHeader(Exchange.FILE_NAME, pid+"_error-"+routeID);
                        exchange.getOut().setBody(sb.toString());
                    }
                }).id("deadLetterProcessor")
                .to("file:{{karaf.home}}/deadLetter?fileExist=append");

        from("cxfrs://bean://rsServer?bindingStyle=SimpleConsumer").routeId("SidoraSolrService")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} :: ${routeId} :: Starting REST Service Request for: ${header.operationName} ... ")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: REST Request headers: ${headers}")
                .choice()
                    .when().simple("${header.operationName} == 'solrReindexAll'")
                        .toD("seda:${header.operationName}?waitForTaskToComplete=Never")
                    .endChoice()
                    .otherwise()
                        .toD("direct:${header.operationName}")
                    .endChoice()
                .end()
                .setHeader(Exchange.CONTENT_TYPE, constant("text/xml"))
                .removeHeaders("*")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} :: ${routeId} :: SidoraSolr: Finished REST Service Request for: ${header.operationName} ... ");

        from("direct:solrRequest").routeId("SidoraSolrRequest")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} :: ${routeId} :: SidoraSolr: Request: Starting processing ...")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: SidoraSolr: Request: PID = ${header.pid}, Index = ${header.solrIndex}, Solr Operation = ${header.solrOperation}")
                .setBody().simple("Hello World From Sidora Solr [ path = ${header.operationName} ]: pid = ${header.pid}, solrIndex = ${header.solrIndex}, solrOperation = ${header.solrOperation}")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} :: ${routeId} :: SidoraSolr: Request: Finished processing ...");

        from("direct:solrDeleteAll").routeId("solrDeleteAll")
                .filter().groovy("request.headers.auth != camelContext.resolvePropertyPlaceholders('{{si.solr.password}}')")
                    .setBody().simple("You Are Not Authorized To Preform This Operation!!!")
                    .stop()
                .end()

                .setHeader("query").simple("*:*")
                .to("direct:solrDeleteByQuery");

        from("direct:solrDeleteByQuery").routeId("solrDeleteByQuery")
                .filter().groovy("request.headers.auth != camelContext.resolvePropertyPlaceholders('{{si.solr.password}}')")
                    .setBody().simple("You Are Not Authorized To Preform This Operation!!!")
                    .stop()
                .end()

                .filter().simple("${header.gsearch_sianct} == 'true'")
                    .setHeader("solrIndex").simple("{{sidora.sianct.default.index}}")
                    .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Delete from {{sidora.sianct.default.index}}. BODY = ${body}")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            Message out = exchange.getIn();
                            UpdateRequest updateRequest = new UpdateRequest();
                            updateRequest.deleteByQuery(out.getHeader("query", String.class));
                            out.setHeader("solrUpdateRequest", updateRequest);
                            out.setBody(null);
                        }
                    })
                    .to("seda:solr")
                    .setHeader("solrResponse").simple("Delete from {{sidora.sianct.default.index}}\nQuery:\n${header.query}\nResponse:\n${body}")
                .end()

                .filter().simple("${header.gsearch_solr} == 'true'")
                    .setHeader("solrIndex").simple("{{sidora.solr.default.index}}")
                    .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Delete from {{sidora.solr.default.index}}. BODY = ${body}")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            Message out = exchange.getIn();
                            UpdateRequest updateRequest = new UpdateRequest();
                            updateRequest.deleteByQuery(out.getHeader("query", String.class));
                            out.setHeader("solrUpdateRequest", updateRequest);
                            out.setBody(null);
                        }
                    })
                    .to("seda:solr")
                    .setHeader("solrResponse").simple("${header.solrResponse}\nDelete from {{sidora.solr.default.index}}\nQuery:\n${header.query}\nResponse:\n${body}")
                .end()
                .setBody().simple("${header.solrResponse}");

        from("activemq:queue:{{sidoraCTSolr.queue}}").routeId("cameraTrapSolrJob")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: RECEIVED JMS\nHeaders:\n${headers}\nBody:${body}")

                //Add the project pids to resource pid list
                .setHeader("PIDAggregation").simple("{{si.ct.root}},${header.PIDAggregation},${header.ProjectPID},${header.SubProjectPID},${header.SitePID},${header.PlotPID}")

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: CT Solr Job RECEIVED, PID's: ${header.PIDAggregation}")

                //remove unnecessary large headers
                .removeHeaders("CamelSchematronValidationReport|datastreamValidationXML|ManifestXML")

                .split().tokenize(",", "PIDAggregation")
                .parallelProcessing(Boolean.parseBoolean(PARALLEL_PROCESSING))
                .streaming()

                    .setHeader("pid").simple("${body}")

                    .choice()
                        .when().simple("${body} != ''") //incase there is not plotPID
                            .to("seda:getFoxml").id("ctJobGetFoxml")

                            .setHeader("origin").xpath("/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME = 'info:fedora/fedora-system:def/model#ownerId']/@VALUE", String.class, ns)
                            .setHeader("dsLabel").xpath("/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME = 'info:fedora/fedora-system:def/model#label']/@VALUE", String.class, ns)
                            .setHeader("state").xpath("/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME = 'info:fedora/fedora-system:def/model#state']/@VALUE", String.class, ns)
                            .setHeader("methodName").simple("ctIngest")

                            .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Processing CT SolrJob [ ${header.origin}, ${header.pid}, ${header.methodName}, ${header.dsLabel}, ${header.state} ]")

                            .filter().simple("${header.pid} in '${header.ResearcherObservationPID},${header.VolunteerObservationPID},${header.ImageObservationPID}'")
                                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: FOUND OBSERVATION")
                                //.to("bean:MyBatchService?method=addJob(*, update, {{sidora.sianct.default.index}})")
                                .setHeader("method").simple("update")
                                .setHeader("solrIndex").simple("{{sidora.sianct.default.index}}")
                                .process(createSolrJob())
                                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Send to createBatchJob:\n${header.solrJob}")
                                .to("seda:processSolrJob").id("createCtSianctSolrJob")
                            .end()

                            //.to("bean:MyBatchService?method=addJob(*, update, {{sidora.solr.default.index}})")
                            .setHeader("method").simple("update")
                            .setHeader("solrIndex").simple("{{sidora.solr.default.index}}")
                            .process(createSolrJob())
                            .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Send to createBatchJob:\n${header.solrJob}")
                            .to("seda:processSolrJob").id("createCTSolrJob")
                        .endChoice()
                .end().id("ctSolrSplitEnd");

        from("activemq:queue:{{solr.apim.update.queue}}").routeId("fedoraApimUpdateSolrJob")
                .setHeader("origin").xpath("/atom:entry/atom:author/atom:name", String.class, ns)
                .setHeader("dsLabel").xpath("/atom:entry/atom:category[@scheme='fedora-types:dsLabel']/@term", String.class, ns)
                .setBody().simple("${header.pid}")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} :: ${routeId} :: Fedora Solr Job RECEIVED [ ${header.origin}, ${header.pid}, ${header.methodName}, ${header.dsLabel} ]")

                //filter out fedora messages from CT ingest we have a separate pipeline for that
                .filter().simple("${header.origin} != '{{si.fedora.user}}' || ${header.pid} not contains '{{si.ct.namespace}}:'")

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: After ctUser Filter [ ${header.origin}, ${header.pid}, ${header.methodName}, ${header.dsLabel} ]")

                .to("direct:getFoxml").id("fedoraApimGetFoxml")
                .setHeader("origin").xpath("/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME = 'info:fedora/fedora-system:def/model#ownerId']/@VALUE", String.class, ns)
                .setHeader("dsLabel").xpath("/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME = 'info:fedora/fedora-system:def/model#label']/@VALUE", String.class, ns)
                .setHeader("state").xpath("/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME = 'info:fedora/fedora-system:def/model#state']/@VALUE", String.class, ns)
                .setHeader("methodName").simple("fedoraApim")
                .setHeader("hasModel").xpath("string-join(//fs:hasModel/@rdf:resource, ',')", String.class, ns)

                // Set the index we are operating and operation
                .choice()
                    //.when().simple("${header.methodName} in 'purge,purgeObject,purgeDatastream' && ${header.pid} contains '{{si.ct.namespace}}:' && ${header.dsLabel} contains 'Observations'")
                    .when().simple("${header.methodName} in 'purge,purgeObject,purgeDatastream' && ${header.hasModel} contains 'datasetCModel' && ${header.dsLabel?.toLowerCase()} contains 'observation'")
                        //.to("bean:MyBatchService?method=addJob(*, delete, {{sidora.sianct.default.index}})")
                        .setHeader("method").simple("delete")
                        .setHeader("solrIndex").simple("{{sidora.sianct.default.index}}")
                        .process(createSolrJob())
                        .to("seda:processSolrJob")
                        // Also create job for gsearch_solr
                        //.to("bean:MyBatchService?method=addJob(*, delete, {{sidora.solr.default.index}})")
                        .setHeader("solrIndex").simple("{{sidora.solr.default.index}}")
                        .process(createSolrJob())
                        .to("seda:processSolrJob")
                    .endChoice()
                    //.when().simple("${header.pid} contains '{{si.ct.namespace}}:' && ${header.dsLabel} contains 'Observations'")
                    .when().simple("${header.hasModel} contains 'datasetCModel' && ${header.dsLabel?.toLowerCase()} contains 'observation'")
                        //.to("bean:MyBatchService?method=addJob(*, update, {{sidora.sianct.default.index}})")
                        .setHeader("method").simple("update")
                        .setHeader("solrIndex").simple("{{sidora.sianct.default.index}}")
                        .process(createSolrJob())
                        .to("seda:processSolrJob")
                        // Also create job for gsearch_solr
                        //.to("bean:MyBatchService?method=addJob(*, update, {{sidora.solr.default.index}})")
                        .setHeader("method").simple("update")
                        .setHeader("solrIndex").simple("{{sidora.solr.default.index}}")
                        .process(createSolrJob())
                        .to("seda:processSolrJob")
                    .endChoice()
                    .when().simple("${header.methodName} in 'purgeObject,purgeDatastream' || ${header.methodName} contains 'purge'")
                        //.to("bean:MyBatchService?method=addJob(*, delete, {{sidora.solr.default.index}})")
                        .setHeader("method").simple("delete")
                        .setHeader("solrIndex").simple("{{sidora.solr.default.index}}")
                        .process(createSolrJob())
                        .to("seda:processSolrJob")
                    .endChoice()
                    .otherwise()
                        //.to("bean:MyBatchService?method=addJob(*, update, {{sidora.solr.default.index}})")
                        .setHeader("method").simple("update")
                        .setHeader("solrIndex").simple("{{sidora.solr.default.index}}")
                        .process(createSolrJob())
                        .to("seda:processSolrJob")
                    .endChoice()
                .end();

        from("seda:solrReindexAll").routeId("solrReindexAll")
                .filter().groovy("request.headers.auth != camelContext.resolvePropertyPlaceholders('{{si.solr.password}}')")
                    .setBody().simple("You Are Not Authorized To Preform This Operation!!!")
                    .stop()
                .end()

                .to("sql:{{sql.clearSolrReindexTable}}?dataSource=#dataSourceReIndex&noop=true").id("clearSianctReindexTable")

                //get count for pagination
                .to("sql:{{sql.solrPidCount}}?outputType=SelectOne&outputHeader=pidCount").id("solrReindexAllPidCount")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} :: ${routeId} :: pidCount = ${header.pidCount}")
                .setHeader("reindexCount").simple("0", Integer.class)

                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message out = exchange.getIn();
                        Integer pidCount = out.getHeader("pidCount", Integer.class);
                        Integer limit = Integer.valueOf(exchange.getContext().resolvePropertyPlaceholders("{{sidora.solr.page.limit}}"));
                        Integer totalBatch = (pidCount + limit - 1) / limit;
                        out.setHeader("totalBatch", totalBatch);
                        exchange.getIn().setHeader("limit", limit);
                    }
                })

                //paging over pids from fedora dB using offset and limit
                //.loopDoWhile(stopLoopPredicate()) //TODO: only in camel 2.17 and up :(
                .loop().simple("${header.totalBatch}").copy()
                    .process(offsetLoopProcessor())

                    .log(LoggingLevel.INFO, LOG_NAME, "sql limit: {{sidora.solr.page.limit}}, offset: ${header.offset}")

                    .to("sql:{{sql.selectPidBatch}}").id("solrReindexAllPidBatch")

                    .setHeader("resultSize").simple("${body.size}")
                    .log(LoggingLevel.INFO, LOG_NAME, "${id} :: ${routeId} :: resultCount =  ${header.resultSize}")

                    .split().simple("${body}")
                        .streaming()
                        .parallelProcessing(Boolean.parseBoolean(PARALLEL_PROCESSING))

                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Split Body: ${body}")
                        .setHeader("pid").simple("${body[pid]}")

                        .to("seda:getFoxml").id("reindexGetFoxml")

                        .setHeader("origin").xpath("/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME = 'info:fedora/fedora-system:def/model#ownerId']/@VALUE", String.class, ns)
                        .setHeader("dsLabel").xpath("/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME = 'info:fedora/fedora-system:def/model#label']/@VALUE", String.class, ns)
                        .setHeader("state").xpath("/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME = 'info:fedora/fedora-system:def/model#state']/@VALUE", String.class, ns)
                        .setHeader("methodName").simple("${header.operationName}")
                        .setHeader("hasModel").xpath("string-join(//fs:hasModel/@rdf:resource, ',')", String.class, ns)

                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: REINDEX [ ${header.origin}, ${header.pid}, ${header.methodName}, ${header.dsLabel}, ${header.state} ]")

                        .filter().simple("${header.hasModel} contains 'datasetCModel' && ${header.dsLabel?.toLowerCase()} contains 'observation' && ${header.gsearch_sianct} == 'true'")// && ${header.state} == 'Active'")
                            .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: **** FOUND OBSERVATION ****")
                            //.to("bean:MyBatchService?method=addJob(*, update, {{sidora.sianct.default.index}})")
                            .setHeader("method").simple("update")
                            .setHeader("solrIndex").simple("{{sidora.sianct.default.index}}")
                            .process(createSolrJob())
                            //header is used for http response only
                            .to("seda:processSolrJob?waitForTaskToComplete=Never").id("reindexCreateSianctJob")
                            //.to("sql:{{sql.insertSolrReindexJob}}?dataSource=#dataSourceReIndex&noop=true").id("insertSianctReindexJob")
                        .end()//end filter

                        .filter().simple("${header.gsearch_solr} == 'true'")// && ${header.state} == 'Active'")
                            //.to("bean:MyBatchService?method=addJob(*, update, {{sidora.solr.default.index}})")
                            .setHeader("method").simple("update")
                            .setHeader("solrIndex").simple("{{sidora.solr.default.index}}")
                            .process(createSolrJob())
                            .to("seda:processSolrJob?waitForTaskToComplete=Never").id("reindexCreateSolrJob")
                            //.to("sql:{{sql.insertSolrReindexJob}}?dataSource=#dataSourceReIndex&noop=true").id("insertSolrReindexJob")
                        .end()//end filter
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} :: ${routeId} :: Processing page ( ${header.CamelLoopIndex}++ of ${header.totalBatch} ), adding: ${header.CamelSplitIndex}++ of ${header.resultSize} from page. REINDEX [ ${header.origin}, ${header.pid}, ${header.methodName}, ${header.dsLabel}, ${header.state}, ${header.index} ]")
                    .end().id("reindexAllSplitEnd")//end split

                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            Message out = exchange.getIn();
                            out.setHeader("reindexCount", out.getHeader("reindexCount", Integer.class) + out.getHeader("resultSize", Integer.class));
                        }
                    })
                    .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Reindex Processed page ( ${header.CamelLoopIndex}++ of ${header.totalBatch} ), pids added: ( ${header.reindexCount} of ${header.pidCount} )")
                .end().id("reindexAllLoopEnd");//end loop


        from("seda:processSolrJob?concurrentConsumers=50").routeId("processSolrJob")
                .setHeader("startTime").simple(String.valueOf(new Date().getTime()), long.class)

                .to("seda:createDoc").id("createDoc")

                //Set end time and solr status for jobs
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message out = exchange.getIn();
                        MySolrJob solrJob = out.getBody(MySolrJob.class);
                        if (solrJob != null) {
                            solrJob.setEndTime(new Date().getTime());
                            solrJob.setSolrStatus("TODO !!!");
                            LOG.debug(logMarker, "Solr Job Status for pid = {}, {}, elapse time: {}, SolrJob={}", solrJob.getPid(), solrJob.getSolrStatus(), solrJob.getElapsed(), solrJob);
                            LOG.info(logMarker, "Solr Job Complete: pid: {}, index: {}, elapse time: {}", solrJob.getPid(), solrJob.getIndex(), solrJob.getElapsed());
                        }
                    }
                })

                .choice()
                    .when().simple("${body} != null && ${body} is 'edu.si.services.solr.MySolrJob'")
                        .to("seda:aggregateJobs")
                    .endChoice()
                    .otherwise()
                        .to("seda:solr").id("sendQueryToSolr")
                    .endChoice()
                .end();

                //TODO: update dB with solrStatus and time
              /*.choice()
                  .when().simple("${header.batchJobs[0].methodName} == 'solrReindexAll'")
                      //.to("seda:updateReindexDb?waitForTaskToComplete=Never")
                  .endChoice()
              .end();*/


        from("seda:aggregateJobs").routeId("aggregateJobs")
                //Aggregate based on solr operations (add/update/delete) and index (gsearch_solr/gsearch_sianct)
                .aggregate(simple("${body.index}"), new MySolrUpdateStrategy())
                    .parallelProcessing(Boolean.parseBoolean(PARALLEL_PROCESSING))
                    .completionSize(Integer.parseInt(BATCH_SIZE))
                    .completionTimeout(Integer.parseInt(COMPLETION_TIMEOUT)).id("aggregateSolrJob")

                    //.log(LoggingLevel.INFO, LOG_NAME, "Solr solrUpdateRequest size: ${header.solrUpdateRequest.getDocuments().size()} [ index: ${header.solrIndex} ]")

                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            int size = exchange.getIn().getHeader("solrUpdateRequest", UpdateRequest.class).getDocuments().size();
                            String index = exchange.getIn().getHeader("solrIndex", String.class);
                            log.info(logMarker, "Solr solrUpdateRequest size: {} [ index: {} ]", size, index);
                        }
                    })
                    .to("seda:solr").id("createBatchJobSendToSolr");


        from("seda:createDoc?concurrentConsumers=50").routeId("createSolrDoc")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Starting CreateSolrDoc [ solrJob = ${header.solrJob} ]")

                .setBody().simple("${header.solrJob}")

                .setHeader("jobInfo").simple("[ solrJob = ${header.solrJob} ])")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Start createDoc ${header.jobInfo}")

                .choice().id("createSolrDocChoice")
                    .when().simple("${body.solrOperation} == 'delete'")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Found *** DELETE *** ${header.jobInfo}")
                    .endChoice()

                    .when().simple("${body.index} == '{{sidora.sianct.default.index}}'")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Found *** SIANCT *** ${header.jobInfo}")
                        .setHeader("solrOperation").simple("update")
                        .setHeader("pid").simple("${body.pid}", String.class)
                        .setHeader("state").simple("${body.state}")

                        .to("velocity:file:{{karaf.home}}/Input/templates/gsearch_sianct-sparql.vsl")
                        .to("seda:sianctFusekiQuery").id("createDocFusekiQuery")

                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Send to XSLT\nHeaders:\n${headers}\nBody:\n${body}")

                        .to("xslt:file:{{karaf.home}}/Input/xslt/batch_CT_foxml-to-gsearch_sianct.xslt?saxon=true").id("foxmlToGsearchSianctXSLT")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: batch_CT_foxml-to-gsearch_sianct output:\n${body}")
                        .process(createSolrInputDocumentProcessor())
                    .endChoice()

                    .otherwise()
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: ${routeId} :: Found *** SOLR ONLY *** ${header.jobInfo}")
                        .setHeader("solrOperation").simple("update")
                        .setHeader("pid").simple("${body.pid}", String.class)

                        //.to("seda:getFoxml").id("createDocGetFoxml")
                        .setBody().simple("${body.foxml}")

                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Send to batch_foxml-to-gsearch_solr XSLT\nHeaders:\n${headers}\nBody:\n${body}")

                        .to("xslt:file:{{karaf.home}}/Input/xslt/batch_foxml-to-gsearch_solr.xslt?saxon=true").id("foxmlToGsearchSolrXSLT")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: batch_foxml-to-gsearch_solr output:\n${body}")
                        .process(createSolrInputDocumentProcessor())
                    .endChoice()
                .end().id("createDocEndChoice");


        from("seda:solr?concurrentConsumers=25").routeId("sidoraSolrUpdate")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Send Request (solrIndex = ${header.solrIndex})\nHeaders: ${headers}\nBody:\n${body}")

                /**
                 * solr update via Solrj with rollback on fail
                 */
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message out = exchange.getIn();
                        String solrIndex = out.getHeader("solrIndex", String.class);
                        UpdateRequest updateRequest = out.getHeader("solrUpdateRequest", UpdateRequest.class);
                        List<SolrInputDocument> docList = updateRequest.getDocuments();

                        if(docList != null && !docList.isEmpty() && docList.size() < Integer.parseInt(BATCH_SIZE) && !solrIndex.equals(exchange.getContext().resolvePropertyPlaceholders("{{sidora.sianct.default.index}}"))) {
                            log.warn(logMarker, "Solr Update Request less than Batch Count!!!: Sending docs to {} count: {}", solrIndex, docList.size());
                        }

                        try (SolrClient client = new HttpSolrClient.Builder(solrHost).build()) {
                            UpdateResponse result = updateRequest.process(client, solrIndex);
                            LOG.debug(logMarker, "Result: " + result);
                            Integer statusCode = result.getStatus();
                            if (statusCode == 0) {
                                LOG.debug(logMarker, "Successful Solr Request.\nResult: {}", result);
                                //Allow solr to handle the commits set in solrconfig.xml.
                                //UpdateResponse commitResult = updateRequest.commit(client, solrIndex);
                                //LOG.debug(logMarker, "Commit Result: {}", commitResult);
                            } else {
                                LOG.error(logMarker, "Solr Update Failed!!!\nResult: {}", result);
                                //AbstractUpdateRequest rollBackResult = updateRequest.rollback();
                                throw new SidoraSolrException("Solr Update Failed!!!:\n" + result);
                            }
                            out.setBody(result);
                        } catch (Exception e) {
                            LOG.error(logMarker, "Solr Update Failed ERROR!!!\nException:\n{}", e.toString());
                            out.setHeader("failedUpdateRequestList", updateRequest.getDocuments());
                            e.printStackTrace();
                            throw new SidoraSolrException("Solr Update Failed ERROR!!!\nException:\n" + e.toString());
                        }
                    }
                }).id("sendToSolr")

                .convertBodyTo(String.class)
                //TODO: check the response

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Solr Response - ${body}");


        from("seda:getFoxml?concurrentConsumers=20").id("getFoxml")
                /**
                 * fetch foxml using camel http component
                 * (NOTE: causes ConcurrentModificationException b/c the camel component is iterating over headers)
                 */
                .removeHeaders("CamelHttp*")
                .setHeader("CamelHttpMethod", constant("GET"))
                .setHeader("CamelHttpQuery").simple("context=public&format=info:fedora/fedora-system:FOXML-1.1")
                .setHeader("CamelHttpUri", simple("{{si.fedora.host}}/objects/${header.pid}/export"))
                .setBody().simple("")

                .setHeader("Authorization").simple("Basic " + Base64.getEncoder().encodeToString((fedoraUser + ":" + fedoraPasword).getBytes("UTF-8")), String.class)

                .toD("http4:CamelHttpUri?headerFilterStrategy=#dropHeadersStrategy&authMethod=Basic&authUsername="+fedoraUser+"&authPassword="+fedoraPasword).id("getFoxml")
                //.toD("http4://localhost:8080/fedora/objects/${header.pid}/export?headerFilterStrategy=#dropHeadersStrategy").id("getFoxml")
                //.toD("jetty://localhost:8080/fedora/objects/${header.pid}/export?copyHeaders=false&mapHttpMessageHeaders=false&headerFilterStrategy=#dropHeadersStrategy").id("getFoxml")
                //.toD("fcrepo:objects/${header.pid}/export?context=public&format=info:fedora/fedora-system:FOXML-1.1").id("getFoxml")

                .convertBodyTo(String.class)

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Fedora Response:\n${body}");

                /**
                 * fetch foxml using HTTPClient
                 */
                /*.process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String pid = exchange.getIn().getHeader("pid", String.class);
                        String fedoraHost = exchange.getContext().resolvePropertyPlaceholders("{{si.fedora.host}}");

                        //CloseableHttpClient client = HttpClientBuilder.create().build();

                        URI uri = new URIBuilder(fedoraHost + "/objects/" + pid + "/export")
                                .setParameter("context", "public")
                                .setParameter("format", "info:fedora/fedora-system:FOXML-1.1")
                                .build();

                        HttpGet httpget = new HttpGet(uri);
                        httpget.setHeader(Exchange.CONTENT_TYPE, "text/xml");
                        httpget.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((fedoraUser + ":" + fedoraPasword).getBytes("UTF-8")));

                        LOG.debug(logMarker, "httpget uri: {}", httpget.getURI());

                        try (CloseableHttpResponse response = client.execute(httpget)) {
                            HttpEntity entity = response.getEntity();

                            LOG.debug(logMarker, "CloseableHttpResponse response.toString(): {}", response.toString());

                            Integer responseCode = response.getStatusLine().getStatusCode();
                            String statusLine = response.getStatusLine().getReasonPhrase();
                            String entityResponse = EntityUtils.toString(entity, "UTF-8");

                            //set the response
                            exchange.getIn().setBody(entityResponse);
                            LOG.trace(logMarker, "Fedora response Body: {}", entityResponse);

                            //copy response headers to camel
//                            org.apache.http.Header[] headers = response.getAllHeaders();
//                            LOG.debug(logMarker, "Fedora response Headers: {}", Arrays.toString(headers));

                            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, responseCode);
                            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_TEXT, statusLine);

                            if (responseCode != 200) {
                                LOG.error(logMarker, "Fedora response [ pid={} ]: " + Exchange.HTTP_RESPONSE_CODE + "= {}, " + Exchange.HTTP_RESPONSE_TEXT + "= {}", pid, responseCode, statusLine);
                                LOG.error(logMarker, "Fedora response entity: {}", entityResponse);
                            } else {
                                LOG.debug(logMarker, "Fedora response [ pid={} ]: " + Exchange.HTTP_RESPONSE_CODE + "= {}, " + Exchange.HTTP_RESPONSE_TEXT + "= {}", pid, responseCode, statusLine);
                            }
                        } catch (Exception e) {
                            throw new Exception("HTTP error sending request", e);
                        }
                        //client.close();
                    }
                }).id("getFoxml");*/

        from("seda:sianctFusekiQuery?concurrentConsumers=5").routeId("sianctFusekiQuery")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Fuseki Query - ${body}")

                //TODO: Watch for ConcurrentModificationException may need to use HTTPClient instead
                .setBody().groovy("\"query=\" + URLEncoder.encode(request.getBody(String.class));")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${routeId} :: Fuseki Query - ${body}\nHeaders:\n${headers}")

                .removeHeaders("CamelHttp*")
                .setHeader("CamelHttpMethod", constant("GET"))
                .setHeader("CamelHttpQuery").simple("output=xml&${body}")
                .setHeader("CamelHttpUri", constant("{{si.fuseki.endpoint}}"))

                .toD("http4:CamelHttpUri?headerFilterStrategy=#dropHeadersStrategy").id("fusekiCall")
                //.toD("http4://localhost:9080/fuseki/fedora3?output=xml&${body}&headerFilterStrategy=#dropHeadersStrategy").id("fusekiCall")

                .convertBodyTo(String.class)

                .log(LoggingLevel.DEBUG, LOG_NAME, "${routeId} :: Fuseki Result:\n${body}")

                //check the response
                .choice()
                    .when().xpath("boolean(not(//ri:results/*[normalize-space()]))", ns)
                        .throwException(SidoraSolrException.class, "Empty Fuseki result for pid = ${header.pid}")
                    .endChoice()
                .end()

                /**
                 * fetch foxml using HTTPClient
                 */
                /*.process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message out = exchange.getIn();
                        String pid = out.getHeader("pid", String.class);
                        String body = out.getBody(String.class);
                        String fusekiHost = exchange.getContext().resolvePropertyPlaceholders("{{si.fuseki.endpoint}}");

                        //CloseableHttpClient client = HttpClientBuilder.create().build();

                        URI uri = new URIBuilder(fusekiHost)
                                //.setParameter("query", URLEncoder.encode(body, "UTF-8"))
                                .setParameter("query", body)
                                .setParameter("output", "xml")
                                .build();

                        HttpGet httpget = new HttpGet(uri);

                        LOG.debug(logMarker, "httpget uri: {}", httpget.getURI());

                        try (CloseableHttpResponse response = client.execute(httpget)) {
                            HttpEntity entity = response.getEntity();

                            LOG.debug(logMarker, "CloseableHttpResponse response.toString(): {}", response.toString());

                            Integer responseCode = response.getStatusLine().getStatusCode();
                            String statusLine = response.getStatusLine().getReasonPhrase();
                            String entityResponse = EntityUtils.toString(entity, "UTF-8");

                            //set the response
                            out.setBody(entityResponse);
                            LOG.trace(logMarker, "Fuseki response Body: {}", entityResponse);

                            //copy response headers to camel
                            org.apache.http.Header[] headers = response.getAllHeaders();
                            LOG.trace(logMarker, "Fuseki response Headers: {}", Arrays.toString(headers));

                            out.setHeader(Exchange.HTTP_RESPONSE_CODE, responseCode);
                            out.setHeader(Exchange.HTTP_RESPONSE_TEXT, statusLine);

                            if (responseCode != 200) {
                                LOG.error(logMarker, "Fuseki response: " + Exchange.HTTP_RESPONSE_CODE + "= {}, " + Exchange.HTTP_RESPONSE_TEXT + "= {}", responseCode, statusLine);
                                LOG.error(logMarker, "Fuseki response entity: {}", entityResponse);
                            } else {
                                LOG.debug(logMarker, "Fuseki response: " + Exchange.HTTP_RESPONSE_CODE + "= {}, " + Exchange.HTTP_RESPONSE_TEXT + "= {}", responseCode, statusLine);
                            }
                        } catch (Exception e) {
                            throw new Exception("HTTP error sending Fuseki request", e);
                        }
                        //client.close();
                    }
                }).id("fusekiCall")*/
                //TODO: check the response

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} :: ${routeId} :: Fuseki Response - ${body}");


        from("seda:updateReindexDb").routeId("updateReindexDb")
                /*.split().simple("${header.batchJobs}")
                .parallelProcessing(Boolean.parseBoolean(PARALLEL_PROCESSING))
                    .to("sql:{{sql.updateSolrReindexJob}}?dataSource=#dataSourceReIndex&noop=true").id("updateSianctReindexJob")
                .end()*/
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message out = exchange.getIn();
                        List<MySolrJob> batchJobList = out.getHeader("batchJobs", List.class);
                        List<Map<String, Object>> batchSqlList = new ArrayList<Map<String, Object>>();
                        for (MySolrJob job : batchJobList) {
                            Map<String, Object> jobMap = new HashMap<String,Object>();
                            jobMap.put("solrDoc", job.getSolrdoc());
                            jobMap.put("solrStatus", job.getSolrStatus());
                            jobMap.put("endTime", job.getEndTime());
                            jobMap.put("elapsed", String.valueOf(job.getElapsed()));
                            jobMap.put("pid", job.getPid());
                            jobMap.put("index", job.getIndex());
                            batchSqlList.add(jobMap);
                        }
                        out.removeHeader("batchJobs");
                        out.setBody(batchSqlList);
                    }
                })
                .to("sql:{{sql.updateSolrReindexJob}}?dataSource=#dataSourceReIndex&noop=true&batch=true").id("updateSianctReindexJob");

        from("seda:storeReceived").routeId("storeReceivedJobs")
                .aggregate(simple("${header.batch}"), new GroupedExchangeAggregationStrategy())
                .parallelProcessing(Boolean.parseBoolean(PARALLEL_PROCESSING))
                .completionSize(Integer.parseInt(BATCH_SIZE))
                .completionTimeout(new Expression() {
                        @Override
                        public <T> T evaluate(Exchange exchange, Class<T> type) {
                            if (exchange.getIn().getHeader("testTimeout", long.class) != null && exchange.getIn().getHeader("testTimeout", long.class) > 0) {
                                return (T) Long.valueOf(exchange.getIn().getHeader("testTimeout", String.class));
                            } else {
                                return (T) Long.valueOf(COMPLETION_TIMEOUT);
                            }
                        }
                    })
                .to("bean:MyDocService?method=printReceived").id("printStoreReceivedJobs");
    }

    /*
        from("direct:checkObjectActive")
                .setBody().simple("ASK FROM <info:edu.si.fedora#ri> WHERE { <info:fedora/${header.pid}> <info:fedora/fedora-system:def/model#state> <info:fedora/fedora-system:def/model#Active> .}")
                .setBody().groovy("\"query=\" + URLEncoder.encode(request.getBody(String.class));")

                .log(LoggingLevel.DEBUG, LOG_NAME, "${routeId} :: Is Active Query - ${body}")

                .removeHeaders("CamelHttp*")
                .setHeader("CamelHttpMethod", constant("GET"))
                .setHeader("CamelHttpQuery").simple("output=xml&${body}")

                .toD("{{si.fuseki.endpoint}}?headerFilterStrategy=#dropHeadersStrategy")
                .convertBodyTo(String.class)

                .choice()
                    .when().xpath("//ri:boolean/text()", Boolean.class, ns)
                        .setHeader("state").simple("Active")
                    .endChoice()
                .end()

                .log(LoggingLevel.DEBUG, LOG_NAME, "${routeId} :: Object State = ${header.state}");
    */
}
