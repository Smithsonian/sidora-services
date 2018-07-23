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
import edu.si.services.solr.aggregationStrategy.MySolrBatchStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.solr.SolrConstants;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

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

    @PropertyInject(value = "sidora.solr.batch.size", defaultValue ="3")
    private String BATCH_SIZE;
    @PropertyInject(value = "sidora.solr.batch.completionTimeout", defaultValue = "2000")
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

    @Override
    public void configure() throws Exception {
        errorHandler(deadLetterChannel("file:{{karaf.home}}/deadLetter?fileName=error-${routeId}&fileExist=append")
                //.useOriginalMessage()
//                .maximumRedeliveries(Integer.parseInt(redeliveryDelay))
//                .redeliveryDelay(Integer.parseInt(maximumRedeliveries))
//                .backOffMultiplier(Integer.parseInt(backOffMultiplier))
                        .maximumRedeliveries(5)
                        .redeliveryDelay(0)
                        .backOffMultiplier(2)
                .useExponentialBackOff()
                .retryAttemptedLogLevel(LoggingLevel.ERROR)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .logRetryStackTrace(true)
                .logExhausted(true)
                .logExhaustedMessageHistory(true)
                .logRetryStackTrace(true)
                .log("${routeId} ERROR_HANDLER Body:${body}")
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

        from("cxfrs://bean://rsServer?bindingStyle=SimpleConsumer").routeId("SidoraSolrService")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: SidoraSolr: Starting REST Service Request for: ${header.operationName} ... ")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id}: SidoraSolr: REST Request headers: ${headers}")
                .recipientList(simple("direct:${header.operationName}"))
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: SidoraSolr: Finished REST Service Request for: ${header.operationName} ... ");

        from("direct:solrRequest").routeId("SidoraSolrRequest")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Request: Starting processing ...")

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id}: SidoraSolr: Request: PID = ${header.pid}, Index = ${header.solrIndex}, Solr Operation = ${header.solrOperation}")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Request: Finished processing ...");

        from("direct:setFedoraAuth").routeId("SidoraSolrSetFedoraAuth")
                //Set the Authorization header for Fedora HTTP calls
                .setHeader("Authorization").simple("Basic " + Base64.getEncoder().encodeToString((fedoraUser + ":" + fedoraPasword).getBytes("UTF-8")), String.class)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} SidoraSolr: Fedora Authorization: ${header.Authorization}");

        from("activemq:queue:{{sidoraCTSolr.queue}}").routeId("cameraTrapJob")
                .log(LoggingLevel.DEBUG, "${routeId} :: [0] RECEIVED Headers:\n${headers}")
                .log(LoggingLevel.DEBUG, "${routeId} :: [0] RECEIVED PIDAggregation: ${header.PIDAggregation}")
//.stop()

                .split().tokenize(",", "PIDAggregation")
                    .setHeader("pid").simple("${body}")

                    .toD("{{si.fedora.host}}/objects?pid=true&label=true&state=true&ownerId=true&terms=&query=pid~${header.pid}&resultFormat=xml&headerFilterStrategy=#dropHeadersStrategy").id("cameraTrapJobFedoraCall")
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
//.stop()
                    .to("bean:MyBatchService?method=addJob(*, update, {{sidora.solr.default.index}})")
                    .log(LoggingLevel.DEBUG, "${routeId} :: [3] Send to createBatchJob:\n${header.solrJob}")
                    .to("seda:createBatchJob");

        from("activemq:queue:{{solr.apim.update.queue}}").routeId("start")
                .setHeader("origin").xpath("/atom:entry/atom:author/atom:name", String.class, ns)
                .setHeader("dsLabel").xpath("/atom:entry/atom:category[@scheme='fedora-types:dsLabel']/@term", String.class, ns)
                .setBody().simple("${header.pid}")

                .log(LoggingLevel.INFO, "${routeId} :: [1] RECEIVED [ ${header.origin}, ${header.pid}, ${header.methodName}, ${header.dsLabel} ]")
                .log(LoggingLevel.DEBUG, "${routeId} :: [0] RECEIVED ${header.receivedJob}")

