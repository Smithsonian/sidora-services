/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.smithsonian.services.fedorarepo.datastream;

import edu.smithsonian.services.fedorarepo.FedoraComponent;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

/**
 *
 * @author jshingler
 */
public class FedoraDatastreamEndpoint extends DefaultEndpoint
{

    private String pid;
    private String name;
    private String type;
    private String group;

    public FedoraDatastreamEndpoint()
    {
    }

    public FedoraDatastreamEndpoint(String uri, FedoraComponent component)
    {
        super(uri, component);
    }

    @Override
    public Producer createProducer() throws Exception
    {
        return new FedoraDatastreamProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception
    {
        throw new UnsupportedOperationException("fedora:datastream cannot start a route");
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

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup(String group)
    {
        this.group = group;
    }


}
