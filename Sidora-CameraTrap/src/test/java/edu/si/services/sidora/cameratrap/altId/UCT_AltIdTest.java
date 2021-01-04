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

import edu.si.services.sidora.cameratrap.CT_BlueprintTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

/**
 * Testing Project and Subproject findObject logic using alternate id's to check if an object exists
 * and falling back to checking using string name. The test assumes neither Project or SubProject exist
 * and will continue to create the object, However actual object creation is intercepted or sent to mock endpoints.
 *
 * @author jbirkhimer
 */
@Ignore
public class UCT_AltIdTest extends CT_BlueprintTestSupport {

    private static String LOG_NAME = "edu.si.mci";

    private static final boolean USE_ACTUAL_FEDORA_SERVER = false;
    protected static final String FEDORA_URI = System.getProperty("si.fedora.host");
    protected static final String FUSEKI_URI = System.getProperty("si.fuseki.host") + "/fedora3";
    protected static final String FITS_URI = System.getProperty("si.fits.host");
    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private String defaultTestProperties = KARAF_HOME + "/test.properties";

    private static final File testManifest = new File("src/test/resources/AltIdSampleData/Unified/deployment_manifest.xml");
    private static final File projectRELS_EXT = new File("src/test/resources/AltIdSampleData/Unified/projectRELS-EXT.rdf");
    private static final File projectDC = new File("src/test/resources/AltIdSampleData/Unified/projectDC.xml");

    private static final File subProjectRELS_EXT = new File("src/test/resources/AltIdSampleData/Unified/subProjectRELS-EXT.rdf");
    private static final File subProjectDC = new File("src/test/resources/AltIdSampleData/Unified/subprojectDC.xml");

    private static final File objectNotFoundFusekiResponse = new File("src/test/resources/AltIdSampleData/objectNotFoundFusekiResponse.xml");


    @Override
    protected String getBlueprintDescriptor() {
        return "Routes/unified-camera-trap-route.xml";
    }

//    @Override
//    protected List<String> loadAdditionalPropertyFiles() {
//        return Arrays.asList(KARAF_HOME + "/etc/system.properties", KARAF_HOME + "/etc/edu.si.sidora.karaf.cfg", KARAF_HOME + "/etc/edu.si.sidora.emammal.cfg");
//    }

    @Override
    protected String[] preventRoutesFromStarting() {
        return new String[]{"UnifiedCameraTrapInFlightConceptStatusPolling"};
    }

