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

package edu.si.services.beans.edansidora;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.FilterDefinition;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.custommonkey.xmlunit.XMLAssert;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * @author jbirkhimer
 */
public class UCT_EdanSidoraTest extends EDAN_CT_BlueprintTestSupport {

    protected static final String EDAN_TEST_URI = System.getProperty("si.ct.uscbi.server");
    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static final String JMS_TEST_QUEUE = "edanIds.apim.update.test";

    private static File testManifest = new File(KARAF_HOME + "/unified-test-deployment/deployment_manifest.xml");
    private static String deploymentZipLoc = "src/test/resources/idsTest.zip"; //scbi_unified_test_deployment.zip";
    private static File deploymentZip;
    private static String expectedFileExists;

    private static Server server;

    @Override
    protected String getBlueprintDescriptor() {
        return "Routes/unified-camera-trap-route.xml";
    }

    /**
     * Starts up an edan endpoint to test against
     *
     * @throws Exception
     */
    @BeforeClass
    public static void startServer() throws Exception {

        System.getProperties().list(System.out);

//        EDAN_TEST_URI = "http://localhost:"+ System.getProperty("dynamic.test.port");
//
//        System.setProperty("si.ct.uscbi.server", EDAN_TEST_URI);

        System.out.println("===========[ EDAN_TEST_URI = " + EDAN_TEST_URI + " ]============");

        // start a simple front service
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        //factory.setAddress(EDAN_TEST_URI);
        factory.setResourceClasses(EdanTestService.class);

        server = factory.create();
        server.start();
        System.out.println("===========[ JAXRSServerFactory Address = " + "" + " ]============");
    }

    @AfterClass
    public static void stopServer() {
        server.stop();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deploymentZip = new File(deploymentZipLoc);
        log.debug("Exchange_FILE_NAME = {}", deploymentZip.getName());
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Test
    public void addImageToEdanAndIdsTest() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(1);

        context.getRouteDefinition("UnifiedCameraTrapAddImageToEdanAndIds").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //weaveByToString(".*edanApiBean.*").replace().log(LoggingLevel.INFO, "Skipping Sending to edanApiBean Bean");
                //weaveByToString(".*idsPushBean.*").replace().log(LoggingLevel.INFO, "Skipping Sending to idsPushBean Bean");
                weaveAddLast().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelFedoraPid", "test:12345");
        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest));

        exchange.getIn().setHeader("CamelFedoraPid", "test:32");
        exchange.getIn().setHeader("ExcludeCurrentImage", false);

        //Single-identification image in a two image sequence set:
//        exchange.getIn().setHeader("ImageSequenceID", "testDeploymentIds1");
//        exchange.getIn().setHeader("imageid", "testDeploymentIds1i1");
//        exchange.getIn().setHeader("CamelFileName", "testDeploymentIds1i1.JPG");
//        template.send("direct:addImageToEdanAndIds3", exchange);

        // Double-identification set:
        exchange.getIn().setHeader("ImageSequenceID", "testImageSequence3");
        exchange.getIn().setHeader("CamelFileName", "testRaccoonAndFox.JPG");
        exchange.getIn().setHeader("imageid", "RaccoonAndFox");
        template.send("direct:addImageToEdanAndIds", exchange);

