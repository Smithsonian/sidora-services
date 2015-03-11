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
 * This software and accompanying documentation is supplied "as is" without
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
import com.yourmediashelf.fedora.client.request.GetDatastream;
import com.yourmediashelf.fedora.client.response.FedoraResponse;
import edu.smithsonian.services.fedorarepo.Headers;
import edu.smithsonian.services.fedorarepo.base.FedoraProducer;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.TypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 *
 * @author davisda
 */
public class FedoraGetDatastreamProducer extends FedoraProducer
{
    private static final Logger LOG = LoggerFactory.getLogger(FedoraGetDatastreamProducer.class);
    private final FedoraGetDatastreamEndpoint endpoint;

    public FedoraGetDatastreamProducer(FedoraGetDatastreamEndpoint endpoint)
    {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {
        TypeConverter converter = exchange.getContext().getTypeConverter();
        Message in = exchange.getIn();

        String pid = this.getParam(this.endpoint.getPid(), in.getHeader(Headers.PID, String.class));
        String dsID = this.getParam(this.endpoint.getDsId());
        String asOfDateTime = this.getParam(this.endpoint.getAsOfDateTime());

        if (pid == null)
        {
            throw new RuntimeException("fedora:getDatastream producer requires a PID");
        }//end if
        else if (dsID == null)
        {
            throw new RuntimeException("fedora:getDatastream producer requires a Datastream name");
        }//end else if

        GetDatastream datastream =
                FedoraClient.getDatastream(pid, dsID).asOfDateTime(asOfDateTime).format("xml");
        FedoraResponse response = datastream.execute();
        
        // This code assumes we want to get the datastream metadata as an XML document so we will handle it as
        // a string.
        //
        // Use the response status for error handling
        String dsMetadata = converter.convertTo(String.class, exchange, response.getEntityInputStream());
        
        Map<String, Object> headers = in.getHeaders();
        headers.put(Headers.STATUS, response.getStatus());

        LOG.debug(String.format("Datastream Metadata: %s (%s) Status = %d", dsID, pid, response.getStatus()));

        Message out = exchange.getOut();
        out.setHeaders(headers);
        out.setBody(dsMetadata);
    }
}
