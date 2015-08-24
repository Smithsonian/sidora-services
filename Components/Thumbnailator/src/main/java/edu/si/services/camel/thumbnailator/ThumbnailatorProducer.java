/*
 * Copyright 2015 Smithsonian Institution.
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

package edu.si.services.camel.thumbnailator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;

/**
 * The Thumbnailator producer.
 *
 * @author jshingler
 * @version 1.0
 */
public class ThumbnailatorProducer extends DefaultProducer
{
    private final ThumbnailatorEndpoint endpoint;

    public ThumbnailatorProducer(ThumbnailatorEndpoint endpoint)
    {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {

        InputStream body = exchange.getIn().getBody(InputStream.class);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        try
        {
            Thumbnails.Builder<? extends InputStream> tb = Thumbnails.of(body);

            //If size isn't set, there isn't anything to do.
            if (endpoint.isSizeSet())
            {
                tb.size(endpoint.getWidth(), endpoint.getHeight()).keepAspectRatio(endpoint.isKeepRatio());

                //Setting quality will slow down the creation of the thumbnail
                // even if the quality is set at (1.0 default). Issue with third party library
                if (endpoint.isQualitySet())
                {
                    tb.outputQuality(endpoint.getQuality());
                }//end if
            }//end if

            
            tb.toOutputStream(bout);

        }//end try
        catch (IOException exception)
        {
            String fileName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
            if (fileName != null && !fileName.isEmpty())
            {
                fileName = String.format(" for %s!", fileName);
            }//end if
            else
            {
                fileName = "!";
            }//end else

            log.warn(String.format("Error creating thumbnail%s", fileName), exception);

            throw exception;
        }//end catch
        finally
        {

            try
            {
                body.close();
            }
            catch (IOException iOException)
            {
            }
        }

        if (log.isTraceEnabled())
        {
            log.trace(String.format("Created thumbnail with params: size=%s keepRatio=%b", endpoint.getSize(), endpoint.isKeepRatio()));
        }//end if

        exchange.getOut().setBody(bout, ByteArrayOutputStream.class);
        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
    }//end process
}//end ThumbnailatorProducer.class
