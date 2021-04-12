///*
// * Copyright 2018-2019 Smithsonian Institution.
// *
// * Licensed under the Apache License, Version 2.0 (the "License"); you may not
// * use this file except in compliance with the License.You may obtain a copy of
// * the License at: http://www.apache.org/licenses/
// *
// * This software and accompanying documentation is supplied without
// * warranty of any kind. The copyright holder and the Smithsonian Institution:
// * (1) expressly disclaim any warranties, express or implied, including but not
// * limited to any implied warranties of merchantability, fitness for a
// * particular purpose, title or non-infringement; (2) do not assume any legal
// * liability or responsibility for the accuracy, completeness, or usefulness of
// * the software; (3) do not represent that use of the software would not
// * infringe privately owned rights; (4) do not warrant that the software
// * is error-free or will be maintained, supported, updated or enhanced;
// * (5) will not be liable for any indirect, incidental, consequential special
// * or punitive damages of any kind or nature, including but not limited to lost
// * profits or loss of data, on any basis arising from contract, tort or
// * otherwise, even if any of the parties has been warned of the possibility of
// * such loss or damage.
// *
// * This distribution includes several third-party libraries, each with their own
// * license terms. For a complete copy of all copyright and license terms, including
// * those of third-party libraries, please see the product release notes.
// */
//
//package edu.si.services;
//
//import org.apache.camel.Exchange;
//import org.apache.camel.builder.AdviceWithRouteBuilder;
//import org.apache.camel.component.mock.MockEndpoint;
//import org.apache.camel.impl.DefaultExchange;
//import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
//import org.junit.Ignore;
//import org.junit.Test;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.net.URLEncoder;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//
//import static org.junit.Assume.assumeFalse;
//import static org.junit.Assume.assumeTrue;
//
///**
// * @author jbirkhimer
// */
//public class EdanBlueprintComponentTest extends CamelBlueprintTestSupport {
//
//    private static final String KARAF_HOME = System.getProperty("karaf.home");
//    private static Properties extra = new Properties();
//
//    private static String TEST_EDAN_ID = "p2b-1515252134647-1516215519247-0"; //QUOTIENTPROD
//    private static String TEST_PROJECT_ID = "testProjectId";
//    private static String TEST_DEPLOYMENT_ID = "testDeploymentId";
//    private static String TEST_IMAGE_ID = "testRaccoonAndFox";
//    private static String TEST_TITLE = "Camera Trap Image Northern Raccoon, Red Fox";
//    private static String TEST_TYPE = "emammal_image";
//    private static String TEST_APP_ID = "QUOTIENTPROD";
//
//    @Override
//    protected String getBlueprintDescriptor() {
//        return "blueprint.xml";
//    }
//
//    protected List<String> loadAdditionalPropertyFiles() {
//        return Arrays.asList(KARAF_HOME + "/etc/system.properties", KARAF_HOME + "/etc/edu.si.sidora.karaf.cfg", KARAF_HOME + "/etc/edu.si.sidora.emammal.cfg");
//    }
//
//    @Override
//    public void setUp() throws Exception {
//        //System.getProperties().list(System.out);
//        log.debug("===================[ KARAF_HOME = {} ]===================", KARAF_HOME);
//
//        List<String> propFileList = loadAdditionalPropertyFiles();
//        if (loadAdditionalPropertyFiles() != null) {
//            for (String propFile : propFileList) {
//                Properties extra = new Properties();
//                try {
//                    extra.load(new FileInputStream(propFile));
//                    this.extra.putAll(extra);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        for (Map.Entry<Object, Object> p : System.getProperties().entrySet()) {
//            if (extra.containsKey(p.getKey())) {
//                extra.setProperty(p.getKey().toString(), p.getValue().toString());
//            }
//        }
//
//        super.setUp();
//    }
//
//    @Override
//    protected String setConfigAdminInitialConfiguration(Properties configAdmin) {
//        configAdmin.putAll(extra);
//        return "edu.si.sidora.emammal";
//    }
//
//    @Test
//    @Ignore
//    public void testEdanComponent() throws Exception {
//        MockEndpoint mockResult = getMockEndpoint("mock:result");
//        mockResult.expectedMinimumMessageCount(1);
//        mockResult.expectedBodiesReceived("Test Body");
//
//        Exchange exchange = new DefaultExchange(context);
//        exchange.getIn().setBody("Test Body");
//
//        template.send("direct:start", exchange);
//
//        assertMockEndpointsSatisfied();
//    }
//
//    @Test
//    @Ignore
//    public void testEdanComponentParams() throws Exception {
//        MockEndpoint mockResult = getMockEndpoint("mock:result");
//        mockResult.expectedMinimumMessageCount(1);
//        mockResult.expectedBodiesReceived("Test Body");
//
//        context.getRouteDefinition("testEdanComponent").adviceWith(context, new AdviceWithRouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                weaveById("edanComponentCall").replace()
//                        .toD("edan:${headers.edanService}?facet=${header.facet}&fqs=${headers.fqs}&linkedContent=${headers.linkedContent}&profile=${headers.profile}&query=${headers.query}&rows=${headers.rows}&sortDir=${headers.start}&url=${headers.url}&edanId=${header.edanId}");
//            }
//        });
//
//        Exchange exchange = new DefaultExchange(context);
//        exchange.getIn().setBody("Test Body");
//        exchange.getIn().setHeader("edanAuthType", "testAuthType");
//        exchange.getIn().setHeader("edanService", "getContent");
//        exchange.getIn().setHeader("facet", "testFacet");
//        exchange.getIn().setHeader("fqs", "testFqs");
//        exchange.getIn().setHeader("linkedContent", "testLinkedContent");
//        exchange.getIn().setHeader("profile", "testProfile");
//        exchange.getIn().setHeader("query", "testQuery");
//        exchange.getIn().setHeader("rows", "testRows");
//        exchange.getIn().setHeader("sortDir", "testSortDir");
//        exchange.getIn().setHeader("start", "testStart");
//        exchange.getIn().setHeader("url", "testUrl");
//        exchange.getIn().setHeader("edanId", "testEdanId");
//
//        template.send("direct:start", exchange);
//
//        assertMockEndpointsSatisfied();
//
//    }
//
//    @Test
//    public void testEdanSearch() throws Exception {
//        MockEndpoint mockResult = getMockEndpoint("mock:result");
//        mockResult.expectedMinimumMessageCount(1);
//        mockResult.expectedBodiesReceived("Test Body");
//
//        context.getRouteDefinition("testEdanComponent").adviceWith(context, new AdviceWithRouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                weaveById("edanComponentCall").replace()
//                        .toD("edan:${headers.edanService}?${header.params}");
//            }
//        });
//
//        Exchange exchange = new DefaultExchange(context);
//        exchange.getIn().setHeader("edanService", "search");
//        exchange.getIn().setHeader("params", "fqs=[\"p.emammal_image.image.id:" + TEST_IMAGE_ID + "\"]");
//
//        template.send("direct:start", exchange);
//
//        assertMockEndpointsSatisfied();
//
//    }
//
//    @Test
//    public void edanMetadataSearchTest() throws Exception {
//        MockEndpoint mockResult = getMockEndpoint("mock:result");
//        mockResult.expectedMessageCount(1);
//
//        context.getRouteDefinition("testEdanComponent").adviceWith(context, new AdviceWithRouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                weaveById("edanComponentCall").replace()
//                        .toD("edan:${headers.edanService}?${header.params}");
//            }
//        });
//
//        Exchange exchange = new DefaultExchange(context);
//        exchange.getIn().setHeader("edanService", "search");
//        exchange.getIn().setHeader("params", "q=app_id:" + TEST_APP_ID + " AND type:" + TEST_TYPE);
//
//        template.send("direct:start", exchange);
//
//        assertMockEndpointsSatisfied();
//    }
//
//    @Test
//    public void edanMetadataSearchFilterQueryTest() throws Exception {
//        MockEndpoint mockResult = getMockEndpoint("mock:result");
//        mockResult.expectedMessageCount(1);
//
//        context.getRouteDefinition("testEdanComponent").adviceWith(context, new AdviceWithRouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                weaveById("edanComponentCall").replace()
//                        .toD("edan:${headers.edanService}?${header.params}");
//            }
//        });
//
//        Exchange exchange = new DefaultExchange(context);
//        exchange.getIn().setHeader("edanService", "search");
//        exchange.getIn().setHeader("params", "fqs=[\"type:" + TEST_TYPE + "\"]");
//
//        template.send("direct:start", exchange);
//
//        assertMockEndpointsSatisfied();
//    }
//
//    @Test
//    public void edanMetadataSearchFilterQueryCtProjectIdTest() throws Exception {
//        MockEndpoint mockResult = getMockEndpoint("mock:result");
//        mockResult.expectedMessageCount(1);
//
//        context.getRouteDefinition("testEdanComponent").adviceWith(context, new AdviceWithRouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                weaveById("edanComponentCall").replace()
//                        .toD("edan:${headers.edanService}?${header.params}");
//            }
//        });
//
//        Exchange exchange = new DefaultExchange(context);
//        exchange.getIn().setHeader("edanService", "search");
//        exchange.getIn().setHeader("params", "fqs=[\"p.emammal_image.project_id:"+ TEST_PROJECT_ID+ "\"]");
//
//        template.send("direct:start", exchange);
//
//        assertMockEndpointsSatisfied();
//    }
//
//    @Test
//    public void edanMetadataSearchFilterQueryCtDeploymentIdTest() throws Exception {
//        MockEndpoint mockResult = getMockEndpoint("mock:result");
//        mockResult.expectedMessageCount(1);
//
//        context.getRouteDefinition("testEdanComponent").adviceWith(context, new AdviceWithRouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                weaveById("edanComponentCall").replace()
//                        .toD("edan:${headers.edanService}?${header.params}");
//            }
//        });
//
//        Exchange exchange = new DefaultExchange(context);
//        exchange.getIn().setHeader("edanService", "search");
//        exchange.getIn().setHeader("params", "fqs=[\"p.emammal_image.deployment_id:" + TEST_DEPLOYMENT_ID + "\"]&rows=20&start=0");
//
//        template.send("direct:start", exchange);
//
//        assertMockEndpointsSatisfied();
//    }
//
//    @Test
//    public void edanMetadataSearchFilterQueryCtProjectAndDeploymentIdTest() throws Exception {
//        MockEndpoint mockResult = getMockEndpoint("mock:result");
//        mockResult.expectedMessageCount(1);
//
//        context.getRouteDefinition("testEdanComponent").adviceWith(context, new AdviceWithRouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                weaveById("edanComponentCall").replace()
//                        .toD("edan:${headers.edanService}?${header.params}");
//            }
//        });
//
//        Exchange exchange = new DefaultExchange(context);
//        exchange.getIn().setHeader("edanService", "search");
//        exchange.getIn().setHeader("params", "fqs=[\"p.emammal_image.project_id:" + TEST_PROJECT_ID + "\", \"p.emammal_image.deployment_id:" + TEST_DEPLOYMENT_ID + "\"]&rows=20&start=0");
//
//        template.send("direct:start", exchange);
//
//        assertMockEndpointsSatisfied();
//    }
//
//    @Test
//    public void edanMetadataSearchFilterQueryImageIdTest() throws Exception {
//        MockEndpoint mockResult = getMockEndpoint("mock:result");
//        mockResult.expectedMessageCount(1);
//
//        context.getRouteDefinition("testEdanComponent").adviceWith(context, new AdviceWithRouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                weaveById("edanComponentCall").replace()
//                        .toD("edan:${headers.edanService}?${header.params}");
//            }
//        });
//
//        Exchange exchange = new DefaultExchange(context);
//        exchange.getIn().setHeader("edanService", "search");
//        exchange.getIn().setHeader("params", "fqs=[\"p.emammal_image.image.id:" + TEST_IMAGE_ID + "\"]");
//
//        template.send("direct:start", exchange);
//
//        assertMockEndpointsSatisfied();
//    }
//
//    @Test
//    public void edanMetadataSearchFilterQueryIdsIdTest() throws Exception {
//        MockEndpoint mockResult = getMockEndpoint("mock:result");
//        mockResult.expectedMessageCount(1);
//
//        context.getRouteDefinition("testEdanComponent").adviceWith(context, new AdviceWithRouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                weaveById("edanComponentCall").replace()
//                        .toD("edan:${headers.edanService}?${header.params}");
//            }
//        });
//
//        Exchange exchange = new DefaultExchange(context);
//        exchange.getIn().setHeader("edanService", "search");
//        exchange.getIn().setHeader("params", "fqs=[\"p.emammal_image.image.id:" + TEST_IMAGE_ID + "\"]&rows=20&start=0");
//
//        template.send("direct:start", exchange);
//
//        assertMockEndpointsSatisfied();
//    }
//
//
//}
