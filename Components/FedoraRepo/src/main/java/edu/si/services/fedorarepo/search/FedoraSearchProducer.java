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

package edu.si.services.fedorarepo.search;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.request.RiSearch;
import com.yourmediashelf.fedora.client.response.RiSearchResponse;
import edu.si.services.fedorarepo.base.FedoraProducer;
import org.apache.camel.Exchange;
import org.apache.camel.Message;

/**
 *
 * @author jshingler
 */
class FedoraSearchProducer extends FedoraProducer
{
    private FedoraSearchEndpoint endpoint;

    public FedoraSearchProducer(FedoraSearchEndpoint endpoint)
    {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {
        String query = exchange.getIn().getBody(String.class);

        //Call Fedora
        RiSearch search = FedoraClient.riSearch(query);

        if (this.hasParam(this.endpoint.getType()))
        {
            search.type(this.endpoint.getType());
        }

        if (this.hasParam(this.endpoint.getLang()))
        {
            search.lang(this.endpoint.getLang());
        }

        if (this.hasParam(this.endpoint.getFormat()))
        {
            search.format(this.endpoint.getFormat());
        }

        if (this.endpoint.getLimit() > 0)
        {
            search.limit(this.endpoint.getLimit());
        }

        RiSearchResponse searchResults = search.execute();

        String results = searchResults.getEntity(String.class);

        Message outputMsg = exchange.getOut();
        outputMsg.setHeaders(exchange.getIn().getHeaders());
        outputMsg.setBody(results);
    }

}
