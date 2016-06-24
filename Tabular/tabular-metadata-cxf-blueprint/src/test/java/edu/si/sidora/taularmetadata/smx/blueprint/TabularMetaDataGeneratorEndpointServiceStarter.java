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

package edu.si.sidora.taularmetadata.smx.blueprint;

import edu.si.sidora.tabularmetadata.smx.blueprint.TabularMetadataGeneratorEndpointImpl;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;

import java.io.IOException;

/**
 * Starts up the rest service to test in the IDE
 * 
 * @author jbirkhimer
 */
public class TabularMetaDataGeneratorEndpointServiceStarter {
    @SuppressWarnings("rawtypes")
    public void startRestService() {
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setAddress("http://localhost:8282/codebook");
        factory.setResourceClasses(TabularMetadataGeneratorEndpointImpl.class);
        factory.setResourceProvider(new SingletonResourceProvider(new TabularMetadataGeneratorEndpointImpl()));
        factory.setProvider(new JAXBElementProvider());
        Server server = factory.create();
        server.start();
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        new TabularMetaDataGeneratorEndpointServiceStarter().startRestService();
        System.in.read();
    }
}