//                .setHeader("batch").simple("jmsReceived")
//                .to("seda:storeReceived?waitForTaskToComplete=Never")

                //filter out fedora messages from CT ingest we have a separate pipeline for that
                .filter().simple("${header.origin} != '{{si.fedora.user}}' || ${header.pid} not contains '{{si.ct.namespace}}:'")

                    .log(LoggingLevel.DEBUG, "${routeId} :: [1] After ctUser Filter [ ${header.origin}, ${header.pid}, ${header.methodName}, ${header.dsLabel} ]")

                        // Set the index we are operating and operation
                        .choice()
                            .when().simple("${header.methodName} == 'purgeDatastream' && ${header.pid} contains '{{si.ct.namespace}}:' && ${header.dsLabel} contains 'Observations'")
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
                            .when().simple("${header.methodName} == 'purgeDatastream'")
                                .to("bean:MyBatchService?method=addJob(*, delete, {{sidora.solr.default.index}})")
                                .to("seda:createBatchJob")
                            .endChoice()
                            .otherwise()
                                .to("bean:MyBatchService?method=addJob(*, update, {{sidora.solr.default.index}})")
                                .to("seda:createBatchJob")
                            .endChoice()
                        .end();

        from("seda:createBatchJob").routeId("createBatchJob")
                .log(LoggingLevel.DEBUG, "***************************************************************************************")
                .log(LoggingLevel.DEBUG, "${routeId} [0] :: Received Job:\n${header.solrJob}\n")
//.stop()

                .setHeader("batch").simple("jobsCreated")
                .to("seda:storeReceived?waitForTaskToComplete=Never")

                //Aggregate based on solr operations (add/update/delete) and index (gsearch_solr/gsearch_sianct)
                .aggregate(simple("${header.solrJob.index}"), new MySolrBatchStrategy()).completionSize(Integer.parseInt(BATCH_SIZE)).completionTimeout(Integer.parseInt(COMPLETION_TIMEOUT))

                    .log(LoggingLevel.INFO, "#######################################[ startBatchJob (size = ${header.batchJobs.size} | solrIndex = ${header.solrIndex}) ]################################################\n${header.batchJobs}")

                    .to("direct:createDoc")
//.stop()

                    .setBody().simple("<update>\n<commit>\n${body}\n</commit>\n</update>")
                    .log(LoggingLevel.DEBUG, "BuildCombinedResponse:\n${body}")
//.stop()
                    .to("direct:solr");

        from("direct:createDoc").routeId("createDoc")
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

                    .setHeader("jobInfo").simple("[ ${header.count} of ${header.batchJobs.size} ] ( operation = ${body.solrOperation}, index = ${body.index} )")
                    .log(LoggingLevel.DEBUG, "++++++++++++++++++++++++++++++++++++++++[ ${header.jobInfo} ]+++++++++++++++++++++++++++++++++++++++++++")
                    .log(LoggingLevel.DEBUG, "${routeId} :: Start createDoc ${header.jobInfo}")
                    .setHeader("jobInfo").simple("${header.jobInfo} pid=${body.pid}, origin=${body.origin}, methodName=${body.methodName}, dsLabel=${body.dsLabel}")
