/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.smithsonian.services.fedorarepo.ingest;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.request.Ingest;
import com.yourmediashelf.fedora.client.response.IngestResponse;
import edu.smithsonian.services.fedorarepo.Headers;
import edu.smithsonian.services.fedorarepo.base.FedoraProducer;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jshingler
 */
class FedoraIngestProducer extends FedoraProducer
{

    private static final Logger LOG = LoggerFactory.getLogger(FedoraIngestProducer.class);
    private final FedoraIngestEnpoint endpoint;
    protected boolean createType;

    public FedoraIngestProducer(FedoraIngestEnpoint endpoint)
    {
        this(endpoint, false);
    }

    FedoraIngestProducer(FedoraIngestEnpoint endpoint, boolean createType)
    {
        super(endpoint);
        this.endpoint = endpoint;
        this.createType = createType;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {

        Message in = exchange.getIn();

        Map<String, Object> headers = in.getHeaders();

        Ingest ingest;

        String value = this.getParam(this.endpoint.getPid(), in.getHeader(Headers.PID, String.class));
        if (value != null)
        {
            ingest = FedoraClient.ingest(value);
        }
        else
        {
            ingest = FedoraClient.ingest();
        }//end else

//        if (!endpoint.createType)
//        {
//
//        }
        value = this.getParam(this.endpoint.getLabel(), in.getHeader(Headers.LABEL, String.class));
        if (value != null)
        {
            ingest.label(value);
        }

        value = this.getParam(this.endpoint.getOwner(), in.getHeader(Headers.OWNER, String.class));
        if (value != null)
        {
            ingest.ownerId(value);
        }

        if (this.hasParam(endpoint.getNamespace()))
        {
            ingest.namespace(endpoint.getNamespace());
        }
        if (this.hasParam(endpoint.getLog()))
        {
            ingest.logMessage(endpoint.getLog());
        }

        IngestResponse response = ingest.execute();

        headers.put(Headers.PID, response.getPid());
        headers.put(Headers.STATUS, response.getStatus());

        LOG.debug(String.format("Ingest: Pid = %s Status = %d", response.getPid(), response.getStatus()));

        Message out = exchange.getOut();
        out.setHeaders(headers);
        out.setBody(in.getBody());

    }

}
