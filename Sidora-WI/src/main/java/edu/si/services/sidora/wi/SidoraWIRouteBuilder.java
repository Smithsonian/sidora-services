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

package edu.si.services.sidora.wi;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.net.HttpCookie;
import java.net.SocketException;
import java.util.*;

/**
 * @author jbirkhimer
 */
public class SidoraWIRouteBuilder extends RouteBuilder {

    @PropertyInject(value = "edu.si.sidora.wi")
    static private String LOG_NAME;
    Marker logMarker = MarkerFactory.getMarker("edu.si.sidora.wi");

    @PropertyInject(value = "si.fedora.user")
    private String fedoraUser;

    @PropertyInject(value = "si.fedora.password")
    private String fedoraPasword;

    @PropertyInject(value = "si.sidora.wi.parallelProcessing", defaultValue = "true")
    private String PARALLEL_PROCESSING;

    @PropertyInject(value = "si.sidora.wi.concurrentConsumers", defaultValue = "20")
    private String CONCURRENT_CONSUMERS;

    @PropertyInject(value = "si.sidora.wi.ct.deployment.queue.size", defaultValue = "5")
    private String CT_PROCESSING_QUEUE_SIZE;

    public Processor cookieProcessor() {
        Processor cookieProcessor = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Message out = exchange.getIn();
                String cookie = null;
                List<String> setCookieList = new ArrayList<>();
                addCookies(exchange.getIn().getHeader("Set-Cookie"), setCookieList);
                addCookies(exchange.getIn().getHeader("Set-Cookie2"), setCookieList);

                List<String> cookieList = new ArrayList<>();
                for (String c : setCookieList) {
                    HttpCookie.parse(c).forEach(httpCookie -> cookieList.add(httpCookie.getName()+"="+httpCookie.getValue()));
                }

                cookie = StringUtils.join(cookieList, "; ");

                log.info("Cookie: {}", cookie);

                out.setHeader("cookie", cookie);
            }
        };

        return cookieProcessor;
    }

    public void addCookies(Object cookieHeader, List<String> cookielist) {
        if (cookieHeader instanceof List) {
            cookielist.addAll((Collection<? extends String>) cookieHeader);
        } else if (cookieHeader instanceof Object[]) {
            Collections.addAll(cookielist, (String[]) cookieHeader);
        } else if (cookieHeader instanceof String) {
            String cookie = ((String) cookieHeader).trim();
            if (cookie.startsWith("[") && cookie.endsWith("]")) {
                // remove the [ ] markers
                cookie = cookie.substring(1, cookie.length() - 1);
                Collections.addAll(cookielist, ((String) cookie).replaceAll("expires=(.*?\\;)", "").split(","));
            } else {
                Collections.addAll(cookielist, ((String) cookie).replaceAll("expires=(.*?\\;)", "").split(","));
            }
        }
    }

    @Override
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
                .redeliveryDelay("{{si.sidora.wi.redeliveryDelay}}")
                .maximumRedeliveries("{{min.sidora.wi.http.redeliveries}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .logNewException(true);

        onException(SidoraWIException.class)
                .useExponentialBackOff()
                .backOffMultiplier(2)
                .redeliveryDelay("{{si.sidora.wi.redeliveryDelay}}")
                .maximumRedeliveries("{{min.sidora.wi.redeliveries}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .logExhausted(true);

        //Retries for all exceptions after response has been sent
        onException(edu.si.services.fedorarepo.FedoraObjectNotFoundException.class)
                .useExponentialBackOff()
                .backOffMultiplier(2)
                .redeliveryDelay(1000)
                .maximumRedeliveries(10)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .logExhausted(true);

        from("activemq:queue:{{sidora.wi.ct.queue}}").routeId("SidoraWIProcessCtMsg")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Starting processing Camera Trap Ingest Message...").id("ctSidoraWIStart")

                //Set the Authorization header for Fedora HTTP calls
                .setHeader("Authorization").simple("Basic " + Base64.getEncoder().encodeToString((fedoraUser + ":" + fedoraPasword).getBytes("UTF-8")), String.class)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Fedora Authorization: ${header.Authorization}")

                //Remove unneeded headers that may cause problems later on
                .removeHeaders("User-Agent|CamelHttpCharacterEncoding|CamelHttpPath|CamelHttpQuery|connection|Content-Length|Content-Type|boundary|CamelCxfRsResponseGenericType|org.apache.cxf.request.uri|CamelCxfMessage|CamelHttpResponseCode|Host|accept-encoding|CamelAcceptContentType|CamelCxfRsOperationResourceInfoStack|CamelCxfRsResponseClass|CamelHttpMethod|incomingHeaders|CamelSchematronValidationReport|datastreamValidationXML")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Received Camera Trap Ingest Message:" +
                        "\nProjectId: ${header.ProjectId}" +
                        "\nSiteId: ${header.SiteId}" +
                        "\nProjectPID: ${header.ProjectPID}" +
                        "\nSubProjectPID: ${header.SubProjectPID}" +
                        "\nPlotPID: ${header.PlotPID}" +
                        "\nSitePID: ${header.SitePID}" +
                        "\nResearcherObservationPID: ${header.ResearcherObservationPID}" +
                        "\nVolunteerObservationPID: ${header.VolunteerObservationPID}" +
                        "\nImageObservationPID: ${header.ImageObservationPID}" +
                        "\nImageCount: ${header.ImageCount}" +
                        "\nPIDAggregation: ${header.PIDAggregation}")

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Camera Trap Ingest Message Headers: ${headers}")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: ***** Delaying for a moment before sending to Seda processing queue *****")
                .delay(1500)

                .to("seda:processCtDeployment?waitForTaskToComplete=Never&size=" + Integer.valueOf(CT_PROCESSING_QUEUE_SIZE)
                        + "&blockWhenFull=true").id("ctStartProcessCtDeployment")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Finished Adding Camera Trap Ingest Message to Seda processing queue");

        from("seda:processCtDeployment?size=" + Integer.valueOf(CT_PROCESSING_QUEUE_SIZE)
                + "&concurrentConsumers=" + Integer.valueOf(CONCURRENT_CONSUMERS))
                .routeId("SidoraWIProcessCtDeployment")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Starting Camera Trap Deployment processing ProjectId: ${header.ProjectId}, SiteId: ${header.SiteId}, SitePID: ${header.SitePID}...")

                .choice()
                    .when().simple("${header.ProjectPID}")
                        .setHeader("PIDAggregation").simple("${header.PIDAggregation},${header.ProjectPID}")
                    .endChoice()
                .end()

                .choice()
                    .when().simple("${header.SubProjectPID}")
                        .setHeader("PIDAggregation").simple("${header.PIDAggregation},${header.SubProjectPID}")
                    .endChoice()
                .end()

                .choice()
                    .when().simple("${header.PlotPID}")
                        .setHeader("PIDAggregation").simple("${header.PIDAggregation},${header.PlotPID}")
                    .endChoice()
                .end()

                .choice()
                    .when().simple("${header.SitePID}")
                        .setHeader("PIDAggregation").simple("${header.PIDAggregation},${header.SitePID}")
                    .endChoice()
                .end()

                .log(LoggingLevel.DEBUG, LOG_NAME, "PIDAggregation list: ${header.PIDAggregation}")

                // use the list of pids that the ct ingest route created and we sent as part of the JMS message
                // We could also get the deployment RELS-EXT and do the same thing or validate the pids in the
                // PIDAggregation header created during the CT ingest against the deployment RELS-EXT pids
                .split().tokenize(",", "PIDAggregation")
                    .parallelProcessing(Boolean.parseBoolean(PARALLEL_PROCESSING))

                    .setHeader("pid").simple("${body}")

                    .log(LoggingLevel.DEBUG, LOG_NAME, "Split Body: ${body}, CamelSplitIndex: ${header.CamelSplitIndex}, CamelSplitSize: ${header.CamelSplitSize}, CamelSplitComplete: ${header.CamelSplitComplete}")

                    //Get model
                    .to("direct:getModel")

                    .choice()
                        .when().simple("${header.hasModel?.toLowerCase()} contains 'image'")
                            //.to("direct:getManifest")
                            .to("direct:checkForPeople").id("checkForPeopleProcessCTMessage")
                    .end()

                    .filter()
                        .simple("${header.hasPeople} == 'true'")
                            .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Has People NO POST to WI for this PID: ${header.pid}, ImageId: ${header.imageid}")
                            .stop()
                    .end()

                    .to("seda:sidoraWiUpdate?waitForTaskToComplete=Never").id("processCTMessageSidoraWiUpdate")

                .end()

                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Finished Camera Trap Deployment processing ProjectId: ${header.ProjectId}, SiteId: ${header.SiteId}, SitePID: ${header.SitePID}...");

        from("activemq:queue:{{sidora.wi.apim.queue}}").routeId("SidoraWIStartProcessingFedoraMessage")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Starting processing ...").id("logStart")
                .log(LoggingLevel.DEBUG, "${id} ${routeId}: JMS Body: ${body}")

                //Set the Authorization header for Fedora HTTP calls
                .setHeader("Authorization").simple("Basic " + Base64.getEncoder().encodeToString((fedoraUser + ":" + fedoraPasword).getBytes("UTF-8")), String.class)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Fedora Authorization: ${header.Authorization}")

                .setHeader("fedoraAtom").simple("${body}", String.class)
                .setHeader("origin").xpath("/atom:entry/atom:author/atom:name", String.class, ns)
                .setHeader("dsID").xpath("/atom:entry/atom:category[@scheme='fedora-types:dsID']/@term", String.class, ns)
                .setHeader("dsLabel").xpath("/atom:entry/atom:category[@scheme='fedora-types:dsLabel']/@term", String.class, ns)

                .filter()
                    .simple("${header.origin} == '{{si.fedora.user}}'")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Filtered CT USER!! [ Origin=${header.origin}, PID=${header.pid}, Method Name=${header.methodName}, dsID=${header.dsID}, dsLabel=${header.dsLabel} ] - No message processing required!").id("logFilteredMessage")
                        .stop()
                .end()

                .filter()
                    .simple("${header.methodName} == 'purgeObject'")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Filtered Delete! PID: ${header.pid}, dsLabel: ${header.dsLabel}")
                        .stop()
                .end()

                .to("direct:checkForCtRoot")

                .filter()
                    .simple("${header.hasCtRoot} != 'true'")
                    .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Filtered Non CT object! PID: ${header.pid}, dsLabel: ${header.dsLabel}")
                    .stop()
                .end()

                //Get model
                .to("direct:getModel")

                .choice()
                    .when().simple("${header.hasModel?.toLowerCase()} contains 'image'")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Found Image... Check for People!!! PID: ${header.pid}")
                        .to("direct:getManifest")
                        .to("direct:checkForPeople").id("checkForPeopleProcessFedoraMessage")
                .end()

                .filter()
                    .simple("${header.hasPeople}")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Has People NO POST to WI for this PID: ${header.pid}, ImageId: ${header.imageid}")
                        .stop()
                .end()

                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Fedora Message Found: origin=${header.origin}, pid=${header.pid}, methodName=${header.methodName}, dsID=${header.dsID}, dsLabel=${header.dsLabel}, hasModel= ${header.hasModel}, hasPeople=${header.hasPeople}")

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Processing Body: ${body}")

                .to("seda:sidoraWiUpdate?waitForTaskToComplete=Never").id("processFedoraMessageSidoraWiUpdate")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Finished processing record update for PID: ${header.pid}.");

        from("direct:checkForCtRoot").routeId("SidoraWICheckForCtRoot")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Checking ${header.pid} has CT root...")
                .setBody().simple("ASK FROM <info:edu.si.fedora#ri> WHERE { <info:fedora/${header.pid}> <http://oris.si.edu/2017/01/relations#isAdministeredBy>+ <info:fedora/{{si.ct.root}}> .}")
                .setBody().groovy("\"query=\" + URLEncoder.encode(request.getBody(String.class));")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Check for CT Root Query - ${body}")

                .setHeader("CamelHttpMethod", constant("GET"))
                .setHeader(Exchange.HTTP_URI).simple("{{si.fuseki.endpoint}}")
                .setHeader("CamelHttpQuery").simple("output=xml&${body}")

                .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy")
                .convertBodyTo(String.class)

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Find Parent Query Result - ${body}")

                /* Example Query Result
                <?xml version="1.0"?>
                <sparql xmlns="http://www.w3.org/2005/sparql-results#">
                  <head>
                  </head>
                  <boolean>true</boolean>
                </sparql>
                */

                .setHeader("hasCtRoot").xpath("//ri:boolean/text()", String.class, ns)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: hasCtRoot: ${header.hasCtRoot}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Finished Find Parent Object...");


        from("direct:getModel").routeId("SidoraWIGetModel")
                .setHeader("CamelHttpMethod").constant("GET")
                .setHeader(Exchange.HTTP_URI).simple("{{si.fedora.host}}/objects/${header.pid}/datastreams/RELS-EXT/content")
                .setHeader(Exchange.HTTP_QUERY).simple("format=xml")
                .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy").id("processFedoraGetRELS-EXT")

                .choice()
                    .when().simple("${body} != null || ${body} != ''")
                        .setHeader("hasModel").xpath("string-join(//fs:hasModel/@rdf:resource, ',')", String.class, ns).id("setHasModel")
                    .endChoice()
                .end();

        from("direct:getManifest").routeId("SidoraWIGetManifest")
                //Find the parent so we can grab the manifest
                .to("direct:findParentObject").id("processFedoraFindParentObject")

                //Grab the manifest from the parent
                .setHeader("CamelHttpMethod").constant("GET")
                .setHeader(Exchange.HTTP_URI).simple("{{si.fedora.host}}/objects/${header.parentPid}/datastreams/MANIFEST/content")
                .setHeader(Exchange.HTTP_QUERY).simple("format=xml")

                .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy").id("processFedoraGetManifestDatastream")

                .setHeader("ManifestXML").simple("${body}", String.class)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Manifest: ${header.ManifestXML}");

        from("direct:getDatastreams").routeId("SidoraWIGetDatastreams")
                //Grab the objects datastreams as xml
                .setHeader("CamelHttpMethod").constant("GET")
                .setHeader(Exchange.HTTP_URI).simple("{{si.fedora.host}}/objects/${header.pid}/datastreams").id("sidoraWICtGetDatastreams")
                .setHeader(Exchange.HTTP_QUERY).simple("format=xml")

                .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy").id("ctProcessGetFedoraDatastream");

        from("direct:checkForPeople").routeId("SidoraWICheckForPeople")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Checking For People in ${header.pid}...")

                .to("direct:getDatastreams")

                //Get the label so we know what the imageid for image we are working with
                .setHeader("imageid").xpath("/objDatastreams:objectDatastreams/objDatastreams:datastream[@dsid='OBJ']/@label", String.class, ns)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: label: ${header.imageid} for PID: ${header.pid}")

                .setHeader("hasPeople").xpath("//ImageSequence[Image[ImageId/text() = $in:imageid]]/ResearcherIdentifications/Identification/SpeciesScientificName[contains(function:properties('si.ct.edanids.speciesScientificName.filter'), text())] != ''", Boolean.class, ns, "ManifestXML");

        from("direct:findParentObject").routeId("SidoraWIFindParentObject").errorHandler(noErrorHandler())
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Starting Find Parent Object...")

                .setBody().simple("SELECT ?subject FROM <info:edu.si.fedora#ri> WHERE { ?subject <info:fedora/fedora-system:def/relations-external#hasResource> <info:fedora/${header.pid}> .}")
                .setBody().groovy("\"query=\" + URLEncoder.encode(request.getBody(String.class));")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Find Parent Query - ${body}")

                .setHeader("CamelHttpMethod", constant("GET"))
                .setHeader(Exchange.HTTP_URI).simple("{{si.fuseki.endpoint}}")
                .setHeader("CamelHttpQuery").simple("output=xml&${body}")

                .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy")
                .convertBodyTo(String.class)

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Find Parent Query Result - ${body}")

                /* Example Query Result
                <?xml version="1.0"?>
                <sparql xmlns="http://www.w3.org/2005/sparql-results#">
                  <head>
                    <variable name="subject"/>
                  </head>
                  <results>
                    <result>
                      <binding name="subject">
                        <uri>info:fedora/test:1398</uri>
                      </binding>
                    </result>
                  </results>
                </sparql>
                */

                .setHeader("parentPid").xpath("substring-after(/ri:sparql/ri:results/ri:result[1]/ri:binding/ri:uri, 'info:fedora/')", String.class, ns)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Parent PID: ${header.parentPid}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Finished Find Parent Object...");

        from("seda:sidoraWiUpdate?concurrentConsumers=" + Integer.valueOf(CONCURRENT_CONSUMERS)).routeId("SidoraWiUpdate")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Start Sidora WI WB Call Update: ${header.pid}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}\n${headers}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}\n${body}")

                .to("direct:workbenchLogin")
                .to("direct:wiWorkbenchObjectChange")

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Workbench WI object_change Result Body:\n${body}")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Finished Sidora WI WB Call Update: ${header.pid}");

        from("direct:workbenchLogin").routeId("SidoraWiWorkbenchLogin")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Starting Workbench Login Request...")

                .removeHeaders("CamelHttp*")
                .setHeader("Content-Type", simple("application/x-www-form-urlencoded"))
                .setHeader(Exchange.HTTP_METHOD, simple("POST"))
                .removeHeader(Exchange.HTTP_QUERY)
                .setBody().simple("name={{wi.workbench.user}}&pass={{wi.workbench.password}}&form_id=user_login&op=Log in")

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Workbench Login Request Body:${body}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Workbench Login Request Headers:${headers}")

                //TODO: replace throwExceptionOnFailure param with onException to catch and handle the exception that is thrown for a 302 redirect response
                .setHeader(Exchange.HTTP_URI).simple("{{camel.workbench.login.url}}")
                .toD("https4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy&throwExceptionOnFailure=false")
                .convertBodyTo(String.class)

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} $[routeId}: Workbench Login received Set-Cookie: ${header.Set-Cookie}")

                .choice()
                    //After workbench login request we should get a Redirect Response Code: 302
                    //There is no need to follow the redirect location if the login was successful we only need the cookie
                    .when().simple("${header.CamelHttpResponseCode} in '302,200'  && ${header.Set-Cookie} != null")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} $[routeId}: Workbench Login successful received Cookie: ${header.Set-Cookie}")

                        //Set the Cookie header from the Set-Cookie that was returned from login
                        .process(cookieProcessor()).id("sidoraWiWBLoginParseSet-CookieHeader")

                        .log(LoggingLevel.INFO, LOG_NAME, "${id} $[routeId}: cookie: ${header.cookie}")

                        //.to("direct:wiWorkbenchObjectChange").id("workbenchLoginWorkbenchObjectChangeCall")
                    .endChoice()
                    .otherwise()
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Workbench Login Failed!!! Response Code: ${header.CamelHttpResponseCode}, Response: ${header.CamelHttpResponseText}, Response Set-Cookie: ${header.Set-Cookie}")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Workbench Login Failed! Response Body:${body}")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Workbench Login Failed! Response Headers:${headers}")

                        .throwException(SidoraWIException.class, "${id} ${routeId}: Workbench Login Failed!!! Response Code: ${header.CamelHttpResponseCode}, Response: ${header.CamelHttpResponseText}, Response Cookie: ${header.Set-Cookie}")
                    .endChoice()
                .end()
                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Finished Workbench Login Request...");

        from("direct:wiWorkbenchObjectChange").routeId("SidoraWi_WorkbenchObjectChange")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} ${routeId}: Notify Workbench Starting...")

                .setHeader(Exchange.HTTP_METHOD, simple("GET"))
                .setBody().simple("")

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Workbench object_change Request Body:${body}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} ${routeId}: Workbench object_change Request Headers:${headers}")

                //http://sidora0d.myquotient.net/~randerson/sidora/sidora0.6test/sidora/object_change/${pid}/${DSID}
                .toD("cxfrs:{{camel.workbench.wi.object.change.url}}/${header.pid}/${header.DSID}?headerFilterStrategy=#dropHeadersStrategy&throwExceptionOnFailure=false&loggingFeatureEnabled=true&exchangePattern=InOut").id("sidoraWiNotifyWorkbenchCall")
                .convertBodyTo(String.class);
    }
}
