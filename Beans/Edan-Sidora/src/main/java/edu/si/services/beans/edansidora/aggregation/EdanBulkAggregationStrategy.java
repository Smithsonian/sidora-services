package edu.si.services.beans.edansidora.aggregation;

import edu.si.services.beans.edansidora.model.IdsAsset;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class EdanBulkAggregationStrategy implements AggregationStrategy
{

    private static final Logger log = LoggerFactory.getLogger(EdanIdsAggregationStrategy.class);

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        Message out = newExchange.getIn();
        JSONArray edanBulkRequests = null;

        String jsonBody = out.getBody(String.class);
        JSONObject newRequest = new JSONObject(jsonBody);

        if (oldExchange == null) {
            edanBulkRequests = new JSONArray();
            edanBulkRequests.put(newRequest);

            newExchange.getIn().setHeader("edanBulkRequests", edanBulkRequests.toString());

            return newExchange;
        }
        else
        {
            String rawList = oldExchange.getIn().getHeader("edanBulkRequests", String.class);
            edanBulkRequests = new JSONArray(rawList);
            edanBulkRequests.put(newRequest);

            return oldExchange;
        }
    }
}