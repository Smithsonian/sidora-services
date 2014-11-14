/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.smithsonian.services.fedorarepo.base;

import com.yourmediashelf.fedora.client.response.FedoraResponse;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultProducer;

/**
 *
 * @author jshingler
 */
public abstract class FedoraProducer extends DefaultProducer
{

    public FedoraProducer(Endpoint endpoint)
    {
        super(endpoint);
    }

    protected boolean hasParam(String param)
    {
        return (param != null && !param.isEmpty() && !"null".equalsIgnoreCase(param));
    }

    protected String getParam(String param)
    {
        if (hasParam(param))
        {
            return param;
        }//end if
        else
        {
            return null;
        }//end else
    }

    protected String getParam(String param, String header)
    {
        String value = null;
        if (param != null && !param.isEmpty())
        {
            if (!"null".equalsIgnoreCase(param))
            {
                value = param;
            }//end if
        }//end if
        else
        {
            value = header;
        }//end else

        return value;
    }

    protected boolean checkStatus(FedoraResponse response)
    {
        //Think checking the status is pointless since the FedoraClient will throw
        // an exception if the execution failed
        return (response.getStatus() >= 200 && response.getStatus() < 300);
    }
}
