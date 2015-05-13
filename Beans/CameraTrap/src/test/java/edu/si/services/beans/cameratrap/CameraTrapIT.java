package edu.si.services.beans.cameratrap;

import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.io.InputStream;

/**
 * Tests the operation of Excel utilities when used as a bean in a Camel route.
 * @author davisda
 */
public class CameraTrapIT extends CamelTestSupport
{
    @Test
    public void testXSLStream() throws Exception
    {
        //InputStream input = this.getClass().getResourceAsStream("/BDD-SP-08.xls");
        //template.sendBody("direct:testXSLStream", input);

        //MockEndpoint ep = getMockEndpoint("mock:result");
        //Message in = ep.getExchanges().get(0).getIn();
        //Object body = in.getBody();

        //String csv = body.toString();
        //assertEquals("Año,Autor,", csv.substring(0, 10));
        //assertEquals(97053, csv.length());
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure()
            {
                //from("direct:testXSLStream")
                //    .noStreamCaching()
                //    .bean(ExcelToCSV.class, "convertExcelToCSV")
                //    .to("mock:result");
            }
        };
    }
}
