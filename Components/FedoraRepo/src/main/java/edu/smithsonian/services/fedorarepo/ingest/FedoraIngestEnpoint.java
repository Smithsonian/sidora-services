/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.smithsonian.services.fedorarepo.ingest;

import edu.smithsonian.services.fedorarepo.FedoraComponent;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.UriParam;

/**
 *
 * @author jshingler
 */
public class FedoraIngestEnpoint extends DefaultEndpoint
{

    protected boolean createType;

    @UriParam
    private String pid;
    @UriParam
    private String label;
    @UriParam
    private String log;
    @UriParam
    private String namespace;
    @UriParam
    private String owner;

    //TODO: Finish Injest with all options...
//    private String format;
//    private boolean ignoreMime;

    public FedoraIngestEnpoint()
    {
        this.createType = false;
    }

    public FedoraIngestEnpoint(String uri, FedoraComponent component)
    {
        this(uri, component, false);
    }

    public FedoraIngestEnpoint(String uri, FedoraComponent component, boolean isCreateType)
    {
        super(uri, component);
        this.createType = isCreateType;
    }
    @Override
    public Producer createProducer() throws Exception
    {
        return new FedoraIngestProducer(this, createType);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception
    {
        throw new UnsupportedOperationException("fedora:ingest cannot start a route");
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }

    public String getPid()
    {
        return pid;
    }

    public void setPid(String pid)
    {
        this.pid = pid;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getOwner()
    {
        return owner;
    }

    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    public String getLog()
    {
        return log;
    }

    public void setLog(String log)
    {
        this.log = log;
    }

}
