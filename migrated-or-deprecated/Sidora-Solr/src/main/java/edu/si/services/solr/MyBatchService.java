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

package edu.si.services.solr;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.PropertyInject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author jbirkhimer
 */
public class MyBatchService {

    private static final Logger LOG = LoggerFactory.getLogger(MyBatchService.class);

    @PropertyInject(value = "sidora.solr.default.index", defaultValue = "gsearch_solr")
    private static String DEFAULT_SOLR_INDEX;

    MySolrJob solrJob;

    public void addJob(Exchange exchange, String solrOperation) {
        addJob(exchange, solrOperation, DEFAULT_SOLR_INDEX);
    }

    public void addJob(Exchange exchange, String solrOperation, String index) {
        Message out = exchange.getIn();

        solrJob = new MySolrJob();
        solrJob.setPid(out.getHeader("pid", String.class));
        solrJob.setOrigin(out.getHeader("origin", String.class));
        solrJob.setMethodName(out.getHeader("methodName", String.class));
        solrJob.setDsLabel(out.getHeader("dsLabel", String.class));
        solrJob.setState(out.getHeader("state", String.class));
        solrJob.setSolrOperation(solrOperation);
        solrJob.setIndex(index);
        solrJob.indexes.add(index);
        solrJob.setFoxml(out.getBody(String.class));

        LOG.debug("******[1]*******\nNEW solrJob = {}", solrJob);

        out.setHeader("solrJob", solrJob);
    }

    public void addIndex(Exchange exchange, String index) {
        Message out = exchange.getIn();
        String pid = out.getHeader("pid", String.class);

        //solrJob = getJobByPid(pid);

        solrJob.indexes.add(index);
        LOG.info("Added Index:\nsolrJob = {}", solrJob);
    }

    /*public MySolrJob getJobByPid(String pid) {

//        List<MySolrJob> receivedJob = new ArrayList<>();
//        receivedJob.add(new MySolrJob("test:1", "origin", "ctIngest", "testLabel", "A", "update", "gsearch_solr"));
//        receivedJob.add(new MySolrJob("test:2", "origin", "ctIngest", "testLabel", "A", "update", "gsearch_solr"));
//        receivedJob.add(new MySolrJob("test:3", "origin", "ctIngest", "testLabel", "A", "update", "gsearch_solr"));

        Comparator<MySolrJob> c = new Comparator<MySolrJob>() {
            @Override
            public int compare(MySolrJob o1, MySolrJob o2) {
                return o1.getPid().compareTo(o2.getPid());
            }
        };

        MySolrJob msj = new MySolrJob();
        msj.setPid(pid);

        int i = Collections.binarySearch(receivedJob, msj, c);
        LOG.info("The solrJob found is : " + receivedJob.get(i).toString());

        return receivedJob.get(i);
    }*/

    /*public void receivedJob(Exchange exchange) {
        Message out = exchange.getIn();
        String pid = out.getHeader("pid", String.class);

        HashMap<String,ArrayList<String>> receivedJob = new HashMap<>();

        ArrayList<String> job = new ArrayList<>();
        job.add(pid);
        job.add(out.getHeader("origin", String.class));
        job.add(out.getHeader("methodName", String.class));
        job.add(out.getHeader("dsLabel", String.class));
        job.add(out.getHeader("state", String.class));

        receivedJob.put(pid, job);
        LOG.debug("Created Received Job: {}", receivedJob);
        out.setHeader("receivedJob", receivedJob);
        out.setHeader("addReceived", "received");
    }*/

    public void setOperationAndIndex(Exchange exchange, String operation, String index) {
        Message out = exchange.getIn();
        String pid = out.getHeader("pid", String.class);

        solrJob = out.getHeader("solrJob", MySolrJob.class);

        solrJob.setMethodName(operation);
        solrJob.indexes.add(index);
        LOG.info("Added Index and MethodName:\nsolrJob = {}", solrJob);
        /*if (solrJob.size() <= 5) {
            solrJob.add(operation);
            solrJob.add(index);
        } else {
            solrJob.set(5, operation);
            solrJob.set(6, index);
        }*/

        //receivedJob.replace(pid, solrJob);

        //out.setHeader("receivedJob", receivedJob);
    }


}
