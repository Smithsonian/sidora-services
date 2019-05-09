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

package edu.si.services.solr;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author jbirkhimer
 */
public class Solr_CT_BlueprintTestSupport extends CamelBlueprintTestSupport {

    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static Properties extra = new Properties();

    private static final int PORT = AvailablePortFinder.getNextAvailable();

    protected static Properties getExtra() {
        return extra;
    }

    protected List<String> loadAdditionalPropertyFiles() {
        return Arrays.asList(KARAF_HOME + "/etc/system.properties", KARAF_HOME + "/etc/edu.si.sidora.karaf.cfg", KARAF_HOME + "/etc/edu.si.sidora.solr.cfg", KARAF_HOME + "/etc/test.properties");
    }

    protected String[] preventRoutesFromStarting() {
        return null;
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();

        //Prevent Certain Routes From Starting
        String[] routeList = preventRoutesFromStarting();
        if (routeList != null) {
            for (String route : routeList) {
                context.getRouteDefinition(route).autoStartup(false);
            }
        }

        return context;
    }

    @Override
    public void setUp() throws Exception {
        //System.getProperties().list(System.out);
        log.info("===================[ KARAF_HOME = {} ]===================", System.getProperty("karaf.home"));

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

        extra.setProperty("dynamic.test.port", String.valueOf(PORT));

        super.setUp();
    }

    @Override
    protected String[] loadConfigAdminConfigurationFile() {
        // which .cfg file to use, and the name of the persistence-id
        return new String[]{KARAF_HOME + "/etc/test.properties", "edu.si.sidora.solr"};
    }

    @Override
    protected String setConfigAdminInitialConfiguration(Properties configAdmin) {
        configAdmin.putAll(extra);
        return "edu.si.sidora.solr";
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry reg = super.createRegistry();
        reg.bind("jsonProvider", org.apache.cxf.jaxrs.provider.json.JSONProvider.class);
        reg.bind("jaxbProvider", org.apache.cxf.jaxrs.provider.JAXBElementProvider.class);
        return reg;
    }
}
