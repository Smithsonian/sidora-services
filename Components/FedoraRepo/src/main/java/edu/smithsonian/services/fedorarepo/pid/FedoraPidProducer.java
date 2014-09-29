/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.smithsonian.services.fedorarepo.pid;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.response.GetNextPIDResponse;
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
class FedoraPidProducer extends DefaultProducer
{

    private static final Logger LOG = LoggerFactory.getLogger(FedoraPidProducer.class);
    private final FedoraPidEndpoint endpoint;

    public FedoraPidProducer(FedoraPidEndpoint endpoint)
    {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {
        //TODO: Catch and rethrow exception?
        GetNextPIDResponse pid = FedoraClient.getNextPID().execute();

        if (pid == null || pid.getStatus() >= 300)
        {
            //TODO: Throw exception
        }//end if
        else
        {
            Message in = exchange.getIn();
            Map<String, Object> headers = in.getHeaders();

            //TODO: Make CamelFedoraPid a constant... where?
            headers.put("CamelFedoraPid", pid.getPid());

            LOG.debug(String.format("Pid: %s Status = %d", pid.getPid(), pid.getStatus()));

            Message out = exchange.getOut();

            out.setBody(in.getBody());
            out.setHeaders(headers);

        }//end else
    }

}
