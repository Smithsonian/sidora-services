/*
 * Copyright 2015 Smithsonian Institution.  
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
package edu.si.services.fedorarepo.base;

import org.apache.camel.Exchange;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author jshingler
 */
public class FedoraProducerTest
{

    FedoraProducer producer;

    public FedoraProducerTest()
    {
        this.producer = new FedoraProducerImpl();
    }

    /**
     * Test of hasParam method, of class FedoraProducer.
     */
    @Test
    public void testHasParam()
    {
        System.out.println("Testing hasParam(String)");

        assertTrue("Parameter should have been found", this.producer.hasParam("param"));
        assertTrue("Parameter should have been found", this.producer.hasParam("."));

        //This returns true also... since hasParam() doesn't trim the potential imput
        assertTrue("Parameter should have been found", this.producer.hasParam(" "));

        assertFalse("Parameter should not have been found", this.producer.hasParam(null));
        assertFalse("Parameter should not have been found", this.producer.hasParam(""));
        assertFalse("Parameter should not have been found", this.producer.hasParam("null"));

    }

    /**
     * Test of getParam method, of class FedoraProducer.
     */
    @Test
    public void testGetParam()
    {
        System.out.println("Testing getParam(String)");
        String expected = "validParam";
        String actual = this.producer.getParam(expected);
        assertEquals("Parameter should have been returned", expected, actual);

        assertNull("Results should have been null", this.producer.getParam(null));
        assertNull("Results should have been null", this.producer.getParam(""));
        assertNull("Results should have been null", this.producer.getParam("null"));

    }

    /**
     * Test of getParam method, of class FedoraProducer.
     */
    @Test
    public void testGetParamOrHeaderValue()
    {
        System.out.println("Testing getParam(String, String)");
        String expected = "valid";
        
        assertEquals("Parameter should have been returned", expected, this.producer.getParam(expected, "invalid"));
        assertEquals("Parameter should have been returned", expected, this.producer.getParam(expected, null));

        assertEquals("Header should have been returned", expected, this.producer.getParam(null, expected));
        assertEquals("Header should have been returned", expected, this.producer.getParam("", expected));

        assertNull("Null should have been returned", this.producer.getParam("null", expected));
        assertNull("Null should have been returned", this.producer.getParam(null, null));
        assertNull("Null should have been returned", this.producer.getParam("", null));

    }

    public class FedoraProducerImpl extends FedoraProducer
    {

        public FedoraProducerImpl()
        {
            super(null);
        }

        @Override
        public void process(Exchange exchange) throws Exception
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

}
