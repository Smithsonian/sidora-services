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

package edu.si.services.beans.edansidora.aggregation;

import edu.si.services.beans.edansidora.model.IdsAsset;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jbirkhimer
 */
public class EdanIdsAggregationStrategy implements AggregationStrategy {

    private static final Logger log = LoggerFactory.getLogger(EdanIdsAggregationStrategy.class);

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        Message out = newExchange.getIn();
        List<IdsAsset> idsAssetList = null;

        IdsAsset asset = new IdsAsset();
        asset.setImageid(out.getHeader("imageid", String.class));
        asset.setSiteId(out.getHeader("SiteId", String.class));
        asset.setIsPublic(out.getHeader("isPublic", String.class));
        asset.setIsInternal(out.getHeader("isInternal", String.class));

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
