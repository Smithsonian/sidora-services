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

package edu.si.services.sidora.rest.mci;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultHeaderFilterStrategy;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.*;

/**
 * @author jbirkhimer
 */
@Ignore
public class MCI_WBLoginCookieTest extends CamelTestSupport {
    static private String LOG_NAME = "edu.si.mci";

    private static Configuration config = null;
    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static Properties props = new Properties();
    private static DefaultHeaderFilterStrategy dropHeadersStrategy;

    protected List<String> loadAdditionalPropertyFiles() {
        return Arrays.asList(KARAF_HOME + "/etc/system.properties", KARAF_HOME + "/etc/edu.si.sidora.mci.cfg", KARAF_HOME + "/sql/mci.sql.properties");
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        return props;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        List<String> propFileList = loadAdditionalPropertyFiles();
        if (loadAdditionalPropertyFiles() != null) {
            for (String propFile : propFileList) {
                Properties extra = new Properties();
                try {
                    extra.load(new FileInputStream(propFile));
                    this.props.putAll(extra);
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

        super.setUp();
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        //Header Filter Strategy to prevent all headers from being added to Fedora HTTP calls except for Authorization
        dropHeadersStrategy = new DefaultHeaderFilterStrategy();
        //dropHeadersStrategy.setOutFilterPattern("^(?!Authorization$).*$");
        dropHeadersStrategy.setOutFilter(new HashSet<>(Arrays.asList("mciProjectXML", "mciProjectDESCMETA", "mciResourceDESCMETA", "CamelHttpMethod", "CamelHttpResponseCode", "CamelHttpResponseText", "Content-Type", "Content-Length", "Transfer-Encoding", "transfer-encoding", "Vary", "Server", "User-Agent", "Host", "Accept", "Cache-Control", "connection", "Expect", "Fuseki-Request-ID", "Location", "Pragma", "Set-Cookie", "X-Content-Type-Options", "X-Frame-Options", "X-UA-Compatible", "CamelSqlRowCount", "CamelSqlUpdateCount", "correlationId", "mciFolderHolder", "mciOwnerName", "mciOwnerPID", "mciResearchProjectLabel", "operationName")));

        JndiRegistry jndiRegistry = super.createRegistry();
        jndiRegistry.bind("dropHeadersStrategy", dropHeadersStrategy);
        return jndiRegistry;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:start")
                        .process(cookieProcessor())
                        .to("mock:result");
            }
        };
    }

    public Processor cookieProcessor() {
        Processor cookieProcessor = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Message out = exchange.getIn();
                String cookie = null;
                Object cookieHeader = exchange.getIn().getHeader("Set-Cookie");

                List<String> cookiesList = new ArrayList<>();

                if (cookieHeader instanceof ArrayList) {
                    for (Object c : (ArrayList) cookieHeader) {
                        if (c instanceof String) {
                            HttpCookie.parse((String) c).forEach(ck -> cookiesList.add(ck.toString()));
                        }
                    }
                } else if (cookieHeader instanceof String) {
                    // trim value before checking for multiple parameters
                    String trimmed = (String) cookieHeader;

                    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                        // remove the [ ] markers
                        trimmed = trimmed.substring(1, trimmed.length() - 1);
                        List<String> list = new ArrayList<String>();
                        String[] values = trimmed.split(",");
                        for (String s : values) {
                            try {
                                String c = HttpCookie.parse(s.split(";", 2)[0]).toString();
                                String f = c.substring(1, c.length() - 1);
                                list.add(c.substring(1, c.length() - 1));
                            } catch (Exception e) {
                                // this string is invalid, jump to the next one.
                            }
                        }
                        cookiesList.addAll(list);
                    } else {
                        HttpCookie.parse(trimmed).forEach(ck -> cookiesList.add(ck.toString()));
                    }
                }

                if (cookiesList.size() > 1) {
                    cookie = StringUtils.join(cookiesList, "; ");
                } else {
                    cookie = cookiesList.get(0);
                }

                out.setHeader("cookie", cookie);
                log.info("{}, Cookie: {}", out.getBody(String.class), cookie);
            }
        };

        return cookieProcessor;
    }

    @Test
    public void testCookieParser() throws Exception {

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(5);
        mockResult.setAssertPeriod(1500);

        ArrayList<String> testCookie = new ArrayList<>();
        testCookie.addAll(Arrays.asList(new String[]{"f4261fb3-bfc7-4f62-b73a-eec35451fcd1=d5fcd4db-7f7f-40e7-bc88-90f20a3b1bb2; expires=Sun, 22-Sep-2019 08:35:39 GMT; Max-Age=2000000; path=/; domain=.somehost.si.edu; secure; HttpOnly", "e160650a-d863-4cc4-bbd7-ce53925632d1=3343b084-9bfb-49f9-8e5e-285e8dbe31a4; expires=Sun, 22-Sep-2019 08:35:39 GMT; Max-Age=2000000; path=/; domain=.somehost.si.edu; secure; HttpOnly"}));
        template.sendBodyAndHeader("direct:start", "test1", "Set-Cookie", testCookie);

        template.sendBodyAndHeader("direct:start", "test2", "Set-Cookie", "f4261fb3-bfc7-4f62-b73a-eec35451fcd1=d5fcd4db-7f7f-40e7-bc88-90f20a3b1bb2; expires=Sun, 22-Sep-2019 08:35:39 GMT; Max-Age=2000000; path=/; domain=.somehost.si.edu; secure; HttpOnly");

        template.sendBodyAndHeader("direct:start", "test3", "Set-Cookie", "f4261fb3-bfc7-4f62-b73a-eec35451fcd1=d5fcd4db-7f7f-40e7-bc88-90f20a3b1bb2; expires=Sun, 22-Sep-2019 08:35:39 GMT; Max-Age=2000000; path=/; domain=.somehost.si.edu; secure; HttpOnly, e160650a-d863-4cc4-bbd7-ce53925632d1=3343b084-9bfb-49f9-8e5e-285e8dbe31a4; expires=Sun, 22-Sep-2019 08:35:39 GMT; Max-Age=2000000; path=/; domain=.somehost.si.edu; secure; HttpOnly");

        template.sendBodyAndHeader("direct:start", "test4", "Set-Cookie", "[f4261fb3-bfc7-4f62-b73a-eec35451fcd1=d5fcd4db-7f7f-40e7-bc88-90f20a3b1bb2; expires=Sun, 22-Sep-2019 08:35:39 GMT; Max-Age=2000000; path=/; domain=.somehost.si.edu; secure; HttpOnly, e160650a-d863-4cc4-bbd7-ce53925632d1=3343b084-9bfb-49f9-8e5e-285e8dbe31a4; expires=Sun, 22-Sep-2019 08:35:39 GMT; Max-Age=2000000; path=/; domain=.somehost.si.edu; secure; HttpOnly]");

        template.sendBodyAndHeader("direct:start", "test5", "Set-Cookie", "[f4261fb3-bfc7-4f62-b73a-eec35451fcd1=d5fcd4db-7f7f-40e7-bc88-90f20a3b1bb2; expires=Sun, 22-Sep-2019 08:35:39 GMT; Max-Age=2000000; path=/; domain=.somehost.si.edu; secure; HttpOnly]");

        assertMockEndpointsSatisfied();
    }
}
