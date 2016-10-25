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

package edu.si.services.sidora.rest.batch.beans;

import org.apache.camel.*;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author jbirkhimer
 */
public class BatchRequestControllerBean {

    private static final Logger LOG = LoggerFactory.getLogger(BatchRequestControllerBean.class);

    private Message out;

    private String correlationId;

    @PropertyInject(value = "batch.staging.dir")
    private String stagingDir;

    @PropertyInject(value = "batch.data.dir")
    private String processingDir;

    @PropertyInject(value = "si.fedora.user")
    private String fedoraUser;

    private File tempfile, metadataFile, sidoraDatastreamFile, dataOutputDir;

    /**
     *
     * @param headers Map of the inbound message headers
     * @return
     */
    public Map<String, Object> createBatchRequest(@Headers Map<String,Object> headers) {

        //correlationId, projectId, resourceCount, processCount
        correlationId = UUID.randomUUID().toString();

        LOG.info("New Batch Process request from {} with ParentId={}, CorrelationId={}", headers.get("operationName"), headers.get("parentId"), correlationId);

        LOG.debug("=============================== fedora user: {} ===============================", fedoraUser);

        Map<String, Object> newBatchRequest = new HashMap<String, Object>();
        newBatchRequest.put("correlationId", correlationId);
        newBatchRequest.put("parentId", headers.get("parentId"));

        newBatchRequest.put("resourceZipFileURL", headers.get("resourceZipFileURL"));
        newBatchRequest.put("metadataFileURL", headers.get("metadataFileURL"));
        newBatchRequest.put("sidoraDatastreamFileURL", headers.get("sidoraDatastreamFileURL"));
        newBatchRequest.put("contentModel", headers.get("contentModel"));
        newBatchRequest.put("resourceOwner", headers.get("resourceOwner"));
        newBatchRequest.put("titleField", headers.get("titleField"));

        return newBatchRequest;
    }

