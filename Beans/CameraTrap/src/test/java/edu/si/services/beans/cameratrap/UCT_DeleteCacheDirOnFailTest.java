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

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.*;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;

/**
 * @author jbirkhimer
 */
public class UCT_DeleteCacheDirOnFailTest extends CT_BlueprintTestSupport {

    private static String LOG_NAME = "edu.si.mci";

    private static final String KARAF_HOME = System.getProperty("karaf.home");

    String deploymentZipLoc = "src/test/resources/UnifiedManifest-TestFiles/scbi_unified_test_deployment.zip";
    String cacheDir = KARAF_HOME + "/UnifiedCameraTrapData";
    String deploymentCacheDir = KARAF_HOME + "/UnifiedCameraTrapData/" + FilenameUtils.getBaseName(deploymentZipLoc);
    File deploymentZip;
    String expectedFileExists;

    private static final CountDownLatch LATCH = new CountDownLatch(1);

    @Override
    protected String getBlueprintDescriptor() {
        return "Routes/unified-camera-trap-route.xml";
    }

    @Override
    protected String[] preventRoutesFromStarting() {
        return new String[]{"UnifiedCameraTrapInFlightConceptStatusPolling"};
    }

    @Override
    public void setUp() throws Exception {
        System.setProperty("si.ct.uscbi.enableS3Routes", "true");

        deleteDirectory(KARAF_HOME + "/ProcessUnified");
        deleteDirectory(KARAF_HOME + "/UnifiedCameraTrapData");

        super.setUp();

        context.getRouteDefinition("CameraTrapDeploymentsPulldownFromS3").noAutoStartup();

        deploymentZip = new File(deploymentZipLoc);
        log.debug("Exchange_FILE_NAME = {}", deploymentZip.getName());

        //Modify the default error handler so that we can send failed exchanges to mock:result for assertions
        // Sending to dead letter does not seem to work as expected for this
        context.setErrorHandlerBuilder(new DefaultErrorHandlerBuilder().onPrepareFailure(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                template.send("mock:result", exchange);
            }
        }));

        template.sendBodyAndHeader("file:{{karaf.home}}/ProcessUnified", deploymentZip, Exchange.FILE_NAME, deploymentZip.getName());
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Test
    public void testIllegalArgumentException() throws Exception {
        expectedFileExists = KARAF_HOME + "/ProcessUnified/Error_UnifiedCameraTrap/s3_upload_success/" + deploymentZip.getName();
        context.getRouteDefinition("UnifiedCameraTrapStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("schematronValidation").replace().setHeader("CamelSchematronValidationStatus").simple("FAILED");
            }
        });
        runTest();
    }

    @Test
    public void testConnectException() throws Exception {

        Integer minConnectExRedelivery = Integer.valueOf(getExtra().getProperty("min.connectEx.redeliveries"));

        expectedFileExists = KARAF_HOME + "/ProcessUnified/Error_UnifiedCameraTrap/s3_upload_success/" + deploymentZip.getName();

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMinimumMessageCount(1);
        mockResult.expectedFileExists(expectedFileExists);
        mockResult.setAssertPeriod(7000);

        MockEndpoint mockError = getMockEndpoint("mock:error");
        mockError.expectedMessageCount(1);
        mockError.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT).isInstanceOf(ConnectException.class);
        mockError.expectedHeaderReceived("redeliveryCount", minConnectExRedelivery);
        mockError.expectedFileExists(expectedFileExists);
        mockResult.setAssertPeriod(7000);

        context.getRouteDefinition("UnifiedCameraTrapProcessParents").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(SetBodyDefinition.class).selectFirst().before().to("mock:result");
                weaveById("logConnectException").after().to("mock:error");
            }
        });

        context.getRouteDefinition("UnifiedCameraTrapFindObjectByPIDPredicate").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //processor used to replace sql query to test onException and retries
                final Processor processor = new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        in.setHeader("redeliveryCount", in.getHeader(Exchange.REDELIVERY_COUNTER, Integer.class));
                            throw new ConnectException("Simulating Connection Exception");
                    }
                };

                //advice sending to database and replace with processor
                weaveByType(ToDynamicDefinition.class).selectFirst().replace().process(processor);

            }
        });

        context.start();

        Thread.sleep(1500);

        assertMockEndpointsSatisfied();

        log.debug("The deployment cache directory we are testing for: {}", deploymentCacheDir);
        boolean cacheDirExists = Files.exists(new File(deploymentCacheDir).toPath());
        log.debug("deploymentCacheDir exists: {}", cacheDirExists);
        //log.debug("CamelExceptionCaught = {}", mockResult.getExchanges().get(0).getProperty("CamelExceptionCaught"));
        assertTrue("Cache directory should not exist", !cacheDirExists);

        log.info("cache dir = {}, file = {}", cacheDir, expectedFileExists);
        assertTrue("There should be a File in the Dir", Files.exists(new File(expectedFileExists).toPath()));
    }

    @Test
    public void testDeploymentPackageException() throws Exception {
        expectedFileExists = KARAF_HOME + "/ProcessUnified/Error_UnifiedCameraTrap/s3_upload_success/" + deploymentZip.getName();
        context.getRouteDefinition("UnifiedCameraTrapValidatePackage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(ChoiceDefinition.class).selectFirst().replace().throwException(DeploymentPackageException.class, "Simulating resource counts do not match Exception");
            }
        });
        runTest();
    }

    @Test
    public void testFileNotFoundException() throws Exception {
        expectedFileExists = KARAF_HOME + "/ProcessUnified/Error_UnifiedCameraTrap/s3_upload_success/" + deploymentZip.getName();
        context.getRouteDefinition("UnifiedCameraTrapValidatePackage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(SplitDefinition.class).selectFirst().replace().throwException(FileNotFoundException.class, "Simulating File Not Found Exception");
            }
        });
        runTest();
    }

    @Test
    public void testFedoraObjectNotFoundException() throws Exception {
        expectedFileExists = KARAF_HOME + "/ProcessUnified/Done/s3_upload_success/" + deploymentZip.getName();
        context.getRouteDefinition("UnifiedCameraTrapStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(ChoiceDefinition.class).selectLast().replace().to("direct:findObjectByPIDPredicate").to("mock:result");
            }
        });
        context.getRouteDefinition("UnifiedCameraTrapFindObjectByPIDPredicate").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(ToDynamicDefinition.class).replace().log(LoggingLevel.INFO, "Skipping Fuseki Call").setBody().simple("<?xml version=\"1.0\"?>\n" +
                        "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">\n" +
                        "  <head>\n" +
                        "  </head>\n" +
                        "  <boolean>false</boolean>\n" +
                        "</sparql>");
            }
        });
        runTest();
    }

    public void runTest() throws Exception {

        String exceptionTestName = this.getTestMethodName();

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMinimumMessageCount(1);
        mockResult.expectedFileExists(expectedFileExists);
        mockResult.setAssertPeriod(7000);
        context.start();

        Thread.sleep(1500);

        assertMockEndpointsSatisfied();

        log.debug("The deployment cache directory we are testing for: {}", deploymentCacheDir);
        boolean cacheDirExists = Files.exists(new File(deploymentCacheDir).toPath());
        log.debug("deploymentCacheDir exists: {}", cacheDirExists);
        //log.debug("CamelExceptionCaught = {}", mockResult.getExchanges().get(0).getProperty("CamelExceptionCaught"));
        if (exceptionTestName.contains("FedoraObjectNotFoundException")) {
            assertTrue("Cache directory should exist", cacheDirExists);
        } else {
            assertTrue("Cache directory should not exist", !cacheDirExists);
        }

        log.info("cache dir = {}, file = {}", cacheDir, expectedFileExists);
        assertTrue("There should be a File in the Dir", Files.exists(new File(expectedFileExists).toPath()));
    }
}
