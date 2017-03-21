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

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author jbirkhimer
 */
public class SidoraMCIServiceRouteBuilder extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(SidoraMCIServiceRouteBuilder.class);

    @PropertyInject(value = "edu.si.mci")
    static private String LOG_NAME;

    @Override
    public void configure() throws Exception {
        Namespaces ns = new Namespaces("ri", "http://www.w3.org/2005/sparql-results#");

        onException(java.net.ConnectException.class,
                java.sql.SQLException.class,
                com.mysql.jdbc.exceptions.jdbc4.CommunicationsException.class,
                org.springframework.jdbc.CannotGetJdbcConnectionException.class,
                edu.si.services.fedorarepo.FedoraObjectNotFoundException.class)
                .useExponentialBackOff()
                .backOffMultiplier("{{mci.backOffMultiplier}}")
                .redeliveryDelay("{{mci.redeliveryDelay}}")
                .maximumRedeliveries("{{mci.maximumRedeliveries}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.WARN)
                .logExhausted(false)
                .handled(true)
                .setBody(simple("Error reported: ${exception.message} - cannot process this message."))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("500"))
                .removeHeaders("mciProjectXML|mciDESCMETA");

        onException(org.apache.camel.component.cxf.CxfOperationException.class)
                .onWhen(simple("${exception.statusCode} not in '200,201'"))
                .useExponentialBackOff()
                .backOffMultiplier("{{mci.backOffMultiplier}}")
                .redeliveryDelay("{{mci.redeliveryDelay}}")
                .maximumRedeliveries("{{mci.maximumRedeliveries}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.WARN)
                .logExhausted(false)
                .handled(true);

        onException(org.xml.sax.SAXParseException.class)
                .useOriginalMessage()
                .handled(true)
                .setBody(simple("Error reported: ${exception.message} - cannot process this message."))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("400"))
                .removeHeaders("mciProjectXML|mciDESCMETA");

        onException(net.sf.saxon.trans.XPathException.class)
                .useOriginalMessage()
                .handled(true)
                .setBody(simple("Error reported: ${exception.message} - cannot process this message."))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("500"))
                .removeHeaders("mciProjectXML|mciDESCMETA");

        onException(MCI_Exception.class)
                .onWhen(simple("${exception.message} contains 'Folder Holder'"))
                .useExponentialBackOff()
                .backOffMultiplier("{{mci.backOffMultiplier}}")
                .redeliveryDelay("{{mci.redeliveryDelay}}")
                .maximumRedeliveries("{{mci.maximumRedeliveries}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.WARN)
                .logExhausted(false)
                .continued(true);

        onException(MCI_Exception.class)
                .useExponentialBackOff()
                .backOffMultiplier("{{mci.backOffMultiplier}}")
                .redeliveryDelay("{{mci.redeliveryDelay}}")
                .maximumRedeliveries("{{mci.maximumRedeliveries}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.WARN)
                .logExhausted(false)
                .handled(true)
                .setBody(simple("Error reported: ${exception.message} - cannot process this message."))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("500"))
                .removeHeaders("mciProjectXML|mciDESCMETA");

        from("cxfrs://bean://rsServer?bindingStyle=SimpleConsumer")
                .routeId("SidoraMCIService")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Starting Sidora MCI Service Request for: ${header.operationName} ... ")
                .recipientList(simple("direct:${header.operationName}"))
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Finished Sidora MCI Service Request for: ${header.operationName} ... ");


        from("direct:addProject")
                .routeId("AddMCIProject")

                .removeHeaders("User-Agent|CamelHttpCharacterEncoding|CamelHttpPath|CamelHttpQuery|CamelHttpUri|connection|Content-Length|Content-Type|boundary|CamelCxfRsResponseGenericType|org.apache.cxf.request.uri|CamelCxfMessage|CamelHttpResponseCode|Host|accept-encoding|CamelAcceptContentType|CamelCxfRsOperationResourceInfoStack|CamelCxfRsResponseClass|CamelHttpMethod")

                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Starting MCI Request - Add MCI Project Concept...")
                .convertBodyTo(String.class, "UTF-8")
                .setHeader("mciProjectXML", simple("${body}", String.class))

                //Get the username from MCI Project XML
                .setHeader("mciFolderHolder").xpath("//Fields/Field[@Name='Folder_x0020_Holder']/substring-after(., 'i:0#.w|us\\')", String.class, ns, "mciProjectXML")
                .log(LoggingLevel.INFO, "Found MCI Folder Holder: ${header.mciFolderHolder}")

                .to("direct:findFolderHolderUserPID")

                .choice()
                    .when().simple("${header.mciOwnerPID} == null")
                        .setHeader("mciFolderHolder").simple("{{mci.default.owner.name}}", String.class)
                        .log(LoggingLevel.WARN, LOG_NAME, "Folder Holder User PID Not Found!!! Retry Finding User PID with default MCI User = ${header.mciFolderHolder}")
                        .to("direct:findDefaultMCIUserPID")
                .end()

                .setBody().simple("${header.mciOwnerPID}", String.class).id("setOwnerPID")

                //Check if Parent User Object (Folder Holder) Exists, else error and quit.
                .to("direct:FindObjectByPIDPredicate")
                .choice()
                    .when().simple("${body} == true")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id}: Root object exists.")
                        .setHeader("CamelFedoraPid", simple("${header.parentId}"))
                        // Add the project
                        .setBody().simple("${header.mciProjectXML}", String.class)
                        .to("direct:mciCreateConcept")
                    .endChoice()
                    .otherwise()
                        .log(LoggingLevel.WARN, LOG_NAME, "${id}: Root object does not exist.")
                        .throwException(new IllegalArgumentException("Root object does not exist."))
                    .end()
                .removeHeaders("mciProjectXML|mciDESCMETA")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Finished MCI Request - Add MCI Project Concept...");

        from("direct:findFolderHolderUserPID").routeId("MCIFindFolderHolderUserPID").errorHandler(noErrorHandler())
                .toD("sql:{{sql.find.mci.user.pid}}").id("queryFolderHolder")
                .log(LoggingLevel.DEBUG, LOG_NAME, "Drupal db SQL Query Result Body: ${body}")
                .choice()
                .when().simple("${body.size} == 0")
                    .log(LoggingLevel.WARN, LOG_NAME, "Folder Holder User PID Not Found!!!")
                    .throwException(MCI_Exception.class, "Folder Holder User PID Not Found!!!")
                .endChoice()
                .when().simple("${body[0][name]} == ${header.mciFolderHolder}")
                    .log(LoggingLevel.INFO, LOG_NAME, "Folder Holder '${header.mciFolderHolder}' User PID Found!!! MCI Folder Holder User PID = ${body[0][user_pid]}")
                    .setHeader("mciOwnerPID").simple("${body[0][user_pid]}")
                .endChoice();

        from("direct:findDefaultMCIUserPID").routeId("MCIFindDefaultUserPID").errorHandler(noErrorHandler())
                .toD("sql:{{sql.find.mci.user.pid}}").id("queryDefaultUser")
                .log(LoggingLevel.DEBUG, LOG_NAME, "Drupal db SQL Query Result Body: ${body}")
                .choice()
                    .when().simple("${body.size} == 0")
                    .log(LoggingLevel.WARN, LOG_NAME, "Default MCI User PID Not Found!!!}")
                    .throwException(MCI_Exception.class, "Default MCI User PID Not Found!!!")
                .endChoice()
                .when().simple("${body[0][name]} == ${header.mciFolderHolder}")
                    .log(LoggingLevel.INFO, LOG_NAME, "Default Folder Holder '${header.mciFolderHolder}' User PID Found!!! MCI Folder Holder User PID = ${body[0][user_pid]}")
                    .setHeader("mciOwnerPID").simple("${body[0][user_pid]}")
                .endChoice();


        from("direct:mciCreateConcept").routeId("MCICreateConcept")
                .toD("xslt:file:{{karaf.home}}/Input/xslt/MCIProjectToSIdoraProject.xsl?saxon=true").id("xsltMCIProjectToSIdoraProject")
                .log(LoggingLevel.INFO, "Transform Successful")
                .setHeader("mciDESCMETA", simple("${body}", String.class))
                .setHeader("mciLabel").xpath("//SIdoraConcept/primaryTitle/titleText/text()", String.class, "mciDESCMETA")

                //Create the MCI Project Concept
                .toD("fedora:create?pid=null&owner=${header.mciFolderHolder}&namespace=si&label=${header.mciLabel}")

                //Add the RELS=EXT Datastream
                .toD("velocity:file:{{karaf.home}}/Input/templates/MCIResourceTemplate.vsl").id("velocityMCIResourceTemplate")
                .toD("fedora:addDatastream?name=RELS-EXT&type=application/rdf+xml&group=X&dsLabel=RDF%20Statements%20about%20this%20object&versionable=false")

                //Add the SIDORA Datastream
                .toD("velocity:file:{{karaf.home}}/Input/templates/MCISidoraTemplate.vsl").id("velocityMCISidoraTemplate")
                .toD("fedora:addDatastream?name=SIDORA&type=text/xml&group=X&dsLabel=SIDORA%20Record&versionable=true")

                //Add the DESCMETA Datastream
                .setBody().simple("${header.mciDESCMETA}", String.class)
                .toD("fedora:addDatastream?name=DESCMETA&type=text/xml&group=X&dsLabel=DESCMETA%20Record&versionable=true")

                //Update the DC datastream
                .toD("xslt:file:{{karaf.home}}/Input/xslt/SIdoraConcept2DC.xsl?saxon=true").id("xsltSIdoraConcept2DC")
                .toD("fedora:addDatastream?name=DC&type=text/xml&group=X")

                //Add the MCI Project XMl to OBJ Datastream
                .setBody().simple("${header.mciProjectXML}", String.class)
                .toD("fedora:addDatastream?name=OBJ&type=text/xml&group=X&dsLabel=${header.mciLabel}&versionable=true")

                //Create the Parent Child Relationship
                .toD("fedora:hasConcept?parentPid=${header.mciOwnerPID}&childPid=${header.CamelFedoraPid}")

                .setHeader("ProjectPID", simple("$header.CamelFedoraPid}"))
                .setBody().simple("OK :: Created PID: ${header.CamelFedoraPid} for Parent PID: ${header.mciOwnerPID}");

        from("direct:FindObjectByPIDPredicate")
                .routeId("MCIFindObjectByPIDPredicate")
                .setBody().simple("ASK FROM <info:edu.si.fedora#ri>{<info:fedora/${body}> ?p ?o .}")
                .setBody().groovy("\"query=\" + URLEncoder.encode(request.getBody(String.class));")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id}: Find Query - ${body}")
                .setHeader("CamelHttpMethod", constant("GET"))
                .toD("cxfrs:{{si.fuseki.endpoint}}?output=xml&${body}&headerFilterStrategy=#dropHeadersStrategy")
                .convertBodyTo(String.class)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id}: Find Query Result - ${body}")
                .setBody(XPathBuilder.xpath("//ri:boolean/text()", String.class).namespaces(ns).logNamespaces())
                .choice()
                    .when().simple("${body} == false")
                        .throwException(edu.si.services.fedorarepo.FedoraObjectNotFoundException.class, "The fedora object not found")
                .end()
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id}: Find Object By PID - ${body}.")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Finished find object by PID.");








    }
}
