/*
 * Copyright 2015 Smithsonian Institution.
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

package edu.si.services.beans.cameratrap;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Intended to be used to aggregate camera trap validation messages from the individual validations in the
 * camel route.  Each validation message will be passed in to the exchange body as a POJO.  This aggregator
 * will append the POJO to a collection to hold them until the aggregation is complete.
 *
 * @author jbirkhimer
 * @author parkjohn
 */
public class CameraTrapValidationMessageAggregationStrategy implements AggregationStrategy{

    private final Logger log = LoggerFactory.getLogger(CameraTrapValidationMessageAggregationStrategy.class);

    /**
     * Overriding the aggregate method expected for the camel aggregation strategy interface to implement
     * @param oldExchange Previous exchange that holds the existing validation messages
     * @param newExchange Current exchange that holds the latest validation message
     * @return The exchange will contain the aggregated validation message for the possible next runs
     */
    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {


        //first time aggregation strategy being invoked so the old exchange isn't available just yet
        if (oldExchange == null) {
            Object bodyObject = newExchange.getIn().getBody();

            //add validation message body to a list and set the list to the exchange for potential additional aggregations
            if (bodyObject instanceof CameraTrapValidationMessage.MessageBean){
                CameraTrapValidationMessage.MessageBean messageBean = (CameraTrapValidationMessage.MessageBean) bodyObject;
                List<CameraTrapValidationMessage.MessageBean> messages = new ArrayList<>();

                //only add validation failed messages  to the bucket
                if (!messageBean.getValidationSuccess())
                    messages.add(messageBean);
                newExchange.getIn().setBody(messages);
            }

            return newExchange;
        }

        //get existing validation messages
        List<CameraTrapValidationMessage.MessageBean> validationMessages = oldExchange.getIn().getBody(List.class);

        if (validationMessages == null){
            log.debug("Validation Message doesn't exist yet.  Creating a new message list...");
            validationMessages = new ArrayList<>();
        }

        Object newExchangeBodyObject = newExchange.getIn().getBody();

        //add validation message bean to the existing bucket
        if (newExchangeBodyObject instanceof CameraTrapValidationMessage.MessageBean){
            CameraTrapValidationMessage.MessageBean message = (CameraTrapValidationMessage.MessageBean)newExchangeBodyObject;

            //only add validation failed messages to the bucket
            if (!((CameraTrapValidationMessage.MessageBean) newExchangeBodyObject).getValidationSuccess())
                validationMessages.add(message);
        }

        //add list of validation messages to the existing bucket.  this is used in inner aggregation logic such as in splitter EIPs
        if (newExchangeBodyObject instanceof List){
            List<CameraTrapValidationMessage.MessageBean> additionalMessages = (List)newExchangeBodyObject;
            validationMessages.addAll(additionalMessages);
        }

        oldExchange.getIn().setBody(validationMessages);

        return oldExchange;
    }
}
