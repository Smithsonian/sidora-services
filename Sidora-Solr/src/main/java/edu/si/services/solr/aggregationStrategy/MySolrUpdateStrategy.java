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

package edu.si.services.solr.aggregationStrategy;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is our own solr batch aggregation strategy where we can control
 * how each splitted message should be combined. As we do not want to
 * loose any message we copy from the new to the old to preserve the
 * doc lines as long we process them
 *
 * @author jbirkhimer
 */
public class MySolrUpdateStrategy implements AggregationStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(MySolrUpdateStrategy.class);

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        // put solr doc together in old exchange by adding the doc from new exchange

        if (oldExchange == null) {
            // the first time we aggregate we only have the new exchange,
            // so we just return it
            return newExchange;
        }

        String docs = oldExchange.getIn().getBody(String.class);
        String newDoc = newExchange.getIn().getBody(String.class);

        oldExchange.getIn().setBody(docs + "\n" + newDoc);

        return oldExchange;
    }
}
