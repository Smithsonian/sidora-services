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
import org.apache.camel.model.SetBodyDefinition;
import org.apache.camel.model.SplitDefinition;
import org.apache.camel.model.ToDynamicDefinition;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.nio.file.Files;

import static org.apache.camel.test.junit5.TestSupport.deleteDirectory;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author jbirkhimer
 */
@CamelSpringBootTest
@SpringBootTest(properties = {
        "logging.file.path=target/logs",
        "processing.dir.base.path=${user.dir}/target",
        "si.ct.uscbi.enableS3Routes=false",
        "camel.springboot.java-routes-exclude-pattern=UnifiedCameraTrapInFlightConceptStatusPolling,UnifiedCameraTrapStartProcessing"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class UCT_DeleteCacheDirOnFailTest {

    private static final Logger log = LoggerFactory.getLogger(UCT_DeleteCacheDirOnFailTest.class);

    @Autowired
    CamelContext context;

    //@EndpointInject(value = "direct:addFITSDataStream")
    @Autowired
    ProducerTemplate template;

    private static String LOG_NAME = "edu.si.mci";

    String testDataDir = "src/test/resources/UnifiedManifest-TestFiles";
    File deploymentZip = new File(testDataDir + "/scbi_unified_test_deployment.zip");
    String deploymentCacheDir;
    String expectedFileExists;

    @PropertyInject(value = "{{si.ct.uscbi.data.dir.path}}")
    private String processDirPath;
    @PropertyInject(value = "{{si.ct.uscbi.process.done.dir.path}}")
    private String processDoneDirPath;
    @PropertyInject(value = "{{si.ct.uscbi.process.error.dir.path}}")
    private String processErrorDirPath;
    @PropertyInject(value = "{{si.ct.uscbi.process.dir.path}}")
    private String processDataDirPath;

    private String s3UploadSuccessDirPath;
    private String s3UploadErrorDirPath;

    @BeforeEach
    public void setUp() throws Exception {

        deleteDirectory(processDirPath);
        deleteDirectory(processDataDirPath);
        deleteDirectory(processDoneDirPath);
        deleteDirectory(processErrorDirPath);

        //Modify the default error handler so that we can send failed exchanges to mock:result for assertions
        // Sending to dead letter does not seem to work as expected for this
        context.adapt(ExtendedCamelContext.class).setErrorHandlerFactory(new DefaultErrorHandlerBuilder().onPrepareFailure(exchange -> template.send("mock:result", exchange)));

        log.debug("Exchange_FILE_NAME = {}", deploymentZip.getName());
        template.sendBodyAndHeader("file:{{si.ct.uscbi.process.dir.path}}", deploymentZip, Exchange.FILE_NAME, deploymentZip.getName());
    }

    @Test
    public void testIllegalArgumentException(TestInfo testInfo) throws Exception {
        expectedFileExists = processErrorDirPath + "/" + deploymentZip.getName();
        AdviceWith.adviceWith(context, "UnifiedCameraTrapStartProcessing", false, a ->
                a.weaveById("schematronValidation").replace().setHeader("CamelSchematronValidationStatus").simple("FAILED"));
        runTest(testInfo);
    }

    @Test
    public void testConnectException(TestInfo testInfo) throws Exception {
        expectedFileExists = processErrorDirPath + "/" + deploymentZip.getName();

        MockEndpoint mockError = context.getEndpoint("mock:error", MockEndpoint.class);
        mockError.expectedMessageCount(1);
        mockError.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT).isInstanceOf(ConnectException.class);
        mockError.expectedHeaderReceived("redeliveryCount", context.resolvePropertyPlaceholders("{{min.connectEx.redeliveries}}"));
        mockError.expectedFileExists(expectedFileExists);
        mockError.setAssertPeriod(7000);

        AdviceWith.adviceWith(context, "UnifiedCameraTrapProcessParents", false, a ->{
                a.weaveByType(SetBodyDefinition.class).selectFirst().before().to("mock:result");
                a.weaveById("logConnectException").after().to("mock:error");
        });

        AdviceWith.adviceWith(context, "UnifiedCameraTrapFindObjectByPIDPredicate", false, a ->{
                a.weaveByType(ToDynamicDefinition.class).selectFirst().replace().process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        in.setHeader("redeliveryCount", in.getHeader(Exchange.REDELIVERY_COUNTER, Integer.class));
                        throw new ConnectException("Simulating Connection Exception");
                    }
                });
        });

        runTest(testInfo);
    }

    @Test
    public void testDeploymentPackageException(TestInfo testInfo) throws Exception {
        expectedFileExists = processErrorDirPath + "/" + deploymentZip.getName();
        AdviceWith.adviceWith(context, "UnifiedCameraTrapValidatePackage", false, a ->
                a.weaveByType(ChoiceDefinition.class).selectFirst().replace().throwException(DeploymentPackageException.class, "Simulating resource counts do not match Exception"));
        runTest(testInfo);
    }

    @Test
    public void testFileNotFoundException(TestInfo testInfo) throws Exception {
        expectedFileExists = processErrorDirPath + "/" + deploymentZip.getName();
        AdviceWith.adviceWith(context, "UnifiedCameraTrapValidatePackage", false, a ->
                a.weaveByType(SplitDefinition.class).selectFirst().replace().throwException(FileNotFoundException.class, "Simulating File Not Found Exception"));
        runTest(testInfo);
    }

    @Test
    public void testFedoraObjectNotFoundException(TestInfo testInfo) throws Exception {
        expectedFileExists = processDoneDirPath + "/" + deploymentZip.getName();
        AdviceWith.adviceWith(context, "UnifiedCameraTrapStartProcessing", true, a ->
                a.weaveByType(ChoiceDefinition.class).selectLast().replace().to("direct:findObjectByPIDPredicate").to("mock:result"));

        AdviceWith.adviceWith(context, "UnifiedCameraTrapFindObjectByPIDPredicate", true, a ->
                a.weaveByType(ToDynamicDefinition.class).replace().log(LoggingLevel.INFO, "Skipping Fuseki Call").setBody().simple("<?xml version=\"1.0\"?>\n" +
                        "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">\n" +
                        "  <head>\n" +
                        "  </head>\n" +
                        "  <boolean>false</boolean>\n" +
                        "</sparql>"));
        runTest(testInfo);
    }

    public void runTest(TestInfo testInfo) throws Exception {

        String exceptionTestName = testInfo.getDisplayName();

        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMinimumMessageCount(1);
        mockResult.expectedFileExists(expectedFileExists);
        mockResult.setAssertPeriod(7000);

        context.start();

        Thread.sleep(1500);

        mockResult.assertIsSatisfied();

        deploymentCacheDir = mockResult.getExchanges().get(0).getIn().getHeader("deploymentDataDir", String.class);

        log.debug("The deployment cache directory we are testing for: {}", deploymentCacheDir);
        boolean cacheDirExists = Files.exists(new File(deploymentCacheDir).toPath());
        log.debug("deploymentCacheDir exists: {}", cacheDirExists);
        //log.debug("CamelExceptionCaught = {}", mockResult.getExchanges().get(0).getProperty("CamelExceptionCaught"));
        /*if (exceptionTestName.contains("FedoraObjectNotFoundException")) {
            assertTrue(cacheDirExists, "Cache directory should exist");
        } else {
            assertTrue(!cacheDirExists, "Cache directory should not exist");
        }*/
        assertTrue(!cacheDirExists, "Cache directory should not exist");
        assertTrue(Files.exists(new File(expectedFileExists).toPath()), "There should be a File in the Dir");
    }

    @Test
    public void testCtIngestDoneDir(TestInfo testInfo) throws Exception {
        expectedFileExists = processDoneDirPath + "/" + deploymentZip.getName();

        log.info("Expected Done File: {}", expectedFileExists);

        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMinimumMessageCount(1);
        mockResult.expectedFileExists(expectedFileExists);

        AdviceWith.adviceWith(context, "UnifiedCameraTrapStartProcessing", false, a ->
                a.weaveById("ctThreads").before().to("mock:result").stop());

        mockResult.assertIsSatisfied();
    }

    @Test
    public void testCtIngestErrorDir(TestInfo testInfo) throws Exception {
        expectedFileExists = processErrorDirPath + "/" + deploymentZip.getName();

        log.info("Expected Error File: {}", expectedFileExists);

        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMinimumMessageCount(1);
        mockResult.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT).isInstanceOf(Exception.class);
        mockResult.expectedFileExists(expectedFileExists);
        mockResult.setAssertPeriod(7000);

        AdviceWith.adviceWith(context, "UnifiedCameraTrapStartProcessing", false, a ->
                a.weaveById("ctThreads").before().process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        in.setHeader("redeliveryCount", in.getHeader(Exchange.REDELIVERY_COUNTER, Integer.class));
                        throw new Exception("Simulating Exception");
                    }
                }));

        mockResult.assertIsSatisfied();
    }
}
