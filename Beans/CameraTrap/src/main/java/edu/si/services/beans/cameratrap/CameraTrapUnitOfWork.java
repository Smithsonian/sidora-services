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

package edu.si.services.beans.cameratrap;

import org.apache.camel.AsyncCallback;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.MDCUnitOfWork;
import org.apache.camel.spi.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * This class extends MDCUnitOfWork and implements the beforeProcess method of Camel UnitOfWork
 * to ensure MDC persist is handled correctly during the routing exchanges.  You may add additional custom MDC keys
 * in this class but currently, we are using the Camel ContextID as the key
 *
 * @author parkjohn
 */
public class CameraTrapUnitOfWork extends MDCUnitOfWork implements UnitOfWork {

    private static final Logger LOG = LoggerFactory.getLogger(CameraTrapUnitOfWork.class);

    public CameraTrapUnitOfWork(Exchange exchange) {
        super(exchange);
    }

    @Override
    public UnitOfWork newInstance(Exchange exchange) {
        return new CameraTrapUnitOfWork(exchange);
    }

    @Override
    public AsyncCallback beforeProcess(Processor processor, Exchange exchange, AsyncCallback callback) {
        return new CameraTrapMDCCallback(callback, exchange.getContext());
    }

    private static final class CameraTrapMDCCallback implements AsyncCallback {

        private static final Logger LOG = LoggerFactory.getLogger(CameraTrapMDCCallback.class);

        private final AsyncCallback delegate;
        private final CamelContext camelContext;

        private CameraTrapMDCCallback(AsyncCallback delegate, CamelContext camelContext) {
            this.delegate = delegate;
            this.camelContext = camelContext;
        }

        public void done(boolean doneSync) {

            try {

                if (camelContext != null) {
                    MDC.put(MDC_CAMEL_CONTEXT_ID, camelContext.getName());
                }

            } finally {
                delegate.done(doneSync);
            }
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}