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
import org.apache.camel.Header;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jbirkhimer
 */
public class ResponseControllerBean {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseControllerBean.class);

    private Message out;
    private String parentPID;
    private String correlationId;
    private Integer processCount;
    private boolean request_complete;
    /**
     *
     * @param exchange
     * @return
     * @throws Exception
     */
    public String batchRequestStatus(Exchange exchange, @Header("batchRequest") ArrayList<HashMap<String,Object>> requestMap, ArrayList<HashMap<String,Object>> statusResponse) throws Exception {

        //final ObjectMapper mapper = new ObjectMapper(); // jackson's objectmapper
//        final ObjectMapper mapper = new ObjectMapper();
//        final BatchRequest pojo = mapper.convertValue(statusResponseMap, BatchRequest.class);

        out = exchange.getIn();

        Map<String, Object> statusRequestMap = new HashMap<>();
        statusRequestMap.putAll(requestMap.get(0));

        LOG.debug("====================[ batchRequestMap ]=====================\n{}", Arrays.toString(statusRequestMap.entrySet().toArray()));

        correlationId = statusRequestMap.get("correlationId").toString();
        parentPID = statusRequestMap.get("parentId").toString();
        String resourceOwner = statusRequestMap.get("resourceOwner").toString();
        request_complete = (boolean) statusRequestMap.get("request_complete");
        Integer resourceCount = (Integer) statusRequestMap.get("resourceCount");
        processCount = (Integer) statusRequestMap.get("processCount");
        String contentModel = statusResponse.get(0).get("contentModel").toString();
        String codebookPID = statusRequestMap.get("codebookPID") == null ? "" : String.valueOf(statusRequestMap.get("codebookPID"));




        StringBuilder resources = new StringBuilder();
        Map<String, Object> statusResponseMap = new HashMap<>();

        for (HashMap hashMap : statusResponse) {
            statusResponseMap.putAll(hashMap);

            resources.append(
                    "        <resource>\n" +
                    "            <file>" + statusResponseMap.get("resourceFile") + "</file>\n" +
                    "            <pid>" + statusResponseMap.get("parentId") + "</pid>\n" +
                    "            <titleField>" + statusResponseMap.get("titleField") + "</titleField>\n" +
                    "            <resourceObjectCreated>" + statusResponseMap.get("resource_created") + "</resourceObjectCreated>\n" +
                    "            <dsDcCreated>" + statusResponseMap.get("ds_dc_created") + "</dsDcCreated>\n" +
                    "            <dsRelsExtCreated>" + statusResponseMap.get("ds_relsExt_created") + "</dsRelsExtCreated>\n" +
                    "            <dsDcMetadata>" + statusResponseMap.get("ds_metadata_created") + "</dsDcMedtadat>\n" +
                    "            <dsObjCreated>" + statusResponseMap.get("ds_obj_created") + "</dsObjCreated>\n" +
                    "            <dsSidoraCreated>" + statusResponseMap.get("ds_sidora_created") + "</dsSidoraCreated>\n" +
                    "            <parentChildRelationshipCreated>" + statusResponseMap.get("parent_child_resource_relationship_created") + "</parentChildRelationshipCreated>\n" +
                    "            <codebookRelationshipCreated>" + statusResponseMap.get("codebook_relationship_created") + "</codebookRelationshipCreated>\n" +
                    "            <complete>" + statusResponseMap.get("resource_complete") + "</complete>\n" +
                    "        </resource>\n"
            );

        }

        //LOG.debug("====================[ Resources ]=====================\n{}", resources.toString());

        StringBuilder responceMessage = new StringBuilder();
        responceMessage.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        responceMessage.append(
                "<Batch>\n" +
                "    <ParentPID>" + parentPID + "</ParentPID>\n" +
                "    <resourceOwner>"+ resourceOwner +"</resourceOwner>\n" +
                "    <CorrelationID>"+ correlationId +"</CorrelationID>\n" +
                "    <ResourceCount>"+ resourceCount +"</ResourceCount>\n" +
                "    <ResourcesProcessed>"+ processCount +"</ResourcesProcessed>\n" +
                "    <BatchDone>"+ request_complete +"</BatchDone>\n" +
                "    <contentModel>"+ contentModel +"</contentModel>\n" +
                "    <codebookPID>" + codebookPID + "</codebookPID>\n" +
                "    <resources>\n" +
                         resources +
                "    </resources>\n" +
                "</Batch>"
        );

        return responceMessage.toString();
    }

}
