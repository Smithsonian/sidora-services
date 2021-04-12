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

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** This class generated the UUID for CorrelationId's and provides helper methods for the camel route.
 * @author jbirkhimer
 */
@Configuration(value = "batchRequestControllerBean")
public class BatchRequestControllerBean {

    private static final Logger LOG = LoggerFactory.getLogger(BatchRequestControllerBean.class);

    private Message out;
    private String correlationId;

    /**
     * Generating Random UUID for CorrelationId
     * @param exchange
     * @return
     */
    public void createCorrelationId(Exchange exchange) {

        out = exchange.getIn();

        correlationId = UUID.randomUUID().toString();

        out.setHeader("correlationId", correlationId);
    }

    /**
     * Set Camel Headers For New Batch Requests
     * @param exchange
     * @throws Exception
     */
    public void processBatchRequest(Exchange exchange) throws Exception {

        out = exchange.getIn();

        Map<String, Object> batchRequestMap = (Map<String, Object>) out.getBody();

        LOG.debug("processBatchRequest - batchRequestMap MAP: {}", batchRequestMap);

        correlationId = batchRequestMap.get("correlationId").toString();

        out.setBody(correlationId);

        out.setHeader("correlationId", correlationId);
        out.setHeader("parentId", batchRequestMap.get("parentId").toString());
        out.setHeader("resourceCount", batchRequestMap.get("resourceCount").toString());
        out.setHeader("resourceOwner", batchRequestMap.get("resourceOwner").toString());

        //Stash the metadata datastream and sidora datastream to a header
        out.setHeader("ds_metadata", batchRequestMap.get("ds_metadata").toString());
        out.setHeader("ds_sidora", batchRequestMap.get("ds_sidora").toString());
        out.setHeader("association", batchRequestMap.get("association").toString());

        //Header is not null if resource is a csv for codebook
        if (batchRequestMap.get("codebookPID") != null) {
            out.setHeader("codebookPID", batchRequestMap.get("codebookPID").toString());
        }

        LOG.debug("Return Headers and Body:\nHeaders:\n{}\nBody:\n{}", out.getHeaders(), out.getBody());
    }

    /**
     * Set the Datastream Created in Resource Table Based on HTTP Response From Fedora
     * @param exchange
     * @return
     */
    public Map<String, Object> updateCreatedStatus(Exchange exchange, String datastream) {
        
        out = exchange.getIn();
        correlationId = out.getHeader("correlationId", String.class);
        String resourceFile = out.getHeader("resourceFile", String.class);
        String pid = out.getHeader("CamelFedoraPid", String.class);
        String titleField = out.getHeader("titleField", String.class);
        String contentModel = out.getHeader("contentModel", String.class);
        Integer statusCode = out.getHeader("CamelHttpResponseCode", Integer.class);

        Map<String, Object> updateCreatedStatus = new HashMap<String, Object>();
        updateCreatedStatus.put("correlationId", correlationId);
        updateCreatedStatus.put("resourceFile", resourceFile);
        updateCreatedStatus.put("pid", pid);
        updateCreatedStatus.put("titleField", titleField);
        updateCreatedStatus.put("contentModel", contentModel);

        updateCreatedStatus.put(datastream, checkStatusCode(statusCode));

        LOG.info("Updating dB Status: CorrelationId: {}, Resource File: {}, Datastream: {}, Status Code: {}", correlationId, resourceFile, datastream, statusCode);

        return updateCreatedStatus;
    }

    /**
     * Check Resource MimeType using Apache Tika
     * @param exchange
     * @throws URISyntaxException
     * @throws MalformedURLException
     */
    public void getMIMEType(Exchange exchange, String resourceFile) throws URISyntaxException, MalformedURLException {

        /**
         * TODO:
         *
         * Need to make sure that mimetypes are consistent with what's used in workbench.
         * See link for workbench mimetype list
         *
         * https://github.com/Smithsonian/sidora-workbench/blob/master/workbench/includes/utils.inc#L1119
         *
         */

        //URL url = new URL(exchange.getIn().getHeader("resourceFile", String.class));
        URL url = new URL(resourceFile);

        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());

        resourceFile = uri.toASCIIString();
        String resourceFileExt = FilenameUtils.getExtension(resourceFile);
        String mimeType = null;

        if (resourceFileExt.equalsIgnoreCase("nef")) {
            mimeType = "image/x-nikon-nef";
        } else if (resourceFileExt.equalsIgnoreCase("dng")) {
            mimeType = "image/x-adobe-dng";
        } else {
            LOG.debug("Checking {} for MIME Type", resourceFile);

            mimeType = new Tika().detect(resourceFile);
        }

        LOG.debug("Batch Process " + resourceFile + " || MIME=" + mimeType);

        exchange.getIn().setHeader("dsMIME", mimeType);
    }

    /**
     * Setting the titleLabel header
     * NOTE: xslt does not work with dynamic xpath set from a param. xslt ver. 3.0 has an evaluate() function that could work and replace this method.
     * @param exchange
     */
    public void setTitleLabel(Exchange exchange) {

        out = exchange.getIn();

        String titleLabel = XPathBuilder.xpath("/" + out.getHeader("titlePath", String.class) + "/text()", String.class)
                .evaluate(exchange.getContext(), out.getBody());

        out.setHeader("titleLabel", titleLabel);
    }

    public void setExtension(Exchange exchange) {
        out = exchange.getIn();

        String objDsLabelExtension = FilenameUtils.getExtension(out.getHeader("resourceFile", String.class));

        out.setHeader("objDsLabelExtension", objDsLabelExtension);
    }

    public void setPrimaryTitleLabel(Exchange exchange) {
        out = exchange.getIn();
        String primaryTitleLabel = FilenameUtils.getBaseName(out.getHeader("objDsLabel", String.class));
        out.setHeader("primaryTitleLabel", primaryTitleLabel);
    }

    /**
     * Check Status Response Codes
     * @param camelHttpResponseCode
     * @return
     */
    private boolean checkStatusCode(Integer camelHttpResponseCode) {
        if (camelHttpResponseCode != 200 || camelHttpResponseCode != 201) {
            return true;
        } else {
            return false;
        }
    }

}
