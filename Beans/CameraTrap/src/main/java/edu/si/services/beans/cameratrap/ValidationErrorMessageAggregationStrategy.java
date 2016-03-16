package edu.si.services.beans.cameratrap;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jbirkhimer on 3/10/16.
 */
public class ValidationErrorMessageAggregationStrategy implements AggregationStrategy{

    final Logger log = LoggerFactory.getLogger(ValidationBean.class);

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

        if (oldExchange == null) {
            return newExchange;
        }

        String oldBody = oldExchange.getIn().getBody(String.class);
        String newBody = newExchange.getIn().getBody(String.class);

        //Filter out the passed validation messages
        if (!oldBody.equals("passed") && !newBody.equals("passed")) {
            oldExchange.getIn().setBody(oldBody + ", " + newBody);
        } else if (oldBody.equals("passed") && !newBody.equals("passed")) {
            oldExchange.getIn().setBody(newBody);
        }

        return oldExchange;
    }
}
