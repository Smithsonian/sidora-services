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
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.velocity.VelocityConstants;
import org.apache.camel.converter.stream.FileInputStreamCache;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.model.StopDefinition;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.DateTool;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Properties;

import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * @author jbirkhimer
 */
public class DerivativesTest extends CT_BlueprintTestSupport {

    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static final String PROJECT_BASE_DIR = System.getProperty("baseDir");
    private static String CT_NAMESPACE;
    private static String CT_OWNER;

    //Camera Trap Deployment Manifest
    private File manifestFile = new File(KARAF_HOME + "/wildlife_insights_test_data/unified-test-deployment/deployment_manifest.xml");
    private String manifest;

    @Override
    protected String getBlueprintDescriptor() {
        return "Routes/derivatives-route.xml";
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        manifest = readFileToString(manifestFile);
        CT_NAMESPACE = context.resolvePropertyPlaceholders("{{si.ct.namespace}}");
        CT_OWNER = context.resolvePropertyPlaceholders("{{si.fedora.user}}");
        deleteDirectory(KARAF_HOME + "/output");
        deleteDirectory(KARAF_HOME + "/staging");
    }

    @Override
    protected String setConfigAdminInitialConfiguration(Properties configAdmin) {
        configAdmin.putAll(getExtra());
        return "edu.si.sidora.derivatives";
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

        String jmsMsg = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/wildlife_insights_test_data/fedora/fedora-atom.vsl", "test body", headers, String.class);

        log.debug("PID: {} | User: {} | Method: {} | Label: {}", pid, origin, methodName, dsLabel);
        log.debug(jmsMsg);
        return jmsMsg;
    }

    @Test
    public void testFilterFedoraUser() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        String pid = CT_NAMESPACE + ":001", origin = "fedoraAdmin", methodName = "modifyDatastreamByReference", dsLabel = "testLabel", dsID = "OBJ", dsLocation = "", logMessage = "";

        String fedoraAPIM = createApimMessage(pid, origin, methodName, dsLabel, dsID, dsLocation, logMessage);

        context.getRouteDefinition("DerivativesStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(StopDefinition.class).before().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setBody(fedoraAPIM);

        template.send("activemq:queue:sidora.apim.update", exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFilterNonOBJ() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        String pid = CT_NAMESPACE + ":001", origin = "someUser", methodName = "modifyDatastreamByReference", dsLabel = "testLabel", dsID = "RELS-EXT",  dsLocation = "", logMessage = "";

        String fedoraAPIM = createApimMessage(pid, origin, methodName, dsLabel, dsID, dsLocation, logMessage);

        context.getRouteDefinition("DerivativesStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(StopDefinition.class).selectIndex(1).before().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setBody(fedoraAPIM);

        template.send("activemq:queue:sidora.apim.update", exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFilterLogMessage() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        String pid = CT_NAMESPACE + ":001", origin = "someUser", methodName = "modifyDatastreamByReference", dsLabel = "testLabel", dsID = "OBJ", dsLocation = "", logMessage = "faceBlurred";

        String fedoraAPIM = createApimMessage(pid, origin, methodName, dsLabel, dsID, dsLocation, logMessage);

        context.getRouteDefinition("DerivativesStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(StopDefinition.class).selectIndex(2).before().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setBody(fedoraAPIM);

        template.send("activemq:queue:sidora.apim.update", exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testWorkbenchCTImageWithFace() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        String pid = CT_NAMESPACE + ":001", origin = "someUser", methodName = "modifyDatastreamByReference", dsLabel = "testWildLifeInsightsDeploymentIds1i4", dsID = "OBJ", dsLocation = "uploaded", logMessage = "";

        String fedoraAPIM = createApimMessage(pid, origin, methodName, dsLabel, dsID, dsLocation, logMessage);

        context.getRouteDefinition("DerivativesProcessMessage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("getContentModels").replace()
                        .setHeader("sitePID").simple(CT_NAMESPACE + ":000")
                        .to("velocity:file:{{karaf.home}}/Input/templates/CTImageResourceTemplate.vsl")
                        .log(LoggingLevel.DEBUG, "Test RELS-EXT:\n${body}");
            }
        });

        context.getRouteDefinition("DerivativesProcessImage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("getObjDs").replace()
                        .setHeader("testDsLabel").simple(dsLabel)
                        .setHeader("testDsMIME").simple("image/jpeg")
                        .to("velocity:file:{{karaf.home}}/wildlife_insights_test_data/fedora/fedora-datastreams-obj.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ datastream Profile:\n${body}");

                weaveById("getObjContent").replace()
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = KARAF_HOME + "/wildlife_insights_test_data/unified-test-deployment/" + dsLabel + ".JPG";
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(new FileInputStreamCache(resourceFile));
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });

                weaveById("saveToStaging").replace()
                        .to("file:target/staging/");

                weaveById("isFaceBlur").after()
                        .log(LoggingLevel.INFO, "Headers:\n${headers}")
                        .to("mock:result")
                        .stop();
            }
        });

