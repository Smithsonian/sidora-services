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

package edu.si.services.fedorarepo.aggregators;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jshingler
 */
public class PidAggregationStrategy implements AggregationStrategy
{
    private static final Logger log = LoggerFactory.getLogger(PidAggregationStrategy.class);

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange)
    {

        String pid = newExchange.getIn().getHeader("CamelFedoraPid", String.class);
        if(oldExchange!=null)
        {
            String PIDAgg = oldExchange.getIn().getHeader("PIDAggregation", String.class);
        }

        if (pid == null)
        {
            //Throw exception or ignore???
            //For now ignore!
            return oldExchange;
        }//end if

        if(oldExchange == null)
        {
            if(newExchange.getIn().getHeader("imageSkipped", Boolean.class) != null && newExchange.getIn().getHeader("imageSkipped", Boolean.class) == true)
            {
                newExchange.getIn().setHeader("skippedImageCount", 1);

                int imageCount = newExchange.getIn().getHeader("ImageCount", Integer.class);
                int resourceCount = newExchange.getIn().getHeader("ResourceCount", Integer.class);

                if(imageCount > 0)
                {
                    imageCount -= 1;
                }

                newExchange.getIn().setHeader("ImageCount", imageCount);

                if(resourceCount > 0)
                {
                    resourceCount -= 1;
                }

                newExchange.getIn().setHeader("ResourceCount", resourceCount);
                /*log.info("PID AGGREGATION FOR EMPTY IMAGE FILE NAME: " + newExchange.getIn().getHeader("imageid", String.class));
                log.info("skippedImageCount: " + newExchange.getIn().getHeader("skippedImageCount", Integer.class));
                log.info("Image Count: " + imageCount);
                log.info("Resource Count: " + resourceCount);*/
                return newExchange;
            }
            else
            {
                newExchange.getIn().setHeader("skippedImageCount", 0);
            }
        }

        if(newExchange.getIn().getHeader("imageSkipped", Boolean.class) != null && newExchange.getIn().getHeader("imageSkipped", Boolean.class) == true)
        {
            int count = oldExchange.getIn().getHeader("skippedImageCount", Integer.class);
            oldExchange.getIn().setHeader("skippedImageCount", count+1);

            int imageCount = oldExchange.getIn().getHeader("ImageCount", Integer.class);
            int resourceCount = oldExchange.getIn().getHeader("ResourceCount", Integer.class);

            if(imageCount > 0)
            {
                imageCount -= 1;
            }

            oldExchange.getIn().setHeader("ImageCount", imageCount);

            if(resourceCount > 0)
            {
                resourceCount -= 1;
            }

            oldExchange.getIn().setHeader("ResourceCount", resourceCount);

            /*log.info("PID AGGREGATION FOR EMPTY IMAGE FILE NAME: " + newExchange.getIn().getHeader("imageid", String.class));
            log.info("skippedImageCount: " + oldExchange.getIn().getHeader("skippedImageCount", Integer.class));
            log.info("Image Count: " + imageCount);
            log.info("Resource Count: " + resourceCount);*/

            return oldExchange;
        }

        if (oldExchange == null || (oldExchange.getIn().getHeader("PIDAggregation") == null))
        {
            newExchange.getIn().setHeader("PIDAggregation", pid);
            return newExchange;
        }//end if

        String aggregation = oldExchange.getIn().getHeader("PIDAggregation", String.class);
        oldExchange.getIn().setHeader("PIDAggregation", String.format("%s,%s", aggregation, pid));

        return oldExchange;
    }

}

