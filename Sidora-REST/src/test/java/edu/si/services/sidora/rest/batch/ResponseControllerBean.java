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

package edu.si.services.sidora.rest.batch;

import org.apache.camel.Exchange;
import org.apache.cxf.helpers.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.UUID;

/**
 * @author jbirkhimer
 */
public class ResponseControllerBean {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseControllerBean.class);

    private String parentPID;
    private URL resourceZipFileURL;
    private URL metadataFileURL;
    private String contentModel;
    private String titleField;
    private String association;
    private String batchCorrelationId;



    //@Handler
    public String newBatchResource(Exchange exchange) throws Exception {
        parentPID = exchange.getIn().getHeader("parentId", String.class);
        contentModel = exchange.getIn().getHeader("contentModel", String.class);


        resourceZipFileURL =new URL("file://" + exchange.getIn().getHeader("resourceZip", String.class));
        metadataFileURL = new URL("file://" + exchange.getIn().getHeader("metadata", String.class));

        batchCorrelationId = UUID.randomUUID().toString();
        //batchCorrelationId = exchange.getIn().getHeader()


        final File zipDestination = new File("target/BatchProcessData/" + batchCorrelationId + "/resources.zip");
        final File metadataDestination = new File("target/BatchProcessData/" + batchCorrelationId + "/metadata.xml");

        try ( InputStream resourceZipFile = resourceZipFileURL.openStream();
              InputStream metadataFile = metadataFileURL.openStream();
              final FileOutputStream zipOutput = openOutputStream(zipDestination, false);
              final FileOutputStream metadataOutput = openOutputStream(metadataDestination, false)) {

            IOUtils.copy(resourceZipFile, zipOutput);
            IOUtils.copy(metadataFile, metadataOutput);
        }

        String parent = null;

        if (zipDestination.getParentFile() != null)
        {
            parent = zipDestination.getParentFile().getName();
            LOG.debug("CamelFileParent: {}", zipDestination.getParentFile());
        }

        exchange.getIn().setHeader("CamelFileAbsolutePath", zipDestination.getAbsolutePath());
        exchange.getIn().setHeader("CamelFileParent", parent);
        exchange.getIn().setHeader("batchCorrelationId", batchCorrelationId);

        StringBuilder responceMessage = new StringBuilder();
        responceMessage.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        responceMessage.append("<Batch><ParentPID>" + parentPID + "</ParentPID><CorrelationId>" + batchCorrelationId +"</CorrelationId></Batch>");



        return responceMessage.toString();
    }

    public static FileOutputStream openOutputStream(final File file, final boolean append) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canWrite() == false) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            final File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file, append);
    }


}
