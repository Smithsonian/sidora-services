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

import edu.si.services.sidora.rest.batch.model.response.BatchRequestResponse;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.dbcp.BasicDataSource;
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
import org.junit.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.xml.bind.JAXBContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**TODO: Fix Tests
 * @author jbirkhimer
 */
@Ignore
public class BatchServiceTest extends CamelBlueprintTestSupport {

    private static final String KARAF_HOME = System.getProperty("karaf.home");

    // Flag to use MySql Server for testing otherwise Derby embedded dB will be used for testing.
    private static final Boolean USE_MYSQL_DB = false;

    private static final String SERVICE_ADDRESS = "/sidora/batch";
    private static final String BASE_URL = "http://localhost:8282" + SERVICE_ADDRESS;

    //Default Test Params
    private static final String correlationId = UUID.randomUUID().toString();
    private static final String parentPid = "si:390403";
    private static final String resourceFileList = "resourceFileList";
    private static final String ds_metadata = "ds_metadata";
    private static final String ds_sidora = "ds_sidora";
    private static final String association = "association";
    private static final String resourceOwner = "batchTestUser";

    private JAXBContext jaxb;
    private CloseableHttpClient httpClient;

    private EmbeddedDatabase db;
    private JdbcTemplate jdbcTemplate;

    private static final Properties props = new Properties();


