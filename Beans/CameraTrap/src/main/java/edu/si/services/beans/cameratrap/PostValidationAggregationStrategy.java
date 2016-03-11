package edu.si.services.beans.cameratrap;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;

import java.util.ArrayList;

/**
 * Created by jbirkhimer on 3/10/16.
 */
public class PostValidationAggregationStrategy implements AggregationStrategy{

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        Object newBody = newExchange.getIn().getBody();
        ArrayList<Object> postValidationMessageList = null;
        if (oldExchange == null) {
            postValidationMessageList = new ArrayList<Object>();
            postValidationMessageList.add(newBody);
            newExchange.getIn().setBody(postValidationMessageList);
            return newExchange;
        } else {
            postValidationMessageList = oldExchange.getIn().getBody(ArrayList.class);
            postValidationMessageList.add(newBody);
            return oldExchange;
        }
    }
}
