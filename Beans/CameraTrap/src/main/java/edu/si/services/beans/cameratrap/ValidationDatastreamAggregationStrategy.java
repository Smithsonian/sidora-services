package edu.si.services.beans.cameratrap;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jbirkhimer on 3/15/16.
 */
public class ValidationDatastreamAggregationStrategy implements AggregationStrategy {

    final Logger log = LoggerFactory.getLogger(PostValidationBean.class);

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

        if (oldExchange == null) {
            return newExchange;
        }

        String oldBody = oldExchange.getIn().getBody(String.class);
        String newBody = newExchange.getIn().getBody(String.class);

        log.debug("Agg (0)\noldBody: " + oldBody + "\nnewBody: " + newBody);


        if (!oldBody.equals("passed") && !newBody.equals("passed")) {

            log.debug("Agg (1) oldBody = " + !oldBody.equals("passed") + ", newBody = " + !newBody.equals("passed"));
            oldExchange.getIn().setBody(oldBody + ", " + newBody);

        } else if (oldBody.equals("passed") && !newBody.equals("passed")) {

            log.debug("Agg (3) oldBody = " + oldBody.equals("passed") + ", newBody = " + !newBody.equals("passed"));
            oldExchange.getIn().setBody(newBody);

        }

        return oldExchange;
    }
}
