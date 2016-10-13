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

import edu.si.services.fedorarepo.FedoraSettings;
import org.apache.camel.Exchange;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.FileUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Properties;

/**
 * @author jbirkhimer
 */
public class BatchResourcesTest extends CamelBlueprintTestSupport {

    private static final String BASE_URL = "http://localhost:8181/sidora/rest/";

//    private static final String PORT_PATH = AvailablePortFinder.getNextAvailable() + "/sidora/rest";
private static final String PORT_PATH = "8282/sidora/rest";
    private static final String CXF_RS_ENDPOINT_URI = "cxfrs://http://localhost:" + PORT_PATH
            + "/batch?resourceClasses=edu.si.services.sidora.rest.batch.BatchResources&bindingStyle=SimpleConsumer";

    private JAXBContext jaxb;
    private CloseableHttpClient httpClient;

    //Temp directories created for testing the camel route
    private static File batchProcessDataDirectory;
    private static File tempInputDirectory, tempConfigDirectory;

    /**
     * Sets up the system properties and Temp directories used by the route.
     * @throws IOException
     */
    @BeforeClass
    public static void setupSysPropsTempResourceDir() throws IOException {
        //Create and Copy the Input dir xslt, etc. files used in the camera trap route
        tempInputDirectory = new File("Input");
        if(!tempInputDirectory.exists()){
            tempInputDirectory.mkdir();
        }

        //The Location of the original Input dir in the project
        File inputSrcDirLoc = new File("../Routes/Camera Trap/Input");

        //Copy the Input src files to the CameraTrap root so the camel route can find them
        FileUtils.copyDirectory(inputSrcDirLoc, tempInputDirectory);

        batchProcessDataDirectory = new File("target/BatchProcessData");
        if(batchProcessDataDirectory.exists()){
            FileUtils.deleteDirectory(batchProcessDataDirectory);
        }
        tempConfigDirectory = new File("Karaf-config");
        if(!tempConfigDirectory.exists()){
            tempConfigDirectory.mkdir();
        }

        FileUtils.copyDirectory(new File("../Routes/Camera Trap/Karaf-config"), tempConfigDirectory);

        //Set the karaf.home property use by the camera trap route
        System.setProperty("karaf.home", "Karaf-config");


        FileInputStream propFile = new FileInputStream( "src/test/resources/test.properties");
        Properties p = new Properties(System.getProperties());
        p.load(propFile);
        System.setProperties(p);
    }


    @Override
    protected String[] loadConfigAdminConfigurationFile() {
        return new String[]{"src/test/resources/test.properties", "edu.si.sidora.karaf"};
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
        if(batchProcessDataDirectory.exists()){
            //FileUtils.deleteDirectory(batchProcessDataDirectory);
        }
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/sidora-batch.xml";
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

    @Test
    public void testMultipartPostWithParametersAndPayload() throws Exception {
        String parentPid = "si:390403";
        String id = "";

        String expectedResponseBody = "<Batch><ParentPID>" + parentPid + "</ParentPID><CorrelationID>" + id + "</CorrelationID></Batch>";

        HttpPost post = new HttpPost("http://localhost:" + PORT_PATH + "/batch/process/addResourceObjects/" + parentPid + "?query=abcd");

        MultipartEntityBuilder builder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.STRICT);

        // Add zip file URL upload
        builder.addTextBody("resourceZipFileURL", "/home/jbirkhimer/IdeaProjects/sidora-services/Sidora-REST/src/test/resources/image-resources.zip", ContentType.TEXT_PLAIN);
        // Add metadata xml file URL upload
        builder.addTextBody("metadataFileURL", "/home/jbirkhimer/IdeaProjects/sidora-services/Sidora-REST/src/test/resources/batch.xml", ContentType.TEXT_PLAIN);
        // Add content model  string
        builder.addTextBody("contentModel", "si:generalImageCModel", ContentType.TEXT_PLAIN);

        post.setEntity(builder.build());

        HttpResponse response = httpClient.execute(post);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String responseBody = EntityUtils.toString(response.getEntity());
        System.out.println("======================== [ RESPONSE ] ========================\n" + responseBody);


        /*HttpResponse response2 = httpClient.execute(post);
        assertEquals(200, response2.getStatusLine().getStatusCode());
        String response2Body = EntityUtils.toString(response2.getEntity());
        System.out.println("======================== [ RESPONSE ] ========================\n" + response2Body);*/

    }

