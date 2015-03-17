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
 * This software and accompanying documentation is supplied â€œas isâ€� without
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Thumbnailator endpoint.
 *
 * @author jshingler
 * @version 1.0
 */
public class ThumbnailatorEndpoint extends DefaultEndpoint
{
    Logger log = LoggerFactory.getLogger(ThumbnailatorComponent.class);

    protected Pattern sizePattern = Pattern.compile("\\(\\s*(?<width>\\d+)\\s*,\\s*(?<height>\\d+)\\s*\\)");
    protected Pattern qualityPattern = Pattern.compile("(?<quality>\\d+)%");

    private boolean keepRatio = true;
    private int width = 0;
    private int height = 0;
    private double quality = 1.0;


    public ThumbnailatorEndpoint()
    {
    }

    public ThumbnailatorEndpoint(String uri, ThumbnailatorComponent component)
    {
        super(uri, component);
    }


    @Override
    public Producer createProducer() throws Exception
    {
        return new ThumbnailatorProducer(this);
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }

    @Override
    public Consumer createConsumer(Processor prcsr) throws Exception
    {
        throw new UnsupportedOperationException("Thumbnailator cannot start a route");
    }

    public boolean isKeepRatio()
    {
        return keepRatio;
    }

    public void setKeepRatio(boolean keepRatio)
    {
        this.keepRatio = keepRatio;
    }

    public String getSize()
    {
        return String.format("(%d,%d)", this.width, this.height);
    }

    public boolean isSizeSet()
    {
        return (this.width > 0 && this.height > 0);
    }

    public void setSize(String size)
    {
        if (size != null && !size.isEmpty())
        {
            Matcher matcher = this.sizePattern.matcher(size);
            if (matcher.matches())
            {
                try
                {
                    this.setSize(Integer.parseInt(matcher.group("width")), Integer.parseInt(matcher.group("height")));
                }//end try
                catch (NumberFormatException numberFormatException)
                {
                    log.warn(String.format("Could not parse size from -> %s", size), numberFormatException);
                }//end catch
            }//end if
        }//end if
    }//end setSize

    public void setSize(int width, int height)
    {
        this.setWidth(width);
        this.setHeight(height);
    }

    public int getWidth()
    {
        return width;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }

    public int getHeight()
    {
        return height;
    }

    public void setHeight(int height)
    {
        this.height = height;
    }

    public boolean isQualitySet()
    {
        return (this.quality < 1.0);
    }

    public double getQuality()
    {
        return quality;
    }

    public void setQuality(double quality)
    {
        this.quality = quality;

        if (this.quality > 1.0)
        {
            this.quality /= 100;
        }
    }

    public void setQuality(String quality)
    {
        if (quality != null && !quality.isEmpty())
        {
            Matcher matcher = this.qualityPattern.matcher(quality);
            if (matcher.matches())
            {
                try
                {
                    this.setQuality(Double.parseDouble(matcher.group("quality")));
                }//end try
                catch (NumberFormatException numberFormatException)
                {
                    log.warn(String.format("Could not parse quality from -> %s", quality), numberFormatException);
                }//end catch
            }//end if
        }//end if
    }//end setQuality
}//end ThumbnailatorEndpoint.class