//        // Double-identification set with raccoon and human:
//        exchange.getIn().setHeader("ImageSequenceID", "testImageSequence3");
//        exchange.getIn().setHeader("CamelFileName", "testImageRaccoonAndMan.JPG");
//        exchange.getIn().setHeader("imageid", "testImageRaccoonAndMan");
//        template.send("direct:addImageToEdanAndIds3", exchange);
//
//        // Set with human:
//        exchange.getIn().setHeader("ImageSequenceID", "testManAlone");
//        exchange.getIn().setHeader("CamelFileName", "testImageMan.JPG");
//        exchange.getIn().setHeader("imageid", "testImageMan");
//        template.send("direct:addImageToEdanAndIds3", exchange);

        /*String[] fileNames = {"testDeploymentIds1i1", "testDeploymentIds1i10", "testDeploymentIds2i1"};
        for (String fileName : fileNames) {
            exchange.getIn().setHeader("imageid", fileName);

            template.send("direct:addImageToEdanAndIds3", exchange);
        }*/

        //log.info("Headers:\n{}", mockEndpoint.getExchanges().get(0).getIn().getHeaders());

        for (Map.Entry<String, Object> entry : mockEndpoint.getExchanges().get(0).getIn().getHeaders().entrySet()) {
            if (!entry.getKey().toString().equals("ManifestXML") && !entry.getKey().toString().equals("edanJson")) {
                log.info(entry.toString());
            }
        }


        assertMockEndpointsSatisfied();

    }

    @Test
    public void addFilterSpeciesNameTest() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(3);
        mockEndpoint.expectedBodiesReceived("false", "true", "true", "true");

        context.getRouteDefinition("UnifiedCameraTrapAddImageToEdanAndIds").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                Namespaces ns = new Namespaces("ri", "http://www.w3.org/2005/sparql-results#");
                weaveByType(ChoiceDefinition.class).replace()
                        .log("=====================[ {{si.ct.edanids.speciesScientificName.filter}} ]========================")
                        .setBody()
                        .xpath("//ImageSequence[Image[ImageId/text() = $in:imageid]]/ResearcherIdentifications/Identification/SpeciesScientificName[contains(function:properties('si.ct.edanids.speciesScientificName.filter'), text())] != ''", String.class, ns, "ManifestXMl")
                        .to("log:test?showAll=true&multiline=true&maxChars=10000");
                weaveAddLast().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelFedoraPid", "test:12345");
        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest));
        exchange.getIn().setHeader("CamelFedoraPid", "test:32");
        exchange.getIn().setHeader("ExcludeCurrentImage", false);


        // Double-identification set:
        exchange.getIn().setHeader("ImageSequenceID", "testImageSequence3");
        exchange.getIn().setHeader("CamelFileName", "testRaccoonAndFox.JPG");
        exchange.getIn().setHeader("imageid", "RaccoonAndFox");
        template.send("direct:addImageToEdanAndIds", exchange);


        // Double-identification set with raccoon and human:
        exchange.getIn().setHeader("ImageSequenceID", "testRaccoonAndMan");
        exchange.getIn().setHeader("imageid", "testImageRaccoonAndMan");
        exchange.getIn().setHeader("CamelFileName", "testImageRaccoonAndMan.JPG");
        template.send("direct:addImageToEdanAndIds", exchange);

        // Set with human:
        exchange.getIn().setHeader("ImageSequenceID", "testManAlone");
        exchange.getIn().setHeader("imageid", "testImageMan");
        exchange.getIn().setHeader("CamelFileName", "testImageMan.JPG");
        template.send("direct:addImageToEdanAndIds", exchange);

        // Set with human and vehicle:
        exchange.getIn().setHeader("ImageSequenceID", "testRaccoonAndVehicleAndMan");
        exchange.getIn().setHeader("imageid", "testImageRaccoonAndVehicleAndMan");
        exchange.getIn().setHeader("CamelFileName", "testImageRaccoonAndVehicleAndMan.JPG");
        template.send("direct:addImageToEdanAndIds", exchange);

        assertMockEndpointsSatisfied();

        List<Exchange> result = mockEndpoint.getExchanges();

        assertEquals("RaccoonAndFox should not be filtered out", result.get(0).getIn().getBody(String.class), "false");
        assertEquals("testImageRaccoonAndMan should be filtered out", result.get(1).getIn().getBody(String.class), "true");
        assertEquals("testImageMan should be filtered out", result.get(2).getIn().getBody(String.class), "true");
        assertEquals("testImageMan should be filtered out", result.get(2).getIn().getBody(String.class), "true");
    }

    @Test
    public void idsPushTest() throws Exception {

        expectedFileExists = getExtra().getProperty("si.ct.uscbi.idsPushLocation") + "/ExportEmammal_emammal_image_testDeploymentId123/ExportEmammal_emammal_image_testDeploymentId123.xml";

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedFileExists(expectedFileExists);
        mockResult.setAssertPeriod(1500);

        context.getRouteDefinition("UnifiedCameraTrapStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(ChoiceDefinition.class).selectLast().replace()
                        .setHeader("SiteId").xpath("//CameraDeploymentID/text()", String.class, "ManifestXML")
                        .to("bean:idsPushBean?method=addToIgnoreList(testDeploymentIds4i1)")

                        .to("bean:idsPushBean?method=createAndPush")
                        .to("mock:result");
            }
        });

        template.sendBodyAndHeader("file:{{karaf.home}}/ProcessUnified", deploymentZip, Exchange.FILE_NAME, deploymentZip.getName());

        assertMockEndpointsSatisfied();

        String testIsilonDir = mockResult.getExchanges().get(0).getIn().getHeader("idsPushDir", String.class);

        log.warn("The test isilon directory we are testing for: {}", testIsilonDir);
        assertTrue("test Isilon directory should exist", Files.exists(new File(testIsilonDir).toPath()));

        log.info("test isilon dir = {}, file = {}", testIsilonDir, expectedFileExists);
        assertTrue("There should be a File in the Dir", Files.exists(new File(expectedFileExists).toPath()));
    }

    @Test
    public void edanIdsExceptionTest () throws Exception {
        Integer minEdanRedelivery = Integer.valueOf(getExtra().getProperty("min.edan.redeliveries"));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("redeliveryCount", minEdanRedelivery);

        MockEndpoint mockError = getMockEndpoint("mock:error");
        mockError.expectedMessageCount(1);
        mockError.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT).isInstanceOf(EdanIdsException.class);
        mockError.expectedHeaderReceived("redeliveryCount", minEdanRedelivery);

        context.getRouteDefinition("UnifiedCameraTrapAddImageToEdanAndIds").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //processor used to replace sql query to test onException and retries
                final Processor processor = new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        in.setHeader("redeliveryCount", in.getHeader(Exchange.REDELIVERY_COUNTER, Integer.class));
                            try {
                                if (in.getHeader("redeliveryCount", Integer.class) == minEdanRedelivery) {
                                    throw new IOException("Outer try exception");
                                }
                                try {
                                    throw new ConnectException("Inner Try Exception");
                                } catch (Exception e) {
                                    throw new EdanIdsException("EdanApiBean error sending Edan request", e);
                                }
                            } catch (Exception e) {
                                throw new EdanIdsException(e);
                            }
                    }
                };

                //advice sending to database and replace with processor
                weaveById("edanApiSendRequest").replace().process(processor);
                weaveById("logEdanIdsException").after().to("mock:error");
                weaveAddLast().to("mock:result").stop();
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelFedoraPid", "test:12345");
        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest));
        exchange.getIn().setHeader("CamelFedoraPid", "test:32");
        exchange.getIn().setHeader("ExcludeCurrentImage", false);


        // Double-identification set:
        exchange.getIn().setHeader("ImageSequenceID", "testImageSequence3");
        exchange.getIn().setHeader("CamelFileName", "testRaccoonAndFox.JPG");
        exchange.getIn().setHeader("imageid", "RaccoonAndFox");
        template.send("direct:addImageToEdanAndIds", exchange);

        assertMockEndpointsSatisfied();

    }
}
