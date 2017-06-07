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

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.PropertyInject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class IdsPushBean {

    private static final Logger LOG = LoggerFactory.getLogger(IdsPushBean.class);

    @PropertyInject(value = "si.ct.uscbi.idsPushLocation")
    private String pushLocation;
    @PropertyInject(value = "si.ct.uscbi.tempLocationForZip")
    private String tempLocation;
    private static File inputLocation;
    private static String deploymentId;
    private Map<String, String> ignored = new HashMap<String, String>();
    private Message out;

    public void createAndPush(Exchange exchange) throws EdanIdsException {

        try {
            out = exchange.getIn();

            inputLocation = new File(out.getHeader("CamelFileAbsolutePath", String.class));
            // get a list of files from current directory
            if (inputLocation.isFile()) {
                inputLocation = inputLocation.getParentFile();
                LOG.debug("Input File Location: " + inputLocation);
            }

            deploymentId = out.getHeader("SiteId", String.class);

            String assetName = "ExportEmammal_emammal_image_" + deploymentId;
            String pushDirPath = pushLocation + "/" + assetName + "/";
            File assetXmlFile = new File(pushDirPath + assetName + ".xml");
            if (!assetXmlFile.getParentFile().exists()) {
                assetXmlFile.getParentFile().mkdirs();
            } else {
                LOG.warn("IDS files for deployment: {} already exists!!", deploymentId);
            }

            LOG.debug("IDS Write Asset Files to: {}", assetXmlFile);
            LOG.debug("IDS inputLocation = {}", inputLocation);
            LOG.debug("IDS deploymentId = {}", deploymentId);

            File files[] = inputLocation.listFiles();

            LOG.debug("Input file list: " + Arrays.toString(files));

            int completed = 0;

            StringBuilder assetXml = new StringBuilder();
            assetXml.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\r\n<Assets>");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(assetXmlFile))) {

                for (int i = 0; i < files.length; i++) {

                    String fileName = files[i].getName();

                    LOG.debug("Started file: {} has ext = {}", files[i], FilenameUtils.getExtension(fileName));

                    // Do not include the manifest file
                    if (!FilenameUtils.getExtension(fileName).contains("xml")) {

                        LOG.debug("Adding File {}", fileName);

                        File sourceImageFile = new File(files[i].getPath());
                        File destImageFile = new File(pushDirPath + "emammal_image_" + fileName);

                        LOG.debug("Copying image asset from {} to {}", sourceImageFile, destImageFile);
                        FileUtils.copyFile(sourceImageFile, destImageFile);

                        assetXml.append("\r\n  <Asset Name=\"");
                        assetXml.append(FilenameUtils.getName(destImageFile.getPath()));
                        assetXml.append("\" IsPublic=\"Yes\" IsInternal=\"No\" MaxSize=\"3000\" InternalMaxSize=\"4000\">");
                        assetXml.append(FilenameUtils.getBaseName(destImageFile.getPath()));
                        assetXml.append("</Asset>");
                    } else {
                        LOG.debug("Deployment Manifest XML Found! Skipping {}", files[i]);
                    }
                }
                assetXml.append("\r\n</Assets>");

                writer.write(assetXml.toString());

                LOG.info("Completed: {} of {}, Wrote Asset XML File to: {}", completed++, files.length, assetXmlFile);
                out.setHeader("idsPushDir", assetXmlFile.getParent());
            } catch (Exception e) {
                throw new EdanIdsException("IdsPushBean error during createAndPush", e);
            }
        } catch (Exception e) {
            throw new EdanIdsException(e);
        }
    }

    public void addToIgnoreList(String imageId) {
        ignored.put(imageId, imageId);
    }
}
