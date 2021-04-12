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

package edu.si.sidora.mci.configs;

import org.apache.camel.support.DefaultHeaderFilterStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jbirkhimer
 */
@Configuration
public class HeaderFilterStrategyConfig {

    @Bean(name = "dropHeadersStrategy")
    public DefaultHeaderFilterStrategy dropHeadersStrategy() {
        DefaultHeaderFilterStrategy dropHeadersStrategy = new DefaultHeaderFilterStrategy();
        dropHeadersStrategy.setOutFilterPattern("^(?!(Authorization|authorization|cookie)).*$");

        Set<String> headers = new HashSet<>();
        headers.add("mciProjectXML");
        headers.add("mciProjectDESCMETA");
        headers.add("mciResourceDESCMETA");
        headers.add("CamelHttpMethod");
        headers.add("CamelHttpResponseCode");
        headers.add("CamelHttpResponseText");
        headers.add("Content-Type");
        headers.add("Content-Length");
        headers.add("Transfer-Encoding");
        headers.add("transfer-encoding");
        headers.add("Vary");
        headers.add("Server");
        headers.add("User-Agent");
        headers.add("Host");
        headers.add("Accept");
        headers.add("Cache-Control");
        headers.add("connection");
        headers.add("Expect");
        headers.add("Fuseki-Request-ID");
        headers.add("Location");
        headers.add("Pragma");
        headers.add("Set-Cookie");
        headers.add("X-Content-Type-Options");
        headers.add("X-Frame-Options");
        headers.add("X-UA-Compatible");
        headers.add("CamelSqlRowCount");
        headers.add("CamelSqlUpdateCount");
        headers.add("correlationId");
        headers.add("mciFolderHolder");
        headers.add("mciOwnerName");
        headers.add("mciOwnerPID");
        headers.add("mciResearchProjectLabel");
        headers.add("operationName");

        dropHeadersStrategy.setOutFilter(headers);

        return dropHeadersStrategy;
    }

    @Bean(name = "wbHttpHeaderFilterStrategy")
    public DefaultHeaderFilterStrategy wbHttpHeaderFilterStrategy() {
        DefaultHeaderFilterStrategy wbHttpHeaderFilterStrategy = new DefaultHeaderFilterStrategy();
        wbHttpHeaderFilterStrategy.setOutFilterPattern(".*");
        return wbHttpHeaderFilterStrategy;
    }

    @Bean(name = "fusekiHttpHeaderFilterStrategy")
    public DefaultHeaderFilterStrategy fusekiHttpHeaderFilterStrategy() {
        DefaultHeaderFilterStrategy fusekiHttpHeaderFilterStrategy = new DefaultHeaderFilterStrategy();
        Set<String> outFilterHeaders = new HashSet<String>(Arrays.asList("ManifestXML", "CamelSchematronValidationReport", "datastreamValidationXML", "Content-Type", "Content-Length", "Transfer-Encoding", "Vary", "Server", "User-Agent", "Host", "CamelHttpMethod", "CamelHttpResponseCode", "CamelHttpResponseText"));
        fusekiHttpHeaderFilterStrategy.getOutFilter().addAll(outFilterHeaders);

        return fusekiHttpHeaderFilterStrategy;
    }


}