//.stop()
                        .choice()
                            .when().simple("${body.solrOperation} == 'delete'")
                                .log(LoggingLevel.INFO, "${routeId} :: Found *** DELETE *** ${header.jobInfo}")
                                .setBody().simple("<delete>${body.pid}</delete>")
                                .endChoice()

                            .when().simple("${body.index} == '{{sidora.sianct.default.index}}'")
                                .log(LoggingLevel.INFO, "${routeId} :: Found *** SIANCT *** ${header.jobInfo}")

                                .setHeader("pid").simple("${body.pid}", String.class)
                                .setHeader("state").simple("${body.state}")

                                /* ****** We will lose the MySolrJob object in the body here ****** */
                                .to("direct:setFusekiQuery").id("fusekiQuery")

                                .log(LoggingLevel.DEBUG, "Send to XSLT\nHeaders:\n${headers}\nBody:\n${body}")

                                .to("xslt:file:{{karaf.home}}/Input/xslt/batch_CT_foxml-to-gsearch_sianct.xslt").id("foxmlToGsearchSianctXSLT")

                                .log(LoggingLevel.DEBUG, "XSLT Body:\n${body}")

                                .setBody().simple("<add>\n${body}</add>")
                            .endChoice()

                            .otherwise()
                                .log(LoggingLevel.INFO, "${routeId} :: Found *** SOLR ONLY *** ${header.jobInfo}")
                                .setHeader("pid").simple("${body.pid}", String.class)

                                .setHeader("CamelHttpMethod", constant("GET"))
                                .setBody().simple("")

                                .toD("http://localhost:8080/fedora/objects/${header.pid}/export?context=public&format=info:fedora/fedora-system:FOXML-1.1&authMethod=Basic&authUsername={{si.fedora.user}}&authPassword={{si.fedora.password}}&headerFilterStrategy=#dropHeadersStrategy").id("getFoxml")

                                .log(LoggingLevel.DEBUG, "Send to batch_foxml-to-gsearch_solr XSLT\nHeaders:\n${headers}\nBody:\n${body}")

                                .to("xslt:file:{{karaf.home}}/Input/xslt/batch_foxml-to-gsearch_solr.xslt").id("foxmlToGsearchSolrXSLT")

                                .log(LoggingLevel.DEBUG, "batch_foxml-to-gsearch_solr output:\n${body}")

                                .setBody().simple("<add>\n${body}</add>")
                            .endChoice()
                        .end().id("createDocEndSplit")
                    .end();

        from("direct:setFusekiQuery").routeId("setFusekiQuery")

                .to("velocity:file:{{karaf.home}}/Input/template/gsearch_sianct-sparql.vsl")

                .log(LoggingLevel.DEBUG, "${routeId} :: Fuseki Query - ${body}")

                .setBody().groovy("\"query=\" + URLEncoder.encode(request.getBody(String.class));")

                .log(LoggingLevel.DEBUG, "${routeId} :: Fuseki Query - ${body}\nHeaders:\n${headers}")
