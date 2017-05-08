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

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author jbirkhimer
 */
public class IdsPushBeanTest {
    private static final Logger LOG = LoggerFactory.getLogger(EdanApiTest.class);
    private static Configuration config = null;
    private static String defaultTestProperties = "src/test/resources/test.properties";
    private static final String testManifest = "src/test/resources/unified-test-deployment/deployment_manifest.xml";
    //Temp directories created for testing the camel validation route
    private static String tempTargetLoc = "target/createZipAndPushTest";
    private static File tempLocationForZipDir, idsPushLocationDir;
    private static String tempLocationForZip, idsPushLocation;
    private static boolean cleanupTempFiles;

    @Test
    public void createZipAndPush() throws Exception {

        //Delete temp directories???
        cleanupTempFiles = false;

        String deploymentId = "testDeploymentId";

        LOG.info("Using Temp Zip Loc: {}", tempLocationForZipDir.getAbsolutePath());
        LOG.info("Using Push Loc: {}", idsPushLocationDir.getAbsolutePath());

        IdsPushBean ipb = new IdsPushBean();
        ipb.setInputLocation(testManifest);
        ipb.setTempLocation(tempLocationForZipDir.getAbsolutePath() + "/");
        ipb.setDeploymentId(deploymentId);
        ipb.setPushLocation(idsPushLocationDir.getAbsolutePath() + "/");
        ipb.addToIgnoreList("ignoreme");
        Map<String, String> returned = ipb.createZipAndPush();
        for(Map.Entry<String, String> entry : returned.entrySet()) {
            LOG.info(entry.getKey() + "\t" + entry.getValue());
        }

        LOG.info("Test Done");

        assertTrue(tempLocationForZipDir.isDirectory());
        if (!returned.get("completionInformation").contains("Removed Temp Zip File? true")) {
            assertTrue(FileUtils.directoryContains(tempLocationForZipDir, new File(tempLocationForZip + "ExportEmammal_emammal_image_" + deploymentId + ".zip")));
        }

        assertTrue(idsPushLocationDir.isDirectory());
        assertTrue(FileUtils.directoryContains(idsPushLocationDir, new File(idsPushLocation + "ExportEmammal_emammal_image_" + deploymentId + ".zip")));
    }

    @BeforeClass
    public static void setUp() throws ConfigurationException {
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.fileBased().setFile(new File(defaultTestProperties)));
        config = builder.getConfiguration();
        builder.save();

        tempLocationForZip = tempTargetLoc + config.getString("si.ct.uscbi.tempLocationForZip");
        idsPushLocation = tempTargetLoc + config.getString("si.ct.uscbi.idsPushLocation");

        LOG.info("Using Temp Loc: {}", tempLocationForZip);
        LOG.info("Using Push Loc: {}", idsPushLocation);

        //Create temp dir's
        tempLocationForZipDir = new File(tempLocationForZip);
        if(!tempLocationForZipDir.exists()){
            tempLocationForZipDir.mkdirs();
        }

        idsPushLocationDir = new File(idsPushLocation);
        if(!idsPushLocationDir.exists()){
            idsPushLocationDir.mkdirs();
        }
    }

    /**
     * Clean up the temp directories after tests are finished
     * @throws IOException
     */
    @AfterClass
    public static void teardown() throws IOException {
        if (cleanupTempFiles) {
            File tempTargetDir = new File(tempTargetLoc);
            if (tempTargetDir.exists()) {
                FileUtils.deleteDirectory(tempTargetDir);
            }
        }
    }

}