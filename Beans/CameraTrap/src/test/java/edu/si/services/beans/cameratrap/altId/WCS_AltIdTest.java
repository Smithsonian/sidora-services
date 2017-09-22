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

package edu.si.services.beans.cameratrap.altId;

import edu.si.services.beans.cameratrap.CT_BlueprintTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * @author jbirkhimer
 */
@Ignore
public class WCS_AltIdTest extends CT_BlueprintTestSupport {

    private static String LOG_NAME = "edu.si.mci";

    private static final boolean USE_ACTUAL_FEDORA_SERVER = false;
    protected static final String FEDORA_URI = System.getProperty("si.fedora.host");
    protected static final String FUSEKI_URI = System.getProperty("si.fuseki.host") + "/fedora3";
    protected static final String FITS_URI = System.getProperty("si.fits.host");
    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private String defaultTestProperties = KARAF_HOME + "/test.properties";

    private static final File testManifest = new File("src/test/resources/AltIdSampleData/WCS/deployment_manifest.xml");
    private static final File projectRELS_EXT = new File("src/test/resources/AltIdSampleData/WCS/projectRELS-EXT.rdf");
    private static final File objectNotFoundFusekiResponse = new File("src/test/resources/AltIdSampleData/objectNotFoundFusekiResponse.xml");


    @Override
    protected String getBlueprintDescriptor() {
        return "Routes/wcs-route.xml";
    }

    @Override
    protected List<String> loadAdditionalPropertyFiles() {
        return Arrays.asList(KARAF_HOME + "/etc/edu.si.sidora.karaf.cfg", KARAF_HOME + "/etc/system.properties");
    }

    @Override
    protected String[] preventRoutesFromStarting() {
        return new String[]{"WCSInFlightConceptStatusPolling"};
    }

    @Override
    public void setUp() throws Exception {
        setUseActualFedoraServer(USE_ACTUAL_FEDORA_SERVER);
        setFedoraServer(FEDORA_URI, System.getProperty("si.fedora.user"), System.getProperty("si.fedora.password"));
        setFuseki(FUSEKI_URI);
        setDefaultTestProperties(defaultTestProperties);
        super.setUp();
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    /**
     * Testing Project findObject logic using alternate id's to check if an object exists
     * and falling back to checking using string name. The test assumes Project does not exist
     * and will continue to create the object, However actual object creation is intercepted or sent to mock endpoints.
     * @throws Exception
     */
    @Test
    public void processParentsMockFedoraTest() throws Exception {
        MockEndpoint mockParents = getMockEndpoint("mock:processParentsResult");
        mockParents.expectedMessageCount(2);
        mockParents.expectedBodiesReceived(readFileToString(projectRELS_EXT));

        context.getRouteDefinition("WCSProcessParents").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //Intercept sending to processPlot
                interceptSendToEndpoint("direct:processPlot").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, "Skipping processPlot");
            }
        });

        context.getRouteDefinition("WCSProcessProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //Intercept sending to fedora:create but provide a pid
                interceptSendToEndpoint("fedora:create.*").skipSendToOriginalEndpoint().setHeader("CamelFedoraPid", simple("test:1"));

                //intercept sending to fedora:addDatastream and send to mock endpoint to assert correct values
                interceptSendToEndpoint("fedora:addDatastream.*RELS-EXT.*").skipSendToOriginalEndpoint().setHeader("routeId", simple("${routeId}")).to("mock:processParentsResult");

                //intercept other calls to fedora that are not needed and skip them
                interceptSendToEndpoint("fedora:hasConcept.*").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, "Skipping fedora:hasConcept");
                interceptSendToEndpoint("fedora:addDatastream.*EAC-CPF.*").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, "Skipping fedora:addDatastream for EAC-CPF");
            }
        });

        context.getRouteDefinition("WCSFindObject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("findObjectFusekiHttpCall").replace().setBody().simple(readFileToString(objectNotFoundFusekiResponse));
            }
        });

        context.start();

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest));
        exchange.getIn().setHeader("CamelFileParent", "someCamelFileParent");
        exchange.getIn().setHeader("CamelFedoraPid", getConfig().getString("si.ct.root"));

        template.send("direct:processParents", exchange);

        assertEquals(readFileToString(projectRELS_EXT), mockParents.getExchanges().get(0).getIn().getBody());

        for (Exchange mockExchange : mockParents.getExchanges()) {
            log.info("Result from {} route: {}", mockExchange.getIn().getHeader("routeId"), mockExchange.getIn().getBody(String.class));
        }

        assertMockEndpointsSatisfied();
    }
}
