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

import edu.si.services.solr.aggregationStrategy.MySolrBatchStrategy;
import edu.si.services.solr.aggregationStrategy.MySolrReindexAggregationStrategy;
import edu.si.services.solr.aggregationStrategy.MySolrUpdateStrategy;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.DirectXmlRequest;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

/**
 * @author jbirkhimer
 */
public class SidoraSolrRouteBuilder extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(SidoraSolrRouteBuilder.class);

    @PropertyInject(value = "edu.si.solr")
    static private String LOG_NAME;

    @PropertyInject(value = "si.fedora.user")
    private String fedoraUser;

    @PropertyInject(value = "si.fedora.password")
    private String fedoraPasword;

    @PropertyInject(value = "sidora.solr.batch.size", defaultValue ="1000")
    private String BATCH_SIZE;
    @PropertyInject(value = "sidora.solr.batch.completionTimeout", defaultValue = "300000")
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
                Integer offset = (Integer) exchange.getIn().getHeader("offset");
                Integer limit = Integer.valueOf(exchange.getContext().resolvePropertyPlaceholders("{{sidora.solr.fuseki.limit}}"));
                if (offset == null) {
                    offset = 0;
                } else {
                    offset += limit;
                }
                exchange.getIn().setHeader("offset", offset);
            }
        };
        return offset;
    }

    @Override
    public void configure() throws Exception {
        /*errorHandler(deadLetterChannel("file:{{karaf.home}}/deadLetter?fileName=error-${routeId}&fileExist=append")
                //.useOriginalMessage()
                .maximumRedeliveries(Integer.parseInt(redeliveryDelay))
                .redeliveryDelay(Integer.parseInt(maximumRedeliveries))
                .backOffMultiplier(Integer.parseInt(backOffMultiplier))
                .useExponentialBackOff()
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .logRetryStackTrace(true)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .logExhausted(true)
                .logExhaustedMessageHistory(true)
                .logStackTrace(true)
                .logHandled(true)
                .log("${routeId} **** ERROR_HANDLER **** FAILURE_ROUTE_ID=${header.CamelFailureRouteId}, FAILURE_ENDPOINT=${header.CamelFailureEndpoint}, TO_ENDPOINT=${header.CamelToEndpoint}"));*/

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

        from("cxfrs://bean://rsServer?bindingStyle=SimpleConsumer").routeId("SidoraSolrService")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: SidoraSolr: Starting REST Service Request for: ${header.operationName} ... ")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id}: SidoraSolr: REST Request headers: ${headers}")
                .recipientList(simple("direct:${header.operationName}"))
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: SidoraSolr: Finished REST Service Request for: ${header.operationName} ... ");

        from("direct:solrRequest").routeId("SidoraSolrRequest")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Request: Starting processing ...")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id}: SidoraSolr: Request: PID = ${header.pid}, Index = ${header.solrIndex}, Solr Operation = ${header.solrOperation}")
                .setBody().simple("Hello World From Sidora Solr [ path = ${header.operationName} ]: pid = ${header.pid}, solrIndex = ${header.solrIndex}, solrOperation = ${header.solrOperation}")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Request: Finished processing ...");

        from("direct:solrDeleteAll").routeId("solrDeleteAll")
                .filter().groovy("request.headers.auth != camelContext.resolvePropertyPlaceholders('{{si.fedora.password}}')")
                    .setBody().simple("You Are Not Authorized To Preform This Operation!!!")
                    .stop()
                .end()

                .setHeader("solrQuery").simple("<update><delete><query>*:*</query></delete></update>")

                .filter().simple("${header.gsearch_sianct} == 'true'")
                    .setHeader("solrIndex").simple("{{sidora.sianct.default.index}}")
                    .setBody().simple("${header.solrQuery}")
                    .log(LoggingLevel.DEBUG, "Delete from {{sidora.sianct.default.index}}. BODY = ${body}")
                    .to("direct:solr")
                    .setHeader("solrResponse").simple("Delete All from {{sidora.sianct.default.index}}\nResponse:\n${body}")
                .end()

                .filter().simple("${header.gsearch_solr} == 'true'")
                    .setHeader("solrIndex").simple("{{sidora.solr.default.index}}")
                    .setBody().simple("${header.solrQuery}")
                    .log(LoggingLevel.DEBUG, "Delete from {{sidora.solr.default.index}}. BODY = ${body}")
                    .to("direct:solr")
                    .setHeader("solrResponse").simple("${header.solrResponse}\nDelete All from {{sidora.solr.default.index}}\nResponse:\n${body}")
                .end()
                .setBody().simple("${header.solrResponse}");

        from("direct:solrDeleteByQuery").routeId("solrDeleteByQuery")
                .filter().groovy("request.headers.auth != camelContext.resolvePropertyPlaceholders('{{si.fedora.password}}')")
                    .setBody().simple("You Are Not Authorized To Preform This Operation!!!")
                    .stop()
                .end()

                .setHeader("solrQuery").simple("<update><delete><query>${header.query}</query></delete></update>")

                .filter().simple("${header.gsearch_sianct} == 'true'")
                    .setHeader("solrIndex").simple("{{sidora.sianct.default.index}}")
                    .setBody().simple("${header.solrQuery}")
                    .log(LoggingLevel.DEBUG, "Delete from {{sidora.sianct.default.index}}. BODY = ${body}")
                    .to("direct:solr")
                    .setHeader("solrResponse").simple("Delete from {{sidora.sianct.default.index}}\nQuery:\n${header.query}\nResponse:\n${body}")
                .end()

                .filter().simple("${header.gsearch_solr} == 'true'")
                    .setHeader("solrIndex").simple("{{sidora.solr.default.index}}")
                    .setBody().simple("${header.solrQuery}")
                    .log(LoggingLevel.DEBUG, "Delete from {{sidora.solr.default.index}}. BODY = ${body}")
                    .to("direct:solr")
                    .setBody().simple("${header.solrResponse}\nDelete from {{sidora.solr.default.index}}\nQuery:\n${header.query}\nResponse:\n${body}")
                .end();

        from("direct:solrReindexAll").routeId("solrReindexAll")
                .filter().groovy("request.headers.auth != camelContext.resolvePropertyPlaceholders('{{si.fedora.password}}')")
                    .setBody().simple("You Are Not Authorized To Preform This Operation!!!")
                    .stop()
                .end()

                //get count for pagination
                .to("velocity:file:{{karaf.home}}/Input/template/fedoraObjectCountFusekiQuery.vsl")
                .to("direct:sianctFusekiQuery").id("reindexAllCountFusekiQuery")

                .setHeader("pidCount").xpath("//ri:binding[@name = 'total']/ri:literal/text()", Integer.class, ns).id("getPidCount")
                .log(LoggingLevel.INFO, "pidCount = ${header.pidCount}")
                .setHeader("reindexCount").simple("0", Integer.class)

                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message out = exchange.getIn();
                        Integer pidCount = out.getHeader("pidCount", Integer.class);
                        Integer limit = Integer.valueOf(exchange.getContext().resolvePropertyPlaceholders("{{sidora.solr.fuseki.limit}}"));
                        Integer totalBatch = (pidCount + limit - 1) / limit;
                        out.setHeader("totalBatch", totalBatch);
                    }
                })

                //pagging over pids from fuseki using offset and limit
                //.loopDoWhile(stopLoopPredicate()) //TODO: only in camel 2.17 and up :(
                .loop().simple("${header.totalBatch}")
                    .process(offsetLoopProcessor())

                    .to("velocity:file:{{karaf.home}}/Input/template/fedoraObjectPagingFusekiQuery.vsl")
                    .to("direct:sianctFusekiQuery").id("reindexAllPagingFusekiQuery")

                    .setHeader("resultSize").xpath("count(//ri:binding[@name = 'pid'])", Integer.class, ns)
                    .log(LoggingLevel.DEBUG, "resultCount =  ${header.resultSize}")

                    .split().tokenizeXML("result")
                        .streaming()
                        .parallelProcessing(Boolean.parseBoolean(PARALLEL_PROCESSING))
                        .aggregationStrategy(new MySolrReindexAggregationStrategy())

                        .log(LoggingLevel.DEBUG, "${routeId} Split Body:\n${body}")

                        .setHeader("pid").xpath("substring-after(//binding[@name = 'pid']/uri , 'info:fedora/')", String.class, ns)
                        .setHeader("origin").xpath("//binding[@name = 'ownerId']/literal", String.class, ns)
                        .setHeader("dsLabel").xpath("//binding[@name = 'label']/literal", String.class, ns)
                        .setHeader("state").xpath("substring-after(//binding[@name = 'state']/uri , 'info:fedora/fedora-system:def/model#')", String.class, ns)
                        .setHeader("methodName").simple("${header.operationName}")

                        .log(LoggingLevel.DEBUG, "${routeId} :: REINDEX [ ${header.origin}, ${header.pid}, ${header.methodName}, ${header.dsLabel}, ${header.state} ]")

                        .filter().simple("${header.pid} contains '{{si.ct.namespace}}:' && ${header.dsLabel} contains 'Observations' && ${header.gsearch_sianct} == 'true'")// && ${header.state} == 'Active'")
                            .log(LoggingLevel.DEBUG, "${routeId} :: **** FOUND OBSERVATION ****")
                            .to("bean:MyBatchService?method=addJob(*, update, {{sidora.sianct.default.index}})")
                            //header is used for http response only
                            .setHeader("reindex_sianct").simple("${header.solrJob}")
                            .to("seda:createBatchJob?waitForTaskToComplete=Never").id("reindexCreateSianctJob")
                        .end()//end filter

                        .filter().simple("${header.gsearch_solr} == 'true'")// && ${header.state} == 'Active'")
                            .to("bean:MyBatchService?method=addJob(*, update, {{sidora.solr.default.index}})")
                            .setHeader("reindex_solr").simple("${header.solrJob}")
                            .to("seda:createBatchJob?waitForTaskToComplete=Never").id("reindexCreateSolrJob")
                        .end()//end filter
                        .log(LoggingLevel.DEBUG, "Processing batch ( ${header.CamelLoopIndex}++ of ${header.totalBatch} ), adding: ${header.CamelSplitIndex}++ of ${header.resultSize} from batch")
                    .end().id("reindexAllSplitEnd")//end split

                    .removeHeaders("reindex_sianct|reindex_solr")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            Message out = exchange.getIn();
                            out.setHeader("reindexCount", out.getHeader("reindexCount", Integer.class) + out.getHeader("resultSize", Integer.class));
                        }
                    })
                    .log(LoggingLevel.INFO, "Reindex Processed batch ( ${header.CamelLoopIndex}++ of ${header.totalBatch} ), pids added: ( ${header.reindexCount} of ${header.pidCount} )")
                .end().id("reindexAllLoopEnd")//end loop

                .setHeader(Exchange.CONTENT_TYPE, constant("text/xml"))
                .to("bean:MyDocService?method=printReindexList").id("printReindexJobs")
                .removeHeaders("*");

        from("activemq:queue:{{sidoraCTSolr.queue}}").routeId("cameraTrapSolrJob")
                .log(LoggingLevel.DEBUG, "${routeId} :: <<<<<<<<<<<<<<<<<<<<<<<[0] RECEIVED >>>>>>>>>>>>>>>>>>>>>\nHeaders:\n${headers}\nBody:${body}")
                .log(LoggingLevel.DEBUG, "${routeId} :: [0] RECEIVED Headers:\n${headers}")
                .log(LoggingLevel.DEBUG, "${routeId} :: [0] RECEIVED PIDAggregation: ${header.PIDAggregation}")

                .split().tokenize(",", "PIDAggregation")
                    .setHeader("pid").simple("${body}")

                .toD("{{si.fedora.host}}/objects?pid=true&label=true&state=true&ownerId=true&query=pid~${header.pid}&resultFormat=xml&headerFilterStrategy=#dropHeadersStrategy").id("cameraTrapJobFedoraCall")
                    .convertBodyTo(String.class)

                    //TODO: check the response
                    .log(LoggingLevel.DEBUG, "${routeId} :: Fedora Data:\n${body}")

                    .setHeader("origin").xpath("//fedora-types:ownerId/text()", String.class, ns)
                    .setHeader("dsLabel").xpath("//fedora-types:label/text()", String.class, ns)
                    .setHeader("state").xpath("//fedora-types:state/text()", String.class, ns)
                    .setHeader("methodName").simple("ctIngest")

                    .log(LoggingLevel.INFO, "${routeId} :: [0] RECEIVED [ ${header.origin}, ${header.pid}, ${header.methodName}, ${header.dsLabel}, ${header.state} ]")
