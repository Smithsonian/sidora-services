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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
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
    private String correlationID;
    private String stagingDir;
    private String processingDir;
    //private String targetLoc;
    private Message out;
    private File tempfile, metadataFile, dataOutputDir;

    @Override
    public void process(Exchange exchange) throws Exception {
        out = exchange.getOut();
        correlationID = exchange.getIn().getHeader("correlationID", String.class);
        stagingDir = "target/staging/";
        processingDir = exchange.getIn().getHeader("processingDir", String.class);
        //targetLoc = processingDir + correlationID;

        //Set location to extract to
        dataOutputDir = new File(processingDir + correlationID);

        //Save the file from URL to temp file
        URL resourceFileURL = new URL("file://" + exchange.getIn().getHeader("resourceZipFileURL", String.class));
        //tempfile = FileUtil.createTempFile(correlationID, ".zip", new File(stagingDir));
        tempfile = new File(stagingDir + correlationID + ".zip");
        LOG.info("Saving URL Resource To Temp File:{}", tempfile);
        FileUtils.copyURLToFile(resourceFileURL, tempfile);
        //Extract the zip archive
        LOG.info("Headers Before Extractor: {}", exchange.getIn().getHeaders());
        exchange.getContext().createProducerTemplate().sendBody("extractor:extract?location=" + processingDir, tempfile);
        LOG.info("Headers After Extractor: {}", exchange.getIn().getHeaders());

        //Now grab the metadata file from URL
        URL metadataFileURL = new URL("file://" + exchange.getIn().getHeader("metadataFileURL", String.class));
        metadataFile = new File(processingDir + correlationID + "/metadata.xml");
        LOG.info("Saving URL Resource To Temp File:{}", tempfile);
        FileUtils.copyURLToFile(metadataFileURL, metadataFile);

        String[] files = dataOutputDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !name.toLowerCase().endsWith(".xml");
            }
        });

        String resourceFileList = Arrays.toString(files).replace("[", "").replace("]", "").replace(", ", ",");

        LOG.info("FileList:{}", resourceFileList);

        //out.setHeaders(updateHeaders(dataOutputDir, exchange.getIn().getHeaders()));
        out.setHeader("resourceList", resourceFileList);

    }

    private Map<String, Object> updateHeaders(File file, Map<String, Object> oldHeaders)
    {
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
        headers.put("CamelFileParent", file.getParentFile().getName());

        return headers;
    }
}
