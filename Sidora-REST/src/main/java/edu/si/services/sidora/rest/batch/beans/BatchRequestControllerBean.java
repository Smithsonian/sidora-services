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
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
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
     * @param exchange
     * @return
     */
    public Map<String, Object> db_insertBatchRequest(Exchange exchange) {

        out = exchange.getIn();

        Map<String, Object> headers = out.getHeaders();

        correlationId = UUID.randomUUID().toString();

        out.setHeader("correlationId", correlationId);

        LOG.info("New Batch Process request from {} with ParentId={}, CorrelationId={}", headers.get("operationName"), headers.get("parentId"), correlationId);

        Map<String, Object> newBatchRequest = new HashMap<String, Object>();
        newBatchRequest.put("correlationId", correlationId);
        newBatchRequest.put("resourceOwner", headers.get("resourceOwner"));
        newBatchRequest.put("parentId", headers.get("parentId"));
        newBatchRequest.put("resourceFileList", headers.get("resourceFileList"));
        //newBatchRequest.put("resourceXML", headers.get("resourceXML"));

        newBatchRequest.put("ds_metadata", headers.get("ds_metadata"));
        newBatchRequest.put("ds_sidora", headers.get("ds_sidora"));
        newBatchRequest.put("association", headers.get("association"));

        //newBatchRequest.put("contentModel", headers.get("contentModel"));
        //newBatchRequest.put("titleField", headers.get("titleField"));
        newBatchRequest.put("codebookPID", headers.get("codebookPID"));
        newBatchRequest.put("resourceCount", headers.get("resourceCount"));

        LOG.debug("New Batch Process Request MAP: {}", newBatchRequest);

        return newBatchRequest;
    }

    /**
     *
     * @param exchange
     * @return
     */
    public Map<String, Object> db_insertResource(Exchange exchange) throws URISyntaxException, MalformedURLException {

        out = exchange.getIn();
        Map<String, Object> headers = out.getHeaders();

        URL url = new URL(out.getBody(String.class));

        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());

        String resourceFile = uri.toASCIIString();

        //String resourceFile = out.getBody(String.class);

        Map<String, Object> newBatchResource = new HashMap<String, Object>();
        newBatchResource.put("correlationId", headers.get("correlationId"));
        newBatchResource.put("resourceFile", resourceFile);
        //newBatchResource.put("parentId", headers.get("parentId"));
        //newBatchResource.put("contentModel", headers.get("contentModel"));
        //newBatchResource.put("resourceOwner", headers.get("resourceOwner"));

        LOG.debug("New Batch Resource MAP: {} || resourceFile: {}", newBatchResource, resourceFile);

        return newBatchResource;

    }

    /**
     *
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
        //out.setHeader("contentModel", batchRequestMap.get("contentModel").toString());
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
    }

    /**
     *
     * @param processCount
     * @return
     */
    public  Map<String, Object> updateProcessCount(@Header("correlationId") String correlationId,
                                                   @ExchangeProperty("CamelSplitIndex") Integer processCount) {

        Map<String, Object> updateProcessCount = new HashMap<String, Object>();
        updateProcessCount.put("correlationId", correlationId);
        updateProcessCount.put("processCount", ++processCount);

        return updateProcessCount;
    }

    /**
     *
     * @param exchange
     * @return
     */
    public  Map<String, Object> updateResourceCreated(Exchange exchange) {
        
        out = exchange.getIn();
        correlationId = out.getHeader("correlationId", String.class);
        String resourceFile = out.getHeader("ds_resourceFile", String.class);
        String pid = out.getHeader("CamelFedoraPid", String.class);
        String titleField = out.getHeader("titleField", String.class);

        Map<String, Object> updateResourceCreated = new HashMap<String, Object>();
        updateResourceCreated.put("correlationId", correlationId);
        updateResourceCreated.put("resourceFile", resourceFile);
        updateResourceCreated.put("pid", pid);
        updateResourceCreated.put("titleField", titleField);

        updateResourceCreated.put("resource_created", checkStatusCode(out.getHeader("CamelHttpResponceCode", Integer.class)));

        return updateResourceCreated;
    }

    public Map<String, Object> updateDsDcCreated(Exchange exchange) {

        out = exchange.getIn();
        correlationId = out.getHeader("correlationId", String.class);
        String resourceFile = out.getHeader("ds_resourceFile", String.class);

        Map<String, Object> updateDsDcCreated= new HashMap<String, Object>();
        updateDsDcCreated.put("correlationId", correlationId);
        updateDsDcCreated.put("resourceFile", resourceFile);

        updateDsDcCreated.put("ds_dc_created", checkStatusCode(out.getHeader("CamelHttpResponceCode", Integer.class)));

        return updateDsDcCreated;
    }

    /**
     *
     * @param exchange
     * @return
     */
    public  Map<String, Object> updateRelsExtCreated(Exchange exchange) {

        out = exchange.getIn();
        correlationId = out.getHeader("correlationId", String.class);
        String resourceFile = out.getHeader("ds_resourceFile", String.class);
        String pid = out.getHeader("CamelFedoraPid", String.class);

        Map<String, Object> updateResourceCreated = new HashMap<String, Object>();
        updateResourceCreated.put("correlationId", correlationId);
        updateResourceCreated.put("resourceFile", resourceFile);
        updateResourceCreated.put("pid", pid);
        updateResourceCreated.put("resource_created", true);

        return updateResourceCreated;
    }

    public Map<String, Object> checkBatchRequestStatus(@Header("correlationId") String correlationId,
                                                       @Header("parentId") String parentId) {

        Map<String, Object> batchRequestStatus = new HashMap<String, Object>();
        batchRequestStatus.put("correlationId", correlationId);
        batchRequestStatus.put("parentId", parentId);

        return batchRequestStatus;

    }

    public void getMIMEType(Exchange exchange) throws URISyntaxException, MalformedURLException {

        /**
         * TODO:
         *
         * Need to make sure that mimetypes are consistent with what's used in workbench.
         * See link for workbench mimetype list
         *
         * https://github.com/Smithsonian/sidora-workbench/blob/master/workbench/includes/utils.inc#L1119
         *
         */

        out = exchange.getIn();

        URL url = new URL(out.getHeader("ds_resourceFile", String.class));

        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());

        String resourceFile = uri.toASCIIString();

        LOG.debug("Checking {} for MIME Type", resourceFile);

        String mimeType = null;

        mimeType = new Tika().detect(resourceFile);

        LOG.debug("Batch Process " + resourceFile + " || MIME=" + mimeType);

        out.setHeader("dsMIME", mimeType);
    }

    private boolean checkStatusCode(Integer camelHttpResponceCode) {
        if (camelHttpResponceCode != 200 || camelHttpResponceCode != 201) {
            return true;
        } else {
            return false;
        }
    }

}
