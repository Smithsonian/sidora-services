/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.smithsonian.services.fedorarepo.pid;

import edu.smithsonian.services.fedorarepo.FedoraComponent;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

/**
 *
 * @author jshingler
 */
public class FedoraPidEndpoint extends DefaultEndpoint
{

    public FedoraPidEndpoint()
    {
    }

    public FedoraPidEndpoint(String uri, FedoraComponent component)
    {
        super(uri, component);
    }

    @Override
    public Producer createProducer() throws Exception
    {
        return new FedoraPidProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception
    {
        throw new UnsupportedOperationException("fedora:nextpid cannot start a route");
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }
}
