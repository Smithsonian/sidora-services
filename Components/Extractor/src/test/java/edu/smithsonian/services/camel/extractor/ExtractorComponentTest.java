package edu.smithsonian.services.camel.extractor;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class ExtractorComponentTest extends CamelTestSupport
{

    //TODO: Add more test!!! Need to make sure that Producer finds the correct new folder if there are multiple folders initially
    @Test
    public void testTarballExtractor() throws Exception
    {
        testExtractor("p1d246-test-targz.tar.gz");
    }

    @Test
    public void testTarExtractor() throws Exception
    {
        testExtractor("p1d246-test-tar.tar");
    }

    @Test
    public void testZipExtractor() throws Exception
    {
        testExtractor("p1d246-test-zip.zip");
    }

    @Test
    public void testOverwrite() throws Exception
    {
        testExtractor("p1d246-test-targz.tar.gz", false);
        testExtractor("p1d246-test-targz.tar.gz");
    }

    public void testExtractor(String archive) throws Exception
    {
        this.testExtractor(archive, true);
    }

    public void testExtractor(String archive, boolean delete) throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(msgCount + 1);

        File file = new File(this.getClass().getResource("/" + archive).toURI());

        template.sendBody("direct:start", file);

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(msgCount++).getIn();
        File body = msg.getBody(File.class);

        try
        {
            assertNotNull("Results should not be null", body);
            assertTrue("Results should be a directory", body.isDirectory());
            assertEquals("Parent directory should be 'TestData'", "TestData", body.getParentFile().getName());
            assertEquals("Directory should contain 4 elements", 4, body.list().length);
        }
        catch (Exception ex)
        {
            delete = true;
            throw ex;
        }
        finally
        {
            if (delete && body != null && body.isDirectory())
            {
                FileUtils.deleteDirectory(body);
            }
        }
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure()
            {
                from("direct:start")
                        .to("extractor:extract?location=TestData")
                        .to("mock:result");
            }
        };
    }

    private int msgCount = 0;

    @Before
    public void beforeTest()
    {
        msgCount = 0;
    }

    @AfterClass
    public static void afterClass()
    {
        try
        {
            FileUtils.deleteDirectory(new File("TestData"));
        }
        catch (IOException ex)
        {
            Logger.getLogger(ExtractorComponentTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
