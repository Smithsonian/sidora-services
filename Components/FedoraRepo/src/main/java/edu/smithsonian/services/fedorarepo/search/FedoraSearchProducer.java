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
package edu.smithsonian.services.fedorarepo.search;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.request.RiSearch;
import com.yourmediashelf.fedora.client.response.RiSearchResponse;
import edu.smithsonian.services.fedorarepo.base.FedoraProducer;
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

        if (this.hasParam(this.endpoint.getLang()))
        {
            search.lang(this.endpoint.getLang());
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
