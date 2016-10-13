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

package edu.si.services.sidora.rest.batch.model;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;

import java.util.Random;
import java.util.UUID;

/**
 * @author jbirkhimer
 */
public class StatusResponseControllerBean {
    private String parentPID;
    private String correlationID;
    private Integer resourcesProcessed;
    private boolean finished = false;
    private Integer max = 10;

    @Handler
    public String newBatchResourceStatus(Exchange exchange) throws Exception {
        parentPID = exchange.getIn().getHeader("parentId", String.class);
        correlationID = exchange.getIn().getHeader("correlationID", String.class);
        finished = false;
        Random r = new Random();
        resourcesProcessed = r.nextInt((max - 0) + 1) + 0;

        if (resourcesProcessed == max) {
            finished = true;
        }

        StringBuilder responceMessage = new StringBuilder();
        //responceMessage.append("******* New Batch Resource for ParentPID = " + parentPID + " ********\n");
        //responceMessage.append("******* Correlation ID = " + correlationID + " ********");
        responceMessage.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        responceMessage.append("<Batch>"
                + "<ParentPID>" + parentPID + "</ParentPID><CorrelationID>" + correlationID + "</CorrelationID>"
                + "<ResourcesProcessed>" + resourcesProcessed + "</ResourcesProcessed>"
                + "<BatchDone>" + finished + "</BatchDone>"
                + "</Batch>");
        return responceMessage.toString();
    }

}
