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
 * This software and accompanying documentation is supplied â€œas isâ€� without
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
package edu.si.services.camel.thumbnailator;

import java.io.IOException;
import java.io.InputStream;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author jshingler
 */
public class SpringThumbnailatorComponentTest extends CamelSpringTestSupport
{

    @Override
    public void setUp() throws Exception
    {
        deleteDirectory("target/inbox");
        deleteDirectory("target/outbox");
        super.setUp();
    }

    @Test
    public void testSpringRoute() throws InterruptedException, IOException
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);
        String name = "HappyBall.jpg";
        InputStream input = this.getClass().getResourceAsStream("/" + name);

        template.sendBody("direct:start", input);

        assertMockEndpointsSatisfied();

        InputStream output = this.getClass().getResourceAsStream(String.format("/Thumbnail-%s", name));
        byte[] expected = new byte[output.available()];
        int read = output.read(expected);

        byte[] actual = mock.getExchanges().get(0).getIn().getBody(byte[].class);

        Assert.assertEquals("Didn't read all the expected bytes", expected.length, read);
        Assert.assertEquals("Size is the same", expected.length, actual.length);
    }

    @Test
    public void testSpringRouteFile() throws InterruptedException
    {
        InputStream input = this.getClass().getResourceAsStream("/rubic_cube.png");
        template.sendBodyAndHeader("file://target/inbox", input, Exchange.FILE_NAME, "test-image.png");

        Thread.sleep(2000);

        assertFileExists("target/outbox/test-image.png");
    }

    @Override
    protected AbstractApplicationContext createApplicationContext()
    {
        return new ClassPathXmlApplicationContext("/ThumbnailatorSpringRoute.xml");
    }
}
