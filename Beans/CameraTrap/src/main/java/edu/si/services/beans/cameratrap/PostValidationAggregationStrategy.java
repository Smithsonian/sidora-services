package edu.si.services.beans.cameratrap;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by jbirkhimer on 3/10/16.
 */
public class PostValidationAggregationStrategy implements AggregationStrategy{

    final Logger log = LoggerFactory.getLogger(PostValidationBean.class);

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            return newExchange;
        }

        log.debug("OLD Exchange: " + oldExchange);
        log.debug("NEW Exchange: " + newExchange);

        String oldBody = oldExchange.getIn().getBody(String.class);
        String newBody = newExchange.getIn().getBody(String.class);
        oldExchange.getIn().setBody(oldBody + ", " + newBody);

        log.debug("Newly Formed Excahnge: " + oldExchange);

        return oldExchange;
    }

    /*public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

        Object oldBody = oldExchange.getIn().getBody();
        Object newBody = newExchange.getIn().getBody();

        log.debug("Agg newBody (0): " + newBody);

        ArrayList<Object> postValidationMessageList = null;

        if (oldExchange == null) {

            postValidationMessageList = new ArrayList<Object>();

            log.debug("Agg newBody (1): " + newBody);

            if (!newBody.equals("passed")) {

                log.debug("Agg newBody (2): " + newBody);

                postValidationMessageList.add(newBody);

            }

            newExchange.getIn().setBody(postValidationMessageList);

            return newExchange;

        } else {

            postValidationMessageList = oldExchange.getIn().getBody(ArrayList.class);

            log.debug("Agg newBody (3): " + newBody);

            if (!newBody.equals("passed")) {

                postValidationMessageList.add(newBody);
            }

            if (!oldBody.equals("passed") && !newBody.equals("passed")) {
                oldExchange.getIn().setBody(oldBody + ", " + newBody);
            } else if (!newBody.equals("passed")) {
                oldExchange.getIn().setBody(newBody);
            }

            return oldExchange;
        }

        *//*Object newHeader = newExchange.getIn().getHeader("validationStatusMessage");
        ArrayList<Object> postValidationMessageList = null;

        if (oldExchange == null) {

            postValidationMessageList = new ArrayList<Object>();
            postValidationMessageList.add(newHeader);
            newExchange.getIn().setHeader("validationStatusMessage", postValidationMessageList);

            return newExchange;

        } else {

            postValidationMessageList = oldExchange.getIn().getHeader("validationStatusMessage", ArrayList.class);
            postValidationMessageList.add(newHeader);

            return oldExchange;
        }*//*
    }*/
}
