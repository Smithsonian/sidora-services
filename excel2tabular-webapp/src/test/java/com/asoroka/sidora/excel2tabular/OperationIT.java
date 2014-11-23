
package com.asoroka.sidora.excel2tabular;

import static com.google.common.io.ByteStreams.toByteArray;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
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

    @Test
    public void testUrlOperation() throws IOException {
        final File dataFileLocation = new File("src/test/resources/small-test.xls");
        final Response r = client.query("url", "file://" + dataFileLocation.getAbsolutePath()).get();
        assertEquals(OK.getStatusCode(), r.getStatus());
        final String text = new String(toByteArray((InputStream) r.getEntity()));
        log.trace("Received {}", text);
    }

}
