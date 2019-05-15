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

package edu.si.services.beans.cameratrap;

import com.amazonaws.util.json.JSONObject;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.converter.stream.FileInputStreamCache;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import static org.apache.http.auth.AuthScope.ANY;
import static org.junit.Assume.assumeTrue;

/**
 * @author jbirkhimer
 */
@Ignore
public class FaceBlurTest extends CamelTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(FaceBlurTest.class);

    protected static String SOLR_SERVER;
    protected static int SOLR_PORT;
    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static final String PROJECT_BASE_DIR = System.getProperty("baseDir");
    private static Properties props = new Properties();
    private static CloseableHttpClient client;

    @BeforeClass
    public static void loadConfig() throws Exception {
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

        //props.putAll(System.getProperties());

        LOG.info("Creating Http Client");
        LOG.info("using user: {}, password: {}", props.getProperty("si.fedora.user"), props.getProperty("si.fedora.password"));

        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(ANY, new UsernamePasswordCredentials(props.getProperty("si.fedora.user"), props.getProperty("si.fedora.password")));
        client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
    }


    @AfterClass
    public static void cleanUp() throws Exception {
        LOG.info("Closing Http Client");
        client.close();
    }

    protected static List<String> loadAdditionalPropertyFiles() {
        return Arrays.asList(KARAF_HOME + "/etc/system.properties", KARAF_HOME + "/etc/edu.si.sidora.karaf.cfg", KARAF_HOME + "/etc/edu.si.sidora.emammal.cfg", KARAF_HOME + "/etc/test.properties");
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        return props;
    }

    public static boolean hostAvailabilityCheck(String host, int port) {
        LOG.info("Checking Host Availability: {}", host + ":" + port);
        try (Socket socket = new Socket(new URL(host).getHost(), port)) {
            return true;
        } catch (IOException ex) {
            /* ignore */
        }
        return false;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {

                getContext().getComponent("properties", PropertiesComponent.class).setOverrideProperties(props);

                from("direct:solrTest").routeId("solrTest")
                        .log(LoggingLevel.INFO, "Solr uri = ${header.uri}, query=${header.CamelHttpQuery}")
                        .toD("${header.uri}")
                        .convertBodyTo(String.class)
                        .log(LoggingLevel.INFO, "Solr Response Body:\n${body}")
                        .to("mock:result");

                from("direct:testPythonScript").routeId("pythonTest")
                        .choice()
                            .when().simple("${header.isCtIngest} == 'true'")
                                .log(LoggingLevel.INFO, "Testing CT Ingest Load Image...")
                                .setBody().simple("${header.dsLabel}.JPG")
                                .setHeader("CamelFileAbsolutePath").simple("{{karaf.home}}//wildlife_insights_test_data/unified-test-deployment")
                                .to("reader:file")
                            .endChoice()
                            .otherwise()
                                .log(LoggingLevel.INFO, "Testing Derivatives Load Image...")
                                .process(new Processor() {
                                    @Override
                                    public void process(Exchange exchange) throws Exception {
                                        Message out = exchange.getIn();
                                        String dsLabel = exchange.getIn().getHeader("dsLabel", String.class);
                                        String resourceFilePath = KARAF_HOME + "/wildlife_insights_test_data/unified-test-deployment/" + dsLabel + ".JPG";
                                        log.info("File path: {}", resourceFilePath);
                                        File resourceFile = new File(resourceFilePath);
                                        if (resourceFile.exists()) {
                                            out.setBody(new FileInputStreamCache(resourceFile));
                                        } else {
                                            out.setBody(null);
                                        }
                                    }
                                })
                            .endChoice()
                        .end()
                        .toD("exec:python?args={{si.ct.wi.faceBlur.script}} {{si.ct.wi.faceBlur.blur_value}} {{si.ct.wi.faceBlur.classifier}}")
                        .log(LoggingLevel.INFO, "**********************")
                        .log(LoggingLevel.INFO, "CamelExecStderr:\n${header.CamelExecStderr}")
                        .log(LoggingLevel.INFO, "**********************")
                        .log(LoggingLevel.INFO, "Headers:\n${headers}")
                        .log(LoggingLevel.INFO, "**********************")
                        .to("file:target/output?fileName=${header.dsLabel}_output.JPG")
                        .to("mock:result");

            }
        };
    }

    public Object doGet(String uri, String fileName) {
        Object entityResponse = null;
        HttpEntity entity = null;
        HttpGet httpMethod = new HttpGet(uri);
        try (CloseableHttpResponse response = client.execute(httpMethod)) {
            entity = response.getEntity();

            Integer responseCode = response.getStatusLine().getStatusCode();
            String statusLine = response.getStatusLine().getReasonPhrase();
            log.info("content type: {}", entity.getContentType().getValue());
            log.debug("headers: {}", response.getAllHeaders());

            if (entity.getContentType() != null && entity.getContentType().getValue().equals("image/jpeg")) {
                File file = File.createTempFile(fileName + "_", ".jpg");
                file.deleteOnExit();
                if (entity != null) {
                    try (FileOutputStream outstream = new FileOutputStream(file)) {
                        entity.writeTo(outstream);
                    }
                }
                entityResponse = file;
            } else {
                entityResponse = EntityUtils.toString(entity, "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return entityResponse;
    }

    public Object doPost(String uri, File uploadFile) {
        Object entityResponse = null;

        HttpPost httpMethod = new HttpPost(uri);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addPart("datafile", new FileBody(uploadFile));

        HttpEntity entity = builder.build();
        httpMethod.setEntity(entity);

        try (CloseableHttpResponse response = client.execute(httpMethod)) {
            entity = response.getEntity();

            Integer responseCode = response.getStatusLine().getStatusCode();
            String statusLine = response.getStatusLine().getReasonPhrase();
            log.info(entity.getContentType().toString());
            log.debug("headers: {}", response.getAllHeaders());

            if (entity.getContentType().getValue().equals("image/jpeg")) {
                entityResponse = entity.getContent();
            } else {
                entityResponse = EntityUtils.toString(entity, "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entityResponse;
    }

    public <T> T xpath(String source, String expression, QName qName) {
        Object result = null;
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(source)));
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            xpath.setNamespaceContext(new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    if (prefix == null) {
                        throw new IllegalArgumentException("No prefix provided!");
                    } else if (prefix.equals("fsmgmt")) {
                        return "http://www.fedora.info/definitions/1/0/management/";
                    } else if (prefix.equals("fedora")) {
                        return "info:fedora/fedora-system:def/relations-external#";
                    } else if (prefix.equals("rdf")) {
                        return "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
                    } else if (prefix.equals("ri")) {
                        return "http://www.w3.org/2005/sparql-results#";
                    } else if (prefix.equals("fits")) {
                        return "http://hul.harvard.edu/ois/xml/ns/fits/fits_output";
                    } else {
                        return XMLConstants.NULL_NS_URI;
                    }
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    return null;
                }

                @Override
                public Iterator getPrefixes(String namespaceURI) {
                    return null;
                }
            });
            result = xpath.evaluate(expression, document, qName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (T) result;
    }

    @Test
    public void testPythonFaceBlurScript() throws Exception {
        deleteDirectory(KARAF_HOME + "/output");
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(2);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("dsLabel", "testWildLifeInsightsDeploymentIds1i4");
        exchange.getIn().setHeader("isCtIngest", "false");

        template.send("direct:testPythonScript", exchange);

        exchange.getIn().setHeader("dsLabel", "testWildLifeInsightsDeploymentIds1i3");
        exchange.getIn().setHeader("isCtIngest", "true");

        template.send("direct:testPythonScript", exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Java version of UpdateFGDC.py script for updating CT deployment FGDC datastream with Camera Make and Model
     * will save updated FGDC to local file
     *
     * @requires hostURL and fitsHost to be set
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testUpdateDeploymentFGDC() throws Exception {

        String hostURL = null;
        String fitsHost = null;

        assumeTrue("Must Provide host url!!!", hostURL != null);
        assumeTrue("Must Provide host url!!!", fitsHost != null);

        String fusekiQuery = URLEncoder.encode("SELECT ?ctPID FROM <info:edu.si.fedora#ri> WHERE {?ctPID <info:fedora/fedora-system:def/model#hasModel> <info:fedora/si:cameraTrapCModel>.}", "UTF-8");

        String fusekiResult = (String) doGet("http://" + hostURL + ":9080/fuseki/fedora3?output=xml&query=" + fusekiQuery, null);

        log.debug("fusekiResult: {}", fusekiResult);

        NodeList deploymentPids = xpath(fusekiResult, "//ri:sparql/ri:results/ri:result/ri:binding/ri:uri/text()", XPathConstants.NODESET);
        Integer deploymentPidCount = deploymentPids.getLength();

        log.info("deployment count: {}", deploymentPidCount);

        for (int k = 0; k < deploymentPids.getLength(); k++) {
            Node fusekiPidListNode = deploymentPids.item(k);

            String deploymentPID = StringUtils.substringAfter(fusekiPidListNode.getTextContent(), "info:fedora/");
            log.info("deploymentPID: {}, {}/{}", deploymentPID, k + 1, deploymentPidCount);

            String deploymentobjectDatastreams = (String) doGet("http://" + hostURL + ":8080/fedora/objects/" + deploymentPID + "/datastreams?format=xml", null);
            boolean hasFGDC = xpath(deploymentobjectDatastreams, "boolean(//@dsid='FGDC')", XPathConstants.BOOLEAN);

            if (hasFGDC) {
                String deploymentRelsExt = (String) doGet("http://" + hostURL + ":8080/fedora/objects/" + deploymentPID + "/datastreams/RELS-EXT/content", null);
                String deploymentFGDC = (String) doGet("http://" + hostURL + ":8080/fedora/objects/" + deploymentPID + "/datastreams/FGDC/content", null);

                log.debug("deployment RELS-EXT: {}", deploymentRelsExt);
                log.debug("deployment FGDC: {}", deploymentFGDC);

                String deploymentImgResourcePid = xpath(deploymentRelsExt, "//fedora:hasResource[1]/@rdf:resource/substring-after(., 'info:fedora/')", XPathConstants.STRING);

                log.info("deployment resource pid: {}", deploymentImgResourcePid);
                if (deploymentImgResourcePid == null || deploymentImgResourcePid.isEmpty()) {
                    log.error("Problem deployment pid: {}", deploymentPID);
                } else {

                    String objDatastreamProfile = (String) doGet("http://" + hostURL + ":8080/fedora/objects/" + deploymentImgResourcePid + "/datastreams/OBJ?format=xml", null);

                    log.debug("objDatastreamProfile: {}", objDatastreamProfile);

                    String objMIME = xpath(objDatastreamProfile, "//fsmgmt:datastreamProfile/fsmgmt:dsMIME/text()", XPathConstants.STRING);
                    String objLabel = xpath(objDatastreamProfile, "//fsmgmt:datastreamProfile/fsmgmt:dsLabel/text()", XPathConstants.STRING);
                    log.info("resource OBJ mime type: {}", objMIME);

                    if (objMIME.contains("image")) {
                        String imgObjURL = "http://" + hostURL + ":8080/fedora/objects/" + deploymentImgResourcePid + "/datastreams/OBJ/content";
                        log.info("imgObjURL: {}", imgObjURL);

                        File tmpImgResourceFile = (File) doGet(imgObjURL, objLabel);

                        if (tmpImgResourceFile.exists()) {

                            String deploymentImgResourceFITS = (String) doPost("http://" + fitsHost + ":8080/fits-1.1.3/examine?file=" + imgObjURL, tmpImgResourceFile);
                            log.debug("deployment resource FITS: {}", deploymentImgResourceFITS);
                            tmpImgResourceFile.delete();

                            String cameraMake = xpath(deploymentImgResourceFITS, "/fits:fits/fits:metadata/fits:image/fits:digitalCameraManufacturer/text()", XPathConstants.STRING);
                            String cameraModel = xpath(deploymentImgResourceFITS, "/fits:fits/fits:metadata/fits:image/fits:digitalCameraModelName/text()", XPathConstants.STRING);

                            log.info("Camera Make: {}, Model: {}", cameraMake, cameraModel);

                            TransformerFactory factory = TransformerFactory.newInstance();
                            Source xslt = new StreamSource(new File(PROJECT_BASE_DIR + "/target/Wildlife-Insights/updateManifestDeployment.xsl"));
                            Transformer transformer = factory.newTransformer(xslt);
                            transformer.setParameter("cameraMake", cameraMake);
                            transformer.setParameter("cameraModel", cameraModel);

                            Source xmlInSource = new StreamSource(new StringReader(deploymentFGDC));

//                            StringWriter xmlOutWriter = new StringWriter();
//                        transformer.transform(xmlInSource, new StreamResult(xmlOutWriter));
//                        log.info("FGDC Output:\n{}", xmlOutWriter.toString());
                            transformer.transform(xmlInSource, new StreamResult(new File(PROJECT_BASE_DIR+ "/src/test/resources/wildlife_insights_test_data/update-script/fgdc_output/" + deploymentPID + "_FGDC_output.xml")));
                        }

                    } else {
                        log.error("resource problem: mimeType: {}, pid: {}", objMIME, deploymentImgResourcePid);
                    }
                }
            } else {
                log.error("Problem deployment has no FGDC for pid: {}", deploymentPID);
            }
            log.info("=======================================================");
        }
    }

    /**
     * Get pids for images that have SpeciesScientificName for FaceBlur processing
     * SpeciesScientificName of {'Camera Trapper', 'False trigger', 'Homo sapien', 'Homo sapiens'}
     * @throws Exception
     */
    @Test
    @Ignore
    public void testGetTestImagePids() throws Exception {

        String hostURL = null;

        assumeTrue("Must Provide host url!!!", hostURL != null);

        String fileName = PROJECT_BASE_DIR + "/src/test/resources/wildlife_insights_test_data/ct_pidList.txt";
        log.info("File path: {}", fileName);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        SOLR_PORT = 8090;
        SOLR_SERVER = "http://" + hostURL + "";
        String uri = SOLR_SERVER + ":" + SOLR_PORT + "/solr/gsearch_sianct/select";
        String solrQuery = "speciesTaxonrv:(\"Homo sapien\" \"Camera Trapper\" \"Homo sapiens\")";
//        String solrQuery = "projectLabel:\"Recreation Effects on mid-Atlantic Wildlife\"";
        String fq1 = "speciesTaxonrv:(\"Homo sapien\" \"Camera Trapper\" \"Homo sapiens\")";
        String fq2 = "datasetLabel:\"Researcher Observations\"";
        String httpQuery = "?q=" + URLEncoder.encode(solrQuery, "UTF-8") + "&fq=" + URLEncoder.encode(fq1, "UTF-8") + "&fq=" + URLEncoder.encode(fq2, "UTF-8") + "&wt=json&indent=true&rows=60";

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelHttpMethod", "GET");
        exchange.getIn().setHeader("CamelHttpQuery", httpQuery);
        exchange.getIn().setHeader("uri", uri);

        template.send("direct:solrTest", exchange);

        assertMockEndpointsSatisfied();

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        int numFound = json.getJSONObject("response").getInt("numFound");

        int count = json.getJSONObject("response").getJSONArray("docs").length();

        log.info("numFound: {}, count: {}", numFound, count);

        /*String fileName = PROJECT_BASE_DIR + "/src/test/resources/wildlife_insights_test_data/ct_pidList.txt";
        log.info("File path: {}", fileName);*/
        File file = new File(fileName);
        if (!file.exists()) file.createNewFile();

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true))) {
            for (int i = 0; i < count; i++) {
                String ctPID = json.getJSONObject("response").getJSONArray("docs").getJSONObject(i).getString("ctPID");
                LOG.info("Found ctPID: {}", ctPID);

                String manifest = (String) doGet("http://" + hostURL + ":8080/fedora/objects/" + ctPID + "/datastreams/MANIFEST/content", null);

                NodeList manifestResultList = xpath(manifest, "//CameraTrapDeployment/ImageSequence/VolunteerIdentifications/Identification/SpeciesScientificName[contains(\"'Camera Trapper', 'False trigger', 'Homo sapien', 'Homo sapiens'\", text())]/parent::Identification/parent::VolunteerIdentifications/parent::ImageSequence/Image/ImageId", XPathConstants.NODESET);

                log.info("manifest result list count: {}", manifestResultList.getLength());

                for (int j = 0; j < manifestResultList.getLength(); j++) {
                    Node manifestListNode = manifestResultList.item(j);
                    log.debug("manifest result list node: {}", manifestListNode.getTextContent());

                    String fusekiQuery = URLEncoder.encode("SELECT ?ctPID FROM <info:edu.si.fedora#ri> WHERE { ?ctPID <info:fedora/fedora-system:def/model#label> '" + manifestListNode.getTextContent() + "'.}", "UTF-8");

                    String fusekiResult = (String) doGet("http://" + hostURL + ":9080/fuseki/fedora3?output=xml&query=" + fusekiQuery, null);

                    log.debug("fusekiResult for label: {}\n{}", manifestListNode.getTextContent(), fusekiResult);

                    NodeList fusekiPidList = xpath(fusekiResult, "//ri:sparql/ri:results/ri:result/ri:binding/ri:uri/text()", XPathConstants.NODESET);

                    log.debug("fuseki xpath result count: {}", fusekiPidList.getLength());

                    for (int k = 0; k < fusekiPidList.getLength(); k++) {
                        Node fusekiPidListNode = fusekiPidList.item(k);

                        String imgPID = StringUtils.substringAfter(fusekiPidListNode.getTextContent(), "info:fedora/");
                        log.info("imgPID: {}, {}/{}", imgPID, j, manifestResultList.getLength());

                        bufferedWriter.append("\n" + imgPID);
                        bufferedWriter.flush();

                    /*File output = new File(PROJECT_BASE_DIR + "/src/test/resources/wildlife_insights_test_data/output/" + imgPID + "_" + "imageId_" + manifestListNode.getTextContent() + ".jpg");
                    FileUtils.copyURLToFile(new URL("http://" + hostURL + ":8080/fedora/objects/" + imgPID + "/datastreams/OBJ/content"), output);*/
                    }
                }
            }
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
    }


}
