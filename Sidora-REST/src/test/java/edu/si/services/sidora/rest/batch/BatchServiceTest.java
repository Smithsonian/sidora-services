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

package edu.si.services.sidora.rest.batch;

import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.xml.bind.JAXBContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

/**TODO: Fix Tests
 * @author jbirkhimer
 */
public class BatchServiceTest extends CamelBlueprintTestSupport {

    //private static final String PORT_PATH = String.valueOf(AvailablePortFinder.getNextAvailable());
    private static final String SERVICE_ADDRESS = "/sidora/rest";
    private static final String BASE_URL = "http://localhost:8282" + SERVICE_ADDRESS;

    //Default Test Params
    private static final String correlationId = UUID.randomUUID().toString();
    private static final String parentPid = "si:390403";
    private static final String resourceFileList = "http://si-fedoradev.si.edu:8080/fedora/objects/si:403105/datastreams/OBJ/content";
    private static final String ds_metadata = "http://si-fedoradev.si.edu:8080/fedora/objects/si:403106/datastreams/OBJ/content";
    private static final String ds_sidora = "http://si-fedoradev.si.edu:8080/fedora/objects/si:403107/datastreams/OBJ/content";
    private static final String association = "http://si-fedoradev.si.edu:8080/fedora/objects/si:403104/datastreams/OBJ/content";
    private static final String resourceOwner = "batchTestUser";

    private JAXBContext jaxb;
    private CloseableHttpClient httpClient;

    //Temp directories created for testing the camel route
    private static File tempInputDirectory, tempConfigDirectory;

    /**
     * Sets up the system properties and Temp directories used by the route.
     * @throws IOException
     */
    @BeforeClass
    public static void setupSysPropsTempResourceDir() throws IOException {
        FileInputStream propFile = new FileInputStream( "src/test/resources/test.properties");
        Properties p = new Properties(System.getProperties());
        p.load(propFile);
        System.setProperties(p);

        //Create and Copy the Input dir xslt, etc. files used in the camera trap route
        tempInputDirectory = new File("Input");
        if(!tempInputDirectory.exists()){
            tempInputDirectory.mkdir();
        }

        //The Location of the original Input dir in the project
        File inputSrcDirLoc = new File("../Routes/Camera Trap/Input");

        //Copy the Input src files to the CameraTrap root so the camel route can find them
        FileUtils.copyDirectory(inputSrcDirLoc, tempInputDirectory);

        tempConfigDirectory = new File("Karaf-config");
        if(!tempConfigDirectory.exists()){
            tempConfigDirectory.mkdir();
        }

        FileUtils.copyDirectory(new File("../Routes/Camera Trap/Karaf-config"), tempConfigDirectory);

        //Set the karaf.home property use by the camera trap route
        System.setProperty("karaf.home", "Karaf-config");

    }

    @Override
    protected String[] loadConfigAdminConfigurationFile() {
        return new String[]{"src/test/resources/test.properties", "edu.si.sidora.batch"};
    }

    /**
     * Clean up the temp directories after tests are finished
     * @throws IOException
     */
    @AfterClass
    public static void teardown() throws IOException {
        if(tempInputDirectory.exists()){
            FileUtils.deleteDirectory(tempInputDirectory);
        }
        if(tempConfigDirectory.exists()){
            FileUtils.deleteDirectory(tempConfigDirectory);
        }
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "Routes/Sidora-Batch/Karaf-config/deploy/sidora-batch.xml";
    }

    public void setUp() throws Exception {
        super.setUp();
        httpClient = HttpClientBuilder.create().build();
        //jaxb = JAXBContext.newInstance(CustomerList.class, Customer.class, Order.class, Product.class);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        httpClient.close();
    }

    //@Test
    public void newBatchRequest_addResourceObjects_Test() throws Exception {

        String expectedResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Batch>\n" +
                "    <ParentPID>" + parentPid + "</ParentPID>\n" +
                "    <CorrelationID>"+ correlationId +"</CorrelationID>\n" +
                "</Batch>\n";

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("BatchProcessAddResourceObjectsRequest").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //weaveById("httpGetResourceList").replace().








            }
        });

        HttpPost post = new HttpPost(BASE_URL + "/batch/process/addResourceObjects/" + parentPid);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.STRICT);

        // Add zip file URL upload
        builder.addTextBody("resourceFileList",resourceFileList, ContentType.TEXT_PLAIN);
        // Add metadata xml file URL upload
        builder.addTextBody(
                "ds_metadata",
                "/home/jbirkhimer/IdeaProjects/sidora-services/Sidora-REST/src/test/resources/test-data/metadataWithTitle.xml",
                ContentType.TEXT_PLAIN
        );
        // Add content model string
        builder.addTextBody("contentModel", "si:generalImageCModel", ContentType.TEXT_PLAIN);
        // Add resourceOwner string
        builder.addTextBody("resourceOwner", parentPid, ContentType.TEXT_PLAIN); //using parentPid for testing so things are easier to find from fedora admin
        // Add title string
        builder.addTextBody("titleField", "Test-Title", ContentType.TEXT_PLAIN);
        // Add sidora datastream file URL string
        builder.addTextBody(
                "ds_sidora",
                "/home/jbirkhimer/IdeaProjects/sidora-services/Sidora-REST/src/test/resources/test-data/sidora-datastream.xml",
                ContentType.TEXT_PLAIN
        );

        post.setEntity(builder.build());

        HttpResponse response = httpClient.execute(post);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String responseBody = EntityUtils.toString(response.getEntity());
        System.out.println("======================== [ RESPONSE ] ========================\n" + responseBody);
        assertEquals(expectedResponseBody, responseBody);

    }

    //@Test
    public void testStatus() throws Exception {
        String parentPid = "si:390403";
        String correlationId = "a1b47c86-0922-4321-b8b3-d4d6abd8d953";

        //HttpPost post = new HttpPost("http://localhost:" + SERVICE_ADDRESS + "/rest/customerservice/customers/multipart/123?query=abcd");
        HttpGet getClient = new HttpGet("http://localhost:" + SERVICE_ADDRESS + "/batch/process/addResourceObjects/" + parentPid + "/" + correlationId);

        HttpResponse response = httpClient.execute(getClient);
        assertEquals(200, response.getStatusLine().getStatusCode());
    }
}