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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.*;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.s3.S3Constants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.LogDefinition;
import org.apache.commons.configuration2.Configuration;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/** Unit Tests for CameraTrap AWS S3 routes using a Mock AmazonS3Client.
 * @author jbirkhimer
 */
public class AwsS3PollAndUploadRouteTest extends CT_BlueprintTestSupport {

    private static final boolean USE_ACTUAL_FEDORA_SERVER = false;
    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private String defaultTestProperties = KARAF_HOME + "/test.properties";
    private static Configuration config = null;

    String deploymentZipLoc = "src/test/resources/UnifiedManifest-TestFiles/scbi_unified_test_deployment.zip";
    File deploymentZip;
    String expectedFileExists;
    Properties props;

    AmazonS3ClientMock amazonS3Client;
    String s3bucket;
    List<Bucket> s3Buckets;
    List<S3ObjectSummary> s3objectList;
    S3Object s3Object;

    @Override
    protected String getBlueprintDescriptor() {
        return "Routes/unified-camera-trap-route.xml";
    }

    @Override
    protected String[] preventRoutesFromStarting() {
        return new String[]{"UnifiedCameraTrapInFlightConceptStatusPolling", "UnifiedCameraTrapStartProcessing"};
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public void setUp() throws Exception {
        System.setProperty("si.ct.uscbi.enableS3Routes", "true");
        super.setUp();

        props = getExtra();

        deleteTestDirectories();

        deploymentZip = new File(deploymentZipLoc);
        log.debug("Exchange_FILE_NAME = {}", deploymentZip.getName());

        configureS3();
    }

    private void configureS3() {
        //Initialize the Mock AmazonS3Client
        amazonS3Client = getAmazonS3Client();

        //Initialize the S3 Buckets
        amazonS3Client.createBucket(props.getProperty("si.ct.uscbi.s3.approved.bucketName"));
        amazonS3Client.createBucket(props.getProperty("si.ct.uscbi.s3.ingested.bucketName"));
        amazonS3Client.createBucket(props.getProperty("si.ct.uscbi.s3.rejected.bucketName"));

        //Get the list of Buckets
        s3Buckets = amazonS3Client.listBuckets();
        log.info("Initialized S3 Buckets: {}", Arrays.toString(s3Buckets.toArray()));

        //Initialize and add an S3 Object
        s3Object = new S3Object();
        s3Object.setBucketName(props.getProperty("si.ct.uscbi.s3.approved.bucketName"));
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
        s3objectList = amazonS3Client.listObjects(props.getProperty("si.ct.uscbi.s3.approved.bucketName")).getObjectSummaries();
        log.info("Initialized S3 Objects: {}", Arrays.toString(s3objectList.toArray()));

        log.info("S3 putObject Requests: {}", Arrays.toString(amazonS3Client.putObjectRequests.toArray()));
    }

    private void deleteTestDirectories() {
        deleteDirectory(props.getProperty("si.ct.uscbi.process.dir.path"));
        deleteDirectory(props.getProperty("si.ct.uscbi.data.dir.path"));
        deleteDirectory(props.getProperty("si.ct.uscbi.stage.dir.path"));
    }

    @Test
    @Ignore("requires camel-aws version 2.18.0+")
    public void testListS3Buckets() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:listBuckets")
                        .to("aws-s3:{{si.ct.uscbi.s3.approved.bucketName}}?amazonS3Client=#amazonS3Client&operation=listBuckets")
                        .to("mock:result");

            }
        });

        template.sendBody("direct:listBuckets", ExchangePattern.InOnly, "");

        List<Bucket> bucketList = mockResult.getExchanges().get(0).getIn().getBody(List.class);
        log.info("Buckets List: {}", Arrays.toString(bucketList.toArray()));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testPulldownAndDeleteFromS3() throws Exception {
        deleteTestDirectories();
        s3bucket = props.getProperty("si.ct.uscbi.s3.approved.bucketName");
        expectedFileExists = props.getProperty("si.ct.uscbi.stage.dir.path")+ "/" + deploymentZip.getName();

        context.getRouteDefinition("CameraTrapDeploymentsPrepareStageToProcess").noAutoStartup();
        context.getRouteDefinition("CameraTrapDeploymentsPulldownFromS3").autoStartup(true);
        context.getRouteDefinition("CameraTrapDeploymentsPulldownFromS3").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(LogDefinition.class).selectLast().after().to("mock:result");
            }
        });

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedFileExists(expectedFileExists);
        mockResult.expectedFileExists(expectedFileExists + ".done");

        assertMockEndpointsSatisfied();

        Exchange resultExchange = mockResult.getExchanges().get(0);

        //Result Exchange Assertions
        assertIsInstanceOf(InputStream.class, resultExchange.getIn().getBody());
        assertEquals(s3bucket, resultExchange.getIn().getHeader(S3Constants.BUCKET_NAME));
        assertEquals(deploymentZip.getName(), resultExchange.getIn().getHeader(S3Constants.KEY));

        //Check that the s3object was deleted from thew correct S3 bucket
        s3objectList = amazonS3Client.listObjects(s3bucket).getObjectSummaries();
        log.info("Object List After: {}", Arrays.toString(s3objectList.toArray()));
        assertFalse(s3objectList.contains(s3Object));

        log.info("Expected deployment file location = {}, done file location = {}", expectedFileExists, expectedFileExists + ".done");
        assertTrue("There should be a Files in the Dir", Files.exists(new File(expectedFileExists).toPath()) && Files.exists(new File(expectedFileExists + ".done").toPath()));
    }

    @Test
    public void testPrepareStageToProcess() throws Exception {
        deleteTestDirectories();
        expectedFileExists = props.getProperty("si.ct.uscbi.process.dir.path") + "/" + deploymentZip.getName();

        //prevent ingest route from starting we don't want to start processing the deployment
        context.getRouteDefinition("UnifiedCameraTrapStartProcessing").noAutoStartup();
        //the UnifiedCameraTrapStartProcessing route would normally create the Process Dir itself
        createDirectory(props.getProperty("si.ct.uscbi.process.dir.path"));

        context.getRouteDefinition("CameraTrapDeploymentsPrepareStageToProcess").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(LogDefinition.class).selectLast().after().to("mock:result");
            }
        });

        context.start();

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedFileExists(expectedFileExists);

        template.sendBodyAndHeader("file:{{si.ct.uscbi.stage.dir.path}}?doneFileName=${file:name}.done", deploymentZip, Exchange.FILE_NAME, deploymentZip.getName());

        assertMockEndpointsSatisfied();
        Thread.sleep(1000); //for machines with slow file i/o sometimes causing test to fail

        log.info("Expected deployment file location = {}", expectedFileExists);
        assertTrue("There should be a File in the Dir", Files.exists(new File(expectedFileExists).toPath()));
        assertFalse("There should be NO done file in the Dir", Files.exists(
                new File(props.getProperty("si.ct.uscbi.stage.dir.path") + "/" + deploymentZip.getName() + ".done").toPath()
                )
        );

    }

    @Test
    public void testDoneDirAndS3UploadSuccess() throws Exception {
        deleteTestDirectories();
        s3bucket = props.getProperty("si.ct.uscbi.s3.ingested.bucketName");

        expectedFileExists = props.getProperty("si.ct.uscbi.process.dir.path") + "/Done"
                + "/" + props.getProperty("si.ct.external.upload.success.dir")
                + "/" + deploymentZip.getName();

        context.getRouteDefinition("CameraTrapCopyIngestedDeploymentsToS3").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //interceptSendToEndpoint("aws-s3.*").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, "Skipping Done deployments AWS S3 upload!").to("mock:result");
                weaveByType(LogDefinition.class).selectLast().after().to("mock:result");
            }
        });

        context.start();

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedFileExists(expectedFileExists);

        template.sendBodyAndHeader("file:{{si.ct.uscbi.process.dir.path}}/Done", deploymentZip, Exchange.FILE_NAME, deploymentZip.getName());

        assertMockEndpointsSatisfied();

        Exchange resultExchange = mockResult.getExchanges().get(0);

        //assert the request was to the correct bucket
        PutObjectRequest putObjectRequest = amazonS3Client.putObjectRequests.get(0);
        assertEquals(s3bucket, putObjectRequest.getBucketName());

        //Result Exchange Assertions
        assertEquals(deploymentZip.getName(), resultExchange.getIn().getHeader(S3Constants.KEY));

        //Response Message Assertions
        assertEquals("3a5c8b1ad448bca04584ecb55b836264", resultExchange.getIn().getHeader(S3Constants.E_TAG));
        assertEquals(deploymentZip.getName(), resultExchange.getIn().getHeader(S3Constants.KEY));
        assertNull(resultExchange.getIn().getHeader(S3Constants.VERSION_ID));

        //Check that the s3object was sent to the correct S3 bucket
        s3objectList = amazonS3Client.listObjects(s3bucket).getObjectSummaries();
        log.info("Object List After: {}", Arrays.toString(s3objectList.toArray()));
        assertTrue(s3objectList.get(0).getBucketName().equals(s3bucket));
        assertTrue(s3objectList.get(0).getKey().equals(deploymentZip.getName()));

        log.info("Expected deployment file location = {}", expectedFileExists);
        assertTrue("There should be a File in the Dir", Files.exists(new File(expectedFileExists).toPath()));
    }

    @Test
    public void testDoneDirS3UploadFailed() throws Exception {
        deleteTestDirectories();
        s3bucket = props.getProperty("si.ct.uscbi.s3.ingested.bucketName");

        expectedFileExists = props.getProperty("si.ct.uscbi.process.dir.path") + "/Done"
                + "/" + props.getProperty("si.ct.external.upload.error.dir")
                + "/" + deploymentZip.getName();

        context.getRouteDefinition("CameraTrapCopyIngestedDeploymentsToS3").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                // use a route scoped onCompletion to be executed when the Exchange failed
                context.getRouteDefinition("CameraTrapCopyIngestedDeploymentsToS3").onCompletion().onFailureOnly()
                        .to("mock:error")
                        .end();

                interceptSendToEndpoint("aws-s3.*").skipSendToOriginalEndpoint()
                        .log(LoggingLevel.INFO, "Simulating AWS S3 upload failure!")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                throw new AmazonS3Exception("Simulating AWS S3 upload failure!");
                            }
                        })
                        .to("mock:result");
            }
        });

        context.start();

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(0);
        mockResult.expectedFileExists(expectedFileExists);

        MockEndpoint mockError = getMockEndpoint("mock:error");
        mockError.expectedMessageCount(1);
        mockError.expectedFileExists(expectedFileExists);
        mockError.expectedPropertyReceived("CamelExceptionCaught", "com.amazonaws.services.s3.model.AmazonS3Exception: Simulating AWS S3 upload failure! (Service: null; Status Code: 0; Error Code: null; Request ID: null), S3 Extended Request ID: null");

        template.sendBodyAndHeader("file:{{si.ct.uscbi.process.dir.path}}/Done", deploymentZip, Exchange.FILE_NAME, deploymentZip.getName());

        assertMockEndpointsSatisfied();

        Exchange resultExchange = mockError.getExchanges().get(0);

        //assert there was No putObject request to the S3 bucket
        assertListSize(amazonS3Client.putObjectRequests, 0);

        //Result Exchange Assertions
        assertNull(resultExchange.getIn().getHeader(S3Constants.E_TAG));
        assertEquals(deploymentZip.getName(), resultExchange.getIn().getHeader(S3Constants.KEY));
        assertNull(resultExchange.getIn().getHeader(S3Constants.VERSION_ID));

        //Check that the s3object was Not sent to the S3 bucket
        s3objectList = amazonS3Client.listObjects(s3bucket).getObjectSummaries();
        log.info("Object List After: {}", Arrays.toString(s3objectList.toArray()));
        assertListSize(s3objectList, 0);

        log.info("Expected deployment file location = {}", expectedFileExists);
        assertTrue("There should be a File in the Dir", Files.exists(new File(expectedFileExists).toPath()));
    }

    @Test
    public void testErrorDirS3UploadSuccess() throws Exception {
        deleteTestDirectories();
        s3bucket = props.getProperty("si.ct.uscbi.s3.rejected.bucketName");

        expectedFileExists = props.getProperty("si.ct.uscbi.process.dir.path") + "/Error_UnifiedCameraTrap"
                + "/" + props.getProperty("si.ct.external.upload.success.dir")
                + "/" + deploymentZip.getName();

        context.getRouteDefinition("CameraTrapCopyErrorDeploymentsToS3").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(LogDefinition.class).selectLast().after().to("mock:result");
            }
        });

        context.start();

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedFileExists(expectedFileExists);

        template.sendBodyAndHeader("file:{{si.ct.uscbi.process.dir.path}}/Error_UnifiedCameraTrap", deploymentZip, Exchange.FILE_NAME, deploymentZip.getName());

        assertMockEndpointsSatisfied();

        Exchange resultExchange = mockResult.getExchanges().get(0);

        //Result Exchange Assertions
        assertEquals(deploymentZip.getName(), resultExchange.getIn().getHeader(S3Constants.KEY));

        //Response Message Assertions
        assertEquals("3a5c8b1ad448bca04584ecb55b836264", resultExchange.getIn().getHeader(S3Constants.E_TAG));
        assertEquals(deploymentZip.getName(), resultExchange.getIn().getHeader(S3Constants.KEY));
        assertNull(resultExchange.getIn().getHeader(S3Constants.VERSION_ID));

        //assert there was a putObject request to the correct bucket
        PutObjectRequest putObjectRequest = amazonS3Client.putObjectRequests.get(0);
        assertEquals(s3bucket, putObjectRequest.getBucketName());

        //Check that the s3object was is correct
        s3objectList = amazonS3Client.listObjects(s3bucket).getObjectSummaries();
        log.info("Object List After: {}", Arrays.toString(s3objectList.toArray()));
        assertTrue(s3objectList.get(0).getBucketName().equals(s3bucket));
        assertTrue(s3objectList.get(0).getKey().equals(deploymentZip.getName()));

        log.info("Expected deployment file location = {}", expectedFileExists);
        assertTrue("There should be a File in the Dir", Files.exists(new File(expectedFileExists).toPath()));
    }

    @Test
    public void testErrorDirS3UploadFailed() throws Exception {
        deleteTestDirectories();

        s3bucket = props.getProperty("si.ct.uscbi.s3.rejected.bucketName");

        expectedFileExists = props.getProperty("si.ct.uscbi.process.dir.path") + "/Error_UnifiedCameraTrap"
                + "/" + props.getProperty("si.ct.external.upload.error.dir")
                + "/" + deploymentZip.getName();

        context.getRouteDefinition("CameraTrapCopyErrorDeploymentsToS3").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                // use a route scoped onCompletion to be executed when the Exchange failed
                context.getRouteDefinition("CameraTrapCopyErrorDeploymentsToS3").onCompletion().onFailureOnly()
                        .to("mock:error")
                        .end();

                interceptSendToEndpoint("aws-s3.*").skipSendToOriginalEndpoint()
                        .log(LoggingLevel.INFO, "Simulating AWS S3 upload failure!")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                throw new AmazonS3Exception("Simulating AWS S3 upload failure!");
                            }
                        })
                        .to("mock:result");
            }
        });

        context.start();

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(0);
        mockResult.expectedFileExists(expectedFileExists);

        MockEndpoint mockError = getMockEndpoint("mock:error");
        mockError.expectedMessageCount(1);
        mockError.expectedFileExists(expectedFileExists);
        mockError.expectedPropertyReceived("CamelExceptionCaught", "com.amazonaws.services.s3.model.AmazonS3Exception: Simulating AWS S3 upload failure! (Service: null; Status Code: 0; Error Code: null; Request ID: null), S3 Extended Request ID: null");

        template.sendBodyAndHeader("file:{{si.ct.uscbi.process.dir.path}}/Error_UnifiedCameraTrap", deploymentZip, Exchange.FILE_NAME, deploymentZip.getName());

        assertMockEndpointsSatisfied();

        Exchange resultExchange = mockError.getExchanges().get(0);

        //assert there was No putObject request to the S3 bucket
        assertListSize(amazonS3Client.putObjectRequests, 0);

        //Result Exchange Assertions
        assertNull(resultExchange.getIn().getHeader(S3Constants.E_TAG));
        assertEquals(deploymentZip.getName(), resultExchange.getIn().getHeader(S3Constants.KEY));
        assertNull(resultExchange.getIn().getHeader(S3Constants.VERSION_ID));

        //Check that the s3object was Not sent to the S3 bucket
        s3objectList = amazonS3Client.listObjects(s3bucket).getObjectSummaries();
        log.info("Object List After: {}", Arrays.toString(s3objectList.toArray()));
        assertListSize(s3objectList, 0);

        log.info("Expected deployment file location = {}", expectedFileExists);
        assertTrue("There should be a File in the Dir", Files.exists(new File(expectedFileExists).toPath()));
    }
}
