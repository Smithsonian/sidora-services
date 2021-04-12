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

package edu.si.services;

import edu.si.services.camel.edan.EdanComponent;
import edu.si.services.camel.edan.EdanConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class EdanRouteBuilderComponentTest extends CamelTestSupport {

    private final EdanConfiguration edanConfiguration = new EdanConfiguration();

    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static Properties props = new Properties();

    private static String TEST_EDAN_ID = "p2b-1515252134647-1516215519247-0"; //QUOTIENTPROD
    private static String TEST_PROJECT_ID = "testProjectId";
    private static String TEST_DEPLOYMENT_ID = "testDeploymentId";
    private static String TEST_IMAGE_ID = "testRaccoonAndFox";
    private static String TEST_TITLE = "Camera Trap Image Northern Raccoon, Red Fox";
    private static String TEST_TYPE = "emammal_image";
    private static String TEST_APP_ID = "QUOTIENTPROD";

    protected List<String> loadAdditionalPropertyFiles() {
        return Arrays.asList(KARAF_HOME + "/etc/system.properties", KARAF_HOME + "/etc/edu.si.sidora.karaf.cfg", KARAF_HOME + "/etc/edu.si.sidora.emammal.cfg");
    }

    @Override
    public void setUp() throws Exception {
        //System.getProperties().list(System.out);
        log.debug("===================[ KARAF_HOME = {} ]===================", KARAF_HOME);

        List<String> propFileList = loadAdditionalPropertyFiles();
        if (loadAdditionalPropertyFiles() != null) {
            for (String propFile : propFileList) {
                Properties extra = new Properties();
                try {
                    extra.load(new FileInputStream(propFile));
                    props.putAll(extra);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        for (Map.Entry<Object, Object> p : System.getProperties().entrySet()) {
            if (props.containsKey(p.getKey())) {
                props.setProperty(p.getKey().toString(), p.getValue().toString());
            }
        }

        edanConfiguration.setEdanBaseUrl(new URL(props.getProperty("si.ct.uscbi.server")));
        edanConfiguration.setApp_id(props.getProperty("si.ct.uscbi.appId"));
        edanConfiguration.setEdan_key(props.getProperty("si.ct.uscbi.edanKey"));

        super.setUp();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        return props;
    }

    /**
     * Setting up the camel context with the EDAN configuration
     *
     * @return CamelContext
     * @throws Exception
     */
    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();

        context.addComponent("edan", new EdanComponent(edanConfiguration));

        return context;
    }

    @Test
    public void testEdanSearch() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMinimumMessageCount(1);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("service", "search");
        exchange.getIn().setHeader("params", "fqs=[\"p.emammal_image.image.id:" + TEST_IMAGE_ID + "\"]");

        template.send("direct:start", exchange);

        assertMockEndpointsSatisfied();

    }

    @Test
    @Ignore
    public void testEdanDelete() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMinimumMessageCount(1);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("service", "delete");
        exchange.getIn().setHeader("params", "id=p2b-1515252134647-1516214976507-0&type=" + TEST_TYPE );

        template.send("direct:delete", exchange);

        assertMockEndpointsSatisfied();

    }

    @Test
    @Ignore
    public void testEdanSearchAfterDelete() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(4);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("service", "getContent");
        exchange.getIn().setHeader("params", "id=p2b-1515252134647-1516214976507-0");

        template.send("direct:start", exchange);

        exchange.getIn().setHeader("service", "search");
        exchange.getIn().setHeader("params", "fqs=[\"p.emammal_image.image.id:testDeploymentIds1i1\"]");

        template.send("direct:start", exchange);

        exchange.getIn().setHeader("service", "getAdminContent");
        exchange.getIn().setHeader("params", "id=p2b-1515252134647-1516214976507-0");

        template.send("direct:start", exchange);

        assertMockEndpointsSatisfied();

    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct:start").id("edanTest")
                        .toD("edan:${header.service}?${header.params}")
                        .log(LoggingLevel.INFO, "Edan ${header.service} body contains:\n ${body}")
                        .to("mock:result");

                from("direct:delete").id("edanDelete")
                        .toD("edan:getContent?id=p2b-1515252134647-1516214976507-0")
                        .log(LoggingLevel.INFO, "Edan getContent body contains:\n ${body}")
                        .toD("edan:${header.service}?${header.params}")
                        .log(LoggingLevel.INFO, "Edan ${header.service} body contains:\n ${body}")
                        .toD("edan:getContent?id=p2b-1515252134647-1516214976507-0")
                        .log(LoggingLevel.INFO, "Edan getContent After Delete body contains:\n ${body}")
                        .to("mock:result");
            }
        };
    }
}
