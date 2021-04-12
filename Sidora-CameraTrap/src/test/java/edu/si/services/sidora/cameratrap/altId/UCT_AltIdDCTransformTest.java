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

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.builder.Namespaces;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

/**
 * @author jbirkhimer
 */
@Disabled("redundant test")
@CamelSpringBootTest
@SpringBootTest(
        classes = {CamelAutoConfiguration.class, UCT_AltIdDCTransformTest.Config.class},
        properties = {"logging.file.path=target/logs"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class UCT_AltIdDCTransformTest {

    private static final Logger log = LoggerFactory.getLogger(UCT_AltIdTest.class);

    @Autowired
    CamelContext context;

    //@EndpointInject(value = "direct:addFITSDataStream")
    @Autowired
    ProducerTemplate template;

    //Camera Trap Deployment Manifest and Field values
    private File manifestLegacyFile = new File("src/test/resources/validDeploymentPkg/deployment_manifest.xml");
    private File manifestUnifiedFile = new File("src/test/resources/AltIdSampleData/Unified/deployment_manifest.xml");
    private File manifestWCSFile = new File("src/test/resources/UnifiedManifest-TestFiles/scbi_unified_stripped_p125d18981/deployment_manifest.xml");
    private String testManifest;

    @Test
    public void projectXsltOutput_Test() throws Exception {

        //Store the Deployment Manifest as string to set the camel ManifestXML header
        testManifest = FileUtils.readFileToString(manifestUnifiedFile, "utf-8");

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelFedoraPid", "test:0001");
        exchange.getIn().setBody(String.valueOf(testManifest));

        MockEndpoint mock = context.getEndpoint("mock:resultProject", MockEndpoint.class);
        mock.setMinimumExpectedMessageCount(1);

        template.send("direct:projectTest", exchange);

        mock.assertIsSatisfied();
    }

    @Test
    public void subprojectXSLTOutput_Test() throws Exception {

        testManifest = FileUtils.readFileToString(manifestUnifiedFile, "utf-8");

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelFedoraPid", "test:0002");
        exchange.getIn().setBody(String.valueOf(testManifest));

        MockEndpoint mockEndpoint = context.getEndpoint("mock:resultSubproject", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        template.send("direct:subprojectTest", exchange);

        mockEndpoint.assertIsSatisfied();
    }

    @TestConfiguration
    public static class Config extends RouteBuilder {

            @Override
            public void configure() throws Exception {

                Namespaces ns = new Namespaces("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
                ns.add("dc", "http://purl.org/dc/elements/1.1/");
                ns.add("eac", "urn:isbn:1-931666-33-4");
                ns.add("xlink", "http://www.w3.org/1999/xlink");

                from("direct:projectTest")
                        //Create the EAC-CPF
                        .to("xslt-saxon:config/xslt/ManifestProject.xsl")
                        .log("Manifest to EAC-CPF Transform Output:\n${body}")
                        //Update the DC datastream with with dc:identifier for the Alternate ID
                        .to("xslt-saxon:config/xslt/Manifest2AltIdDC_identifier.xsl")
                        .log("EAC-CPF to DC Transform Output:\n${body}")
                        .to("mock:resultProject");

                from("direct:subprojectTest")
                        //Create the EAC-CPF
                        .to("xslt-saxon:config/xslt/ManifestSubproject.xsl")
                        .log("Manifest to EAC-CPF Transform Output:\n${body}")
                        //Update the DC datastream with with dc:identifier for the Alternate ID
                        .to("xslt-saxon:config/xslt/Manifest2AltIdDC_identifier.xsl")
                        .log("EAC-CPF to DC Transform Output:\n${body}")
                        .to("mock:resultSubproject");


            }
    }
}
