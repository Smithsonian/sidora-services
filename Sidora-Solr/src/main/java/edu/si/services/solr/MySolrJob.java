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

import org.apache.camel.PropertyInject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * @author jbirkhimer
 */
public class MySolrJob {

    private static final Logger LOG = LoggerFactory.getLogger(MySolrJob.class);

    String pid;
    String origin;
    String methodName;
    String dsLabel;
    String state;
    String solrOperation;
    String index;
    ArrayList<String> indexes;

    @PropertyInject(value = "sidora.solr.default.index", defaultValue = "gsearch_solr")
    private static String DEFAULT_SOLR_INDEX;


    public MySolrJob() {
        DEFAULT_SOLR_INDEX = (DEFAULT_SOLR_INDEX == null) ? "gsearch_solr" : DEFAULT_SOLR_INDEX;
        this.indexes = new ArrayList<>();
    }

    public MySolrJob(String pid, String origin, String methodName, String dsLabel) {
        this(pid, origin, methodName, dsLabel, null);
    }

    public MySolrJob(String pid, String origin, String methodName, String dsLabel, String state) {
        this(pid, origin, methodName, dsLabel, state, null);
    }

    public MySolrJob(String pid, String origin, String methodName, String dsLabel, String state, String solrOperation) {
        this(pid, origin, methodName, dsLabel, state, solrOperation, DEFAULT_SOLR_INDEX);
    }

    public MySolrJob(String pid, String origin, String methodName, String dsLabel, String state, String solrOperation, String index) {
        this.pid = pid;
        this.origin = origin;
        this.methodName = methodName;
        this.dsLabel = dsLabel;
        this.state = state;
        this.solrOperation = solrOperation;
        this.index = index;
        this.indexes.add(DEFAULT_SOLR_INDEX);

        LOG.info("MySolrJob :: DEFAULT_SOLR_INDEX = {} | {}", DEFAULT_SOLR_INDEX, this.toString());
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getDsLabel() {
        return dsLabel;
    }

    public void setDsLabel(String dsLabel) {
        this.dsLabel = dsLabel;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSolrOperation() {
        return solrOperation;
    }

    public void setSolrOperation(String solrOperation) {
        this.solrOperation = solrOperation;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public ArrayList<String> getIndexes() {
        return indexes;
    }

    private void setIndexes(ArrayList<String> indexes) {
        this.indexes = indexes;
    }

    @Override
    public String toString() {
        return "MySolrJob{" +
                "pid='" + pid + '\'' +
                ", origin='" + origin + '\'' +
                ", methodName='" + methodName + '\'' +
                ", dsLabel='" + dsLabel + '\'' +
                ", state='" + state + '\'' +
                ", solrOperation='" + solrOperation + '\'' +
                ", index='" + index + '\'' +
                ", indexes=" + indexes +
                '}';
    }
}