    //@Test
    public void testStatus() throws Exception {
        String parentPid = "si:390403";
        String correlationId = "a1b47c86-0922-4321-b8b3-d4d6abd8d953";

        //HttpPost post = new HttpPost("http://localhost:" + PORT_PATH + "/rest/customerservice/customers/multipart/123?query=abcd");
        HttpGet getClient = new HttpGet("http://localhost:" + PORT_PATH + "/batch/process/addResourceObjects/" + parentPid + "/" + correlationId);

        HttpResponse response = httpClient.execute(getClient);
        assertEquals(200, response.getStatusLine().getStatusCode());
    }

    /*
    @Before
    public void setUpTests() {
        if (httpClient == null) {
            httpClient = HttpClients.createDefault();
        }
    }

    @After
    public void closeHttpClient() throws IOException {
        httpClient.close();
        httpClient = null;
    }

    @Override
    protected String useOverridePropertiesWithConfigAdmin(Dictionary props) throws Exception {
        props.put("api.root.url", BASE_URL);
        return "com.capgemini.example";
    }

    @Test
    public void testRouteReturnsAPostObjectInJson() throws Exception {
        String url = BASE_URL + "/1/blog/post/1";
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        String stringResponse = EntityUtils.toString(response.getEntity());
        //Post blogPost = new ObjectMapper().readValue(stringResponse, Post.class);
        //assertEquals("My Title", blogPost.getTitle());
        //assertEquals(1, blogPost.getId());
    }

    @Test
    public void testRouteReturnsAPostObjectInXML() throws Exception {
        String url = BASE_URL + "/1/blog/post/1";
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Accept", MediaType.APPLICATION_XML);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        String stringResponse = EntityUtils.toString(response.getEntity());
        //JAXBContext jaxbContext = JAXBContext.newInstance(Post.class);
        //Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        //Post blogPost = (Post) unmarshaller.unmarshal(new StringReader(stringResponse));
        //assertEquals("My Title", blogPost.getTitle());
    }*/

    /*@Test
    public void testNewResourceBatchMetadata () throws IOException {
        String parentPid = "si:390403";

        HttpPost post = new HttpPost(BASE_URL + "batch/newBatchResource/" + parentPid);

        //Multipart attachments
        File resourceZip = new File(getClass().getResource("/image-resource.zip").getFile());
        File metadataFile = new File("/batch.xml");
        String metadata = FileUtils.readFileToString(metadataFile);
        File associationFile = new File("/general-image-association");
        String association = FileUtils.readFileToString(associationFile);

        // Create Multipart instance
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        // Add zip file upload
        builder.addBinaryBody("resourceZip", resourceZip, ContentType.create("application/zip"), "image-resources.zip");
        // Add metadata xml file upload
        builder.addTextBody("metadata", metadata, ContentType.APPLICATION_XML);
        // Add image association file upload
        builder.addTextBody("association", association, ContentType.TEXT_PLAIN);
        // Add content model  string
        builder.addTextBody("content-model ", "si:generalImageCModel", ContentType.TEXT_PLAIN);

        HttpEntity httpEntity = builder.build();

        post.setEntity(httpEntity);

        // execute the post request
        System.out.println("executing request " + post.getRequestLine());
        HttpResponse response = httpClient.execute(post);

        // Read the response
        if (response != null) {
            HttpEntity responseEntity = response.getEntity();

            System.out.println("---------------------------------------------------------------");
            System.out.println(response.getStatusLine());

            if (responseEntity != null) {
                // Read the response string if required
                InputStream responseStream = responseEntity.getContent() ;
                if (responseStream != null){
                    BufferedReader br = new BufferedReader (new InputStreamReader (responseStream)) ;
                    String responseLine = br.readLine() ;
                    String tempResponseString = "" ;
                    while (responseLine != null){
                        tempResponseString = tempResponseString + responseLine + System.getProperty("line.separator") ;
                        responseLine = br.readLine() ;
                    }
                    br.close() ;
                    if (tempResponseString.length() > 0){
                        System.out.println(tempResponseString);
                    }
                }
                responseStream.close();
            }

            *//*if (responseEntity != null) {

                System.out.println("Response content length: " + responseEntity.getContentLength());
                System.out.println("Response content type: " + responseEntity.getContentType().getValue());
                System.out.println("Project Rank : " + responseEntity.getContent()..getContent().getRank());
            }
            if (responseEntity != null) {
                responseEntity.consumeContent();
            }*//*
        }

    }*/
}
