
package com.asoroka.sidora.excel2tabular;

import static com.google.common.io.ByteStreams.toByteArray;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-xml/default-operation.xml")
public class OperationIT {

    @Inject
    private WebClient client;

    private static final Logger log = getLogger(OperationIT.class);

    private static String correctMimeType = "Content-Type: text/csv";

    private static String correctSheetId = "Content-ID: <Sheet:1>";

    private static String correctContent =
            "\"Animal counts\",\"Animal color\",\"Animal size\",\"IsMammal\",\"Confused calculation\"\n" +
                    "1,\"Red\",\"Big\",true,#VALUE!\n" +
                    "3,\"Blue\",\"Little\",false,#VALUE!\n" +
                    "4,\"Green\",\"Tiny\",true,#VALUE!";

    @Test
    public void testUrlOperation() throws IOException {
        final File dataFileLocation = new File("src/test/resources/small-test.xls");
        final Response r = client.query("url", "file://" + dataFileLocation.getAbsolutePath()).get();
        assertEquals(OK.getStatusCode(), r.getStatus());
        final String text = new String(toByteArray((InputStream) r.getEntity()));
        log.trace("Received {}", text);
        assertTrue(text.contains(correctMimeType));
        assertTrue(text.contains(correctSheetId));
        assertTrue(text.contains(correctContent));
    }
}