    @Test
    public void newBatchRequest_addResourceObjects_Test() throws Exception {

        String resourceListXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/audio/audioFiles.xml"));

        String expectedHTTPResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Batch>\n" +
                "    <ParentPID>" + parentPid + "</ParentPID>\n" +
                "    <CorrelationID>"+ correlationId +"</CorrelationID>\n" +
                "</Batch>\n";

        BatchRequestResponse expectedCamelResponseBody = new BatchRequestResponse();
        expectedCamelResponseBody.setParentPID(parentPid);
        expectedCamelResponseBody.setCorrelationId(correlationId);

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(1);

        context.getComponent("sql", SqlComponent.class).setDataSource(db);
        context.getRouteDefinition("BatchProcessResources").autoStartup(false);

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("BatchProcessAddResourceObjects").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("httpGetResourceList").replace().setBody(simple(resourceListXML));

                weaveByToString(".*bean:batchRequestControllerBean.*").replace().setHeader("correlationId", simple(correlationId));

                weaveAddLast().to("mock:result");

            }
        });

        HttpPost post = new HttpPost(BASE_URL + "/addResourceObjects/" + parentPid);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.STRICT);

        // Add filelist xml URL upload
        builder.addTextBody("resourceFileList", resourceFileList, ContentType.TEXT_PLAIN);
        // Add metadata xml file URL upload
        builder.addTextBody("ds_metadata", ds_metadata, ContentType.TEXT_PLAIN);
        // Add sidora xml URL upload
        builder.addTextBody("ds_sidora", ds_sidora, ContentType.TEXT_PLAIN);
        // Add association xml URL upload
        builder.addTextBody("association", association, ContentType.TEXT_PLAIN);
        // Add resourceOwner string
        builder.addTextBody("resourceOwner", resourceOwner, ContentType.TEXT_PLAIN);

        post.setEntity(builder.build());

        HttpResponse response = httpClient.execute(post);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String responseBody = EntityUtils.toString(response.getEntity());
        log.debug("======================== [ RESPONSE ] ========================\n" + responseBody);

        assertEquals(expectedHTTPResponseBody, responseBody);

        log.debug("===============[ DB Requests ]================\n{}", jdbcTemplate.queryForList("select * from sidora.camelBatchRequests"));
        log.debug("===============[ DB Resources ]===============\n{}", jdbcTemplate.queryForList("select * from sidora.camelBatchResources"));

        BatchRequestResponse camelResultBody = (BatchRequestResponse) mockEndpoint.getExchanges().get(0).getIn().getBody();

        assertIsInstanceOf(BatchRequestResponse.class, camelResultBody);
        assertEquals(camelResultBody.getParentPID(), parentPid);
        assertEquals(camelResultBody.getCorrelationId(), correlationId);

        assertMockEndpointsSatisfied();

    }

    /**
     * Testing the request status
     * (Note: The unit tests are using Derby (which is case sensitive) so the sql queries wont work)
     *
     * @throws Exception
     */
    @Test
    public void testStatus() throws Exception {

        String parentPid = "si:123456";
        String correlationId = "b0d7500a-34be-467a-8cbd-599c6c37b522";

        //log.info("===============[ DB Requests ]================\n{}", jdbcTemplate.queryForMap("select * from sidora.camelBatchRequests"));
        //log.info("===============[ DB Resources ]===============\n{}", jdbcTemplate.queryForMap("select * from sidora.camelBatchResources"));

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(1);

        context.getComponent("sql", SqlComponent.class).setDataSource(db);
        context.getRouteDefinition("BatchProcessResources").autoStartup(false);

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("BatchProcessRequestStatus").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                //weaveByToString(".*sql.checkRequestStatus.*").replace().to("sql:select * from sidora.camelBatchRequests where (correlationId = :#correlationId AND parentId = :#parentId)?outputHeader=batchRequest&outputType=SelectList").to("log:{{edu.si.batch}}?level=INFO&multiline=true&showAll=true").to("mock:result").stop();

                //weaveByToString(".*sql.checkRequestStatus.*").replace().log("=========================\n{{sql.checkRequestStatus}}\n=================================").to("mock:result").stop();

                weaveAddLast().to("mock:result");

            }
        });

        HttpGet getClient = new HttpGet(BASE_URL + "/requestStatus/" + parentPid + "/" + correlationId);

        HttpResponse response = httpClient.execute(getClient);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String responseBody = EntityUtils.toString(response.getEntity());
        log.debug("======================== [ RESPONSE ] ========================\n" + responseBody);

        log.info("===============[ DB Requests ]===============\n{}", jdbcTemplate.queryForList("select * from sidora.camelBatchRequests where (\"correlationId\" = '" + correlationId + "' AND \"parentId\" = '" + parentPid + "')"));

        log.info("===============[ DB Resource ]===============\n{}", jdbcTemplate.queryForList("select * from sidora.camelBatchResources where (\"correlationId\" = '" + correlationId + "' AND \"parentId\" = '" + parentPid + "')"));
    }

    @Override
    protected String setConfigAdminInitialConfiguration(Properties configAdmin) {
        configAdmin.putAll(props);
        return "edu.si.sidora.batch";
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry reg = super.createRegistry();

        if (USE_MYSQL_DB) {
            BasicDataSource testMySQLdb = new BasicDataSource();
            testMySQLdb.setDriverClassName("com.mysql.jdbc.Driver");
            testMySQLdb.setUrl("jdbc:mysql://" + props.getProperty("mysql.host") + ":" + props.getProperty("mysql.port") + "/" + props.getProperty("mysql.database") + "?zeroDateTimeBehavior=convertToNull");
            testMySQLdb.setUsername(props.getProperty("mysql.username").toString());
            testMySQLdb.setPassword(props.getProperty("mysql.password").toString());
            reg.bind("dataSource", testMySQLdb);
        } else {
            reg.bind("dataSource", db);
        }

        reg.bind("dataSource", db);

        reg.bind("jsonProvider", org.apache.cxf.jaxrs.provider.json.JSONProvider.class);
        reg.bind("jaxbProvider", org.apache.cxf.jaxrs.provider.JAXBElementProvider.class);

        return reg;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "deploy/sidora-batch.xml";
    }

    protected List<String> loadAdditionalPropertyFiles() {
        return Arrays.asList(KARAF_HOME + "/etc/system.properties", KARAF_HOME + "/etc/edu.si.sidora.batch.cfg", KARAF_HOME + "/sql/batch.process.sql.properties");
    }

    @Override
    public void setUp() throws Exception {
        log.info("===================[ KARAF_HOME = {} ]===================", System.getProperty("karaf.home"));

        List<String> propFileList = loadAdditionalPropertyFiles();
        if (loadAdditionalPropertyFiles() != null) {
            for (String propFile : propFileList) {
                Properties extra = new Properties();
                try {
                    extra.load(new FileInputStream(propFile));
                    props.putAll(extra);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        for (Map.Entry<Object, Object> p : System.getProperties().entrySet()) {
            if (props.containsKey(p.getKey())) {
                props.setProperty(p.getKey().toString(), p.getValue().toString());
            }
        }

        super.setUp();

        httpClient = HttpClientBuilder.create().build();
        //jaxb = JAXBContext.newInstance(CustomerList.class, Customer.class, Order.class, Product.class);

        db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.DERBY).setName("derbyTestDB").addScripts("sql/createAndPopulateDatabase.sql").build();

//        FileInputStream sqlFile = new FileInputStream("src/test/resources/sql/createAndPopulateDatabase.sql");
//        Properties sql = new Properties(System.getProperties());
//        sql.load(sqlFile);

        jdbcTemplate = new JdbcTemplate(db);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        httpClient.close();
        db.shutdown();
    }
}
