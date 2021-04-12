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

package edu.si.services.sidora.derivatives;

import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.velocity.VelocityConstants;
import org.apache.camel.converter.stream.FileInputStreamCache;
import org.apache.camel.model.StopDefinition;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.DateTool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.HashMap;

import static org.apache.camel.test.junit5.TestSupport.*;

/**
 * @author jbirkhimer
 */
@CamelSpringBootTest
@SpringBootTest(properties = {
        "logging.file.path=target/logs",
        "staging.dir=target/staging",
        "si.ct.wi.faceBlur.script=target/config/FaceBlurrer/FaceBlurrer.py",
        "si.ct.wi.faceBlur.classifier=target/config/FaceBlurrer/haarcascades/haarcascade_frontalface_alt.xml"
}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class DerivativesTest {

    private static final Logger log = LoggerFactory.getLogger(DerivativesTest.class);

    @PropertyInject(value = "{{si.ct.namespace}}")
    private static String CT_NAMESPACE;
    @PropertyInject(value = "{{si.fedora.user}}")
    private static String CT_OWNER;

    //Camera Trap Deployment Manifest
    private File manifestFile = new File("src/test/resources/wildlife_insights_test_data/unified-test-deployment/deployment_manifest.xml");

    @Autowired
    private CamelContext context;
    @Autowired
    private ProducerTemplate template;

    @BeforeAll
    public static void setUp() throws Exception {
        deleteDirectory("target/output");
        deleteDirectory("target/staging");
    }

    public String createApimMessage(String pid, String origin, String methodName, String dsLabel, String dsID, String testDsLocation ,String logMessage) {
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("testOrigin", origin);
        headers.put("testMethodName", methodName);
        headers.put("testPID", pid);
        headers.put("testDsId", dsID);
        headers.put("testDsLabel", dsLabel);
        headers.put("testDsLocation", testDsLocation);
        headers.put("testLogMessage", logMessage);


        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("date", new DateTool());
        velocityContext.put("headers", headers);

        headers.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext);

        String jmsMsg = template.requestBodyAndHeaders("velocity:file:src/test/resources/wildlife_insights_test_data/fedora/fedora-atom.vsl", "test body", headers, String.class);

        log.debug("PID: {} | User: {} | Method: {} | Label: {}", pid, origin, methodName, dsLabel);
        log.debug(jmsMsg);
        return jmsMsg;
    }

    @Test
    public void testFilterFedoraUser() throws Exception {
        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(1);

        String pid = CT_NAMESPACE + ":001", origin = "fedoraAdmin", methodName = "modifyDatastreamByReference", dsLabel = "testLabel", dsID = "OBJ", dsLocation = "", logMessage = "";

        String fedoraAPIM = createApimMessage(pid, origin, methodName, dsLabel, dsID, dsLocation, logMessage);

        AdviceWith.adviceWith(context, "DerivativesStartProcessing", false, a ->
                a.weaveByType(StopDefinition.class).before().to("mock:result"));

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setBody(fedoraAPIM);

        template.send("activemq:queue:sidora.apim.update", exchange);

        mockResult.assertIsSatisfied();
    }

    @Test
    public void testFilterNonOBJ() throws Exception {
        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(1);

        String pid = CT_NAMESPACE + ":001", origin = "someUser", methodName = "modifyDatastreamByReference", dsLabel = "testLabel", dsID = "RELS-EXT",  dsLocation = "", logMessage = "";

        String fedoraAPIM = createApimMessage(pid, origin, methodName, dsLabel, dsID, dsLocation, logMessage);

        AdviceWith.adviceWith(context, "DerivativesStartProcessing", false, a ->
                a.weaveByType(StopDefinition.class).selectIndex(1).before().to("mock:result"));

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setBody(fedoraAPIM);

        template.send("activemq:queue:sidora.apim.update", exchange);

        mockResult.assertIsSatisfied();
    }

    @Test
    public void testFilterLogMessage() throws Exception {
        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(1);

        String pid = CT_NAMESPACE + ":001", origin = "someUser", methodName = "modifyDatastreamByReference", dsLabel = "testLabel", dsID = "OBJ", dsLocation = "", logMessage = "faceBlurred";

        String fedoraAPIM = createApimMessage(pid, origin, methodName, dsLabel, dsID, dsLocation, logMessage);

        AdviceWith.adviceWith(context, "DerivativesStartProcessing", false, a ->
                a.weaveByType(StopDefinition.class).selectIndex(2).before().to("mock:result"));

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setBody(fedoraAPIM);

        template.send("activemq:queue:sidora.apim.update", exchange);

        mockResult.assertIsSatisfied();
    }

    @Test
    public void testWorkbenchCTImageWithFace() throws Exception {
        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("isBlurred", "true");
        mockResult.expectedHeaderReceived("blurRequired", "true");

        String pid = CT_NAMESPACE + ":001", origin = "someUser", methodName = "modifyDatastreamByReference", dsLabel = "testWildLifeInsightsDeploymentIds1i4", dsID = "OBJ", dsLocation = "uploaded", logMessage = "";

        String fedoraAPIM = createApimMessage(pid, origin, methodName, dsLabel, dsID, dsLocation, logMessage);

        AdviceWith.adviceWith(context, "DerivativesProcessMessage", false, a ->
                a.weaveById("getContentModels").replace()
                        .setHeader("sitePID").simple(CT_NAMESPACE + ":000")
                        .to("velocity:file:src/test/resources/templates/CTImageResourceTemplate.vsl")
                        .log(LoggingLevel.DEBUG, "Test RELS-EXT:\n${body}"));

        AdviceWith.adviceWith(context, "DerivativesProcessImage", false, a -> {
                a.weaveById("getObjDs").replace()
                        .setHeader("testDsLabel").simple(dsLabel)
                        .setHeader("testDsMIME").simple("image/jpeg")
                        .to("velocity:file:src/test/resources/wildlife_insights_test_data/fedora/fedora-datastreams-obj.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ datastream Profile:\n${body}");

                a.weaveById("getObjContent").replace()
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = "src/test/resources/wildlife_insights_test_data/unified-test-deployment/" + dsLabel + ".JPG";
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(new FileInputStreamCache(resourceFile));
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });

//                a.weaveById("saveToStaging").replace()
//                        .to("file:target/staging/");

                a.weaveById("isFaceBlur").after()
                        .log(LoggingLevel.INFO, "Headers:\n${headers}")
                        .to("mock:result")
                        .stop();
        });

        AdviceWith.adviceWith(context, "DerivativesIsFaceBlur", false, a -> {
                a.weaveById("getParentDs").replace()
                        .setHeader("testDsID").simple("MANIFEST")
                        .to("velocity:file:src/test/resources/wildlife_insights_test_data/fedora/fedora_datastreams.vsl")
                        .log(LoggingLevel.DEBUG, "Test Parent Datastreams:\n${body}");

                a.weaveById("getObjectXMl").replace()
                        .setHeader("testObjLabel").simple(dsLabel)
                        .to("velocity:file:src/test/resources/wildlife_insights_test_data/fedora/fedora-objectProfile.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ Profile:\n${body}");

                a.weaveById("getParentManifest").replace()
                        .process(exchange -> exchange.getIn().setBody(manifestFile))
                        .log(LoggingLevel.DEBUG, "Test Manifest:\n${body}");

                a.weaveById("saveFaceBlurOutputToStaging").replace()
                        .to("file:{{staging.dir}}?fileName=${header.CamelFileName}_output.JPG");

                a.weaveById("replaceOBJ").replace()
                        .log(LoggingLevel.INFO, "Body Type: ${body.class.name}")
                        .setHeader("tmpCamelFileNameProduced").simple("${header.CamelFileNameProduced}")
                        .to("file:target/output?fileName=${header.dsLabel}.JPG")
                        .setHeader("CamelFileNameProduced").simple("${header.tmpCamelFileNameProduced}");

                a.weaveById("addSidoraDS").replace().log("Skipping fedora addDatastream SIDORA!!!");
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setBody(fedoraAPIM);

        template.send("activemq:queue:sidora.apim.update", exchange);

        mockResult.assertIsSatisfied();

        Thread.sleep(1500);

        assertFileExists("target/output/" + dsLabel + ".JPG");
    }

    @Test
    public void testWorkbenchCTImageWithFaceHTTP() throws Exception {
        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("isBlurred", "true");
        mockResult.expectedHeaderReceived("blurRequired", "true");

        String pid = CT_NAMESPACE + ":001", origin = "someUser", methodName = "modifyDatastreamByReference", dsLabel = "testWildLifeInsightsDeploymentIds1i4", dsID = "OBJ", dsLocation = "http://test.com", logMessage = "";

        String fedoraAPIM = createApimMessage(pid, origin, methodName, dsLabel, dsID, dsLocation, logMessage);

        AdviceWith.adviceWith(context, "DerivativesProcessMessage", false, a ->
                a.weaveById("getContentModels").replace()
                        .setHeader("sitePID").simple(CT_NAMESPACE + ":000")
                        .to("velocity:file:src/test/resources/templates/CTImageResourceTemplate.vsl")
                        .log(LoggingLevel.DEBUG, "Test RELS-EXT:\n${body}"));

        AdviceWith.adviceWith(context, "DerivativesProcessImage", false, a -> {
                a.weaveById("getObjDs").replace()
                        .setHeader("testDsLabel").simple(dsLabel)
                        .setHeader("testDsMIME").simple("image/jpeg")
                        .to("velocity:file:src/test/resources/wildlife_insights_test_data/fedora/fedora-datastreams-obj.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ datastream Profile:\n${body}");

                a.weaveById("getObjContent").replace()
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = "src/test/resources/wildlife_insights_test_data/unified-test-deployment/" + dsLabel + ".JPG";
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(new FileInputStreamCache(resourceFile));
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });

//                a.weaveById("saveToStaging").replace()
//                        .to("file:target/staging/");

                a.weaveById("isFaceBlur").after()
                        .log(LoggingLevel.INFO, "Headers:\n${headers}")
                        .to("mock:result")
                        .stop();
        });

        AdviceWith.adviceWith(context, "DerivativesIsFaceBlur", false, a -> {
                a.weaveById("getParentDs").replace()
                        .setHeader("testDsID").simple("MANIFEST")
                        .to("velocity:file:src/test/resources/wildlife_insights_test_data/fedora/fedora_datastreams.vsl")
                        .log(LoggingLevel.DEBUG, "Test Parent Datastreams:\n${body}");

                a.weaveById("getObjectXMl").replace()
                        .setHeader("testObjLabel").simple(dsLabel)
                        .to("velocity:file:src/test/resources/wildlife_insights_test_data/fedora/fedora-objectProfile.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ Profile:\n${body}");

                a.weaveById("getParentManifest").replace()
                        .process(exchange -> exchange.getIn().setBody(manifestFile))
                        .log(LoggingLevel.DEBUG, "Test Manifest:\n${body}");

                a.weaveById("saveFaceBlurOutputToStaging").replace()
                        .to("file:{{staging.dir}}?fileName=${header.CamelFileName}_output.JPG");

                a.weaveById("replaceOBJ").replace()
                        .log(LoggingLevel.INFO, "Body Type: ${body.class.name}")
                        .setHeader("tmpCamelFileNameProduced").simple("${header.CamelFileNameProduced}")
                        .to("file:target/output?fileName=${header.dsLabel}.JPG")
                        .setHeader("CamelFileNameProduced").simple("${header.tmpCamelFileNameProduced}");

                a.weaveById("addSidoraDS").replace().log("Skipping fedora addDatastream SIDORA!!!");
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setBody(fedoraAPIM);

        template.send("activemq:queue:sidora.apim.update", exchange);

        mockResult.assertIsSatisfied();

        Thread.sleep(1500);

        assertFileExists("target/output/" + dsLabel + ".JPG");
    }

    @Test
    public void testWorkbenchCTImageNoFace() throws Exception {
        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("isBlurred", "false");
        mockResult.expectedHeaderReceived("blurRequired", "false");

        String pid = CT_NAMESPACE + ":001", origin = "someUser", methodName = "modifyDatastreamByReference", dsLabel = "testWildLifeInsightsDeploymentIds2i1", dsID = "OBJ", dsLocation = "uploaded", logMessage = "";

        String fedoraAPIM = createApimMessage(pid, origin, methodName, dsLabel, dsID, dsLocation, logMessage);

        AdviceWith.adviceWith(context, "DerivativesProcessMessage", false, a ->
                a.weaveById("getContentModels").replace()
                        .setHeader("sitePID").simple(CT_NAMESPACE + ":000")
                        .to("velocity:file:src/test/resources/templates/CTImageResourceTemplate.vsl")
                        .log(LoggingLevel.DEBUG, "Test RELS-EXT:\n${body}"));

        AdviceWith.adviceWith(context, "DerivativesProcessImage", false, a ->{
                a.weaveById("getObjDs").replace()
                        .setHeader("testDsLabel").simple(dsLabel)
                        .setHeader("testDsMIME").simple("image/jpeg")
                        .to("velocity:file:src/test/resources/wildlife_insights_test_data/fedora/fedora-datastreams-obj.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ datastream Profile:\n${body}");

                a.weaveById("getObjContent").replace()
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = "src/test/resources/wildlife_insights_test_data/unified-test-deployment/" + dsLabel + ".JPG";
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(new FileInputStreamCache(resourceFile));
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });

//                a.weaveById("saveToStaging").replace()
//                        .to("file:target/staging/");

                a.weaveById("isFaceBlur").after()
                        .log(LoggingLevel.INFO, "Headers:\n${headers}")
                        .to("mock:result")
                        .stop();
        });

        AdviceWith.adviceWith(context, "DerivativesIsFaceBlur", false, a -> {
                a.weaveById("getParentDs").replace()
                        .setHeader("testDsID").simple("MANIFEST")
                        .to("velocity:file:src/test/resources/wildlife_insights_test_data/fedora/fedora_datastreams.vsl")
                        .log(LoggingLevel.DEBUG, "Test Parent Datastreams:\n${body}");

                a.weaveById("getObjectXMl").replace()
                        .setHeader("testObjLabel").simple(dsLabel)
                        .to("velocity:file:src/test/resources/wildlife_insights_test_data/fedora/fedora-objectProfile.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ Profile:\n${body}");

                a.weaveById("getParentManifest").replace()
                        .process(exchange -> exchange.getIn().setBody(manifestFile))
                        .log(LoggingLevel.DEBUG, "Test Manifest:\n${body}");

                a.weaveById("addSidoraDS").replace().log("Skipping fedora addDatastream SIDORA!!!");
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setBody(fedoraAPIM);

        template.send("activemq:queue:sidora.apim.update", exchange);

        mockResult.assertIsSatisfied();

        Thread.sleep(1500);

        assertFileNotExists("target/output/" + dsLabel + ".JPG");
    }

    @Test
    public void testWorkbenchNonCTImage() throws Exception {
        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(1);

        String pid = CT_NAMESPACE + ":001", origin = "someUser", methodName = "modifyDatastreamByReference", dsLabel = "nonCtImage", dsID = "OBJ",  dsLocation = "uploaded", logMessage = "";

        String fedoraAPIM = createApimMessage(pid, origin, methodName, dsLabel, dsID, dsLocation, logMessage);

        AdviceWith.adviceWith(context, "DerivativesProcessMessage", false, a ->
                a.weaveById("getContentModels").replace()
                        .setHeader("sitePID").simple(CT_NAMESPACE + ":000")
                        .to("velocity:file:src/test/resources/templates/CTImageResourceTemplate.vsl")
                        .log(LoggingLevel.DEBUG, "Test RELS-EXT:\n${body}"));

        AdviceWith.adviceWith(context, "DerivativesProcessImage", false, a ->{
                a.weaveById("getObjDs").replace()
                        .setHeader("testDsLabel").simple(dsLabel)
                        .setHeader("testDsMIME").simple("image/jpeg")
                        .to("velocity:file:src/test/resources/wildlife_insights_test_data/fedora/fedora-datastreams-obj.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ datastream Profile:\n${body}");

                a.weaveById("getObjContent").replace()
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = "src/test/resources/wildlife_insights_test_data/test-images/" + dsLabel + ".jpg";
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(new FileInputStreamCache(resourceFile));
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });

//                a.weaveById("saveToStaging").replace()
//                        .to("file:target/staging/");

                a.weaveById("isFaceBlur").after()
                        .log(LoggingLevel.INFO, "Headers:\n${headers}")
                        .to("mock:result")
                        .stop();
        });

        AdviceWith.adviceWith(context, "DerivativesIsFaceBlur", false, a ->
                a.weaveById("getParentDs").replace()
                        .setHeader("testDsID").simple("TEST")
                        .to("velocity:file:src/test/resources/wildlife_insights_test_data/fedora/fedora_datastreams.vsl")
                        .log(LoggingLevel.DEBUG, "Test Parent Datastreams:\n${body}"));

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setBody(fedoraAPIM);

        template.send("activemq:queue:sidora.apim.update", exchange);

        mockResult.assertIsSatisfied();

        Thread.sleep(1500);

        assertFileNotExists("target/output/" + dsLabel + ".JPG");
    }
}
