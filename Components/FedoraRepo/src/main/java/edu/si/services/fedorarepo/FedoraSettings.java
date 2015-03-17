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

    public FedoraSettings()
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