//.stop()

                    .filter().simple("${header.pid} in '${header.ResearcherObservationPID},${header.VolunteerObservationPID},${header.ImageObservationPID}'")
                        .log(LoggingLevel.DEBUG, "${routeId} :: [2] FOUND OBSERVATION")
                        .to("bean:MyBatchService?method=addJob(*, update, {{sidora.sianct.default.index}})")
                        .log(LoggingLevel.DEBUG, "${routeId} :: [2] Send to createBatchJob:\n${header.solrJob}")
                        .to("seda:createBatchJob")
                    .end()

                    .to("bean:MyBatchService?method=addJob(*, update, {{sidora.solr.default.index}})")
                    .log(LoggingLevel.DEBUG, "${routeId} :: [3] Send to createBatchJob:\n${header.solrJob}")
                    .to("seda:createBatchJob");

        from("activemq:queue:{{solr.apim.update.queue}}").routeId("fedoraApimUpdateSolrJob")
                .setHeader("origin").xpath("/atom:entry/atom:author/atom:name", String.class, ns)
                .setHeader("dsLabel").xpath("/atom:entry/atom:category[@scheme='fedora-types:dsLabel']/@term", String.class, ns)
                .setBody().simple("${header.pid}")

                .log(LoggingLevel.INFO, "${routeId} :: [1] RECEIVED [ ${header.origin}, ${header.pid}, ${header.methodName}, ${header.dsLabel} ]")
                .log(LoggingLevel.DEBUG, "${routeId} :: [0] RECEIVED ${header.receivedJob}")

