package edu.si.services.beans.edansidora.aggregation;

import edu.si.services.beans.edansidora.model.IdsAsset;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class IdsBatchAggregationStrategy implements AggregationStrategy {

    private static final Logger log = LoggerFactory.getLogger(EdanIdsAggregationStrategy.class);

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        Message out = newExchange.getIn();
        List<IdsAsset> idsAssetList = null;

        IdsAsset asset = out.getBody(IdsAsset.class);


        if (oldExchange == null) {
            idsAssetList = new ArrayList<>();
            idsAssetList.add(asset);

            newExchange.getIn().setHeader("idsAssetList", idsAssetList);

            return newExchange;
        } else {
            idsAssetList = oldExchange.getIn().getHeader("idsAssetList", List.class);
            idsAssetList.add(asset);

            return oldExchange;
        }
    }
}
