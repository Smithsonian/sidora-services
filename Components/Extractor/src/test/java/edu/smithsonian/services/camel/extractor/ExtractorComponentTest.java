package edu.smithsonian.services.camel.extractor;

import java.io.File;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class ExtractorComponentTest extends CamelTestSupport
{

    //TODO: Add more test!!! Need to make sure that Producer finds the correct new folder if there are multiple folders initially
    @Test
    public void testTarballExtractor() throws Exception
    {
        testExtractor("p1d246-test.tar.gz");
    }

    @Test
    public void testTarExtractor() throws Exception
    {
        testExtractor("p1d246-test.tar");
    }

    @Test
    public void testZipExtractor() throws Exception
    {
        testExtractor("p1d246-test.zip");
    }

    public void testExtractor(String archive) throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);

        File file = new File(this.getClass().getResource("/" + archive).toURI());

        template.sendBody("direct:start", file);

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(0).getIn();
        File body = msg.getBody(File.class);

        try
        {
            assertNotNull("Results should not be null", body);
            assertTrue("Results should be a directory", body.isDirectory());
            assertEquals("Parent directory should be 'TestData'", "TestData", body.getParentFile().getName());
            assertEquals("Directory should contain 4 elements", 4, body.list().length);
        }
        finally
        {
            if (body != null && body.isDirectory())
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
            public void configure()
            {
                from("direct:start")
                        .to("extractor:extract?location=TestData")
                        .to("mock:result");
            }
        };
    }
}
