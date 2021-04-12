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

import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jbirkhimer
 */
@CamelSpringBootTest
@SpringBootTest(classes = {CamelAutoConfiguration.class, CameraTrapIngestManifestSchemaValidationTest.Config.class},
        properties = {"logging.file.path=target/logs"}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class CameraTrapIngestManifestSchemaValidationTest {

    private static final Logger log = LoggerFactory.getLogger(AwsS3PollAndUploadRouteTest.class);

    @Autowired
    private CamelContext context;
    @Autowired
    private ProducerTemplate template;

    //Camera Trap Deployment Manifest for tests
    private File manifestUnifiedFile = new File("src/test/resources/Manifest_Schema_Validation_TestFiles/unified_deployment_manifest.xml");

    private String manifest;
    private String xsdFile, schFile;

    //Camel Headers Map
    private Map<String, Object> headers;

    @Test
    public void unifiedManifestSchematromValidationTests() throws Exception {
        manifest = FileUtils.readFileToString(manifestUnifiedFile);
        xsdFile = "Unified_eMammalDeploymentManifest.xsd";
        schFile = "Unified_eMammalDeploymentManifest.sch";

        runManifestSchematronValidationTest("ProjectId", "nonIntTestValue", "SUCCESS"); //ProjectId text value changed to nonIntRestValue
        runManifestSchematronValidationTest("ProjectId", "FAILED"); //ProjectId with empty text value
        runManifestSchematronValidationTest("ProjectName", "FAILED"); //ProjectName with empty text value
        runManifestSchematronValidationTest("SUCCESS"); //Valid Manifest
    }

    /**
     * Manifest Schema Validation Testing Route
     * @throws Exception
     */
    private void runManifestSchematronValidationTest(String camelSchematronValidationStatus) throws Exception {
        runManifestSchematronValidationTest(null, null, camelSchematronValidationStatus);
    }

    /**
     * Manifest Schema Validation Testing Route
     * @param fieldToTest the manifest node being tested
     * @throws Exception
     */
    private void runManifestSchematronValidationTest(String fieldToTest, String camelSchematronValidationStatus) throws Exception {
        runManifestSchematronValidationTest(fieldToTest, null, camelSchematronValidationStatus);
    }

    /**
     * Manifest Schema Validation Testing Route
     * @param fieldToTest the manifest node being tested
     * @param newFieldValue the new value for the node being tested
     * @throws Exception
     */
    public void runManifestSchematronValidationTest(String fieldToTest, String newFieldValue, String camelSchematronValidationStatus) throws Exception {

        headers = new HashMap<>();
        if (fieldToTest != null) {
            headers.put("fieldToTest", fieldToTest);
            headers.put("newFieldValue", newFieldValue);
        }
        headers.put("xsdFile", xsdFile);
        headers.put("schFile", schFile);

        MockEndpoint mockEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.setRetainLast(1);
        mockEndpoint.setMinimumExpectedMessageCount(1);
        mockEndpoint.expectedHeaderReceived("CamelSchematronValidationStatus", camelSchematronValidationStatus);

        template.sendBodyAndHeaders("direct:start", manifest, headers);

        mockEndpoint.assertIsSatisfied();
        mockEndpoint.reset();
    }

    @TestConfiguration
    public static class Config extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            // logging configuration for tracer and formatter
            //Tracer tracer = new Tracer();
            //tracer.setLogLevel(LoggingLevel.WARN);
            //tracer.setTraceOutExchanges(true);
            //DefaultTraceFormatter formatter = new DefaultTraceFormatter();
            //formatter.setShowOutBody(true);
            //formatter.setShowOutBodyType(true);
            //formatter.setShowShortExchangeId(true);
            //formatter.setMaxChars(500);
            //tracer.setFormatter(formatter);
            //getContext().addInterceptStrategy(tracer);

            /**
             * Generic Route used for AdviceWithRouteBuilder
             */
            from("direct:start")
                    .routeId("testRoute")
                    .choice()
                        .when(header("fieldToTest"))
                            .log(LoggingLevel.INFO, "=======(0)======= Testing Manifest with \"${header.newFieldValue}\" value for: ${header.fieldToTest}")
                            //use and xslt transform to modify the manifest for testing purposes
                            .to("xslt-saxon:file:src/test/resources/Manifest_Schema_Validation_TestFiles/paramUpdateManifest.xsl")
                            .log(LoggingLevel.DEBUG, "=======(1)======= Modified manifest being tested:\n${body}")
                        //.endChoice()
                        .otherwise()
                            .log(LoggingLevel.INFO, "=======(0)======= Testing Unmodified Manifest")
                            .log(LoggingLevel.DEBUG, "=======(1)======= Unmodified manifest being tested:\n${body}")
                        //.endChoice()
                        .end()
                    //check the manifest using xsd schema
                    .toD("validator:file:config/schemas/${header.xsdFile}")
                    //check the manifest using schematron
                    .toD("schematron:file:config/schemas/${header.schFile}")
                    .log(LoggingLevel.INFO, "=======(2)======= Schematron Validation Status - ${header.CamelSchematronValidationStatus}")
                    .choice()
                        .when(simple("${header.CamelSchematronValidationStatus} == 'FAILED'"))
                            .log(LoggingLevel.DEBUG, "*************************************************************\n"
                                    + "Schematron Validation Status - ${header.CamelSchematronValidationStatus}\n"
                                    + "----------------------------------------------------------------\n"
                                    + "Schematron Validation Report -\n ${header.CamelSchematronValidationReport}\n"
                                    + "*************************************************************\n")
                        .end()
                    .to("mock:result");
        }
    }
}
