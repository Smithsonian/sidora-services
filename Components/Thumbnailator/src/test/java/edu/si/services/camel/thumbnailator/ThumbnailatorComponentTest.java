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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import static org.apache.camel.builder.Builder.constant;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Assert;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class ThumbnailatorComponentTest extends CamelTestSupport
{

    private static final String GIF_IMAGE = "tornator2.gif";
    private static final String PNG_IMAGE = "rubic_cube.png";
    private static final String JPG_IMAGE = "HappyBall.jpg";

    private static final String[] IMAGES =
    {
        JPG_IMAGE, PNG_IMAGE, GIF_IMAGE
    };

    public void testDefaultThumbnailatorEndpoint() throws Exception
    {
        context.getEndpoint("uri", ThumbnailatorEndpoint.class);
        context.removeEndpoints("uri");
    }

    @Test(expected = CamelExecutionException.class)
    public void testThumbnailatorError() throws Exception
    {
        log.info("Tesing exception in route");
        template.sendBody("direct:start", new ByteArrayInputStream("Malformed image".getBytes()));
    }

    @Test
    public void testThumbnailator_JPG() throws Exception
    {
        log.info("Tesing creation of JPG thumbnail");
        this.testCreateThumbnail(JPG_IMAGE);
    }

    @Test
    public void testThumbnailator_PNG() throws Exception
    {
        log.info("Tesing creation of PNG thumbnail");
        this.testCreateThumbnail(PNG_IMAGE);
    }

    @Test
    public void testThumbnailator_GIF() throws Exception
    {
        log.info("Tesing creation of JPG thumbnail");
        this.testCreateThumbnail(GIF_IMAGE);
    }

    @Test
    public void testThumbnailator_Stress_10ImagesIn1sec() throws InterruptedException, IOException
    {
        this.testStresser(10, 1, 1000);
    }

    @Test
    public void testThumbnailator_Stress_30ImagesIn1sec5Thread() throws InterruptedException, IOException
    {
        this.testStresser(30, 5, 1000);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            public void configure()
            {
                from("direct:start")
                        .to("thumbnailator:image?keepRatio=false&size=(72,96)")
                        .to("mock:result");
            }
        };
    }

    private List<String> createTestList(int count)
    {
        List<String> list = new ArrayList<String>();
        Random random = new Random();

        for (int i = 0; i < count; i++)
        {
            list.add(IMAGES[random.nextInt(31) % 3]);
        }

        return list;
    }

    private void testStresser(int count, int poolSize, long maxTime) throws InterruptedException, IOException
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(count);

        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        for (final String name : this.createTestList(count))
        {
            executor.submit(new Callable<Object>()
            {
                public Object call() throws Exception
                {
                    InputStream input = this.getClass().getResourceAsStream(String.format("/%s", name));
                    template.sendBodyAndHeader("direct:start", input, Exchange.FILE_NAME, constant(name));
                    return null;
                }
            });
        }

        assertMockEndpointsSatisfied(maxTime, TimeUnit.MILLISECONDS);

        for (Exchange e : mock.getExchanges())
        {
            this.testResults(e);
        }

        mock.reset();

        executor.shutdownNow();
    }

    private void testCreateThumbnail(String name) throws IOException, InterruptedException
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);

        InputStream input = this.getClass().getResourceAsStream(String.format("/%s", name));

        template.sendBodyAndHeader("direct:start", input, Exchange.FILE_NAME, constant(name));

        assertMockEndpointsSatisfied();

        OutputStream result = mock.getExchanges().get(0).getIn().getBody(OutputStream.class);
        this.testResults(name, result);

        mock.reset();
    }//end testCreateThumbnail

    private void testResults(Exchange exchange) throws IOException
    {
        String fileName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
        assertNotNull("Name cannot be null", fileName);
        this.testResults(fileName, exchange.getIn().getBody(OutputStream.class));
    }

    private void testResults(String name, OutputStream result) throws IOException
    {
        assertNotNull("Results should not be null", result);
        if (result instanceof ByteArrayOutputStream)
        {
            byte[] actual = ((ByteArrayOutputStream) result).toByteArray();

            InputStream output = this.getClass().getResourceAsStream(String.format("/Thumbnail-%s", name));
            byte[] expected = new byte[output.available()];
            int read = output.read(expected);
            Assert.assertEquals("Didn't read all the expected bytes", expected.length, read);
            Assert.assertEquals("Size is the same", expected.length, actual.length);
        }//end if
        else
        {
            fail("Output results were not an instance of ByteArrayOutputStream");
        }
    }

}
