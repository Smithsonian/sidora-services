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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jbirkhimer
 */
public class ResponseControllerBean {
    private String parentPID;
    private String correlationId;
    private Integer processCount;
    private boolean complete = false;

    /**
     *
     * @param statusResponse
     * @return
     * @throws Exception
     */
    public String batchRequestStatus(Exchange exchange, ArrayList<HashMap<String,Object>> statusResponse) throws Exception {

        Map<String, Object> statusResponseMap = new HashMap<>();
        for (HashMap hashMap : statusResponse) {
            statusResponseMap.putAll(hashMap);
        }

        //final ObjectMapper mapper = new ObjectMapper(); // jackson's objectmapper
//        final ObjectMapper mapper = new ObjectMapper();
//        final BatchRequest pojo = mapper.convertValue(statusResponseMap, BatchRequest.class);

        parentPID = statusResponseMap.get("parentId").toString();
        correlationId = statusResponseMap.get("correlationId").toString();
        complete = (boolean) statusResponseMap.get("complete");
        processCount = (Integer) statusResponseMap.get("processCount");

        StringBuilder responceMessage = new StringBuilder();
        responceMessage.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        responceMessage.append("<Batch>"
                + "<ParentPID>" + parentPID + "</ParentPID><correlationId>" + correlationId + "</correlationId>"
                + "<ResourcesProcessed>" + processCount + "</ResourcesProcessed>"
                + "<BatchDone>" + complete + "</BatchDone>"
                + "</Batch>");

        return responceMessage.toString();
    }

}
