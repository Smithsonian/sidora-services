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

package edu.si.services.edansidora;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * @author jbirkhimer
 */
public class EdanSidoraTest extends EDAN_CT_BlueprintTestSupport {

    private static String LOG_NAME = "edu.si.uctingest";

    private static final boolean USE_ACTUAL_FEDORA_SERVER = false;
    private String defaultTestProperties = "src/test/resources/test.properties";

    private static final File testManifest = new File("src/test/resources/unified-test-deployment/deployment_manifest.xml");
    private static final File projectRELS_EXT = new File("src/test/resources/test-data/projectRELS-EXT.rdf");
    private static final File subProjectRELS_EXT = new File("src/test/resources/test-data/subprojectRELS-EXT.rdf");
    private static final File objectNotFoundFusekiResponse = new File("src/test/resources/test-data/objectNotFoundFusekiResponse.xml");

    @Override
    protected String getBlueprintDescriptor() {
        return "Routes/unified-camera-trap-route.xml";
    }

    @Override
    protected List<String> loadAdditionalPropertyFiles() {
        return Arrays.asList("target/test-classes/etc/edu.si.sidora.karaf.cfg", "target/test-classes/etc/system.properties", "target/test-classes/etc/edu.si.sidora.emammal.cfg");
    }

    @Override
    protected String[] preventRoutesFromStarting() {
        return new String[]{"UnifiedCameraTrapInFlightConceptStatusPolling"};
    }

    @Before
    @Override
    public void setUp() throws Exception {
        setUseActualFedoraServer(USE_ACTUAL_FEDORA_SERVER);
        setDefaultTestProperties(defaultTestProperties);
        super.setUp();
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Test
    @Ignore
    public void edanJsonTest() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(3);

        context.getRouteDefinition("UnifiedCameraTrapAddImageToEdanAndIds2").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToString(".*edanApiBean.*").replace().log(LoggingLevel.INFO, "Skipping Sending to edanApiBean Bean");
                weaveByToString(".*idsPushBean.*").replace().log(LoggingLevel.INFO, "Skipping Sending to idsPushBean Bean");
                weaveAddLast().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelFedoraPid", "test:12345");
        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest));

        String[] fileNames = {"d18981s1i1.JPG", "d18981s1i10.JPG", "d18981s2i1.JPG"};
        for (String fileName : fileNames) {
            exchange.getIn().setHeader("CamelFileName", fileName);

            template.send("direct:addImageToEdanAndIds2", exchange);
        }

        //log.info("Headers:\n{}", mockEndpoint.getExchanges().get(0).getIn().getHeaders());

        for (Map.Entry<String, Object> entry : mockEndpoint.getExchanges().get(0).getIn().getHeaders().entrySet()) {
            if (!entry.getKey().toString().equals("ManifestXML") && !entry.getKey().toString().equals("edanJson")) {
                log.info(entry.toString());
            }
        }


        assertMockEndpointsSatisfied();

    }


}
