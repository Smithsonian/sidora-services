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

package edu.si.services.sidora.rest.batch.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.PropertyInject;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * @author jbirkhimer
 */
public class ProcessFileURL implements Processor {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessFileURL.class);

    private String batchCorrelationId;

    @PropertyInject(value = "batch.staging.dir")
    private String stagingDir;

    @PropertyInject(value = "batch.data.dir")
    private String processingDir;

    private Message out;
    private File tempfile, metadataFile, sidoraDatastreamFile, dataOutputDir;

    @Override
    public void process(Exchange exchange) throws Exception {


        out = exchange.getOut();
        batchCorrelationId = exchange.getIn().getHeader("batchCorrelationId", String.class);
//        stagingDir = exchange.getIn().getHeader("stagingDir", String.class);
//        processingDir = exchange.getIn().getHeader("processingDir", String.class);

        //Set location to extract to
        dataOutputDir = new File(processingDir + batchCorrelationId);

        //Save the file from URL to temp file
        URL resourceFileURL = new URL("file://" + exchange.getIn().getHeader("resourceZipFileURL", String.class));
        tempfile = new File(stagingDir + batchCorrelationId + ".zip");
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
        URL metadataFileURL = new URL("file://" + exchange.getIn().getHeader("metadataFileURL", String.class));
        metadataFile = new File(processingDir + batchCorrelationId + "/metadata.xml");
        LOG.debug("Saving URL Metadata Datastream To File:{}", metadataFile);

        try {
            FileUtils.copyURLToFile(metadataFileURL, metadataFile);
        } catch (IOException e) {
            System.err.println("Caught IOException: Unable to copy Metadata Datastream file from: " + metadataFileURL + " to: " + metadataFile + "\n" + e.getMessage());
            //e.printStackTrace();
        }

        //Grab the sidora datastream file from URL
        URL sidoraDatastreamFileURL = new URL("file://" + exchange.getIn().getHeader("sidoraDatastreamFileURL", String.class));
        sidoraDatastreamFile = new File(processingDir + batchCorrelationId + "/sidora.xml");
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

        out.setHeaders(updateHeaders(dataOutputDir, exchange.getIn().getHeaders()));
        out.setHeader("resourceList", resourceFileList);
        out.setHeader("resourceCount", resourceFileList.length());

        //Stash the metadata datastream and sidora datastream to a header
        out.setHeader("metadataXML", FileUtils.readFileToString(metadataFile.getCanonicalFile(), Charsets.UTF_8));
        out.setHeader("sidoraXML", FileUtils.readFileToString(sidoraDatastreamFile.getCanonicalFile(), Charsets.UTF_8));

        LOG.debug("Metadata datastream: {}\nSidora datasream: {}", FileUtils.readFileToString(metadataFile.getCanonicalFile(),Charsets.UTF_8),
                FileUtils.readFileToString(sidoraDatastreamFile.getCanonicalFile(), Charsets.UTF_8));

    }

    private Map<String, Object> updateHeaders(File file, Map<String, Object> oldHeaders)
    {
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
