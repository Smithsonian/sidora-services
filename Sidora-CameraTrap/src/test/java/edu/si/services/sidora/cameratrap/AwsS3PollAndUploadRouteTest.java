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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.*;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.s3.S3Component;
import org.apache.camel.component.aws.s3.S3Constants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.LogDefinition;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.DisableJmx;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.Assertions;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.apache.camel.test.junit5.TestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Unit Tests for CameraTrap AWS S3 routes using a Mock AmazonS3Client.
 * @author jbirkhimer
 */
@CamelSpringBootTest
@SpringBootTest(
        properties = {"logging.file.path=target/logs",
                "processing.dir.base.path=${user.dir}/target",
                "si.ct.uscbi.enableS3Routes=true",
                "camel.component.aws-s3.auto-discover-client=false",
                "camel.component.aws-s3.autowired-enabled=false"}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisableJmx
@ActiveProfiles("test")
@UseAdviceWith
public class AwsS3PollAndUploadRouteTest {

    private static final Logger log = LoggerFactory.getLogger(AwsS3PollAndUploadRouteTest.class);

    @Autowired
    private CamelContext context;
    @Autowired
    private ProducerTemplate template;

    String deploymentZipLoc = "src/test/resources/UnifiedManifest-TestFiles/scbi_unified_test_deployment.zip";
    File deploymentZip;
    String expectedFileExists;

    AmazonS3ClientMock amazonS3Client;
    String s3bucket;
    List<Bucket> s3Buckets;
    List<S3ObjectSummary> s3objectList;
    S3Object s3Object;

    @PropertyInject(value = "{{si.ct.uscbi.process.dir.path}}")
    private String processDirPath;

    @PropertyInject(value = "{{si.ct.uscbi.stage.dir.path}}")
    private String stageDirPath;

    @PropertyInject(value = "{{si.ct.uscbi.process.done.dir.path}}")
    private String processDoneDirPath;

    @PropertyInject(value = "{{si.ct.uscbi.process.error.dir.path}}")
    private String processErrorDirPath;

    @PropertyInject(value = "{{si.ct.uscbi.process.dir.path}}")
    private String processDataDirPath;

    @PropertyInject(value = "{{si.ct.external.upload.success.dir}}")
    private String s3UploadSuccessDirPath;
    @PropertyInject(value = "{{si.ct.external.upload.error.dir}}")
    private String s3UploadErrorDirPath;

//    @PropertyInject(value = "{{camel.component.aws-s3.access-key}}")
    @PropertyInject(value = "{{si.ct.uscbi.aws.accessKey}}")
    private String accessKey;
//    @PropertyInject(value = "{{camel.component.aws-s3.secret-key}}")
    @PropertyInject(value = "{{si.ct.uscbi.aws.secretKey}}")
    private String secretKey;
    @PropertyInject(value = "{{si.ct.uscbi.s3.approved.bucketName}}")
    private String s3ApprovedBucket;
    @PropertyInject(value = "{{si.ct.uscbi.s3.ingested.bucketName}}")
    private String s3IngestBucket;
    @PropertyInject(value = "{{si.ct.uscbi.s3.rejected.bucketName}}")
    private String s3RejectedBucket;


    @BeforeEach
    public void setUp() throws Exception {
        deleteTestDirectories();

        deploymentZip = new File(deploymentZipLoc);
        log.debug("Exchange_FILE_NAME = {}", deploymentZip.getName());

        configureS3();
    }

    private void configureS3() {
        /*
            For integration testing:
            look into using camel-test-infra-aws-v1 and LocalStackContainer.Service.S3
         */

        //Initialize the Mock AmazonS3Client
        amazonS3Client = new AmazonS3ClientMock(accessKey, secretKey);

        context.getComponent("aws-s3", S3Component.class)
                .getConfiguration()
                .setAmazonS3Client(amazonS3Client);

        //Initialize the S3 Buckets
        amazonS3Client.createBucket(s3ApprovedBucket);
        amazonS3Client.createBucket(s3IngestBucket);
        amazonS3Client.createBucket(s3RejectedBucket);

        //Get the list of Buckets
        s3Buckets = amazonS3Client.listBuckets();
        log.info("Initialized S3 Buckets: {}", Arrays.toString(s3Buckets.toArray()));



        log.info("S3 putObject Requests: {}", Arrays.toString(amazonS3Client.putObjectRequests.toArray()));
    }

    private void deleteTestDirectories() {
        deleteDirectory(processDirPath);
        deleteDirectory(processDataDirPath);
        deleteDirectory(stageDirPath);
        deleteDirectory(processDoneDirPath);
        deleteDirectory(processErrorDirPath);
    }

    @Test
    public void testListS3Buckets() throws Exception {
        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(1);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:listBuckets")
                        .to("aws-s3:{{si.ct.uscbi.s3.approved.bucketName}}?operation=listBuckets")
                        .to("mock:result");
            }
        });

        context.start();

        template.sendBody("direct:listBuckets", "");

        mockResult.assertIsSatisfied();

        List<Bucket> bucketList = mockResult.getExchanges().get(0).getIn().getBody(List.class);
        log.info("Buckets List: {}", Arrays.toString(bucketList.toArray()));
    }

    @Test
    public void testPulldownAndDeleteFromS3() throws Exception {
        //Initialize and add an S3 Object
        s3Object = new S3Object();
        s3Object.setBucketName(s3ApprovedBucket);
        s3Object.setKey(deploymentZip.getName());
        if (deploymentZip != null) {
            try {
                s3Object.setObjectContent(new FileInputStream(deploymentZip));
            } catch (FileNotFoundException e) {
                throw new AmazonServiceException("Cannot store the file object.", e);
            }
        }
        amazonS3Client.objects.add(s3Object);

        //Get the list of Objects
        s3objectList = amazonS3Client.listObjects(s3ApprovedBucket).getObjectSummaries();
        log.info("Initialized S3 Objects: {}", Arrays.toString(s3objectList.toArray()));

        deleteTestDirectories();
        expectedFileExists = stageDirPath + "/" + deploymentZip.getName();

        AdviceWith.adviceWith(context, "CameraTrapDeploymentsPulldownFromS3", false, a ->
                a.weaveByType(LogDefinition.class).selectLast().after().to("mock:result")).autoStartup(true);

        //prevent ingest route from starting we don't want to start processing the deployment
        AdviceWith.adviceWith(context, "CameraTrapDeploymentsPrepareStageToProcess", false, a -> {}).autoStartup(false);

        context.start();

        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(1);
        mockResult.expectedFileExists(expectedFileExists);
        mockResult.expectedFileExists(expectedFileExists + ".done");

        mockResult.assertIsSatisfied();

        Exchange resultExchange = mockResult.getExchanges().get(0);

        //Result Exchange Assertions
        assertIsInstanceOf(InputStream.class, resultExchange.getIn().getBody());
        assertEquals(s3ApprovedBucket, resultExchange.getIn().getHeader(S3Constants.BUCKET_NAME));
        assertEquals(deploymentZip.getName(), resultExchange.getIn().getHeader(S3Constants.KEY));

        //Check that the s3object was deleted from thew correct S3 bucket
        s3objectList = amazonS3Client.listObjects(s3ApprovedBucket).getObjectSummaries();
        log.info("Object List After: {}", Arrays.toString(s3objectList.toArray()));
        assertFalse(s3objectList.contains(s3Object));

        log.info("Expected deployment file location = {}, done file location = {}", expectedFileExists, expectedFileExists + ".done");
        Assertions.assertTrue(Files.exists(new File(expectedFileExists).toPath()) && Files.exists(new File(expectedFileExists + ".done").toPath()), "There should be a Files in the Dir");
    }

    @Test
    public void testPrepareStageToProcess() throws Exception {
        deleteTestDirectories();
        expectedFileExists = processDirPath + "/" + deploymentZip.getName();

        AdviceWith.adviceWith(context, "CameraTrapDeploymentsPrepareStageToProcess", false, a ->
                a.weaveByType(LogDefinition.class).selectLast().after().to("mock:result"));

        //prevent ingest route from starting we don't want to start processing the deployment
        AdviceWith.adviceWith(context, "UnifiedCameraTrapStartProcessing", false, a -> {}).autoStartup(false);
        //the UnifiedCameraTrapStartProcessing route would normally create the Process Dir itself
        createDirectory(processDirPath);

        context.start();

        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(1);
        mockResult.expectedFileExists(expectedFileExists);

        ProducerTemplate template = context.createProducerTemplate();
        template.sendBodyAndHeader("file:{{si.ct.uscbi.stage.dir.path}}?doneFileName=${file:name}.done", deploymentZip, Exchange.FILE_NAME, deploymentZip.getName());

        mockResult.assertIsSatisfied();
        Thread.sleep(1000); //for machines with slow file i/o sometimes causing test to fail

        log.info("Expected deployment file location = {}", expectedFileExists);
        Assertions.assertTrue(Files.exists(new File(expectedFileExists).toPath()), "There should be a File in the Dir");
        assertFalse(Files.exists(new File(stageDirPath + "/" + deploymentZip.getName() + ".done").toPath()), "There should be NO done file in the Dir");
    }

    @Test
    public void testDoneDirAndS3UploadSuccess() throws Exception {
        deleteTestDirectories();

        expectedFileExists = processDoneDirPath
                + "/" + s3UploadSuccessDirPath
                + "/" + deploymentZip.getName();

        AdviceWith.adviceWith(context, "CameraTrapCopyIngestedDeploymentsToS3", false, a -> {
                //a.interceptSendToEndpoint("aws-s3.*").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, "Skipping Done deployments AWS S3 upload!").to("mock:result");
                a.weaveByType(LogDefinition.class).selectLast().after().to("mock:result");
        }).autoStartup(true);

        context.start();

        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(1);
        mockResult.expectedFileExists(expectedFileExists);

        ProducerTemplate template = context.createProducerTemplate();
        template.sendBodyAndHeader("file:{{si.ct.uscbi.process.done.dir.path}}", deploymentZip, Exchange.FILE_NAME, deploymentZip.getName());

        mockResult.assertIsSatisfied();

        Exchange resultExchange = mockResult.getExchanges().get(0);

        //assert the request was to the correct bucket
        UploadPartRequest uploadPartRequest = amazonS3Client.uploadPartRequests.get(0);
        assertEquals(s3IngestBucket, uploadPartRequest.getBucketName());

        //Result Exchange Assertions
        assertEquals(deploymentZip.getName(), resultExchange.getIn().getHeader(S3Constants.KEY));

        //Response Message Assertions
        assertEquals("3a5c8b1ad448bca04584ecb55b836264", resultExchange.getIn().getHeader(S3Constants.E_TAG));
        assertEquals(deploymentZip.getName(), resultExchange.getIn().getHeader(S3Constants.KEY));
        assertNull(resultExchange.getIn().getHeader(S3Constants.VERSION_ID));

        //Check that the s3object was sent to the correct S3 bucket
        s3objectList = amazonS3Client.listObjects(s3IngestBucket).getObjectSummaries();
        log.info("Object List After: {}", Arrays.toString(s3objectList.toArray()));
        assertThat(s3objectList)
                .extracting(S3ObjectSummary::getBucketName)
//                .anyMatch(value -> value.matches(s3IngestBucket) // shorter with java.lang.String#matches
                .anySatisfy(value -> assertThat(value).matches(s3IngestBucket)); // nicer error message with StringAssert
        assertThat(s3objectList)
                .extracting(S3ObjectSummary::getKey)
//                .anyMatch(value -> value.matches(deploymentZip.getName()) // shorter with java.lang.String#matches
                .anySatisfy(value -> assertThat(value).matches(deploymentZip.getName())); // nicer error message with StringAssert

        log.info("Expected deployment file location = {}", expectedFileExists);
        Assertions.assertTrue(Files.exists(new File(expectedFileExists).toPath()), "There should be a File in the Dir");
    }

    @Test
    public void testDoneDirS3UploadFailed() throws Exception {
        deleteTestDirectories();

        expectedFileExists = processDoneDirPath
                + "/" + s3UploadErrorDirPath
                + "/" + deploymentZip.getName();

        AdviceWith.adviceWith(context, "CameraTrapCopyIngestedDeploymentsToS3", true, a -> {
            // use a route scoped onCompletion to be executed when the Exchange failed
            a.onCompletion().onFailureOnly()
                    .to("mock:error");

            a.interceptSendToEndpoint("aws-s3.*").skipSendToOriginalEndpoint()
                    .log(LoggingLevel.INFO, "Simulating AWS S3 upload failure!")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            throw new AmazonS3Exception("Simulating AWS S3 upload failure!");
                        }
                    })
                    .to("mock:result");
        }).autoStartup(true);

        context.start();

        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(0);
        mockResult.expectedFileExists(expectedFileExists);

        MockEndpoint mockError = context.getEndpoint("mock:error", MockEndpoint.class);
        mockError.expectedMessageCount(1);
        mockError.expectedFileExists(expectedFileExists);
        mockError.expectedPropertyReceived("CamelExceptionCaught", "com.amazonaws.services.s3.model.AmazonS3Exception: Simulating AWS S3 upload failure! (Service: null; Status Code: 0; Error Code: null; Request ID: null; S3 Extended Request ID: null), S3 Extended Request ID: null");

        ProducerTemplate template = context.createProducerTemplate();
        template.sendBodyAndHeader("file:{{si.ct.uscbi.process.done.dir.path}}", deploymentZip, Exchange.FILE_NAME, deploymentZip.getName());

        mockResult.assertIsSatisfied();
        mockError.assertIsSatisfied();

        Exchange resultExchange = mockError.getExchanges().get(0);

        //assert there was No uploadPartRequest request to the S3 bucket
        assertListSize(amazonS3Client.uploadPartRequests, 0);

        //Result Exchange Assertions
        assertNull(resultExchange.getIn().getHeader(S3Constants.E_TAG));
        assertEquals(deploymentZip.getName(), resultExchange.getIn().getHeader(S3Constants.KEY));
        assertNull(resultExchange.getIn().getHeader(S3Constants.VERSION_ID));

        //Check that the s3object was Not sent to the S3 bucket
        s3objectList = amazonS3Client.listObjects(s3IngestBucket).getObjectSummaries();
        log.info("Object List After: {}", Arrays.toString(s3objectList.toArray()));
        assertListSize(s3objectList, 0);

        log.info("Expected deployment file location = {}", expectedFileExists);
        Assertions.assertTrue(Files.exists(new File(expectedFileExists).toPath()), "There should be a File in the Dir");
    }

    @Test
    public void testErrorDirS3UploadSuccess() throws Exception {
        deleteTestDirectories();

        expectedFileExists = processErrorDirPath
                + "/" + s3UploadSuccessDirPath
                + "/" + deploymentZip.getName();

        AdviceWith.adviceWith(context, "CameraTrapCopyErrorDeploymentsToS3", false, a ->
                a.weaveByType(LogDefinition.class).selectLast().after().to("mock:result")).autoStartup(true);

        context.start();

        assertEquals(ServiceStatus.Started, context.getStatus());

        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(1);
        mockResult.expectedFileExists(expectedFileExists);

        ProducerTemplate template = context.createProducerTemplate();
        template.sendBodyAndHeader("file:{{si.ct.uscbi.process.error.dir.path}}", deploymentZip, Exchange.FILE_NAME, deploymentZip.getName());

        mockResult.assertIsSatisfied();

        Exchange resultExchange = mockResult.getExchanges().get(0);

        //Result Exchange Assertions
        assertEquals(deploymentZip.getName(), resultExchange.getIn().getHeader(S3Constants.KEY));

        //Response Message Assertions
        assertEquals("3a5c8b1ad448bca04584ecb55b836264", resultExchange.getIn().getHeader(S3Constants.E_TAG));
        assertEquals(deploymentZip.getName(), resultExchange.getIn().getHeader(S3Constants.KEY));
        assertNull(resultExchange.getIn().getHeader(S3Constants.VERSION_ID));

        //assert there was a uploadPartRequest request to the correct bucket
        UploadPartRequest uploadPartRequest = amazonS3Client.uploadPartRequests.get(0);
        assertEquals(s3RejectedBucket, uploadPartRequest.getBucketName());

        //Check that the s3object was is correct
        s3objectList = amazonS3Client.listObjects(s3RejectedBucket).getObjectSummaries();
        log.info("Object List After: {}", Arrays.toString(s3objectList.toArray()));
        assertThat(s3objectList)
                .extracting(S3ObjectSummary::getBucketName)
//                .anyMatch(value -> value.matches(s3IngestBucket) // shorter with java.lang.String#matches
                .anySatisfy(value -> assertThat(value).matches(s3RejectedBucket)); // nicer error message with StringAssert
        assertThat(s3objectList)
                .extracting(S3ObjectSummary::getKey)
//                .anyMatch(value -> value.matches(deploymentZip.getName()) // shorter with java.lang.String#matches
                .anySatisfy(value -> assertThat(value).matches(deploymentZip.getName())); // nicer error message with StringAssert

        log.info("Expected deployment file location = {}", expectedFileExists);
        Assertions.assertTrue(Files.exists(new File(expectedFileExists).toPath()), "There should be a File in the Dir");
    }

    @Test
    public void testErrorDirS3UploadFailed() throws Exception {
        deleteTestDirectories();

        expectedFileExists = processErrorDirPath
                + "/" + s3UploadErrorDirPath
                + "/" + deploymentZip.getName();

        AdviceWith.adviceWith(context, "CameraTrapCopyErrorDeploymentsToS3", false, a -> {
            // use a route scoped onCompletion to be executed when the Exchange failed
            a.onCompletion().onFailureOnly()
                    .to("mock:error");

            a.interceptSendToEndpoint("aws-s3.*").skipSendToOriginalEndpoint()
                    .log(LoggingLevel.INFO, "Simulating AWS S3 upload failure!")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            throw new AmazonS3Exception("Simulating AWS S3 upload failure!");
                        }
                    })
                    .to("mock:result");
        }).autoStartup(true);

        context.start();

        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(0);
        mockResult.expectedFileExists(expectedFileExists);

        MockEndpoint mockError = context.getEndpoint("mock:error", MockEndpoint.class);
        mockError.expectedMessageCount(1);
        mockError.expectedFileExists(expectedFileExists);
        mockError.expectedPropertyReceived("CamelExceptionCaught", "com.amazonaws.services.s3.model.AmazonS3Exception: Simulating AWS S3 upload failure! (Service: null; Status Code: 0; Error Code: null; Request ID: null; S3 Extended Request ID: null), S3 Extended Request ID: null");

        ProducerTemplate template = context.createProducerTemplate();
        template.sendBodyAndHeader("file:{{si.ct.uscbi.process.error.dir.path}}", deploymentZip, Exchange.FILE_NAME, deploymentZip.getName());

        mockResult.assertIsSatisfied();
        mockError.assertIsSatisfied();

        Exchange resultExchange = mockError.getExchanges().get(0);

        //assert there was No uploadPartRequest request to the S3 bucket
        assertListSize(amazonS3Client.uploadPartRequests, 0);

        //Result Exchange Assertions
        assertNull(resultExchange.getIn().getHeader(S3Constants.E_TAG));
        assertEquals(deploymentZip.getName(), resultExchange.getIn().getHeader(S3Constants.KEY));
        assertNull(resultExchange.getIn().getHeader(S3Constants.VERSION_ID));

        //Check that the s3object was Not sent to the S3 bucket
        s3objectList = amazonS3Client.listObjects(s3RejectedBucket).getObjectSummaries();
        log.info("Object List After: {}", Arrays.toString(s3objectList.toArray()));
        assertListSize(s3objectList, 0);

        log.info("Expected deployment file location = {}", expectedFileExists);
        Assertions.assertTrue(Files.exists(new File(expectedFileExists).toPath()), "There should be a File in the Dir");
    }
}
