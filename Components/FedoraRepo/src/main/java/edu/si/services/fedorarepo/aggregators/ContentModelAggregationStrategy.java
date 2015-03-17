package edu.si.services.fedorarepo.aggregators;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;

import java.util.HashSet;

/**
 * Created by davisda on 3/6/15.
 */
public class ContentModelAggregationStrategy implements AggregationStrategy
{
    @Override
    @SuppressWarnings("unchecked")
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange)
    {
        String headerName = "ContentModels";
        HashSet<String> contentModelList;
                
        if ((oldExchange == null) || (oldExchange.getIn().getHeader(headerName) == null))
        {
            // The first time we only have the new exchange
            contentModelList = new HashSet<String>();
            newExchange.getIn().setHeader(headerName, contentModelList);
            oldExchange = newExchange;
        }
        else
        {
            contentModelList = (HashSet<String>) oldExchange.getIn().getHeader(headerName);
        } // end else

        String contentModel = newExchange.getIn().getBody(String.class);
        if ((contentModel != null) && !contentModel.trim().isEmpty())
        {
            contentModelList.add(contentModel);   
        }
        
        return oldExchange;
    }
}
