package edu.si.services.solr;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author jbirkhimer
 */
public class ReindexInfo {

    String pidCount;
    String limit;
    String totalBatch;
    String batchSize;
    String page;
    String sianctIndex;
    String solrIndex;
    long startTime;
    long endTime;
    private String elapsed;

    public ReindexInfo() {
        //this.startTime = new Date().getTime();
    }

    public ReindexInfo(String pidCount, String limit, String totalBatch, String page) {
        this.pidCount = pidCount;
        this.limit = limit;
        this.totalBatch = totalBatch;
        this.page = page;
    }

    public String getPidCount() {
        return pidCount;
    }

    public void setPidCount(String pidCount) {
        this.pidCount = pidCount;
    }

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public String getTotalBatch() {
        return totalBatch;
    }

    public void setTotalBatch(String totalBatch) {
        this.totalBatch = totalBatch;
    }

    public String getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(String batchSize) {
        this.batchSize = batchSize;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getSianctIndex() {
        return sianctIndex;
    }

    public void setSianctIndex(String sianctIndex) {
        this.sianctIndex = sianctIndex;
    }

    public String getSolrIndex() {
        return solrIndex;
    }

    public void setSolrIndex(String solrIndex) {
        this.solrIndex = solrIndex;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
        this.elapsed = getElapsed();
    }

    public String getElapsed() {
        return String.format("%tT", (new Date().getTime() - startTime) - TimeZone.getDefault().getRawOffset());
    }

    public void setElapsed(String elapsed) {
        this.elapsed = elapsed;
    }

    @Override
    public String toString() {
        return "ReindexInfo:" +
                "\n reindex sianct = " + sianctIndex +
                "\n reindex solr = " + solrIndex +
                "\n pidCount = " + pidCount +
                "\n batch size = " + batchSize +
                "\n startTime = " + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date(startTime)) +
                "\n page limit = " + limit +
                "\n total pages = " + totalBatch +
                "\n current status = page " + page + " of " + totalBatch +
                "\n elapsed = " + getElapsed();
    }
}