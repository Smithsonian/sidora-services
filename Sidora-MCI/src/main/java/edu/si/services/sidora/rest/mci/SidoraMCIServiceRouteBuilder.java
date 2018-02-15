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

package edu.si.services.sidora.rest.mci;

import edu.si.services.fedorarepo.FedoraObjectNotFoundException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XsltBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;


/**
 * @author jbirkhimer
 */
public class SidoraMCIServiceRouteBuilder extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(SidoraMCIServiceRouteBuilder.class);

    @PropertyInject(value = "edu.si.mci")
    static private String LOG_NAME;

    @PropertyInject(value = "mci.retryAttemptedLogLevel")
    static private String retryAttemptedLogLevel;
    @PropertyInject(value = "mci.retriesExhaustedLogLevel")
    static private String retriesExhaustedLogLevel;

    @Override
    public void configure() throws Exception {
        Namespaces ns = new Namespaces("ri", "http://www.w3.org/2005/sparql-results#");

        //Continue on database exceptions in AddMCIProject route
        onException(java.sql.SQLException.class,
                com.mysql.jdbc.exceptions.jdbc4.CommunicationsException.class,
                org.springframework.jdbc.CannotGetJdbcConnectionException.class)
                .onWhen(simple("${routeId} == 'AddMCIProject'"))
                .maximumRedeliveries("3")
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .logExhausted(false)
                .continued(true)
                .log(LoggingLevel.ERROR, LOG_NAME, "[${routeId}] :: correlationId = ${header.correlationId} :: Error reported: ${exception.message} - cannot process this message.");

        //Send Response for validation exceptions in AddMCIProject
        onException(org.apache.camel.ValidationException.class, net.sf.saxon.trans.XPathException.class)
                .onWhen(simple("${routeId} == 'AddMCIProject'"))
                .handled(true)
                .setHeader("error").simple("[${routeId}] :: correlationId = ${header.correlationId} :: Error reported: ${exception.message} - cannot process this message.")
                .log(LoggingLevel.ERROR, LOG_NAME, "${header.error}")
                .to("log:{{edu.si.mci}}?showAll=true&maxChars=100000&multiline=true&level=DEBUG")
                .toD("sql:{{sql.errorProcessingRequest}}?dataSource=requestDataSource").id("onExceptionAddValidationErrorsToDB")
                .setBody(simple("${header.error}"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("400"))
                .removeHeaders("mciProjectXML|mciProjectDESCMETA|mciResourceDESCMETA|error");

        //Retries for all exceptions after response has been sent
        onException(java.net.ConnectException.class,
                org.xml.sax.SAXParseException.class,
                net.sf.saxon.trans.XPathException.class,
                java.sql.SQLException.class,
                com.mysql.jdbc.exceptions.jdbc4.CommunicationsException.class,
                org.springframework.jdbc.CannotGetJdbcConnectionException.class,
                edu.si.services.fedorarepo.FedoraObjectNotFoundException.class)
                .useExponentialBackOff()
                .backOffMultiplier("{{mci.backOffMultiplier}}")
                .redeliveryDelay("{{mci.redeliveryDelay}}")
                .maximumRedeliveries("{{mci.maximumRedeliveries}}")
                .retryAttemptedLogLevel(LoggingLevel.valueOf(retryAttemptedLogLevel))
                .retriesExhaustedLogLevel(LoggingLevel.valueOf(retriesExhaustedLogLevel))
                .logExhausted("{{mci.logExhausted}}")
                .setHeader("error").simple("[${routeId}] :: correlationId = ${header.correlationId} :: Error reported: ${exception.message} - cannot process this message.")
                .log(LoggingLevel.ERROR, LOG_NAME, "${header.error}")
                .toD("sql:{{sql.errorProcessingRequest}}?dataSource=requestDataSource").id("processProjectOnExceptionSQL");

        //Retry and continue when Folder Holder is not found in the Drupal dB
        onException(MCI_Exception.class)
                .useExponentialBackOff()
                .backOffMultiplier("{{mci.backOffMultiplier}}")
                .redeliveryDelay("{{mci.redeliveryDelay}}")
                .maximumRedeliveries("{{mci.maximumRedeliveries}}")
                .retryAttemptedLogLevel(LoggingLevel.valueOf(retryAttemptedLogLevel))
                .retriesExhaustedLogLevel(LoggingLevel.valueOf(retriesExhaustedLogLevel))
                .logExhausted(false)
                .continued(true)
                .setHeader("mciOwnerPID").simple("{{mci.default.owner.pid}}")
                .log(LoggingLevel.WARN, LOG_NAME, "${exception.message} :: Using Default User PID ${header.mciOwnerPID}!!!").id("MCI_ExceptionOnException");



        from("cxfrs://bean://rsServer?bindingStyle=SimpleConsumer")
                .routeId("SidoraMCIService")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Starting Sidora MCI Service Request for: ${header.operationName} ... ")
                .recipientList(simple("direct:${header.operationName}"))
                .removeHeaders("mciProjectXML|mciProjectDESCMETA|mciResourceDESCMETA|error")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Finished Sidora MCI Service Request for: ${header.operationName} ... ");

        from("direct:addProject")
                .routeId("AddMCIProject")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Starting MCI Request - Add MCI Project...")

                .convertBodyTo(String.class)
                .setHeader("incomingHeaders").simple("${headers}", String.class)
                .setHeader("mciProjectXML", simple("${body}", String.class))
                //.setHeader("correlationId").simple(correlationId)
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        //Generating Random UUID for CorrelationId
                        exchange.getIn().setHeader("correlationId", UUID.randomUUID().toString());
                    }
                })

                .log(LoggingLevel.INFO, LOG_NAME, "${id} Using correlationId = ${headers.correlationId}")

                //Add the request to the database (on exception log and continue)
                .toD("sql:{{sql.addRequest}}?dataSource=requestDataSource").id("createRequest")

                .setBody().simple("${header.mciProjectXML}", String.class)

                //Validate the incoming XML and do the transform (on exception log, update dB, and send error in response)
                .to("validator:file:{{karaf.home}}/Input/schemas/MCIProjectSchema.xsd").id("MCIProjectSchemaValidation")
                .toD("xslt:file:{{karaf.home}}/Input/xslt/MCIProjectToSIdoraProject.xsl?saxon=true").id("xsltMCIProjectToSIdoraProject")
                .log(LoggingLevel.DEBUG, "Transform Successful")
                .setHeader("mciProjectDESCMETA", simple("${body}", String.class))

                //Remove unneeded headers that may cause problems later on
                .removeHeaders("User-Agent|CamelHttpCharacterEncoding|CamelHttpPath|CamelHttpQuery|connection|Content-Length|Content-Type|boundary|CamelCxfRsResponseGenericType|org.apache.cxf.request.uri|CamelCxfMessage|CamelHttpResponseCode|Host|accept-encoding|CamelAcceptContentType|CamelCxfRsOperationResourceInfoStack|CamelCxfRsResponseClass|CamelHttpMethod|incomingHeaders")

                //Process the MCI Project Asynchronously
                .to("seda:processProject?waitForTaskToComplete=Never").id("sedaProcessProject")

                //Send OK response for valid requests
                .setBody().simple("OK :: Created :: CorrelationId: ${header.correlationId}")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Finished MCI Request - Add MCI Project...");

        from("seda:processProject").routeId("ProcessMCIProject")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Starting MCI Request - Process MCI Project...")

                .toD("sql:{{sql.consumeRequest}}?dataSource=requestDataSource").id("consumeRequest")

                //Get the username from MCI Project XML
                .setHeader("mciFolderHolder").xpath("//Fields/Field[@Name='Folder_x0020_Holder']/substring-after(., 'i:0#.w|us\\')", String.class, ns, "mciProjectXML").id("folderHolderXpath")
                .log(LoggingLevel.DEBUG, "Found MCI Folder Holder: ${header.mciFolderHolder}")

                .to("direct:findFolderHolderUserPID").id("findFolderHolderUserPID")

                .setBody().simple("${header.mciOwnerPID}", String.class).id("setOwnerPID")

                //Check if Parent User Object (Folder Holder) Exists, else error and quit.
                .to("direct:FindObjectByPIDPredicate").id("FindObjectByPIDPredicate")

                .choice()
                    .when().simple("${body} == true")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id}: Root object exists.")
                        .to("direct:mciCreateConcept").id("mciCreateConcept")
                    .endChoice()
                    .otherwise()
                        .log(LoggingLevel.WARN, LOG_NAME, "${id}: Root object does not exist.")
                        .throwException(new IllegalArgumentException("Root object does not exist."))
                    .end()
                .toD("sql:{{sql.completeRequest}}?dataSource=requestDataSource").id("completeRequest")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Finished MCI Request - Successfully Processed MCI Project Request :: conceptPID = ${header.projectPID}, resourcePID = ${header.projectResourcePID}  :: correlationId = ${header.correlationId}");

        from("direct:findFolderHolderUserPID").routeId("MCIFindFolderHolderUserPID").errorHandler(noErrorHandler())
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Starting MCI Request - Find MCI Folder Holder User PID...")
                .toD("sql:{{sql.find.mci.user.pid}}?dataSource=drupalDataSource").id("queryFolderHolder")
                .log(LoggingLevel.DEBUG, LOG_NAME, "Drupal db SQL Query Result Body: ${body}")
                .choice()
                    .when().simple("${body.size} != 0 && ${body[0][user_pid]} != null")
                        .log(LoggingLevel.INFO, LOG_NAME, "Folder Holder '${header.mciFolderHolder}' User PID Found!!! MCI Folder Holder User PID = ${body[0][user_pid]}")
                        .setHeader("mciOwnerPID").simple("${body[0][user_pid]}")
                    .endChoice()
                    .otherwise()
                        .setBody().simple("[${routeId}] :: correlationId = ${header.correlationId} :: Folder Holder ${header.mciFolderHolder} User PID Not Found!!!")
                        .log(LoggingLevel.WARN, LOG_NAME, "${body}")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                throw new MCI_Exception(exchange.getIn().getBody(String.class));
                            }
                        }).id("throwMCIException")
                        //.throwException(MCI_Exception.class, "Folder Holder User PID Not Found!!!").id("throwMCIException")
                    .end()
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Finished MCI Request - Find MCI Folder Holder User PID...");

        from("direct:mciCreateConcept").routeId("MCICreateConcept")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Starting MCI Request - Create MCI Project Concept...")

                .setHeader("mciLabel").xpath("//SIdoraConcept/primaryTitle/titleText/text()", String.class, ns, "mciProjectDESCMETA")
                .setHeader("mciLabel").groovy("URLEncoder.encode(request.headers.mciLabel)")

                //Create the MCI Project Concept
                .toD("fedora:create?pid=null&owner=${header.mciFolderHolder}&namespace=si&label=${header.mciLabel}")
                .setHeader("projectPID", simple("${header.CamelFedoraPid}"))

                //Create the Parent Child Relationship
                .toD("fedora:hasConcept?parentPid=${header.mciOwnerPID}&childPid=${header.CamelFedoraPid}")

                //Add the SIDORA Datastream
                .toD("velocity:file:{{karaf.home}}/Input/templates/MCIProjectSidoraTemplate.vsl").id("velocityMCIProjectSidoraTemplate")
                .toD("fedora:addDatastream?name=SIDORA&type=text/xml&group=X&dsLabel=SIDORA%20Record&versionable=true")

                //Add the DESCMETA Datastream
                .setBody().simple("${header.mciProjectDESCMETA}", String.class)
                .toD("fedora:addDatastream?name=DESCMETA&type=text/xml&group=X&dsLabel=DESCMETA%20Record&versionable=true")

                //Update the DC datastream
                .toD("xslt:file:{{karaf.home}}/Input/xslt/MCIProjectSIdoraConcept2DC.xsl?saxon=true").id("xsltProjectSIdoraConcept2DC")
                .toD("fedora:addDatastream?name=DC&type=text/xml&group=X")

                //Add the MCI Project XMl as a resource
                .to("direct:mciCreateResource").id("mciCreateResource")

                .setHeader("CamelFedoraPid", simple("${header.projectPID}"))

                //Add the RELS=EXT Datastream
                .toD("velocity:file:{{karaf.home}}/Input/templates/MCIProjectTemplate.vsl").id("velocityMCIProjectTemplate")
                .toD("fedora:addDatastream?name=RELS-EXT&type=application/rdf+xml&group=X&dsLabel=RDF%20Statements%20about%20this%20object&versionable=false")

                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Finished MCI Request - Create MCI Project Concept - PID = ${header.projectPID} :: correlationId = ${header.correlationId}");

        from("direct:mciCreateResource").routeId("MCICreateResource")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Starting MCI Request - Create MCI Project Resource...")

                //Create the MCI Project Concept
                .toD("fedora:create?pid=null&owner=${header.mciFolderHolder}&namespace=si&label=${header.mciLabel}")
                .setHeader("projectResourcePID", simple("${header.CamelFedoraPid}"))

                //Add the MCI Project XMl to OBJ Datastream
                .setBody().simple("${header.mciProjectXML}", String.class)
                .toD("fedora:addDatastream?name=OBJ&type=text/xml&group=X&dsLabel=OBJ&versionable=true")

                //Add the DESCMETA Datastream
                .toD("xslt:file:{{karaf.home}}/Input/xslt/MCIProjectToSIdoraGeneralResource.xsl?saxon=true").id("xsltMCIProjectToSIdoraGeneralResource")
                .toD("fedora:addDatastream?name=DESCMETA&type=text/xml&group=X&dsLabel=DESCMETA%20Record&versionable=true")

                //Update the DC datastream
                .toD("xslt:file:{{karaf.home}}/Input/xslt/MCIProjectSIdoraConcept2DC.xsl?saxon=true").id("xsltResourceSIdoraConcept2DC")
                .toD("fedora:addDatastream?name=DC&type=text/xml&group=X")

                //Add the RELS=EXT Datastream
                .toD("velocity:file:{{karaf.home}}/Input/templates/MCIResourceTemplate.vsl").id("velocityMCIResourceTemplate")
                .toD("fedora:addDatastream?name=RELS-EXT&type=application/rdf+xml&group=X&dsLabel=RDF%20Statements%20about%20this%20object&versionable=false")

                //TODO: update the derivatives route to create FITS datastream for si:genericCModel
                //Create the FITS datastream
                .setBody().simple("${header.mciProjectXML}", String.class)
                .to("direct:addFITSDataStream")

                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Finished MCI Request - Create MCI Project Resource - PID = ${header.projectResourcePID} :: correlationId = ${header.correlationId}");

        from("direct:addFITSDataStream").routeId("MCIProjectAddFITSDataStream")
                .log(LoggingLevel.INFO, LOG_NAME, "Started processing FITS...")

                .to("file://staging/")

                .setHeader("CamelHttpMethod", constant("GET"))
                .setHeader("CamelHttpQuery", simple("file={{karaf.home}}/${header.CamelFileNameProduced}"))
                .toD("cxfrs:{{si.fits.host}}/examine?headerFilterStrategy=#dropHeadersStrategy").id("fitsRequest")
                .convertBodyTo(String.class, "UTF-8")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} FITS Web Service Response Body:\\n${body}")

                .choice()
                    .when().simple("${header.CamelHttpResponseCode} == 200")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "FITS Web Service Response Body: ${body}")
                        .convertBodyTo(String.class)
                        .to("fedora:addDatastream?name=FITS&type=text/xml&dsLabel=FITS%20Generated%20Image%20Metadata&group=X&versionable=false")
                    .endChoice()
                    .otherwise()
                        .log(LoggingLevel.WARN, LOG_NAME, "FITS processing failed. PID: ${headers.CamelFedoraPid}  Error Code: ${headers.CamelHttpResponseCode}")
                .end()
                .recipientList().simple("exec:rm?args=-f ${header.CamelFileNameProduced}")
                .choice()
                    .when().simple("${headers.CamelExecExitValue} != 0")
                        .log(LoggingLevel.WARN, LOG_NAME, "Unable to delete working file. Filename: ${headers.CamelFileNameProduced}")
                .end()
                .log(LoggingLevel.INFO, LOG_NAME, "Finished processing FITS...");


        from("direct:FindObjectByPIDPredicate")
                .routeId("MCIFindObjectByPIDPredicate")

                .setBody().simple("ASK FROM <info:edu.si.fedora#ri>{<info:fedora/${body}> ?p ?o .}")
                .setBody().groovy("\"query=\" + URLEncoder.encode(request.getBody(String.class));")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id}: Find Query - ${body}")

                .setHeader("CamelHttpMethod", constant("GET"))
                .setHeader("CamelHttpQuery").simple("output=xml&${body}")
                .toD("cxfrs:{{si.fuseki.endpoint}}?headerFilterStrategy=#dropHeadersStrategy")
                .convertBodyTo(String.class)

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id}: Find Query Result - ${body}")

                .setBody().xpath("//ri:boolean/text()", String.class, ns)

                .choice()
                    .when().simple("${body} == false")
                        .setBody().simple("The fedora object '${header.mciOwnerPID}' not found!!!")
                        .log(LoggingLevel.ERROR, LOG_NAME, "${body}")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                throw new FedoraObjectNotFoundException(exchange.getIn().getBody(String.class));
                            }
                        })
                        //.throwException(edu.si.services.fedorarepo.FedoraObjectNotFoundException.class, "The fedora object not found")
                .end()

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id}: Find Object By PID - ${body}.")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Finished find object by PID.");
    }
}