    /*@Override
    public void setUp() throws Exception {
        setUseActualFedoraServer(USE_ACTUAL_FEDORA_SERVER);
        setFedoraServer(FEDORA_URI, System.getProperty("si.fedora.user"), System.getProperty("si.fedora.password"));
        setFuseki(FUSEKI_URI);
        setDefaultTestProperties(defaultTestProperties);
        super.setUp();
    }*/

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    /**
     * Testing Project and Subproject findObject logic using alternate id's to check if an object exists
     * and falling back to checking using string name. The test assumes neither Project or SubProject exist
     * and will continue to create the object, However actual object creation is intercepted or sent to mock endpoints.
     *
     * @throws Exception
     */
    @Test
    public void processParentsMockFedoraTest() throws Exception {

        MockEndpoint mockProjectDC = getMockEndpoint("mock:projectDCResult");
        mockProjectDC.expectedMessageCount(1);
        //mockProjectDC.expectedBodiesReceived(readFileToString(projectDC));

        MockEndpoint mockSubprojectDC = getMockEndpoint("mock:subprojectDCResult");
        mockSubprojectDC.expectedMessageCount(1);
        //mockSubprojectDC.expectedBodiesReceived(readFileToString(subProjectDC));

        /* Advicewith the routes as needed for this test */
        context.getRouteDefinition("UnifiedCameraTrapProcessParents").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("direct:findObjectByPIDPredicate.*").skipSendToOriginalEndpoint()
                        .log(LoggingLevel.INFO, "Skipping findObjectByPIDPredicate")
                        .setBody().simple("true");
                //Intercept sending to processPlot we are only testing Project and Subproject RELS-EXT Creation
                interceptSendToEndpoint("direct:processPlot").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, "Skipping processPlot");
            }
        });

        context.getRouteDefinition("UnifiedCameraTrapProcessProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                //intercept calls to fedora that are not needed and skip them or replace with the expected response
                weaveByToString(".*create.*").replace()
                        .setHeader("CamelFedoraPid", simple("test:1"));
                weaveByToString(".*addDatastream.*RELS-EXT.*").replace().log(LoggingLevel.INFO, "Skipping fedora:addDatastream for RELS-EXT");
                weaveByToString(".*hasConcept.*").replace().log(LoggingLevel.INFO, "Skipping fedora:hasConcept");
                weaveByToString(".*addDatastream.*EAC-CPF.*").replace().log(LoggingLevel.INFO, "Skipping fedora:addDatastream for EAC-CPF");
                weaveByToString(".*getDatastreamDissemination.*DC.*").replace()
                        .log(LoggingLevel.INFO, "Skipping fedora:getDatastreamDissemination")
                        .setBody().simple("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" +
                        "           xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                        "           xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "           xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
                        "    <dc:title>Test Project</dc:title>\n" +
                        "    <dc:identifier>test:1</dc:identifier>\n" +
                        "</oai_dc:dc>");

                // Intercept sending DC datastream to Fedora and send to mock for assertion
                weaveById("addProjectDC").replace()
                        .log(LoggingLevel.INFO, "Skipping fedora:addDatastream for DC")
                        .setHeader("routeId", simple("${routeId}"))
                        .to("mock:projectDCResult");
            }
        });

        context.getRouteDefinition("UnifiedCameraTrapProcessSubproject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                //intercept calls to fedora that are not needed and skip them or replace with the expected response
                weaveByToString(".*create.*").replace()
                        .setHeader("CamelFedoraPid", simple("test:1"));
                weaveByToString(".*addDatastream.*RELS-EXT.*").replace().log(LoggingLevel.INFO, "Skipping fedora:addDatastream for RELS-EXT");
                weaveByToString(".*hasConcept.*").replace().log(LoggingLevel.INFO, "Skipping fedora:hasConcept");
                weaveByToString(".*addDatastream.*EAC-CPF.*").replace()
                        .log(LoggingLevel.INFO, "Skipping fedora:addDatastream for FGDC-Research");
                weaveByToString(".*getDatastreamDissemination.*DC.*").replace()
                        .log(LoggingLevel.INFO, "Skipping fedora:getDatastreamDissemination")
                        .setBody().simple("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" +
                        "           xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                        "           xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "           xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
                        "    <dc:title>Test Subproject</dc:title>\n" +
                        "    <dc:identifier>test:2</dc:identifier>\n" +
                        "</oai_dc:dc>");

                //intercept sending DC datastream to Fedora and send to mock for assertion
                weaveById("addSubprojectDC").replace()
                        .log(LoggingLevel.INFO, "Skipping fedora:addDatastream for DC")
                        .setHeader("routeId", simple("${routeId}"))
                        .to("mock:subprojectDCResult");
            }
        });

        context.getRouteDefinition("UnifiedCameraTrapFindObject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //replace the actual fuseki http call and provide our own response
                weaveById("findObjectFusekiHttpCall").replace().setBody().simple(readFileToString(objectNotFoundFusekiResponse));
            }
        });

        context.start();

        //Initialize the exchange with body and headers as needed
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest));
        exchange.getIn().setHeader("deploymentPackageId", "somedeploymentPackageId");
        exchange.getIn().setHeader("CamelFedoraPid", getExtra().getProperty("si.ct.root"));

        // The endpoint we want to start from with the exchange body and headers we want
        template.send("direct:processParents", exchange);

        assertMockEndpointsSatisfied();

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreWhitespace(true);

        //assert that the expected bodies have the correct Project and SubProject DC datastream
        assertXMLEqual(readFileToString(projectDC), mockProjectDC.getExchanges().get(0).getIn().getBody(String.class));
        assertXMLEqual(readFileToString(subProjectDC), mockSubprojectDC.getExchanges().get(0).getIn().getBody(String.class));
    }

    @Test
    public void findObjectTest() throws Exception {
        MockEndpoint mockAltIDResult = getMockEndpoint("mock:altIdResult");
        mockAltIDResult.expectedMessageCount(8);

        MockEndpoint mockNameResult = getMockEndpoint("mock:nameResult");
        mockNameResult.expectedMessageCount(8);

        /* Advicewith the routes as needed for this test */
        context.getRouteDefinition("UnifiedCameraTrapProcessProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToString(".*create.*").replace().stop();
            }
        });

        context.getRouteDefinition("UnifiedCameraTrapFindObject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //replace the actual fuseki http call and provide our own response
                weaveById("whenFindByAltId").after().to("mock:altIdResult");
                weaveById("whenFindByName").after().to("mock:nameResult");
                weaveById("findObjectFusekiHttpCall").replace().setBody().simple(readFileToString(objectNotFoundFusekiResponse));
            }
        });

        context.start();

        //Initialize the exchange with body and headers as needed
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest));
        exchange.getIn().setHeader("deploymentPackageId", "somedeploymentPackageId");
        exchange.getIn().setHeader("CamelFedoraPid", getExtra().getProperty("si.ct.root"));

        // The endpoint we want to start from with the exchange body and headers we want
        template.send("direct:processProject", exchange);

        assertMockEndpointsSatisfied();
    }
}
