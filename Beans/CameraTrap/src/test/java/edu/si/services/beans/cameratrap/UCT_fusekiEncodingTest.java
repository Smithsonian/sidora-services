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

package edu.si.services.beans.cameratrap;

import edu.si.services.beans.cameratrap.CT_BlueprintTestSupport;
import edu.si.services.fedorarepo.FedoraObjectNotFoundException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.LogDefinition;
import org.apache.commons.io.FilenameUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author jbirkhimer
 */
public class UCT_fusekiEncodingTest extends CT_BlueprintTestSupport {

    private static String LOG_NAME = "edu.si.test";

    private static final boolean USE_ACTUAL_FEDORA_SERVER = false;
    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private String defaultTestProperties = KARAF_HOME + "/test.properties";

    //Camera Trap Deployment Manifest
    private File manifestFile = new File("src/test/resources/SID-912/deployment_manifest.xml");

    private static final CountDownLatch LATCH = new CountDownLatch(1);

    @Override
    protected String getBlueprintDescriptor() {
        return "Routes/unified-camera-trap-route.xml";
    }

    @Override
    protected List<String> loadAdditionalPropertyFiles() {
        return Arrays.asList(KARAF_HOME + "/etc/edu.si.sidora.karaf.cfg", KARAF_HOME + "/etc/system.properties");
    }

    @Override
    protected String[] preventRoutesFromStarting() {
        return new String[]{"UnifiedCameraTrapInFlightConceptStatusPolling"};
    }

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
    @Ignore //Ignore this test b/c it needs fuseki running
    public void findObjectTest() throws Exception {

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        MockEndpoint mockError = getMockEndpoint("mock:error");
        mockError.expectedMessageCount(8);

        context.getRouteDefinition("UnifiedCameraTrapProcessParents").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("direct:processParents").skipSendToOriginalEndpoint().to("direct:findObject")
                        .to("mock:result");

            }
        });

        context.getRouteDefinition("UnifiedCameraTrapFindObject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(LogDefinition.class).selectFirst().before().to("mock:error");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelFedoraPid", "test:123456");
        exchange.getIn().setBody("Panama: Ticks &amp; Climate Change");

        template.send("direct:processParents", exchange);

        assertMockEndpointsSatisfied();
    }
}
