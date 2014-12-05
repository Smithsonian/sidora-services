/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.smithsonian.services.fedorarepo.datastream;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.request.AddDatastream;
import com.yourmediashelf.fedora.client.response.AddDatastreamResponse;
import edu.smithsonian.services.fedorarepo.Headers;
import edu.smithsonian.services.fedorarepo.base.FedoraProducer;
import java.io.InputStream;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jshingler
 */
public class FedoraDatastreamProducer extends FedoraProducer
{

    private static final Logger LOG = LoggerFactory.getLogger(FedoraDatastreamProducer.class);
    private final FedoraDatastreamEndpoint endpoint;

    public FedoraDatastreamProducer(FedoraDatastreamEndpoint endpoint)
    {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {
        Message in = exchange.getIn();

        InputStream body = in.getBody(InputStream.class);

        if (body == null)
        {
            throw new RuntimeException("fedora:datastream producer requires a body");
        }

        String pid = this.getParam(this.endpoint.getPid(), in.getHeader(Headers.PID, String.class));
        String name = this.getParam(this.endpoint.getName());
        String type = this.getParam(this.endpoint.getType());
        String group = this.getParam(this.endpoint.getGroup());

        if (pid == null)
        {
            throw new RuntimeException("fedora:datastream producer requires a parent PID");
        }//end if
        else if (name == null)
        {
            throw new RuntimeException("fedora:datastream producer requires a Datastream name");
        }//end else if
        else if (type == null && !"RELS-EXT".equalsIgnoreCase(name) && !"RELS-INT".equalsIgnoreCase(name))
        {
            //TODO: Needed if Body is string ... could default
            //TODO: Needed if Body is File... could type be determined? (Didn't work first time when testing... try again?)
            throw new RuntimeException("fedora:datastream producer requires a Datastream type");
        }//end else if
        else if (group == null && !"RELS-EXT".equalsIgnoreCase(name) && !"RELS-INT".equalsIgnoreCase(name))
        {
            throw new RuntimeException("fedora:datastream producer requires a Datastream control group");
        }//end else if

        AddDatastream datastream = FedoraClient.addDatastream(pid, name).content(body).mimeType(type).controlGroup(group);

        AddDatastreamResponse response = datastream.execute();

        Map<String, Object> headers = in.getHeaders();
        headers.put(Headers.STATUS, response.getStatus());

        LOG.debug(String.format("Datastream: %s (%s) Status = %d [Mime Type = %s Control Group = %s]", name, pid, response.getStatus(), type, group));

        Message out = exchange.getOut();
        out.setHeaders(headers);
        out.setBody(in.getBody());
    }

}
