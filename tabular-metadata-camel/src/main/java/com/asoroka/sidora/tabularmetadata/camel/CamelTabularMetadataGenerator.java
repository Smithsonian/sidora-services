
package com.asoroka.sidora.tabularmetadata.camel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.camel.Handler;
import org.apache.camel.Header;
import org.apache.camel.Message;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.TabularMetadataGenerator;

/**
 * A simple extension of {@link TabularMetadataGenerator} with "glue" for Apache Camel integrations. It should be
 * configured exactly as TabularMetadataGenerator and then used with the Camel Bean endpoint.
 * 
 * @author ajs6f
 * @see com.asoroka.sidora.tabularmetadata.TabularMetadataGenerator
 */
public class CamelTabularMetadataGenerator extends TabularMetadataGenerator {

    /**
     * A header with this name should be present in the input {@link Message} and should contain the URL of the
     * tabular data source.
     */
    public static final String tabularDataUrlHeaderName = "com.asoroka.sidora.tabularmetadata.camel.tabularDataUrl";

    @Handler
    public TabularMetadata getMetadata(@Header(tabularDataUrlHeaderName) final String dataUrl)
            throws MalformedURLException, IOException {
        return super.getMetadata(new URL(dataUrl));
    }
}
