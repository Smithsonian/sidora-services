/*
 * Copyright 2015 Smithsonian Institution.
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

package edu.si.services.fedorarepo.datastream;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.request.AddDatastream;
import com.yourmediashelf.fedora.client.response.AddDatastreamResponse;
import edu.si.services.fedorarepo.Headers;
import edu.si.services.fedorarepo.base.FedoraProducer;
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
public class FedoraAddDatastreamProducer extends FedoraProducer
{

    private static final Logger LOG = LoggerFactory.getLogger(FedoraAddDatastreamProducer.class);
    private final FedoraAddDatastreamEndpoint endpoint;

    public FedoraAddDatastreamProducer(FedoraAddDatastreamEndpoint endpoint)
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
        String dsLabel = this.getParam(this.endpoint.getDsLabel());

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

        AddDatastream datastream =
            FedoraClient.addDatastream(pid, name).content(body).mimeType(type).controlGroup(group).dsLabel(dsLabel).versionable(this.endpoint.isVersionable());
        AddDatastreamResponse response = datastream.execute();

        Map<String, Object> headers = in.getHeaders();
        headers.put(Headers.STATUS, response.getStatus());

        LOG.debug(String.format("Datastream: %s (%s) Status = %d [Mime Type = %s Control Group = %s]", name, pid, response.getStatus(), type, group));

        Message out = exchange.getOut();
        out.setHeaders(headers);
        out.setBody(in.getBody());
    }

}