        context.getRouteDefinition("DerivativesIsFaceBlur").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("getParentDs").replace()
                        .setHeader("testDsID").simple("MANIFEST")
                        .to("velocity:file:{{karaf.home}}/wildlife_insights_test_data/fedora/fedora_datastreams.vsl")
                        .log(LoggingLevel.DEBUG, "Test Parent Datastreams:\n${body}");

                weaveById("getObjectXMl").replace()
                        .setHeader("testObjLabel").simple(dsLabel)
                        .to("velocity:file:{{karaf.home}}/wildlife_insights_test_data/fedora/fedora-objectProfile.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ Profile:\n${body}");

                weaveById("getParentManifest").replace()
                        .setBody().simple(manifest)
                        .log(LoggingLevel.DEBUG, "Test Manifest:\n${body}");

                weaveById("saveFaceBlurOutputToStaging").replace()
                        .to("file://target/staging?fileName=${header.CamelFileName}");

                weaveById("replaceOBJ").replace()
                        .log(LoggingLevel.INFO, "Body Type: ${body.class.name}")
                        .to("file:target/output?fileName=${header.dsLabel}.JPG");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setBody(fedoraAPIM);

        template.send("activemq:queue:sidora.apim.update", exchange);

        assertMockEndpointsSatisfied();

        Thread.sleep(1500);

