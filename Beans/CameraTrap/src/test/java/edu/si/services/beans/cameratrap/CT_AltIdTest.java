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

import edu.si.services.fedorarepo.FedoraComponent;
import edu.si.services.fedorarepo.FedoraObjectNotFoundException;
import edu.si.services.fedorarepo.FedoraSettings;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author jbirkhimer
 */
public class CT_AltIdTest extends CamelBlueprintTestSupport {

    private static String LOG_NAME = "edu.si.mci";

    private static final Boolean USE_ACTUAL_FEDORA_SERVER = true;
    private static Configuration config = null;

    @Test
    public void ctProcessProjectAltIdTest() throws Exception {

        String ctTestManifest = FileUtils.readFileToString(new File("src/test/resources/AltIdSampleData/p117d22157/deployment_manifest.xml"));
        String parentRELS_EXT = FileUtils.readFileToString(new File("src/test/resources/AltIdSampleData/test-data/projectRELS-EXT.rdf"));
        String parentEAC_CPF = FileUtils.readFileToString(new File("src/test/resources/AltIdSampleData/test-data/projectEAC-CPF.xml"));

        MockEndpoint mockEndpointResult = getMockEndpoint("mock:result");
        mockEndpointResult.expectedMessageCount(1);
        MockEndpoint mockEndpointAddDatastream = getMockEndpoint("mock:addDatastream");
        mockEndpointAddDatastream.expectedMessageCount(2);
        mockEndpointAddDatastream.expectedBodiesReceived(parentRELS_EXT, parentEAC_CPF);

        context.getRouteDefinition("UnifiedCameraTrapProcessProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //Modify the onException so that test run faster by defining a new onException for FedoraObjectNotFoundExceptions
                context.getRouteDefinition("UnifiedCameraTrapProcessProject")
                        .onException(FedoraObjectNotFoundException.class)
                        .useOriginalMessage()
                        .onRedeliveryRef("inFlightConceptCheckProcessor")
                        .useExponentialBackOff()
                        .backOffMultiplier("2")
                        .redeliveryDelay("1000")
                        .maximumRedeliveries("1")
                        .retryAttemptedLogLevel(LoggingLevel.WARN)
                        .retriesExhaustedLogLevel(LoggingLevel.WARN)
                        .logExhausted(false)
                        .continued(true)
                        .end();

                //Intercept sending to fedora
                interceptSendToEndpoint("fedora:create.*").skipSendToOriginalEndpoint().setHeader("CamelFedoraPid", simple("test:12345"));
                interceptSendToEndpoint("fedora:addDatastream.*RELS-EXT.*").skipSendToOriginalEndpoint().to("mock:addDatastream");
                interceptSendToEndpoint("fedora:hasConcept.*").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, "Skipping fedora:hasConcept");
                interceptSendToEndpoint("fedora:addDatastream.*EAC-CPF.*").skipSendToOriginalEndpoint().to("mock:addDatastream");

                weaveAddLast().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", ctTestManifest);
        exchange.getIn().setHeader("CamelFileParent", "someCamelFileParent");
        exchange.getIn().setHeader("CamelFedoraPid", config.getString("si.ct.root"));

        template.send("direct:processProject", exchange);

        assertEquals(parentRELS_EXT, mockEndpointAddDatastream.getExchanges().get(0).getIn().getBody());
        assertEquals(parentEAC_CPF, mockEndpointAddDatastream.getExchanges().get(1).getIn().getBody());

        assertMockEndpointsSatisfied();

    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();

        if (USE_ACTUAL_FEDORA_SERVER) {
            FedoraSettings fedoraSettings = new FedoraSettings(
                    String.valueOf(config.getString("si.fedora.host")),
                    String.valueOf(config.getString("si.fedora.user")),
                    String.valueOf(config.getString("si.fedora.password"))
            );

            FedoraComponent fedora = new FedoraComponent();
            fedora.setSettings(fedoraSettings);

            //Adding the Fedora Component to the the context using the setting above
            context.addComponent("fedora", fedora);
        }

        return context;
    }

    @Override
    protected String[] loadConfigAdminConfigurationFile() {
        return new String[]{"src/test/resources/test.properties", "edu.si.sidora.karaf"};
    }


    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry reg = super.createRegistry();
        return reg;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "Routes/unified-camera-trap-route.xml";
    }


    @Before
    @Override
    public void setUp() throws Exception {
        System.setProperty("karaf.home", "target/test-classes");

        List<String> propFileList = Arrays.asList("target/test-classes/etc/edu.si.sidora.karaf.cfg", "target/test-classes/etc/system.properties");

        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.fileBased().setFile(new File("src/test/resources/test.properties")));
        config = builder.getConfiguration();

        for (String propFile : propFileList) {

            FileBasedConfigurationBuilder<FileBasedConfiguration> builder2 =
                    new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                            .configure(params.fileBased().setFile(new File(propFile)));

            for (Iterator<String> i = builder2.getConfiguration().getKeys(); i.hasNext();) {
                String key = i.next();
                Object value = builder2.getConfiguration().getProperty(key);
                if (!config.containsKey(key)) {
                    config.setProperty(key, value);
                }
            }
        }

        builder.save();

        super.setUp();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {

        Properties extra = new Properties();

        for (Iterator<String> i = config.getKeys(); i.hasNext();) {
            String key = i.next();
            Object value = config.getProperty(key);
            extra.setProperty(key, String.valueOf(value));
        }

        return extra;
    }
}
