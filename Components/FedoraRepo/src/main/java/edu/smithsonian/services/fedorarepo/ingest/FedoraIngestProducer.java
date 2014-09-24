/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.smithsonian.services.fedorarepo.ingest;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.request.Ingest;
import com.yourmediashelf.fedora.client.response.IngestResponse;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jshingler
 */
class FedoraIngestProducer extends DefaultProducer
{

    private static final Logger LOG = LoggerFactory.getLogger(FedoraIngestProducer.class);
    private final FedoraIngestEnpoint endpoint;

    public FedoraIngestProducer(FedoraIngestEnpoint endpoint)
    {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {

        Message in = exchange.getIn();
        String pid = this.endpoint.getPid();

        Ingest ingest = null;

        //??? Add the ability to get the PID from header also???
        if (pid == null || pid.isEmpty())
        {
            ingest = FedoraClient.ingest();
        }
        else
        {
            ingest = FedoraClient.ingest(pid);
        }//end else

        IngestResponse response = ingest.execute();

        Map<String, Object> headers = in.getHeaders();
        headers.put("CamelFedoraPid", response.getPid());
        headers.put("CamelFedoraStatus", response.getStatus());

        Message out = exchange.getOut();
        out.setHeaders(headers);
        out.setBody(in.getBody());

    }

}