//                .setHeader("batch").simple("jmsReceived")
//                .to("seda:storeReceived?waitForTaskToComplete=Never")

                //filter out fedora messages from CT ingest we have a separate pipeline for that
                .filter().simple("${header.origin} != '{{si.fedora.user}}' && ${header.pid} not contains '{{si.ct.namespace}}:'")

                    .log(LoggingLevel.DEBUG, "${routeId} :: [1] After ctUser Filter [ ${header.origin}, ${header.pid}, ${header.methodName}, ${header.dsLabel} ]")

                        // Set the index we are operating and operation
                        .choice()
                            .when().simple("${header.methodName} in 'purge,purgeObject,purgeDatastream' && ${header.pid} contains '{{si.ct.namespace}}:' && ${header.dsLabel} contains 'Observations'")
                                .to("bean:MyBatchService?method=addJob(*, delete, {{sidora.sianct.default.index}})")
                                .to("seda:createBatchJob")
                                // Also create job for gsearch_solr
                                .to("bean:MyBatchService?method=addJob(*, delete, {{sidora.solr.default.index}})")
                                .to("seda:createBatchJob")
                            .endChoice()
                            .when().simple("${header.pid} contains '{{si.ct.namespace}}:' && ${header.dsLabel} contains 'Observations'")
                                .to("bean:MyBatchService?method=addJob(*, update, {{sidora.sianct.default.index}})")
                                .to("seda:createBatchJob")
                                // Also create job for gsearch_solr
                                .to("bean:MyBatchService?method=addJob(*, update, {{sidora.solr.default.index}})")
                                .to("seda:createBatchJob")
                            .endChoice()
                            .when().simple("${header.methodName} in 'purgeObject,purgeDatastream' || ${header.methodName} contains 'purge'")
                                .to("bean:MyBatchService?method=addJob(*, delete, {{sidora.solr.default.index}})")
                                .to("seda:createBatchJob")
                            .endChoice()
                            .otherwise()
                                .to("bean:MyBatchService?method=addJob(*, update, {{sidora.solr.default.index}})")
                                .to("seda:createBatchJob")
                            .endChoice()
                        .end();

        from("seda:createBatchJob").routeId("createBatchSolrJob")
                .log(LoggingLevel.DEBUG, "***************************************************************************************")
                .log(LoggingLevel.DEBUG, "${routeId} [0] :: Received Job:\n${header.solrJob}\n")

                .setHeader("batch").simple("jobsCreated")
                .to("seda:storeReceived?waitForTaskToComplete=Never")

                //Aggregate based on solr operations (add/update/delete) and index (gsearch_solr/gsearch_sianct)
                .aggregate(simple("${header.solrJob.index}"), new MySolrBatchStrategy()).completionSize(Integer.parseInt(BATCH_SIZE)).completionTimeout(Integer.parseInt(COMPLETION_TIMEOUT))

                    .log(LoggingLevel.DEBUG, "#######################################[ startBatchJob (size = ${header.batchJobs.size} | solrIndex = ${header.solrIndex}) ]################################################\n${header.batchJobs}")

                    .setHeader("startTime").simple(String.valueOf(new Date().getTime()), long.class)

                    .to("direct:createDoc")

                    .setBody().simple("<update>\n<commit>\n<add>\n${body}\n</add>\n</commit>\n</update>")
                    .log(LoggingLevel.DEBUG, "BuildCombinedResponse:\n${body}")

                    .to("direct:solr")

                    //Set end time and solr status for jobs
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            Message out = exchange.getIn();
                            List<MySolrJob> batchJobsList = out.getHeader("batchJobs", List.class);
                            if (batchJobsList != null && batchJobsList.size() > 0) {
                                for (int i = 0; i < batchJobsList.size(); i++) {
                                    batchJobsList.get(i).setEndTime(new Date().getTime());
                                    batchJobsList.get(i).setSolrStatus(out.getBody(String.class));
                                    log.debug("Solr Status for pid = {}, {}, elapse time: ", batchJobsList.get(i).pid, batchJobsList.get(i).solrStatus, String.format("%tT", batchJobsList.get(i).getElapsed() - TimeZone.getDefault().getRawOffset()));
                                }

                                long startTime = batchJobsList.get(0).getStartTime();
                                long endTime = new Date().getTime();
                                long elapsed = endTime - startTime;
                                String elapsedJob0 = String.format("%tT", batchJobsList.get(0).getElapsed() - TimeZone.getDefault().getRawOffset());
                                log.info("Solr Batch Complete: size = {}, index: {}, elapse time: {}", batchJobsList.size(), batchJobsList.get(0).getIndex(), String.format("%tT", elapsed - TimeZone.getDefault().getRawOffset()));
                            }
                        }
                    });

        from("direct:createDoc").routeId("createSolrDoc")
                .log(LoggingLevel.DEBUG, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@[ Start createDoc (size = ${header.batchJobs.size}) ]@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
                .log(LoggingLevel.DEBUG, "${routeId} :: Start createDoc (size = ${header.batchJobs.size}):\nbatchJobs: ${header.batchJobs}\n Body: ${body}")

                //Split a List of MySolrJob objects ex. [MySolrJob1{pid=test.smx.home:23, owner=testCTUser, methodName=ctIngest, dsLabel=Volunteer Observations, state=A, solrOperation=update, index=gsearch_sianct, indexes=[gsearch_sianct]}, MySolrJob2{...}, MySolrJob3{...}]
                .split().simple("${header.batchJobs}").aggregationStrategy(new MySolrUpdateStrategy()).parallelProcessing(Boolean.parseBoolean(PARALLEL_PROCESSING))
                    //put a counter in a header
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            int count = exchange.getProperty("CamelSplitIndex", int.class);
                            exchange.getIn().setHeader("count", count + 1);
                        }
                    })

                    .setHeader("solrJob").simple("${body}")

                    .setHeader("jobInfo").simple("[ ${header.count} of ${header.batchJobs.size} ] ( operation = ${body.solrOperation}, index = ${body.index} )")
                    .log(LoggingLevel.DEBUG, "++++++++++++++++++++++++++++++++++++++++[ ${header.jobInfo} ]+++++++++++++++++++++++++++++++++++++++++++")
                    .log(LoggingLevel.DEBUG, "${routeId} :: Start createDoc ${header.jobInfo}")
                    .setHeader("jobInfo").simple("${header.jobInfo} pid=${body.pid}, origin=${body.origin}, methodName=${body.methodName}, dsLabel=${body.dsLabel}")

                        .choice()
                            .when().simple("${body.solrOperation} == 'delete'")
                                .log(LoggingLevel.DEBUG, "${routeId} :: Found *** DELETE *** ${header.jobInfo}")
                                .setBody().simple("<delete>${body.pid}</delete>")
                            .endChoice()

                            .when().simple("${body.index} == '{{sidora.sianct.default.index}}'")
                                .log(LoggingLevel.DEBUG, "${routeId} :: Found *** SIANCT *** ${header.jobInfo}")

                                .setHeader("pid").simple("${body.pid}", String.class)
                                .setHeader("state").simple("${body.state}")

                                .to("velocity:file:{{karaf.home}}/Input/template/gsearch_sianct-sparql.vsl")
                                .to("direct:sianctFusekiQuery").id("createDocFusekiQuery")

                                .log(LoggingLevel.DEBUG, "Send to XSLT\nHeaders:\n${headers}\nBody:\n${body}")

                                .to("xslt:file:{{karaf.home}}/Input/xslt/batch_CT_foxml-to-gsearch_sianct.xslt").id("foxmlToGsearchSianctXSLT")

                                .log(LoggingLevel.DEBUG, "XSLT Body:\n${body}")
                            .endChoice()

                            .otherwise()
                                .log(LoggingLevel.DEBUG, "${routeId} :: Found *** SOLR ONLY *** ${header.jobInfo}")
                                .setHeader("pid").simple("${body.pid}", String.class)

                                /**
                                 * fetch foxml using camel http component
                                 * (NOTE: causes ConcurrentModificationException b/c the camel component is iterating over headers)
                                 */
                                /*.removeHeaders("CamelHttp*")
                                .setHeader("CamelHttpMethod", constant("GET"))
                                .setHeader("CamelHttpQuery").simple("context=public&format=info:fedora/fedora-system:FOXML-1.1")
                                .setBody().simple("")

                                .setHeader("Authorization").simple("Basic " + Base64.getEncoder().encodeToString((fedoraUser + ":" + fedoraPasword).getBytes("UTF-8")), String.class)

                                //.toD("{{si.fedora.host}}/objects/${header.pid}/export?copyHeaders=false&mapHttpMessageHeaders=false&headerFilterStrategy=#dropHeadersStrategy").id("getFoxml")
                                //.toD("http4://localhost:8080/fedora/objects/${header.pid}/export?copyHeaders=false&mapHttpMessageHeaders=false&headerFilterStrategy=#dropHeadersStrategy").id("getFoxml")
                                //.toD("jetty://localhost:8080/fedora/objects/${header.pid}/export?copyHeaders=false&mapHttpMessageHeaders=false&headerFilterStrategy=#dropHeadersStrategy").id("getFoxml")
                                //.toD("fcrepo:objects/${header.pid}/export?context=public&format=info:fedora/fedora-system:FOXML-1.1").id("getFoxml")*/

                                /**
                                 * fetch foxml using HTTPClient
                                 */
                                .process(new Processor() {
                                    @Override
                                    public void process(Exchange exchange) throws Exception {
                                        Message out = exchange.getIn();
                                        String pid = out.getHeader("pid", String.class);
                                        String fedoraHost = exchange.getContext().resolvePropertyPlaceholders("{{si.fedora.host}}");

                                        CloseableHttpClient client = HttpClientBuilder.create().build();

                                        URI uri = new URIBuilder(fedoraHost + "/objects/" + pid + "/export")
                                                .setParameter("context", "public")
                                                .setParameter("format", "info:fedora/fedora-system:FOXML-1.1")
                                                .build();

                                        HttpGet httpget = new HttpGet(uri);
                                        httpget.setHeader(Exchange.CONTENT_TYPE, "text/xml");
                                        httpget.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((fedoraUser + ":" + fedoraPasword).getBytes("UTF-8")));

                                        LOG.debug("httpget uri: {}", httpget.getURI());

                                        try (CloseableHttpResponse response = client.execute(httpget)) {
                                            HttpEntity entity = response.getEntity();

                                            LOG.debug("CloseableHttpResponse response.toString(): {}", response.toString());

                                            Integer responseCode = response.getStatusLine().getStatusCode();
                                            String statusLine = response.getStatusLine().getReasonPhrase();
                                            String entityResponse = EntityUtils.toString(entity, "UTF-8");

                                            //set the response
                                            out.setBody(entityResponse);
                                            LOG.debug("Fedora response Body: {}", entityResponse);

                                            //copy response headers to camel
                                            org.apache.http.Header[] headers = response.getAllHeaders();
                                            LOG.debug("Fedora response Headers: {}", Arrays.toString(headers));

                                            out.setHeader(Exchange.HTTP_RESPONSE_CODE, responseCode);
                                            out.setHeader(Exchange.HTTP_RESPONSE_TEXT, statusLine);

                                            if (responseCode != 200) {
                                                LOG.error("Fedora response: " + Exchange.HTTP_RESPONSE_CODE + "= {}, " + Exchange.HTTP_RESPONSE_TEXT + "= {}", responseCode, statusLine);
                                                LOG.error("Fedora response entity: {}", entityResponse);
                                            } else {
                                                LOG.debug("Fedora response: " + Exchange.HTTP_RESPONSE_CODE + "= {}, " + Exchange.HTTP_RESPONSE_TEXT + "= {}", responseCode, statusLine);
                                            }
                                        } catch (Exception e) {
                                            throw new Exception("HTTP error sending request", e);
                                        }
                                        client.close();
                                    }
                                }).id("getFoxml")

                                .log(LoggingLevel.DEBUG, "Send to batch_foxml-to-gsearch_solr XSLT\nHeaders:\n${headers}\nBody:\n${body}")

                                .to("xslt:file:{{karaf.home}}/Input/xslt/batch_foxml-to-gsearch_solr.xslt").id("foxmlToGsearchSolrXSLT")

                                .log(LoggingLevel.DEBUG, "batch_foxml-to-gsearch_solr output:\n${body}")
                            .endChoice()
                        .end().id("createDocEndChoice")

                .end().id("createDocEndSplit");

        from("direct:sianctFusekiQuery").routeId("sianctFusekiQuery")
                .log(LoggingLevel.DEBUG, "${routeId} :: Fuseki Query - ${body}")

                //TODO: Watch for ConcurrentModificationException may need to use HTTPClient instead
                /*.setBody().groovy("\"query=\" + URLEncoder.encode(request.getBody(String.class));")
                .log(LoggingLevel.DEBUG, "${routeId} :: Fuseki Query - ${body}\nHeaders:\n${headers}")

                .removeHeaders("CamelHttp*")
                .setHeader("CamelHttpMethod", constant("GET"))
                .setHeader("CamelHttpQuery").simple("output=xml&${body}")
                .toD("{{si.fuseki.endpoint}}?output=xml&${body}&headerFilterStrategy=#dropHeadersStrategy").id("fusekiCall")
                .convertBodyTo(String.class)*/

                /**
                 * fetch foxml using HTTPClient
                 */
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message out = exchange.getIn();
                        String pid = out.getHeader("pid", String.class);
                        String body = out.getBody(String.class);
                        String fusekiHost = exchange.getContext().resolvePropertyPlaceholders("{{si.fuseki.endpoint}}");

                        CloseableHttpClient client = HttpClientBuilder.create().build();

                        URI uri = new URIBuilder(fusekiHost)
                                //.setParameter("query", URLEncoder.encode(body, "UTF-8"))
                                .setParameter("query", body)
                                .setParameter("output", "xml")
                                .build();

                        HttpGet httpget = new HttpGet(uri);

                        LOG.debug("httpget uri: {}", httpget.getURI());

                        try (CloseableHttpResponse response = client.execute(httpget)) {
                            HttpEntity entity = response.getEntity();

                            LOG.debug("CloseableHttpResponse response.toString(): {}", response.toString());

                            Integer responseCode = response.getStatusLine().getStatusCode();
                            String statusLine = response.getStatusLine().getReasonPhrase();
                            String entityResponse = EntityUtils.toString(entity, "UTF-8");

                            //set the response
                            out.setBody(entityResponse);
                            LOG.debug("Fuseki response Body: {}", entityResponse);

                            //copy response headers to camel
                            org.apache.http.Header[] headers = response.getAllHeaders();
                            LOG.debug("Fuseki response Headers: {}", Arrays.toString(headers));

                            out.setHeader(Exchange.HTTP_RESPONSE_CODE, responseCode);
                            out.setHeader(Exchange.HTTP_RESPONSE_TEXT, statusLine);

                            if (responseCode != 200) {
                                LOG.error("Fuseki response: " + Exchange.HTTP_RESPONSE_CODE + "= {}, " + Exchange.HTTP_RESPONSE_TEXT + "= {}", responseCode, statusLine);
                                LOG.error("Fuseki response entity: {}", entityResponse);
                            } else {
                                LOG.debug("Fuseki response: " + Exchange.HTTP_RESPONSE_CODE + "= {}, " + Exchange.HTTP_RESPONSE_TEXT + "= {}", responseCode, statusLine);
                            }
                        } catch (Exception e) {
                            throw new Exception("HTTP error sending Fuseki request", e);
                        }
                        client.close();
                    }
                }).id("fusekiCall")
                //TODO: check the response

                .log(LoggingLevel.DEBUG, "${routeId} :: Fuseki Response - ${body}");

        from("direct:solr").routeId("sidoraSolrUpdate")
                .log(LoggingLevel.DEBUG, "${routeId} :: Send Batch to (solrIndex = ${header.solrIndex}) for batch (size = ${header.batchJobs.size}) [ batchJobs: ${header.batchJobs} ]\n${body}")

                /**
                 * solr update using camel http component
                 * (NOTE: causes ConcurrentModificationException b/c the camel component is iterating over headers)
                 */
                /*.removeHeaders("CamelHttp*")
                .removeHeader("CamelHttpQuery")
                .setHeader("CamelHttpMethod", constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("text/xml"))
                .toD("{{sidora.solr.endpoint}}/${header.solrIndex}/update?headerFilterStrategy=#dropHeadersStrategy&throwExceptionOnFailure=true").id("sendToSolr")*/

                /**
                 * solr update via HTTPClient
                 */
                /*.process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message out = exchange.getIn();
                        String solrIndex = out.getHeader("solrIndex", String.class);
                        String solrHost = exchange.getContext().resolvePropertyPlaceholders("{{sidora.solr.endpoint}}");

                        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

                            URIBuilder builder = new URIBuilder(solrHost + "/" + solrIndex + "/update");
                            URI uri = builder.build();

                            HttpPost httpPost = new HttpPost(uri);
                            httpPost.setHeader(Exchange.CONTENT_TYPE, "text/xml");
                            //pass the body string request in the entity
                            HttpEntity entity = new ByteArrayEntity(out.getBody(String.class).getBytes("UTF-8"));
                            httpPost.setEntity(entity);


                            LOG.debug("httpget uri: {}", httpPost.getURI());

                            try (CloseableHttpResponse response = client.execute(httpPost)) {
                                HttpEntity responseEntity = response.getEntity();

                                LOG.debug("CloseableHttpResponse response.toString(): {}", response.toString());

                                Integer responseCode = response.getStatusLine().getStatusCode();
                                String statusLine = response.getStatusLine().getReasonPhrase();
                                String entityResponse = EntityUtils.toString(responseEntity, "UTF-8");

                                //set the response
                                out.setBody(entityResponse);
                                LOG.info("Solr response Body: {}", entityResponse);

                                //copy response headers to camel
                                Header[] headers = response.getAllHeaders();
                                LOG.info("Solr response Headers: {}", Arrays.toString(headers));

                                out.setHeader(Exchange.HTTP_RESPONSE_CODE, responseCode);
                                out.setHeader(Exchange.HTTP_RESPONSE_TEXT, statusLine);

                                if (responseCode != 200) {
                                    LOG.info("Solr response: " + Exchange.HTTP_RESPONSE_CODE + "= {}, " + Exchange.HTTP_RESPONSE_TEXT + "= {}", responseCode, statusLine);
                                    LOG.error("Solr response entity: {}", entityResponse);
                                } else {
                                    LOG.info("Solr response: " + Exchange.HTTP_RESPONSE_CODE + "= {}, " + Exchange.HTTP_RESPONSE_TEXT + "= {}", responseCode, statusLine);
                                }
                            } catch (Exception e) {
                                throw new Exception("HTTP error sending request", e);
                            }
                        } catch (Exception e) {

                        }
                    }
                }).id("sendToSolr")*/

                /**
                 * solr update via Solrj with rollback on fail
                 */
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message out = exchange.getIn();
                        String solrIndex = out.getHeader("solrIndex", String.class);
                        String solrHost = exchange.getContext().resolvePropertyPlaceholders("{{sidora.solr.endpoint}}");
                        String body = out.getBody(String.class);

                        //Solrj method 1
                        /*try (SolrClient client = new HttpSolrClient.Builder(solrHost + "/" + solrIndex).build()) {
                            ContentStreamUpdateRequest req = new ContentStreamUpdateRequest("/update");
                            req.addContentStream(new ContentStreamBase.StringStream(body, "application/xml"));
                            //req.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);

                            NamedList<Object> result = client.request(req);
                            LOG.info("Result: " + result);
                            Integer statusCode = (Integer)((NamedList)result.get("responseHeader")).get("solrStatus");
                            if (statusCode == 0) {
                                LOG.info("Successful index");
                                client.commit();
                            } else {`
                                client.rollback();
                                throw new Exception("Failed:\n"+result);
                            }
                            out.setBody(result);
                        } catch (Exception e) {
                            throw new Exception("Failed!!!!!");
                        }*/


                        //Solrj method 2
                        try (SolrClient client = new HttpSolrClient.Builder(solrHost + "/" + solrIndex).build()) {
                            DirectXmlRequest dxr = new DirectXmlRequest("/update", body);
                            NamedList<Object> result = client.request(dxr);
                            LOG.debug("Result: " + result);
                            Integer statusCode = (Integer) ((NamedList) result.get("responseHeader")).get("status");
                            if (statusCode == 0) {
                                LOG.debug("Successful Solr Request.\nResult: {}", result);
                                client.commit();
                            } else {
                                LOG.error("Solr update failed rollback!!!\nResult: {}", result);
                                client.rollback();
                                throw new Exception("Failed:\n" + result);
                            }
                            out.setBody(result);
                        } catch (Exception e) {
                            LOG.error("Solr Update Failed ERROR!!!\nBody:\n{}\nException:\n{}", out.getBody(), e.toString());
                            e.printStackTrace();
                            throw new Exception("Failed Solr Update!!!!!");
                        }
                    }
                }).id("sendToSolr")

                .convertBodyTo(String.class)
                //TODO: check the response

                .log(LoggingLevel.DEBUG, "${routeId} :: Solr Response - ${body}");

        from("seda:storeReceived").routeId("storeReceivedJobs")
                .filter().simple("${header.solrJob} == null")
                    //add jobs that were filtered so we can compare results
                    .to("bean:MyBatchService?method=addJob(*, ${header.methodName}, ${header.dsLabel})")
                .end()
                .aggregate(simple("${header.batch}"), new GroupedExchangeAggregationStrategy())
                    .completionSize(simple("${header.TOTAL_BATCH_COUNT}"))
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

                .log(LoggingLevel.DEBUG, "${routeId} :: Is Active Query - ${body}")

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

                .log(LoggingLevel.DEBUG, "${routeId} :: Object State = ${header.state}");
    */
}
