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

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.velocity.VelocityConstants;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultHeaderFilterStrategy;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.DateTool;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.HashMap;

/**
 * @author jbirkhimer
 */
@Ignore //for manual testing only
public class CT_Ingest_JMS_Test extends CamelTestSupport {

    /**
     * header used for enabling test url value so that we can create new edan records for testing
     * sets url = url_test_randomUUID
     */
    private static final String ENABLE_TEST_URL = "true";

    private static final Logger LOG = LoggerFactory.getLogger(CT_Ingest_JMS_Test.class);

    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static final String ACTIVEMQ_SERVER = "localhost";

    private static String fedoraHost = "http://localhost:8080/fedora";
    private static String fedoraUser = "fedoraUser";
    private static String fedoraPasword = "fedoraPassword";
    private static DefaultHeaderFilterStrategy dropHeadersStrategy;

    private static AggregationStrategy aggregationStrategy = new AggregationStrategy() {
        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            Object resourcePid = newExchange.getIn().getHeader("pid", String.class);
            String ResearcherObservationPID = newExchange.getIn().getHeader("ResearcherObservationPID", String.class);
            String VolunteerObservationPID = newExchange.getIn().getHeader("VolunteerObservationPID", String.class);
            String ImageObservationPID = newExchange.getIn().getHeader("ImageObservationPID", String.class);
            
            if (oldExchange == null) {
                newExchange.getIn().setHeader("PIDAggregation", resourcePid);

                if (ResearcherObservationPID !=null && !ResearcherObservationPID.isEmpty()) {
                    newExchange.getIn().setHeader("ResearcherObservationPID", ResearcherObservationPID);
                }
                if (VolunteerObservationPID !=null && !VolunteerObservationPID.isEmpty()) {
                    newExchange.getIn().setHeader("VolunteerObservationPID", VolunteerObservationPID);
                }
                if (ImageObservationPID !=null && !ImageObservationPID.isEmpty()) {
                    newExchange.getIn().setHeader("ImageObservationPID", ImageObservationPID);
                }
                return newExchange;
            }

            if (ResearcherObservationPID !=null && !ResearcherObservationPID.isEmpty()) {
                oldExchange.getIn().setHeader("ResearcherObservationPID", ResearcherObservationPID);
            }
            if (VolunteerObservationPID !=null && !VolunteerObservationPID.isEmpty()) {
                oldExchange.getIn().setHeader("VolunteerObservationPID", VolunteerObservationPID);
            }
            if (ImageObservationPID !=null && !ImageObservationPID.isEmpty()) {
                oldExchange.getIn().setHeader("ImageObservationPID", ImageObservationPID);
            }

            String aggregation = oldExchange.getIn().getHeader("PIDAggregation", String.class);
            oldExchange.getIn().setHeader("PIDAggregation", String.format("%s,%s", aggregation, resourcePid));
            return oldExchange;
        }
    };

    private static final RouteBuilder buildCtIngestMessage = new RouteBuilder() {
        @Override
        public void configure() throws Exception {
            Namespaces ns = new Namespaces("atom", "http://www.w3.org/2005/Atom");
            ns.add("fedora", "info:fedora/fedora-system:def/relations-external#");
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

            from("direct:start").routeId("BuildCtIngestMessageRoute")

                    //header used for enabling test url value so that we can create new edan records for testing
                    //sets url = url_test_randomUUID
                    .setHeader("test_edan_url").simple(ENABLE_TEST_URL)

                    //Set the Authorization header for Fedora HTTP calls
                    .setHeader("Authorization").simple("Basic " + Base64.getEncoder().encodeToString((fedoraUser + ":" + fedoraPasword).getBytes("UTF-8")), String.class)

                    .to("direct:getManifest")
                    .setHeader("ManifestXML").simple("${body}", String.class)
                    .setHeader("ProjectId").xpath("//ProjectId", String.class, ns, "ManifestXML")
                    .setHeader("SiteId").xpath("//CameraDeploymentID", String.class, ns, "ManifestXML")
                    .setHeader("ImageCount").xpath("count(//ImageFileName)" , String.class, ns, "ManifestXML")

                    .setHeader("pid", simple("${header.SitePID}"))
                    .to("direct:getRelsExt")
                    .log(LoggingLevel.DEBUG, "deployment RELS-EXT: ${body}")

                    .split().xtokenize("//fedora:hasResource", ns).aggregationStrategy(aggregationStrategy)
                        .setHeader("pid").xpath("substring-after(//fedora:hasResource/@rdf:resource, 'info:fedora/')", String.class, ns)
                        .log(LoggingLevel.DEBUG, "resource pid: ${header.pid}")

                        /*.to("direct:getRelsExt")
                        //.log(LoggingLevel.DEBUG, "resource RELS-EXT: ${body}")

                        .setHeader("isObservation").xpath("contains(string-join(//fs:hasModel/@rdf:resource, ','), 'datasetCModel')", String.class, ns)
                        .log(LoggingLevel.DEBUG, "resource is observation: ${header.isObservation}")

                        .choice()
                            .when().simple("${header.isObservation} == 'true'")
                                .to("direct:getDatastreams")
                                //.log(LoggingLevel.DEBUG, "resource datastreams: ${body}")
                                .setHeader("objLabel").xpath("/objDatastreams:objectDatastreams/objDatastreams:datastream[@dsid='OBJ']/@label", String.class, ns)
                                .log(LoggingLevel.DEBUG, "resource objLabel: ${header.objLabel}")
                            .endChoice()
                        .end()*/

                        .to("direct:getDatastreams")
                        .log(LoggingLevel.DEBUG, "resource datastreams: ${body}")
                        .setHeader("objLabel").xpath("/objDatastreams:objectDatastreams/objDatastreams:datastream[@dsid='OBJ']/@label", String.class, ns)
                        .log(LoggingLevel.DEBUG, "resource objLabel: ${header.objLabel}")

                        .choice()
                            .when().simple("${header.objLabel} == 'Researcher Observations'")
                                .setHeader("ResearcherObservationPID").simple("${header.pid}")
                            .endChoice()
                            .when().simple("${header.objLabel} == 'Volunteer Observations'")
                                .setHeader("VolunteerObservationPID").simple("${header.pid}")
                            .endChoice()
                            .when().simple("${header.objLabel} == 'Image Observations'")
                                .setHeader("ImageObservationPID").simple("${header.pid}")
                            .endChoice()
                        .end()

                        .log(LoggingLevel.DEBUG, "\nResearcherObservationPID: ${header.ResearcherObservationPID}\nVolunteerObservationPID: ${header.VolunteerObservationPID}\nImageObservationPID: ${header.ImageObservationPID}")
                        
                    .end()

                    .setBody().simple("")

                    //Remove unneeded headers that may cause problems later on
                    .removeHeaders("Content-Type|CamelHttpResponseCode|CamelHttpQuery|CamelHttpMethod|encoding|Accept-Ranges|Authorization|breadcrumbId|CamelHttpResponseText|CamelHttpUri|content-disposition|filename|Date|Server|Set-Cookie|Transfer-Encoding|objLabel|pid")

                    .log(LoggingLevel.INFO, "Headers:\n${headers}")
                    .log(LoggingLevel.DEBUG, "Body:\n${body}")

                    .log(LoggingLevel.INFO, "\nProjectId: ${header.ProjectId}\nSiteId: ${header.SiteId}\nSitePID: ${header.SitePID}\nResearcherObservationPID: ${header.ResearcherObservationPID}\nVolunteerObservationPID: ${header.VolunteerObservationPID}\nImageObservationPID: ${header.ImageObservationPID}\nImageCount: ${header.ImageCount}\nPIDAggregation: ${header.PIDAggregation}")

                    .to("activemq:queue:edanIds.ct.update")

                    .log(LoggingLevel.DEBUG, "Done!!!");


            from("direct:getRelsExt")
                    .setHeader("CamelHttpMethod").constant("GET")
                    .setHeader(Exchange.HTTP_URI).simple(fedoraHost + "/objects/${header.pid}/datastreams/RELS-EXT/content")
                    .log(LoggingLevel.DEBUG, "${header.CamelHttpUri}")
                    .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy")
                    .convertBodyTo(String.class);

            from("direct:getDatastreams")
                    .setHeader("CamelHttpMethod").constant("GET")
                    .setHeader(Exchange.HTTP_URI).simple(fedoraHost + "/objects/${header.pid}/datastreams")
                    .log(LoggingLevel.DEBUG, "${header.CamelHttpUri}")
                    .setHeader(Exchange.HTTP_QUERY).simple("format=xml")
                    .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy")
                    .convertBodyTo(String.class);


            from("direct:getManifest")
                    .setHeader("CamelHttpMethod").constant("GET")
                    .setHeader(Exchange.HTTP_URI).simple(fedoraHost + "/objects/${header.SitePID}/datastreams/MANIFEST/content")
                    .log(LoggingLevel.DEBUG, "${header.CamelHttpUri}")
                    .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy").id("processFedoraGetManifestDatastream")
                    .convertBodyTo(String.class);


            from("direct:sendFedoraAPIM")

                    .setHeader("test_edan_url").simple(ENABLE_TEST_URL)

                    .log(LoggingLevel.INFO, "Fedora APIM message:\nHeaders:\n${headers}\nBody:\n${body}")

                    .toD("activemq:queue:edanIds.apim.update")

                    .log(LoggingLevel.INFO, "Sent Fedora APIM message!!!");

        }
    };

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();

        ActiveMQComponent activeMQComponent = new ActiveMQComponent();
        activeMQComponent.setBrokerURL("tcp://"+ACTIVEMQ_SERVER+":61616");
        activeMQComponent.setUserName("smx");
        activeMQComponent.setPassword("smx");
        camelContext.addComponent("activemq", activeMQComponent);

        camelContext.addRoutes(buildCtIngestMessage);

        return camelContext;
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        //Header Filter Strategy to prevent all headers from being added to Fedora HTTP calls except for Authorization
        dropHeadersStrategy = new DefaultHeaderFilterStrategy();
        dropHeadersStrategy.setOutFilterPattern("^(?!Authorization$).*$");

        JndiRegistry jndiRegistry = super.createRegistry();
        jndiRegistry.bind("dropHeadersStrategy", dropHeadersStrategy);
        return jndiRegistry;
    }

    public void sendCtIngestMessage(String deploymentPid) throws Exception {
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("SitePID", deploymentPid);

        template.send("direct:start", exchange);
    }

    public void sendFedoraApimMessage(String pid, String label, String dsID, String mimeType, String fedoraModel, String user, String methodName) {
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("origin", user);
        headers.put("methodName", methodName);
        headers.put("testPID", pid);
        headers.put("testDsLabel", label);
        headers.put("testDsId", dsID);
        headers.put("testObjMimeType", mimeType);
        headers.put("testFedoraModel", fedoraModel);

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("date", new DateTool());
        velocityContext.put("headers", headers);

        headers.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext);

        String jmsMsg = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_atom.vsl", "test body", headers, String.class);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setBody(jmsMsg);

        template.send("direct:sendFedoraAPIM", exchange);

    }

    //Encoding Issue in Project Name (Investigating "Disgust" in Raccoons)
    @Test
    public void test_d36529() throws Exception {
        //ProjectId: p229
        //SiteId: d36529
        //SitePID: ct:10621728
        //ResearcherObservationPID: ct:10622662
        //VolunteerObservationPID: ct:10622664
        //ImageObservationPID:
        //ImageCount: 438
        //154M
        sendCtIngestMessage("ct:10621728");
    }

    @Test
    public void test_d53936() throws Exception {
        //ProjectId: p163
        //SiteId: d53936
        //SitePID: ct:12310696
        //ResearcherObservationPID: ct:12310705
        //VolunteerObservationPID: ct:12310706
        //ImageObservationPID:
        //ImageCount: 8

        sendCtIngestMessage("ct:12310696");
    }

    @Test
    public void test_d54515() throws Exception {
        //ProjectId: p273
        //SiteId: d54515
        //SitePID: ct:12103751
        //ResearcherObservationPID: ct:12104986
        //VolunteerObservationPID: ct:12104987
        //ImageObservationPID:
        //ImageCount: 279
        //100M
        sendCtIngestMessage("ct:12103751");
    }

    @Test
    public void test_d44243() throws Exception {
        //1.1G
        sendCtIngestMessage("ct:10663221");
    }

    @Test
    public void test_d50499() throws Exception {
        //5.0G
        sendCtIngestMessage("ct:10913787");
    }

    @Test
    public void test_fedoaraAPIM() {

        //sendFedoraApimMessage("ct:12310698", "d53936s2i1", "OBJ", "image/jpg", "info:fedora/si:generalImageCModel", "someUser", "modifyDatastreamByValue");

        sendFedoraApimMessage("ct:10621914", "d36529s24i5", "OBJ", "image/jpg", "info:fedora/si:generalImageCModel", "someUser", "modifyDatastreamByValue");

        //sendFedoraApimMessage("ct:12310698", "d53936s2i1", "OBJ", "image/jpg", "info:fedora/si:generalImageCModel", "someUser", "purgeObject");
    }

}
