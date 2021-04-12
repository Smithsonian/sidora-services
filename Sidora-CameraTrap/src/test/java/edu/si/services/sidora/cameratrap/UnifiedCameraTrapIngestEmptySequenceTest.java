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

package edu.si.services.sidora.cameratrap;

import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.apache.camel.test.junit5.TestSupport.deleteDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the Unified Camera Trap Ingest Pipeline
 * TODO: Add more tests
 * @author jbirkhimer
 */
@Disabled("test is not complete, still needs work")
@CamelSpringBootTest
@SpringBootTest(properties = {
        "logging.file.path=target/logs",
        "processing.dir.base.path=${user.dir}/target",
        "si.ct.uscbi.enableS3Routes=false",
        "camel.springboot.java-routes-exclude-pattern=UnifiedCameraTrapInFlightConceptStatusPolling,UnifiedCameraTrapStartProcessing"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class UnifiedCameraTrapIngestEmptySequenceTest {

    private static final Logger log = LoggerFactory.getLogger(UnifiedCameraTrapIngestEmptySequenceTest.class);

    @Autowired
    CamelContext context;

    //@EndpointInject(value = "direct:addFITSDataStream")
    @Autowired
    ProducerTemplate template;

    //Test Data Directory contains the datastreams and other resources for the tests
    private String testDataDir = "src/test/resources/UnifiedManifest-TestFiles";

    //Camera Trap Deployment Info for testing
    private String deploymentId = "10002000";
    private int ManifestCameraDeploymentId = 0001;

    //Mock endpoint to be used for assertions
    private MockEndpoint mockEndpoint;

    //Camel Headers Map
    private Map<String, Object> headers;

    //Camera Trap Deployment Manifest
    private File manifestFile = new File(testDataDir + "/emptySequenceDeploymentPkg/deployment_manifest.xml");
    private String manifest;
    File deploymentZip;

    @PropertyInject(value = "{{si.ct.uscbi.process.dir.path}}")
    private String processDirPath;
    @PropertyInject(value = "{{si.ct.uscbi.process.done.dir.path}}")
    private String processDoneDirPath;
    @PropertyInject(value = "{{si.ct.uscbi.process.error.dir.path}}")
    private String processErrorDirPath;

    /**
     * Initialize the camel headers, deployment manifest, and test data
     * @throws Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        //Store the Deployment Manifest as string to set the camel ManifestXML header
        manifest = FileUtils.readFileToString(manifestFile);

        //Initialize the expected camel headers
        headers = new HashMap<>();
        headers.put("deploymentId", deploymentId);
        headers.put("ManifestCameraDeploymentId", ManifestCameraDeploymentId);
        headers.put("ManifestXML", String.valueOf(manifest));
        headers.put("validationErrors", "validationErrors");
        headers.put("ProjectPID", "test:0000");
        headers.put("SitePID", "test:0000");
        headers.put("CamelFedoraPid", "test:1");

        deleteDirectory(processDirPath);
        deleteDirectory(processDoneDirPath);
        deleteDirectory(processErrorDirPath);
    }

    /**
     * Testing the headers created in the UnifiedCameraTrapAddImageResource route
     * @throws Exception
     * TODO: Add more test coverage for this route
     */
    @Test
    public void addImageResourceEmptySequence_Test() throws Exception {
        //RouteId and endpoint uri
        String routeId = "UnifiedCameraTrapAddImageResource";
        String routeURI = "direct:addImageResource";

        //The expected header values
        int skippedImageCountHeaderExpected = 0;

        mockEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);

        MockEndpoint mockError = context.getEndpoint("mock:error", MockEndpoint.class);

        //Configure and use adviceWith to mock for testing purpose
        AdviceWith.adviceWith(context, routeId, false, a -> {
                    a.onException(IllegalArgumentException.class).to("mock:error").continued(true);

                    //add the mock:result endpoint and stop the route after the headers we are testing have been created
                    a.weaveByToString(".*reader:file.*").before().to("mock:result").stop();

                    a.weaveByType(ChoiceDefinition.class).before().process(exchange -> {
                       Message out = exchange.getIn();
                       log.debug("debug here");
                    });
                }
        );

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedHeaderReceived("AdjustedResourceCount", 0);

        mockError.expectedMessageCount(1);

        template.sendBodyAndHeaders(routeURI, "2970s1i1.JPG", headers);

        mockEndpoint.assertIsSatisfied();
        mockError.assertIsSatisfied();

        int skippedImageCountHeaderResult = mockEndpoint.getExchanges().get(0).getIn().getHeader("AdjustedResourceCount", Integer.class);

        //Assertions
        assertEquals(skippedImageCountHeaderExpected, skippedImageCountHeaderResult, "imageid header assertEquals failed!");
    }
}
