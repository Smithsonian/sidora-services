/*
 * Copyright 2018-2019 Smithsonian Institution.
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

package edu.si.services.sidora.edansidora.aggregation;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.PropertyInject;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class EdanBulkAggregationStrategy implements AggregationStrategy {

    private static final Logger log = LoggerFactory.getLogger(EdanIdsAggregationStrategy.class);

    @PropertyInject(value = "edu.si.edanIds")
    static private String LOG_NAME;
    Marker logMarker = MarkerFactory.getMarker("edu.si.edanIds");

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        Message out = newExchange.getIn();

        String edanContent = out.getBody(String.class);
        JSONObject edanRecord = new JSONObject(edanContent);

        JSONArray edanBulkRequests = null;

        if (oldExchange == null) {
            edanBulkRequests = new JSONArray();
            edanBulkRequests.put(edanRecord);

            newExchange.getIn().setHeader("edanBulkRequests", edanBulkRequests);

            return newExchange;
        } else {
            edanBulkRequests = oldExchange.getIn().getHeader("edanBulkRequests", JSONArray.class);
            edanBulkRequests.put(edanRecord);
            return oldExchange;
        }
    }
}