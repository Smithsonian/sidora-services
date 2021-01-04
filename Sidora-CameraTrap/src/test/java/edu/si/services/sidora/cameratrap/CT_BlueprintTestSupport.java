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

package edu.si.services.sidora.cameratrap;

import edu.si.services.fedorarepo.FedoraComponent;
import edu.si.services.fedorarepo.FedoraSettings;
import org.apache.camel.CamelContext;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author jbirkhimer
 */
public class CT_BlueprintTestSupport extends CamelBlueprintTestSupport {

    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static Properties extra = new Properties();
    private static AmazonS3ClientMock amazonS3Client;

    protected static Properties getExtra() {
        return extra;
    }

    protected static AmazonS3ClientMock getAmazonS3Client() {
        return amazonS3Client;
    }

    protected List<String> loadAdditionalPropertyFiles() {
        return Arrays.asList(KARAF_HOME + "/etc/system.properties", KARAF_HOME + "/etc/edu.si.sidora.karaf.cfg", KARAF_HOME + "/etc/edu.si.sidora.emammal.cfg", KARAF_HOME + "/etc/test.properties");
    }

    protected String[] preventRoutesFromStarting() {
        return null;
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();

        //add fedora component using test properties to the context
        FedoraSettings fedoraSettings = new FedoraSettings(extra.getProperty("si.fedora.host"), extra.getProperty("si.fedora.user"), extra.getProperty("si.fedora.password"));
        FedoraComponent fedora = new FedoraComponent();
        fedora.setSettings(fedoraSettings);
        context.addComponent("fedora", fedora);

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
    protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
        services.put("amazonS3Client", asService(amazonS3Client, null));
    }

    @Override
    public void setUp() throws Exception {
        //System.getProperties().list(System.out);
        log.debug("===================[ KARAF_HOME = {} ]===================", System.getProperty("karaf.home"));

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

        //Initialize the Mock AmazonS3Client
        amazonS3Client = new AmazonS3ClientMock(extra.getProperty("si.ct.uscbi.aws.accessKey"), extra.getProperty("si.ct.uscbi.aws.secretKey"));

        super.setUp();
    }

    @Override
    protected String setConfigAdminInitialConfiguration(Properties configAdmin) {
        configAdmin.putAll(extra);
        return "edu.si.sidora.karaf";
    }
}
