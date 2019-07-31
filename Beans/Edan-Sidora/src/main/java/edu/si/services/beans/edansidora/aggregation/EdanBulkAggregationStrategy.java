package edu.si.services.beans.edansidora.aggregation;

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