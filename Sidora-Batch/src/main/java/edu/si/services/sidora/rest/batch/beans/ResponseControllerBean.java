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

import edu.si.services.sidora.rest.batch.model.response.BatchRequestResponse;
import edu.si.services.sidora.rest.batch.model.status.BatchStatus;
import edu.si.services.sidora.rest.batch.model.status.ResourceStatus;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** This class is used to create the POJO that JAXB will use to return XML as a response
 * @author jbirkhimer
 */
@Configuration(value = "responseControllerBean")
public class ResponseControllerBean {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseControllerBean.class);

    private Message out;

    /**
     * Return The Detailed Status for a Batch Request as a POJO for JAXB/JSON Databinding
     * @param exchange
     * @param requestMap
     * @param statusResponse
     * @return
     */
    public BatchStatus batchStatus(Exchange exchange,
                                   @Header("batchRequest") List<LinkedCaseInsensitiveMap<String>> requestMap,
                                   @Header("statusResponse") List<LinkedCaseInsensitiveMap<String>> statusResponse) {

        out = exchange.getIn();

        Map<String, Object> statusRequestMap = new HashMap<>();
        statusRequestMap.putAll(requestMap.get(0));

        BatchStatus batchStatus = new BatchStatus();

        batchStatus.setParentPID(statusRequestMap.get("parentId").toString());
        batchStatus.setResourceOwner(statusRequestMap.get("resourceOwner").toString());
        batchStatus.setCorrelationId(statusRequestMap.get("correlationId").toString());
        batchStatus.setResourceCount((Integer) statusRequestMap.get("resourceCount"));
        batchStatus.setResourcesProcessed((Integer) statusRequestMap.get("processCount"));
        batchStatus.setBatchDone((boolean) statusRequestMap.get("request_complete"));
        batchStatus.setContentModel(statusResponse.get(0).get("contentModel").toString());
        String codebookPID = statusRequestMap.get("codebookPID") == null ? "" : String.valueOf(statusRequestMap.get("codebookPID"));
        batchStatus.setCodebookPID(codebookPID);

        ArrayList<ResourceStatus> resourceStatusArrayList = new ArrayList<>();

        for (LinkedCaseInsensitiveMap hashMap : statusResponse) {
            ResourceStatus resourceStatus = new ResourceStatus();
            resourceStatus.setFile(String.valueOf(hashMap.get("resourceFile")));
            //resourceStatus.setPid(String.valueOf(hashMap.get("pid")) == null ? "" : String.valueOf(hashMap.get("pid")));
            resourceStatus.setPid(String.valueOf(hashMap.get("pid")));
            //resourceStatus.setTitle(String.valueOf(hashMap.get("titleLabel")) == null ? "" : String.valueOf(hashMap.get("titleLabel")));
            resourceStatus.setTitle(String.valueOf(hashMap.get("titleLabel")));
            resourceStatus.setResourceObjectCreated((Boolean) hashMap.get("resource_created"));
            resourceStatus.setDsDcCreated((Boolean) hashMap.get("ds_dc_created"));
            resourceStatus.setDsRelsExtCreated((Boolean) hashMap.get("ds_relsExt_created"));
            resourceStatus.setDsMetadata((Boolean) hashMap.get("ds_metadata_created"));
            resourceStatus.setDsObjCreated((Boolean) hashMap.get("ds_obj_created"));
            resourceStatus.setDsTnCreated((Boolean) hashMap.get("ds_tn_created"));
            resourceStatus.setDsSidoraCreated((Boolean) hashMap.get("ds_sidora_created"));
            resourceStatus.setParentChildRelationshipCreated((Boolean) hashMap.get("parent_child_resource_relationship_created"));
            resourceStatus.setCodebookRelationshipCreated((Boolean) hashMap.get("codebook_relationship_created"));
            resourceStatus.setComplete((Boolean) hashMap.get("resource_complete"));

            resourceStatusArrayList.add(resourceStatus);
        }

        batchStatus.setResources(resourceStatusArrayList);

        return batchStatus;
    }


    /**
     * Response for a New Batch Request as a POJO for JAXB/JSON Databinding
     * @param exchange
     * @return
     */
    public BatchRequestResponse batchRequestResponse(Exchange exchange) {

        out = exchange.getIn();

        BatchRequestResponse batchRequestResponse = new BatchRequestResponse();

        batchRequestResponse.setParentPID(String.valueOf(out.getHeader("parentId")));
        batchRequestResponse.setCorrelationId(String.valueOf(out.getHeader("correlationId")));

        return batchRequestResponse;

    }
}