        assertFileExists(PROJECT_BASE_DIR + "/target/output/" + dsLabel + ".JPG");
    }

    @Test
    public void testWorkbenchCTImageWithFaceHTTP() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        String pid = CT_NAMESPACE + ":001", origin = "someUser", methodName = "modifyDatastreamByReference", dsLabel = "testWildLifeInsightsDeploymentIds1i4", dsID = "OBJ", dsLocation = "http://test.com", logMessage = "";

        String fedoraAPIM = createApimMessage(pid, origin, methodName, dsLabel, dsID, dsLocation, logMessage);

        context.getRouteDefinition("DerivativesProcessMessage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("getContentModels").replace()
                        .setHeader("sitePID").simple(CT_NAMESPACE + ":000")
                        .to("velocity:file:{{karaf.home}}/Input/templates/CTImageResourceTemplate.vsl")
                        .log(LoggingLevel.DEBUG, "Test RELS-EXT:\n${body}");
            }
        });

        context.getRouteDefinition("DerivativesProcessImage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("getObjDs").replace()
                        .setHeader("testDsLabel").simple(dsLabel)
                        .setHeader("testDsMIME").simple("image/jpeg")
                        .to("velocity:file:{{karaf.home}}/wildlife_insights_test_data/fedora/fedora-datastreams-obj.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ datastream Profile:\n${body}");

                weaveById("getObjContent").replace()
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = KARAF_HOME + "/wildlife_insights_test_data/unified-test-deployment/" + dsLabel + ".JPG";
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(new FileInputStreamCache(resourceFile));
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });

                weaveById("saveToStaging").replace()
                        .to("file:target/staging/");

                weaveById("isFaceBlur").after()
                        .log(LoggingLevel.INFO, "Headers:\n${headers}")
                        .to("mock:result")
                        .stop();
            }
        });

        context.getRouteDefinition("DerivativesIsFaceBlur").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("getParentDs").replace()
                        .setHeader("testDsID").simple("MANIFEST")
                        .to("velocity:file:{{karaf.home}}/wildlife_insights_test_data/fedora/fedora_datastreams.vsl")
                        .log(LoggingLevel.DEBUG, "Test Parent Datastreams:\n${body}");

                weaveById("getObjectXMl").replace()
                        .setHeader("testObjLabel").simple(dsLabel)
                        .to("velocity:file:{{karaf.home}}/wildlife_insights_test_data/fedora/fedora-objectProfile.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ Profile:\n${body}");

                weaveById("getParentManifest").replace()
                        .setBody().simple(manifest)
                        .log(LoggingLevel.DEBUG, "Test Manifest:\n${body}");

                weaveById("saveFaceBlurOutputToStaging").replace()
                        .to("file://target/staging?fileName=${header.CamelFileName}");

                weaveById("replaceOBJ").replace()
                        .log(LoggingLevel.INFO, "Body Type: ${body.class.name}")
                        .to("file:target/output?fileName=${header.dsLabel}.JPG");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setBody(fedoraAPIM);

        template.send("activemq:queue:sidora.apim.update", exchange);

        assertMockEndpointsSatisfied();

        Thread.sleep(1500);

        assertFileExists(PROJECT_BASE_DIR + "/target/output/" + dsLabel + ".JPG");
    }

    @Test
    public void testWorkbenchCTImageNoFace() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        String pid = CT_NAMESPACE + ":001", origin = "someUser", methodName = "modifyDatastreamByReference", dsLabel = "testWildLifeInsightsDeploymentIds2i1", dsID = "OBJ", dsLocation = "uploaded", logMessage = "";

        String fedoraAPIM = createApimMessage(pid, origin, methodName, dsLabel, dsID, dsLocation, logMessage);

        context.getRouteDefinition("DerivativesProcessMessage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("getContentModels").replace()
                        .setHeader("sitePID").simple(CT_NAMESPACE + ":000")
                        .to("velocity:file:{{karaf.home}}/Input/templates/CTImageResourceTemplate.vsl")
                        .log(LoggingLevel.DEBUG, "Test RELS-EXT:\n${body}");
            }
        });

        context.getRouteDefinition("DerivativesProcessImage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("getObjDs").replace()
                        .setHeader("testDsLabel").simple(dsLabel)
                        .setHeader("testDsMIME").simple("image/jpeg")
                        .to("velocity:file:{{karaf.home}}/wildlife_insights_test_data/fedora/fedora-datastreams-obj.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ datastream Profile:\n${body}");

                weaveById("getObjContent").replace()
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = KARAF_HOME + "/wildlife_insights_test_data/unified-test-deployment/" + dsLabel + ".JPG";
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(new FileInputStreamCache(resourceFile));
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });

                weaveById("saveToStaging").replace()
                        .to("file:target/staging/");

                weaveById("isFaceBlur").after()
                        .log(LoggingLevel.INFO, "Headers:\n${headers}")
                        .to("mock:result")
                        .stop();
            }
        });

        context.getRouteDefinition("DerivativesIsFaceBlur").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("getParentDs").replace()
                        .setHeader("testDsID").simple("MANIFEST")
                        .to("velocity:file:{{karaf.home}}/wildlife_insights_test_data/fedora/fedora_datastreams.vsl")
                        .log(LoggingLevel.DEBUG, "Test Parent Datastreams:\n${body}");

                weaveById("getObjectXMl").replace()
                        .setHeader("testObjLabel").simple(dsLabel)
                        .to("velocity:file:{{karaf.home}}/wildlife_insights_test_data/fedora/fedora-objectProfile.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ Profile:\n${body}");

                weaveById("getParentManifest").replace()
                        .setBody().simple(manifest)
                        .log(LoggingLevel.DEBUG, "Test Manifest:\n${body}");

                weaveById("saveFaceBlurOutputToStaging").replace()
                        .to("file://target/staging?fileName=${header.CamelFileName}");

                weaveById("replaceOBJ").replace()
                        .log(LoggingLevel.INFO, "Body Type: ${body.class.name}")
                        .to("file:target/output?fileName=${header.dsLabel}.JPG");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setBody(fedoraAPIM);

        template.send("activemq:queue:sidora.apim.update", exchange);

        assertMockEndpointsSatisfied();

        Thread.sleep(1500);

        assertFileNotExists(PROJECT_BASE_DIR + "/target/output/" + dsLabel + ".JPG");
    }

    @Test
    public void testWorkbenchNonCTImage() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        String pid = CT_NAMESPACE + ":001", origin = "someUser", methodName = "modifyDatastreamByReference", dsLabel = "nonCtImage", dsID = "OBJ",  dsLocation = "uploaded", logMessage = "";

        String fedoraAPIM = createApimMessage(pid, origin, methodName, dsLabel, dsID, dsLocation, logMessage);

        context.getRouteDefinition("DerivativesProcessMessage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("getContentModels").replace()
                        .setHeader("sitePID").simple(CT_NAMESPACE + ":000")
                        .to("velocity:file:{{karaf.home}}/Input/templates/CTImageResourceTemplate.vsl")
                        .log(LoggingLevel.DEBUG, "Test RELS-EXT:\n${body}");
            }
        });

        context.getRouteDefinition("DerivativesProcessImage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("getObjDs").replace()
                        .setHeader("testDsLabel").simple(dsLabel)
                        .setHeader("testDsMIME").simple("image/jpeg")
                        .to("velocity:file:{{karaf.home}}/wildlife_insights_test_data/fedora/fedora-datastreams-obj.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ datastream Profile:\n${body}");

                weaveById("getObjContent").replace()
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = KARAF_HOME + "/wildlife_insights_test_data/test-images/" + dsLabel + ".jpg";
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(new FileInputStreamCache(resourceFile));
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });

                weaveById("saveToStaging").replace()
                        .to("file:target/staging/");

                weaveById("isFaceBlur").after()
                        .log(LoggingLevel.INFO, "Headers:\n${headers}")
                        .to("mock:result")
                        .stop();
            }
        });

        context.getRouteDefinition("DerivativesIsFaceBlur").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("getParentDs").replace()
                        .setHeader("testDsID").simple("TEST")
                        .to("velocity:file:{{karaf.home}}/wildlife_insights_test_data/fedora/fedora_datastreams.vsl")
                        .log(LoggingLevel.DEBUG, "Test Parent Datastreams:\n${body}");

                weaveById("getObjectXMl").replace()
                        .setHeader("testObjLabel").simple(dsLabel)
                        .to("velocity:file:{{karaf.home}}/wildlife_insights_test_data/fedora/fedora-objectProfile.vsl")
                        .log(LoggingLevel.DEBUG, "Test OBJ Profile:\n${body}");

                weaveById("getParentManifest").replace()
                        .setBody().simple(manifest)
                        .log(LoggingLevel.DEBUG, "Test Manifest:\n${body}");

                weaveById("saveFaceBlurOutputToStaging").replace()
                        .to("file://target/staging?fileName=${header.CamelFileName}");

                weaveById("replaceOBJ").replace()
                        .log(LoggingLevel.INFO, "Body Type: ${body.class.name}")
                        .to("file:target/output?fileName=${header.dsLabel}.JPG");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("pid", pid);
        exchange.getIn().setHeader("methodName", methodName);
        exchange.getIn().setBody(fedoraAPIM);

        template.send("activemq:queue:sidora.apim.update", exchange);

        assertMockEndpointsSatisfied();

        Thread.sleep(1500);

        assertFileNotExists(PROJECT_BASE_DIR + "/target/output/" + dsLabel + ".JPG");
    }
}
