/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.smithsonian.services.fedorarepo;

import com.yourmediashelf.fedora.client.FedoraCredentials;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author jshingler
 */
public class FedoraSettings 
{

    private FedoraCredentials credentials;

    public FedoraSettings()
    {
        this.credentials = null;
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
