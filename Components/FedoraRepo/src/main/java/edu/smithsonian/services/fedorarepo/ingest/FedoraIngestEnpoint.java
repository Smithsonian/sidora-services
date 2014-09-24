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

/**
 *
 * @author jshingler
 */
public class FedoraIngestEnpoint extends DefaultEndpoint
{

    private String pid;

    public FedoraIngestEnpoint()
    {
    }

    public FedoraIngestEnpoint(String uri, FedoraComponent component)
    {
        super(uri, component);
    }

    @Override
    public Producer createProducer() throws Exception
    {
        return new FedoraIngestProducer(this);
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

}
