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

package edu.si.services.fedorarepo.ingest;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.request.Ingest;
import com.yourmediashelf.fedora.client.response.IngestResponse;
import edu.si.services.fedorarepo.Headers;
import edu.si.services.fedorarepo.base.FedoraProducer;
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
    private final FedoraIngestEndpoint endpoint;
    protected boolean createType;

    public FedoraIngestProducer(FedoraIngestEndpoint endpoint)
    {
        this(endpoint, false);
    }

    FedoraIngestProducer(FedoraIngestEndpoint endpoint, boolean createType)
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
