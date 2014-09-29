/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.smithsonian.services.fedorarepo.aggregators;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;

/**
 *
 * @author jshingler
 */
public class PidAggregationStrategy implements AggregationStrategy
{

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange)
    {
        
        String pid = newExchange.getIn().getHeader("CamelFedoraPid", String.class);

        if (pid == null)
        {
            //Throw exception or ignore???
            //For now ignore!
            return oldExchange;
        }//end if

        if (oldExchange == null)
        {
            newExchange.getIn().setBody(pid);
            return newExchange;
        }//end if

        String body = oldExchange.getIn().getBody(String.class);

        oldExchange.getIn().setBody(String.format("%s,%s", body, pid), String.class);

        return oldExchange;
    }
}
