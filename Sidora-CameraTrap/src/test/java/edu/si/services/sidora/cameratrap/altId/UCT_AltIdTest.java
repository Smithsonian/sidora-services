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

package edu.si.services.sidora.cameratrap.altId;

import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * Testing Project and Subproject findObject logic using alternate id's to check if an object exists
 * and falling back to checking using string name. The test assumes neither Project or SubProject exist
 * and will continue to create the object, However actual object creation is intercepted or sent to mock endpoints.
 *
 * @author jbirkhimer
 */
@CamelSpringBootTest
@SpringBootTest(properties = {
        "logging.file.path=target/logs",
        "processing.dir.base.path=${user.dir}/target",
        "si.ct.uscbi.enableS3Routes=false",
        "camel.springboot.java-routes-exclude-pattern=UnifiedCameraTrapInFlightConceptStatusPolling,UnifiedCameraTrapStartProcessing"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class UCT_AltIdTest {

    private static final Logger log = LoggerFactory.getLogger(UCT_AltIdTest.class);

    @Autowired
    CamelContext context;

    //@EndpointInject(value = "direct:addFITSDataStream")
    @Autowired
    ProducerTemplate template;

    private static String LOG_NAME = "edu.si.mci";

    private static final boolean USE_ACTUAL_FEDORA_SERVER = false;

    @PropertyInject(value = "{{si.ct.root}}")
    protected static String CT_ROOT_PID;

    @PropertyInject(value = "{{si.fedora.host}}")
    protected static String FEDORA_URI;

    @PropertyInject(value = "{{si.fedora.user}}")
    protected static String FEDORA_USER;
    @PropertyInject(value = "{{si.fedora.password}}")
    protected static String FEDORA_PASSWORD;

    @PropertyInject(value = "{{si.fuseki.endpoint}}")
    protected static String FUSEKI_URI;

    @PropertyInject(value = "{{fits.host}}")
    protected static String FITS_URI;

    private static final File testManifest = new File("src/test/resources/AltIdSampleData/Unified/deployment_manifest.xml");
    private static final File projectRELS_EXT = new File("src/test/resources/AltIdSampleData/Unified/projectRELS-EXT.rdf");
    private static final File projectDC = new File("src/test/resources/AltIdSampleData/Unified/projectDC.xml");

    private static final File subProjectRELS_EXT = new File("src/test/resources/AltIdSampleData/Unified/subProjectRELS-EXT.rdf");
    private static final File subProjectDC = new File("src/test/resources/AltIdSampleData/Unified/subprojectDC.xml");

    private static final File objectNotFoundFusekiResponse = new File("src/test/resources/AltIdSampleData/objectNotFoundFusekiResponse.xml");

    /*@Override
    public void setUp() throws Exception {
        setUseActualFedoraServer(USE_ACTUAL_FEDORA_SERVER);
        setFedoraServer(FEDORA_URI, FEDORA_USER, FEDORA_PASSWORD));
        setFuseki(FUSEKI_URI);
        setDefaultTestProperties(defaultTestProperties);
        super.setUp();
    }*/

    /**
     * Testing Project and Subproject findObject logic using alternate id's to check if an object exists
     * and falling back to checking using string name. The test assumes neither Project or SubProject exist
     * and will continue to create the object, However actual object creation is intercepted or sent to mock endpoints.
     *
     * @throws Exception
     */
    @Test
    public void processParentsMockFedoraTest() throws Exception {

        MockEndpoint mockProjectDC = context.getEndpoint("mock:projectDCResult", MockEndpoint.class);
        mockProjectDC.expectedMessageCount(1);
        //mockProjectDC.expectedBodiesReceived(readFileToString(projectDC));

        MockEndpoint mockSubprojectDC = context.getEndpoint("mock:subprojectDCResult", MockEndpoint.class);
        mockSubprojectDC.expectedMessageCount(1);
        //mockSubprojectDC.expectedBodiesReceived(readFileToString(subProjectDC));

        /* Advicewith the routes as needed for this test */
        AdviceWith.adviceWith(context, "UnifiedCameraTrapProcessParents", false, a ->{
                a.interceptSendToEndpoint("direct:findObjectByPIDPredicate.*").skipSendToOriginalEndpoint()
                        .log(LoggingLevel.INFO, "Skipping findObjectByPIDPredicate")
                        .setBody().simple("true");
                //Intercept sending to processPlot we are only testing Project and Subproject RELS-EXT Creation
                a.interceptSendToEndpoint("direct:processPlot").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, "Skipping processPlot");
        });

        AdviceWith.adviceWith(context, "UnifiedCameraTrapProcessProject", false, a ->{

                //intercept calls to fedora that are not needed and skip them or replace with the expected response
                a.weaveByToString(".*create.*").replace()
                        .setHeader("CamelFedoraPid").simple("test:1");
                a.weaveByToString(".*addDatastream.*RELS-EXT.*").replace().log(LoggingLevel.INFO, "Skipping fedora:addDatastream for RELS-EXT");
                a.weaveByToString(".*hasConcept.*").replace().log(LoggingLevel.INFO, "Skipping fedora:hasConcept");
                a.weaveByToString(".*addDatastream.*EAC-CPF.*").replace().log(LoggingLevel.INFO, "Skipping fedora:addDatastream for EAC-CPF");
                a.weaveByToString(".*getDatastreamDissemination.*DC.*").replace()
                        .log(LoggingLevel.INFO, "Skipping fedora:getDatastreamDissemination")
                        .setBody().simple("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" +
                        "           xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                        "           xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "           xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
                        "    <dc:title>Test Project</dc:title>\n" +
                        "    <dc:identifier>test:1</dc:identifier>\n" +
                        "</oai_dc:dc>");

                // Intercept sending DC datastream to Fedora and send to mock for assertion
                a.weaveById("addProjectDC").replace()
                        .log(LoggingLevel.INFO, "Skipping fedora:addDatastream for DC")
                        .setHeader("routeId").simple("${routeId}")
                        .to("mock:projectDCResult");
        });

        AdviceWith.adviceWith(context, "UnifiedCameraTrapProcessSubproject", false, a -> {

                //intercept calls to fedora that are not needed and skip them or replace with the expected response
                a.weaveByToString(".*create.*").replace()
                        .setHeader("CamelFedoraPid").simple("test:1");
                a.weaveByToString(".*addDatastream.*RELS-EXT.*").replace().log(LoggingLevel.INFO, "Skipping fedora:addDatastream for RELS-EXT");
                a.weaveByToString(".*hasConcept.*").replace().log(LoggingLevel.INFO, "Skipping fedora:hasConcept");
                a.weaveByToString(".*addDatastream.*EAC-CPF.*").replace()
                        .log(LoggingLevel.INFO, "Skipping fedora:addDatastream for FGDC-Research");
                a.weaveByToString(".*getDatastreamDissemination.*DC.*").replace()
                        .log(LoggingLevel.INFO, "Skipping fedora:getDatastreamDissemination")
                        .setBody().simple("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" +
                        "           xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                        "           xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "           xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
                        "    <dc:title>Test Subproject</dc:title>\n" +
                        "    <dc:identifier>test:2</dc:identifier>\n" +
                        "</oai_dc:dc>");

                //intercept sending DC datastream to Fedora and send to mock for assertion
                a.weaveById("addSubprojectDC").replace()
                        .log(LoggingLevel.INFO, "Skipping fedora:addDatastream for DC")
                        .setHeader("routeId").simple("${routeId}")
                        .to("mock:subprojectDCResult");
        });

        AdviceWith.adviceWith(context, "UnifiedCameraTrapFindObject", false, a ->
                //replace the actual fuseki http call and provide our own response
                a.weaveById("findObjectFusekiHttpCall").replace().setBody().simple(readFileToString(objectNotFoundFusekiResponse, "utf-8")));

        context.start();

        //Initialize the exchange with body and headers as needed
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest, "utf-8"));
        exchange.getIn().setHeader("deploymentPackageId", "somedeploymentPackageId");
        exchange.getIn().setHeader("CamelFedoraPid", CT_ROOT_PID);

        // The endpoint we want to start from with the exchange body and headers we want
        template.send("direct:processParents", exchange);

        mockProjectDC.assertIsSatisfied();
        mockSubprojectDC.assertIsSatisfied();

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreWhitespace(true);

        //assert that the expected bodies have the correct Project and SubProject DC datastream
//        assertXMLEqual(readFileToString(projectDC), mockProjectDC.getExchanges().get(0).getIn().getBody(String.class));
//        assertXMLEqual(readFileToString(subProjectDC), mockSubprojectDC.getExchanges().get(0).getIn().getBody(String.class));
    }

    @Test
    public void findObjectTest() throws Exception {
        MockEndpoint mockAltIDResult = context.getEndpoint("mock:altIdResult", MockEndpoint.class);
        mockAltIDResult.expectedMessageCount(8);

        MockEndpoint mockNameResult = context.getEndpoint("mock:nameResult", MockEndpoint.class);
        mockNameResult.expectedMessageCount(8);

        /* Advicewith the routes as needed for this test */
        AdviceWith.adviceWith(context, "UnifiedCameraTrapProcessProject", false, a ->
                a.weaveByToString(".*create.*").replace().stop());

        AdviceWith.adviceWith(context, "UnifiedCameraTrapFindObject", false, a ->{
                //replace the actual fuseki http call and provide our own response
                a.weaveById("whenFindByAltId").after().to("mock:altIdResult");
                a.weaveById("whenFindByName").after().to("mock:nameResult");
                a.weaveById("findObjectFusekiHttpCall").replace().setBody().simple(readFileToString(objectNotFoundFusekiResponse, "utf-8"));
        });

        context.start();

        //Initialize the exchange with body and headers as needed
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest, "utf-8"));
        exchange.getIn().setHeader("deploymentPackageId", "somedeploymentPackageId");
        exchange.getIn().setHeader("CamelFedoraPid", CT_ROOT_PID);

        // The endpoint we want to start from with the exchange body and headers we want
        template.send("direct:processProject", exchange);

        mockAltIDResult.assertIsSatisfied();
        mockNameResult.assertIsSatisfied();
    }
}
