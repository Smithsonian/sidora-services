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
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.builder.Namespaces;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
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
import java.util.List;
import java.util.Map;

/**
 * @author jbirkhimer
 */
@CamelSpringBootTest
@SpringBootTest(classes = {CamelAutoConfiguration.class, SchematronTest.Config.class},
        properties = {"logging.file.path=target/logs"}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class SchematronTest {

    private static final Logger LOG = LoggerFactory.getLogger(SchematronTest.class);

    @Autowired
    private CamelContext context;
    @Autowired
    private ProducerTemplate template;

    private File manifestFile = new File("src/test/resources/Manifest_Schema_Validation_TestFiles/unified_deployment_manifest.xml");

    @TestConfiguration
    public static class Config extends RouteBuilder {
        @Override
        public void configure() throws Exception {

            Namespaces ns = new Namespaces("svrl", "http://purl.oclc.org/dsdl/svrl");

            from("direct:start").routeId("schematronValidationTestRoute")
                    .convertBodyTo(String.class)
                    .log(LoggingLevel.DEBUG, "${id}: Body:\n${body}")
                    .to("schematron:file:config/schemas/Unified_eMammalDeploymentManifest.sch").id("schematronValidation")
                    .log(LoggingLevel.INFO, "${id}: Schematron Validation Status - ${header.CamelSchematronValidationStatus}")
                    .choice()
                        .when(simple("${in.header.CamelSchematronValidationStatus} == 'FAILED'"))
                            .log(LoggingLevel.ERROR, "${id}: Schematron Validation Status - ${header.CamelSchematronValidationStatus}")
                            .log(LoggingLevel.DEBUG, "${id}: Schematron Validation Report -\\n ${header.CamelSchematronValidationReport}\"")
                            .setBody().xpath("//svrl:failed-assert/svrl:text/text()", List.class, ns, "CamelSchematronValidationReport")
                            .log(LoggingLevel.ERROR, "${id}: Schematron validation error(s):\n${body}")
                            //.throwException(new IllegalArgumentException("Schematron validation failed."))
                            .to("mock:error")
                        .endChoice()
                        .otherwise()
                            .log(LoggingLevel.INFO, "${id}: Schematron validation succeeded.")
                            .to("mock:result")
                        .endChoice()
                    .end();
        }
    }

    @Test
    public void schematronSuccessTest() throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        MockEndpoint mockFailed = context.getEndpoint("mock:error", MockEndpoint.class);
        mockFailed.expectedMessageCount(0);

        template.sendBody("direct:start", manifestFile);

        mockEndpoint.assertIsSatisfied();
        mockFailed.assertIsSatisfied();
    }

    @Test
    public void schematronFailedTest() throws Exception {

        AdviceWith.adviceWith(context, "schematronValidationTestRoute", true, a-> {
            a.weaveById("schematronValidation").before()
                    //use and xslt transform to modify the manifest for testing purposes
                    .to("xslt-saxon:file:src/test/resources/Manifest_Schema_Validation_TestFiles/paramUpdateManifest.xsl")
                    .log(LoggingLevel.DEBUG, "${id}: Body:\n${body}");
        });

        MockEndpoint mockEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(0);

        MockEndpoint mockFailed = context.getEndpoint("mock:error", MockEndpoint.class);
        mockFailed.expectedMessageCount(1);

        Map<String, Object> headers = new HashMap<>();
        headers.put("fieldToTest", "ProjectId");
        headers.put("newFieldValue", null);

        template.sendBodyAndHeaders("direct:start", manifestFile, headers);

        mockFailed.assertIsSatisfied();
        mockEndpoint.assertIsSatisfied();
    }
}