    /**
     *
     * @param exchange
     * @param batchRequestMap
     * @throws Exception
     */
    public void processBatchRequest(Exchange exchange, Map<String, Object> batchRequestMap) throws Exception {
        out = exchange.getIn();

        correlationId = batchRequestMap.get("correlationId").toString();
        LOG.debug("===================================>>>>>>>>>>>>>>>>>>>>>>>>>>>> parentId={}", batchRequestMap.get("parentId"));
        LOG.debug("=============================== fedora user: {} ===============================", fedoraUser);

        //Set location to extract to
        dataOutputDir = new File(processingDir + correlationId);

        //Save the file from URL to temp file
        //URL resourceFileURL = new URL("file://" + exchange.getIn().getHeader("resourceZipFileURL", String.class));
        URL resourceFileURL = new URL("file://" + batchRequestMap.get("resourceZipFileURL"));
        tempfile = new File(stagingDir + correlationId + ".zip");
        LOG.debug("Saving URL Resource To Temp File:{}", tempfile);

        try {
            FileUtils.copyURLToFile(resourceFileURL, tempfile);
        } catch (IOException e) {
            System.err.println("Caught IOException: Unable to copy Batch Resource file from: " + resourceFileURL + " to: " + tempfile + "\n" + e.getMessage());
            //e.printStackTrace();
        }

        //Extract the zip archive using a producer template to the extractor component
        LOG.debug("Headers Before Extractor: {}", exchange.getIn().getHeaders());
        exchange.getContext().createProducerTemplate().sendBody("extractor:extract?location=" + processingDir, tempfile);
        LOG.debug("Headers After Extractor: {}", exchange.getIn().getHeaders());

        //Delete the temp file
        tempfile.delete();

        //Grab the metadata datastream file from URL
        //URL metadataFileURL = new URL("file://" + exchange.getIn().getHeader("metadataFileURL", String.class));
        URL metadataFileURL = new URL("file://" + batchRequestMap.get("metadataFileURL"));
        metadataFile = new File(processingDir + correlationId + "/metadata.xml");
        LOG.debug("Saving URL Metadata Datastream To File:{}", metadataFile);

        try {
            FileUtils.copyURLToFile(metadataFileURL, metadataFile);
        } catch (IOException e) {
            System.err.println("Caught IOException: Unable to copy Metadata Datastream file from: " + metadataFileURL + " to: " + metadataFile + "\n" + e.getMessage());
            //e.printStackTrace();
        }

        //Grab the sidora datastream file from URL
        //URL sidoraDatastreamFileURL = new URL("file://" + exchange.getIn().getHeader("sidoraDatastreamFileURL", String.class));
        URL sidoraDatastreamFileURL = new URL("file://" + batchRequestMap.get("sidoraDatastreamFileURL"));
        sidoraDatastreamFile = new File(processingDir + correlationId + "/sidora.xml");
        LOG.debug("Saving URL for Sidora Datastream to File:{}", sidoraDatastreamFile);

        try {
            FileUtils.copyURLToFile(sidoraDatastreamFileURL, sidoraDatastreamFile);
        } catch (IOException e) {
            System.err.println("Caught IOException: Unable to copy Sidora Datastream file from: " + sidoraDatastreamFileURL + " to: " + sidoraDatastreamFile + "\n" + e.getMessage());
            //e.printStackTrace();
        }

        //Get a list of the resources to process filtering out xml files
        String[] files = dataOutputDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {

                if (name.toLowerCase().contains("sidora.xml") || name.toLowerCase().contains("metadata.xml")) {
                    LOG.debug("Found {} filter out of resource file list", name);
                    return false;
                } else {
                    LOG.debug("Found {} add it to the resource file list", name);
                    return true;
                }
                //return !name.toLowerCase().endsWith(".xml");
            }
        });

        String resourceFileList = Arrays.toString(files).replace("[", "").replace("]", "").replace(", ", ",");

        LOG.debug("FileList:{}", resourceFileList);

        Map<String, Object> updateResourceCount = new HashMap<String, Object>();
        updateResourceCount.put("correlationId", correlationId);
        updateResourceCount.put("resourceCount", files.length);

        out.setBody(updateResourceCount);

        out.setHeader("correlationId", correlationId);
        out.setHeader("parentId", batchRequestMap.get("parentId").toString());
        out.setHeader("contentModel", batchRequestMap.get("contentModel").toString());
        out.setHeader("resourceList", resourceFileList);
        out.setHeader("resourceCount", resourceFileList.length());
        out.setHeader("resourceOwner", batchRequestMap.get("resourceOwner").toString());

        //Stash the metadata datastream and sidora datastream to a header
        out.setHeader("metadataXML", FileUtils.readFileToString(metadataFile.getCanonicalFile(), Charsets.UTF_8));
        out.setHeader("sidoraXML", FileUtils.readFileToString(sidoraDatastreamFile.getCanonicalFile(), Charsets.UTF_8));

        out.setHeaders(updateFileHeaders(dataOutputDir, exchange.getIn().getHeaders()));

        LOG.debug("Metadata datastream: {}\nSidora datasream: {}", FileUtils.readFileToString(metadataFile.getCanonicalFile(),Charsets.UTF_8),
                FileUtils.readFileToString(sidoraDatastreamFile.getCanonicalFile(), Charsets.UTF_8));

    }

    /**
     *
     * @param processCount
     * @return
     */
    public  Map<String, Object> updateProcessCount(@Header("correlationId") String correlationId,
                                                   @ExchangeProperty("CamelSplitIndex") Integer processCount) {

        LOG.debug("=============================== updateProcessCount ===============================", fedoraUser);

        Map<String, Object> updateProcessCount = new HashMap<String, Object>();
        updateProcessCount.put("correlationId", correlationId);
        updateProcessCount.put("processCount", ++processCount);

        return updateProcessCount;
    }

    public Map<String, Object> checkBatchRequestStatus(@Header("correlationId") String correlationId, @Header("parentId") String parentId) {

        Map<String, Object> batchRequestStatus = new HashMap<String, Object>();
        batchRequestStatus.put("correlationId", correlationId);
        batchRequestStatus.put("parentId", parentId);

        return batchRequestStatus;

    }

    private Map<String, Object> updateFileHeaders(File file, Map<String, Object> oldHeaders) {
        String parent = null;

        if (file.getParentFile() != null)
        {
            parent = file.getParentFile().getName();
            LOG.debug("CamelFileParent: {}", file.getParentFile());
        }

        Map<String, Object> headers = new HashMap<String, Object>(oldHeaders);

//      FIXME: Use Camel GenericFile to correctly populate these fields!!!
        headers.put("CamelFileLength", file.length());
        headers.put("CamelFileLastModified", file.lastModified());
        headers.put("CamelFileNameOnly", file.getName());
        headers.put("CamelFileNameConsumed", file.getName());
        headers.put("CamelFileName", file.getName());
        headers.put("CamelFileRelativePath", file.getPath());
        headers.put("CamelFilePath", file.getPath());
        headers.put("CamelFileAbsolutePath", file.getAbsolutePath());
        headers.put("CamelFileAbsolute", false);
        headers.put("CamelFileParent", parent);
        headers.put("CamelFileParentDir", file.getAbsolutePath());

        return headers;
    }


}
