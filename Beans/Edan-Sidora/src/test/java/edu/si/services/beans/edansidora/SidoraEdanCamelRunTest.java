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

package edu.si.services.beans.edansidora;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.velocity.VelocityConstants;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.DateTool;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;

import static org.apache.commons.io.FileUtils.readFileToString;

/** Only for testing locally against test server
 * @author jbirkhimer
 */
@Ignore
public class SidoraEdanCamelRunTest extends CamelTestSupport {

    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static File testManifest = new File(KARAF_HOME + "/unified-test-deployment/deployment_manifest.xml");
    private static final String TEST_SERVER = "localhost";

    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();

        ActiveMQComponent activeMQComponent = new ActiveMQComponent();
        activeMQComponent.setBrokerURL("tcp://"+TEST_SERVER+":61616");
        activeMQComponent.setUserName("smx");
        activeMQComponent.setPassword("smx");
        camelContext.addComponent("activemq", activeMQComponent);

        return camelContext;
    }

    @Test
    public void testMavenCamelRun() throws Exception {
        String manifest = readFileToString(testManifest);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", manifest);
        exchange.getIn().setHeader("addEdanIds", "true");
        exchange.getIn().setHeader("ProjectId", "testProjectId");
        exchange.getIn().setHeader("SiteId", "testSubprojectId");
        exchange.getIn().setHeader("SitePID", "test.smx.home:15");
        exchange.getIn().setHeader("PIDAggregation", "test.smx.home:16,test.smx.home:17,test.smx.home:18,test.smx.home:19,test.smx.home:20,test.smx.home:21,test.smx.home:22,test.smx.home:23");
        exchange.getIn().setHeader("ResearcherObservationPID", "test.smx.home:22");
        exchange.getIn().setHeader("VolunteerObservationPID", "test.smx.home:23");

        template.send("activemq:queue:edanIds.ct.update.test", exchange);
    }
}
