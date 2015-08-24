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
import com.yourmediashelf.fedora.client.request.GetDatastreamDissemination;
import com.yourmediashelf.fedora.client.response.FedoraResponse;
import edu.si.services.fedorarepo.Headers;
import edu.si.services.fedorarepo.base.FedoraProducer;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 *
 * @author davisda
 */
public class FedoraGetDatastreamDisseminationProducer extends FedoraProducer
{
    private static final Logger LOG = LoggerFactory.getLogger(FedoraGetDatastreamDisseminationProducer.class);
    private final FedoraGetDatastreamDisseminationEndpoint endpoint;

    public FedoraGetDatastreamDisseminationProducer(FedoraGetDatastreamDisseminationEndpoint endpoint)
    {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {
        Message in = exchange.getIn();

        String pid = this.getParam(this.endpoint.getPid(), in.getHeader(Headers.PID, String.class));
        String dsID = this.getParam(this.endpoint.getDsId());
        String asOfDateTime = this.getParam(this.endpoint.getAsOfDateTime());

        if (pid == null)
        {
            throw new RuntimeException("fedora:getDatastreamDissemination producer requires a PID");
        }//end if
        else if (dsID == null)
        {
            throw new RuntimeException("fedora:getDatastreamDissemination producer requires a Datastream name");
        }//end else if

        GetDatastreamDissemination datastream =
                FedoraClient.getDatastreamDissemination(pid, dsID).asOfDateTime(asOfDateTime).download(this.endpoint.isDownload());
        FedoraResponse response = datastream.execute();
        
        // This code put the burden on the route to manage the return type. By returning the input stream we can
        // handle a large datastream efficiently.
        //
        // Use the response status for error handling
        Object dsContent = response.getEntityInputStream();
        
        Map<String, Object> headers = in.getHeaders();
        headers.put(Headers.STATUS, response.getStatus());

        LOG.debug(String.format("Datastream: %s (%s) Status = %d", dsID, pid, response.getStatus()));

        Message out = exchange.getOut();
        out.setHeaders(headers);
        out.setBody(dsContent);
    }
}
