/*
 * Copyright 2014 Smithsonian Institution.
 *
 * Permission is granted to use, copy, modify,
 * and distribute this software and its documentation for educational, research
 * and non-profit purposes, without fee and without a signed licensing
 * agreement, provided that this notice, including the following two paragraphs,
 * appear in all copies, modifications and distributions.  For commercial
 * licensing, contact the Office of the Chief Information Officer, Smithsonian
 * Institution, 380 Herndon Parkway, MRC 1010, Herndon, VA. 20170, 202-633-5256.
 *
 * This software and accompanying documentation is supplied â€œas isâ€� without
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
