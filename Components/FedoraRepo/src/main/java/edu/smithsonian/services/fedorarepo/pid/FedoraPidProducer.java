/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.smithsonian.services.fedorarepo.pid;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.request.GetNextPID;
import com.yourmediashelf.fedora.client.response.GetNextPIDResponse;
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
class FedoraPidProducer extends FedoraProducer
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
        GetNextPID request = FedoraClient.getNextPID();

        if (this.hasParam(this.endpoint.getNamespace()))
        {
            request.namespace(this.endpoint.getNamespace());
        }//end if

        GetNextPIDResponse pid = request.execute();

        Message in = exchange.getIn();
        Map<String, Object> headers = in.getHeaders();

        headers.put(Headers.PID, pid.getPid());
        headers.put(Headers.STATUS, pid.getStatus());

        LOG.debug(String.format("Pid: %s Status = %d", pid.getPid(), pid.getStatus()));

        Message out = exchange.getOut();

        out.setBody(in.getBody());
        out.setHeaders(headers);
    }

}
