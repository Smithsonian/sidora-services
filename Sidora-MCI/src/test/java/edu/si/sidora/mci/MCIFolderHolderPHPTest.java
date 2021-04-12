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

package edu.si.sidora.mci;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * run with a drupal mysql database and set config in test.properties
 * @author jbirkhimer
 */
@Disabled
public class MCIFolderHolderPHPTest extends CamelTestSupport {

    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private BasicDataSource MYSQL_DB;
    private static Properties extra = new Properties();

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        List<String> propFileList = loadAdditionalPropertyFiles();
        if (loadAdditionalPropertyFiles() != null) {
            for (String propFile : propFileList) {
                Properties extra = new Properties();
                try {
                    extra.load(new FileInputStream(propFile));
                    this.extra.putAll(extra);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        for (Map.Entry<Object, Object> p : System.getProperties().entrySet()) {
            if (extra.containsKey(p.getKey())) {
                extra.setProperty(p.getKey().toString(), p.getValue().toString());
            }
        }

        MYSQL_DB = new BasicDataSource();
        MYSQL_DB.setDriverClassName("com.mysql.jdbc.Driver");
        MYSQL_DB.setUrl(extra.getProperty("drupal.db.url"));
        MYSQL_DB.setUsername(extra.getProperty("drupal.db.username").toString());
        MYSQL_DB.setPassword(extra.getProperty("drupal.db.password").toString());

        super.setUp();

        context.getRegistry().bind("drupalDataSource", MYSQL_DB);
    }

    protected List<String> loadAdditionalPropertyFiles() {
        return Arrays.asList(KARAF_HOME + "/etc/system.properties", KARAF_HOME + "/etc/edu.si.sidora.karaf.cfg", KARAF_HOME + "/etc/application-test.properties", KARAF_HOME + "/sql/application-sql.properties");
    }

    protected static Properties getExtra() {
        return extra;
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        return extra;
    }

    @Test
    public void testPhpArrayDeserialize() throws Exception {

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived("si-user:5");

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .toD("sql:{{sql.find.mci.user.data}}?dataSource=#drupalDataSource&outputType=SelectOne&outputHeader=drupalUserData").id("queryFolderHolder")
                        .log(LoggingLevel.INFO, "Drupal db SQL Query Result: ${header.drupalUserData}")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String drupalUserData = out.getHeader("drupalUserData", String.class);

                                List<String> args = new ArrayList<>();
                                args.add("-r");
                                args.add("echo unserialize('"+ drupalUserData +"')[\"islandora_user_pid\"];");

                                out.setHeader("CamelExecCommandArgs", args);
                                log.info("Stop");
                            }
                        })

                        .log("***** php args ***** = ${header.CamelExecCommandArgs}")
                        .toD("exec:php?useStderrOnEmptyStdout=true")
                        .log("===== user_pid =====> ${body}")
                        .to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("mciFolderHolder", "testFolderHolder");

        template.send("direct:start", exchange);

        assertMockEndpointsSatisfied();


    }
}
