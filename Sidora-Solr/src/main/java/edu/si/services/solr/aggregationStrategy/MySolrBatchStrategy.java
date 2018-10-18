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

import edu.si.services.solr.MySolrJob;
import edu.si.services.solr.SidoraSolrException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jbirkhimer
 */
public class MySolrBatchStrategy implements AggregationStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(MySolrBatchStrategy.class);

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        Message out = newExchange.getIn();
        List<MySolrJob> batchJobs = null;
        MySolrJob solrJob = out.getHeader("solrJob", MySolrJob.class);

        if (oldExchange == null) {
            batchJobs = new ArrayList<>();
            batchJobs.add(solrJob);

            newExchange.getIn().setHeader("batchJobs", batchJobs);
            newExchange.getIn().setHeader("solrIndex", solrJob.getIndex());

            return newExchange;
        } else {
            String expectedIndex = oldExchange.getIn().getHeader("solrIndex", String.class);
            if (solrJob.getIndex().equalsIgnoreCase(expectedIndex)) {
                batchJobs = oldExchange.getIn().getHeader("batchJobs", List.class);
                batchJobs.add(out.getHeader("solrJob", MySolrJob.class));
                return oldExchange;
            } else {
                /*
                TODO: create unit test for this what happens to the already aggregated jobs?
                They should still be processed and the error job should be sent to deadLetter
                 */
                oldExchange.setException(new SidoraSolrException("Batch Solr Aggregation Failed!!! Found a different solr index than expected. expected: " + expectedIndex + ", found: " + solrJob.getIndex()));
                return oldExchange;
            }
        }
    }

/*
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        Message out = newExchange.getIn();

        ArrayList<Object> pidList, originList, methodNameList, dsLabelList = null;

        if (oldExchange == null) {
            pidList = new ArrayList<Object>();
            pidList.add(out.getBody());

            originList = new ArrayList<Object>();
            originList.add(out.getHeader("origin"));

            methodNameList = new ArrayList<Object>();
            methodNameList.add(out.getHeader("methodName"));

            dsLabelList = new ArrayList<Object>();
            dsLabelList.add(out.getHeader("dsLabel"));

            newExchange.getIn().setBody(pidList);
            newExchange.getIn().setHeader("originList", originList);
            newExchange.getIn().setHeader("methodNameList", methodNameList);
            newExchange.getIn().setHeader("dsLabelList", dsLabelList);

            return newExchange;
        } else {
            pidList = oldExchange.getIn().getBody(ArrayList.class);
            pidList.add(out.getBody());

            originList = oldExchange.getIn().getHeader("originList", ArrayList.class);
            originList.add(out.getHeader("origin"));

            methodNameList = oldExchange.getIn().getHeader("methodNameList", ArrayList.class);
            methodNameList.add(out.getHeader("methodName"));

            dsLabelList = oldExchange.getIn().getHeader("dsLabelList", ArrayList.class);
            dsLabelList.add(out.getHeader("dsLabel"));
            return oldExchange;
        }
    }
*/

}
