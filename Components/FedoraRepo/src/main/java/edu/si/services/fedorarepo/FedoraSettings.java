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

package edu.si.services.fedorarepo;

import com.yourmediashelf.fedora.client.FedoraCredentials;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author jshingler
 */
public class FedoraSettings 
{

    //
    //  TODO: Add Unit Test if this gets used!!!
    //
    private FedoraCredentials credentials;

    public FedoraSettings(FedoraCredentials fedoraCredentials)
    {
        this.credentials = null;
    }
    
    public enum FedoraType
    {
        getDatastream, getDatastreamDissemination, addDatastream, purgeDatastream
    }

    public FedoraSettings(String baseUrl, String username, String password)
    {
        try
        {
            this.credentials = new FedoraCredentials(baseUrl, username, password);
        }
        catch (MalformedURLException ex)
        {
            this.credentials = null;
        }
    }

    public boolean hasCredentials()
    {
        return this.credentials != null;
    }

    public FedoraCredentials getCredentials()
    {
        return this.credentials;
    }

    public void setHost(String url) throws MalformedURLException
    {
        this.credentials.setBaseUrl(new URL(url));
    }

    public void setHost(URL baseUrl)
    {
        this.credentials.setBaseUrl(baseUrl);
    }

    public URL getHost()
    {
        return this.credentials.getBaseUrl();
    }

    public String getUsername()
    {
        return credentials.getUsername();
    }

    public void setUsername(String username)
    {
        credentials.setUsername(username);
    }

    public String getPassword()
    {
        return credentials.getPassword();
    }

    public void setPassword(String password)
    {
        credentials.setPassword(password);
    }
}