//.stop()
                .setHeader("CamelHttpMethod", constant("GET"))
                .setHeader("CamelHttpQuery").simple("output=xml&${body}")

                .toD("{{si.fuseki.endpoint}}?headerFilterStrategy=#dropHeadersStrategy")
                .convertBodyTo(String.class)
                //TODO: check the response

                .log(LoggingLevel.DEBUG, "${routeId} :: Fuseki Response - ${body}");

        from("direct:solr").routeId("solr")
                .log(LoggingLevel.DEBUG, "${routeId} :: Send Batch to (solrIndex = ${header.solrIndex}) for batch (size = ${header.batchJobs.size}) [ batchJobs: ${header.batchJobs} ]\n${body}")

                .setHeader("CamelHttpMethod", constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("text/xml"))

                .toD("http://localhost:8091/solr/${header.solrIndex}/update?headerFilterStrategy=#dropHeadersStrategy").id("sendToSolr")
                .convertBodyTo(String.class)
                //TODO: check the response

                .log(LoggingLevel.INFO, "${routeId} :: Solr Response - ${body}");

        /*from("direct:solrCommit").routeId("solrCommit")
                .setHeader(SolrConstants.OPERATION, constant(SolrConstants.OPERATION_COMMIT))
                .log(LoggingLevel.DEBUG, "${routeId} :: Send Commit to (solrIndex = ${header.solrIndex}) for batch (${header.index} | size = ${header.batchJobs.size}) [ batchJobs: ${header.batchJobs} ]")
                .toD("solr://localhost:8091/solr/${header.solrIndex}");*/

        from("direct:checkObjectActive")
                .setBody().simple("ASK FROM <info:edu.si.fedora#ri> WHERE { <info:fedora/${header.pid}> <info:fedora/fedora-system:def/model#state> <info:fedora/fedora-system:def/model#Active> .}")
                .setBody().groovy("\"query=\" + URLEncoder.encode(request.getBody(String.class));")

                .log(LoggingLevel.DEBUG, "${routeId} :: Is Active Query - ${body}")

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

        from("seda:storeReceived").routeId("storeReceivedJobs")
                .filter().simple("${header.solrJob} == null")
                //add jobs that were filtered so we can compare results
                .to("bean:MyBatchService?method=addJob(*, ${header.methodName}, ${header.dsLabel})")
                .end()
                .aggregate(simple("${header.batch}"), new GroupedExchangeAggregationStrategy()).completionSize(simple("${header.TOTAL_BATCH_COUNT}")).completionTimeout(2000)
                    .to("bean:MyDocService?method=printReceived").id("printStoreReceivedJobs");
    }

    /*@Override
    public void configure() throws Exception {
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

        //Exception handling
        onException(Exception.class, HttpOperationFailedException.class, SocketException.class)
                .onWhen(exchangeProperty(Exchange.TO_ENDPOINT).regex("^(http|https|cxfrs?(:http|:https)|http4):.*"))
                .useExponentialBackOff()
                .backOffMultiplier(2)
                .redeliveryDelay("{{sidoraSolr.redeliveryDelay}}")
                .maximumRedeliveries("{{sidorasolr.max.http.redeliveries}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .logNewException(true)
                .setHeader("error").simple("[${routeId}] :: SidoraSolr Error reported: ${exception.message}")
                .log(LoggingLevel.ERROR, LOG_NAME, "${header.error}\nCamel Headers:\n${headers}").id("logSidoraSolrHTTPException");

        from("activemq:queue:{{solr.apim.update.queue}}").routeId("SidoraSolrStartProcessing")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Starting JMS processing...").id("logStart")

                //Set the Authorization header for Fedora HTTP calls
                .setHeader("Authorization").simple("Basic " + Base64.getEncoder().encodeToString((fedoraUser + ":" + fedoraPasword).getBytes("UTF-8")), String.class)

                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Fedora JMS Message Found: PID=${header.pid}, Method Name=${header.methodName}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} SidoraSolr: Fedora JMS Message Headers: ${headers}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} SidoraSolr: Fedora JMS Message Body: ${body}")

                //Remove unneeded headers that may cause problems later on
                .removeHeaders("User-Agent|CamelHttpCharacterEncoding|CamelHttpPath|CamelHttpQuery|connection|Content-Length|Content-Type|boundary|CamelCxfRsResponseGenericType|org.apache.cxf.request.uri|CamelCxfMessage|CamelHttpResponseCode|Host|accept-encoding|CamelAcceptContentType|CamelCxfRsOperationResourceInfoStack|CamelCxfRsResponseClass|CamelHttpMethod|incomingHeaders|CamelSchematronValidationReport|datastreamValidationXML")


                //TODO: AggregateWithFilter
                // Filter out messages from CT ingest
                // Aggregate on header methodName

                //Filter out Fedora Messages created from the CT Ingest route
                .filter()
                    .simple("${header.origin} == '{{si.fedora.user}}' && " +
                            " ${header.pid} contains '{{si.ct.namespace}}:'")
                    .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Filtered CT JMS mesage [ Origin=${header.origin}, pid=${header.pid}, Method Name=${header.methodName}, dsID=${header.dsID}, dsLabel=${header.dsLabel} ] - No message processing required.").id("logFilteredMessage")
                    .stop()
                .end()

                .setHeader("dsLabel").xpath("/atom:entry/atom:category[@scheme='fedora-types:dsLabel']/@term", String.class, ns)

                //Route Messages to the correct destination
                .choice()
                    .when().simple("${header.methodName} == 'purgeDatastream'")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Purge Datastream Found for PID: ${header.pid}")
                        .to("direct:solrDelete")
                    .endChoice()
                    .otherwise()
                        //Filter Observations for gsearch_scianct index
                        .filter().simple("${header.pid} contains '{{si.ct.namespace}}:' && ${header.dsLabel} contains 'Observations'")
                            .to("direct:updateScianct")
                        .end()
                        .to("direct:updateSolr")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Finished processing Fedora JMS Message for PID: ${header.pid}.")
                    .endChoice()
                .end()
                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Finished JMS processing...");


        from("direct:solrDelete").routeId("SidoraSolrStartDelete")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Start Solr Delete for PID = ${header.pid}")

                .setBody().simple("${header.pid}")
                .setHeader(SolrConstants.OPERATION, constant(SolrConstants.OPERATION_DELETE_BY_ID))

                // gsearch_scianct index delete
                .filter().simple("${header.pid} contains '{{si.ct.namespace}}:' && ${header.dsLabel} contains 'Observations'")
                    .setHeader("solrURL").simple("{{solr.host}}:{{solr.port}}/solr/{{gsearch_scianct.index}}")
                    .toD("solr://${header.solrURL}")

                    // commit for gsearch_scianct
                    .setHeader(SolrConstants.OPERATION,constant(SolrConstants.OPERATION_COMMIT))
                    .toD("solr://${header.solrURL}")

                    .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Finished Solr Delete PID = ${header.pid} from {{gsearch_scianct.index}}")
                .end()

                // gsearch_solr index delete
                .setBody().simple("${header.pid}")
                .setHeader(SolrConstants.OPERATION, constant(SolrConstants.OPERATION_DELETE_BY_ID))
                .setHeader("solrURL").simple("{{solr.host}}:{{solr.port}}/solr/{{gsearch_solr.index}}")
                .toD("solr://${header.solrURL}")

                // commit for gsearch_solr
                .setHeader(SolrConstants.OPERATION,constant(SolrConstants.OPERATION_COMMIT))
                .toD("solr://${header.solrURL}")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Finished Solr Delete PID = ${header.pid} from {{gsearch_scianct.index}}");

        from("direct:updateSolr").routeId("SidoraSolrStartUpdate")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Start Solr Update for PID = ${header.pid}")

                .setBody().simple("${header.pid}")
                .setHeader(SolrConstants.OPERATION, constant(SolrConstants.OPERATION_DELETE_BY_ID))

                // gsearch_scianct index delete
                .filter().simple("${header.pid} contains '{{si.ct.namespace}}:' && ${header.dsLabel} contains 'Observations'")
                .setHeader("solrURL").simple("{{solr.host}}:{{solr.port}}/solr/{{gsearch_scianct.index}}")
                .toD("solr://${header.solrURL}")

                // commit for gsearch_scianct
                .setHeader(SolrConstants.OPERATION,constant(SolrConstants.OPERATION_COMMIT))
                .toD("solr://${header.solrURL}")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Finished Solr Delete PID = ${header.pid} from {{gsearch_scianct.index}}")
                .end()

                // gsearch_solr index delete
                .setBody().simple("${header.pid}")
                .setHeader(SolrConstants.OPERATION, constant(SolrConstants.OPERATION_DELETE_BY_ID))
                .setHeader("solrURL").simple("{{solr.host}}:{{solr.port}}/solr/{{gsearch_solr.index}}")
                .toD("solr://${header.solrURL}")

                // commit for gsearch_solr
                .setHeader(SolrConstants.OPERATION,constant(SolrConstants.OPERATION_COMMIT))
                .toD("solr://${header.solrURL}")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Finished Solr Delete PID = ${header.pid} from {{gsearch_scianct.index}}");

        from("activemq:queue:{{sidoraCTSolr.queue}}").routeId("SidoraSolrStartProcessingCT")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Starting JMS processing...").id("logStart")

                //Set the Authorization header for Fedora HTTP calls
                .setHeader("Authorization").simple("Basic " + Base64.getEncoder().encodeToString((fedoraUser + ":" + fedoraPasword).getBytes("UTF-8")), String.class)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id}: SidoraSolr: JMS Headers: ${headers}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id}: SidoraSolr: JMS Body: ${body}")

                //Remove unneeded headers that may cause problems later on
                .removeHeaders("User-Agent|CamelHttpCharacterEncoding|CamelHttpPath|CamelHttpQuery|connection|Content-Length|Content-Type|boundary|CamelCxfRsResponseGenericType|org.apache.cxf.request.uri|CamelCxfMessage|CamelHttpResponseCode|Host|accept-encoding|CamelAcceptContentType|CamelCxfRsOperationResourceInfoStack|CamelCxfRsResponseClass|CamelHttpMethod|incomingHeaders|CamelSchematronValidationReport|datastreamValidationXML")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Camera Trap Fedora JMS Message Found: PID=${header.pid}, Method Name=${header.methodName}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} SidoraSolr: Camera Trap Fedora JMS Message Headers: ${headers}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} SidoraSolr: Camera Trap Fedora JMS Message Body: ${body}")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} SidoraSolr: Finished JMS processing...");

    }*/
}
