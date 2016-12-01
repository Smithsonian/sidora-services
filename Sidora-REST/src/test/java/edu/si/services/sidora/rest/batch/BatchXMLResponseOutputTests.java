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

import edu.si.services.sidora.rest.batch.model.responce.BatchRequestResponse;
import edu.si.services.sidora.rest.batch.model.status.BatchStatus;
import edu.si.services.sidora.rest.batch.model.status.ResourceStatus;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;


/**
 * @author jbirkhimer
 */
public class BatchXMLResponseOutputTests {
    private static final Logger LOG = LoggerFactory.getLogger(BatchXMLResponseOutputTests.class);

    @Test
    public void requestXMLtest() throws JAXBException, FileNotFoundException {
        String expectedOutput = getExpectedOutput("request");

        StringWriter sw = new StringWriter();

        JAXBContext contextObj = JAXBContext.newInstance(BatchRequestResponse.class);

        Marshaller marshallerObj = contextObj.createMarshaller();
        marshallerObj.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        BatchRequestResponse requestStatus = new BatchRequestResponse("si:390403", "61f8627d-445e-45ae-82dc-78a2518fcde6");

        //marshallerObj.marshal(requestStatus, new FileOutputStream("target/BatchRequestResponse.xml"));
        marshallerObj.marshal(requestStatus, sw);

        LOG.info("Request XML Generated:\n{}", sw.toString());

        assertEquals(expectedOutput, sw.toString());
    }

    @Test
    public void statusXMLtest() throws JAXBException, FileNotFoundException {

        String expectedOutput = getExpectedOutput("status");

        StringWriter sw = new StringWriter();

        JAXBContext contextObj = JAXBContext.newInstance(BatchStatus.class);

        Marshaller marshallerObj = contextObj.createMarshaller();
        marshallerObj.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);


        ResourceStatus resource1 = new ResourceStatus("http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/image/batch_0_ramlani_Hydrangeas.jpg", "si:392370", "pdf_batch_111416(0)", true, true, true, true, true, true, true, false, true, true);

        ResourceStatus resource2 = new ResourceStatus("http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/image/batch_0_ramlani_Chrysanthemum.jpg", "si:392371", "pdf_batch_111416(0)", true, true, true, true, true, true, false, true, true, true);

        ResourceStatus resource3 = new ResourceStatus("http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/image/batch_0_ramlani_Desert.jpg", "si:392372", "pdf_batch_111416(0)", true, true, true, true, true, true, false, true, true, true);

        ArrayList<ResourceStatus> list = new ArrayList<>();
        list.add(resource1);
        list.add(resource2);
        list.add(resource3);

        BatchStatus batchStatus = new BatchStatus("si:390403", "ramlani", "61f8627d-445e-45ae-82dc-78a2518fcde6", 3, 3, true, "si:generalImageCModel", null, list);

        //marshallerObj.marshal(batchStatus, new FileOutputStream("target/BatchStatus.xml"));
        marshallerObj.marshal(batchStatus, sw);

        LOG.info("Status XML Generated:\n{}", sw.toString());

        assertEquals(expectedOutput, sw.toString());
    }

    private String getExpectedOutput(String expected) {
        String expectedResult = "";

        if (expected.equals("request")) {
            expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Batch>\n" +
                    "    <ParentPID>si:390403</ParentPID>\n" +
                    "    <CorrelationID>61f8627d-445e-45ae-82dc-78a2518fcde6</CorrelationID>\n" +
                    "</Batch>\n";
        } else if (expected.equals("status")) {
            expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Batch>\n" +
                    "    <BatchDone>true</BatchDone>\n" +
                    "    <CorrelationID>61f8627d-445e-45ae-82dc-78a2518fcde6</CorrelationID>\n" +
                    "    <ParentPID>si:390403</ParentPID>\n" +
                    "    <resourceOwner>ramlani</resourceOwner>\n" +
                    "    <ResourceCount>3</ResourceCount>\n" +
                    "    <ResourcesProcessed>3</ResourcesProcessed>\n" +
                    "    <contentModel>si:generalImageCModel</contentModel>\n" +
                    "    <resources>\n" +
                    "        <resource>\n" +
                    "            <file>http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/image/batch_0_ramlani_Hydrangeas.jpg</file>\n" +
                    "            <pid>si:392370</pid>\n" +
                    "            <title>pdf_batch_111416(0)</title>\n" +
                    "            <resourceObjectCreated>true</resourceObjectCreated>\n" +
                    "            <dsDcCreated>true</dsDcCreated>\n" +
                    "            <dsRelsExtCreated>true</dsRelsExtCreated>\n" +
                    "            <dsMetadata>true</dsMetadata>\n" +
                    "            <dsObjCreated>true</dsObjCreated>\n" +
                    "            <dsTnCreated>true</dsTnCreated>\n" +
                    "            <dsSidoraCreated>true</dsSidoraCreated>\n" +
                    "            <parentChildRelationshipCreated>false</parentChildRelationshipCreated>\n" +
                    "            <codebookRelationshipCreated>true</codebookRelationshipCreated>\n" +
                    "            <complete>true</complete>\n" +
                    "        </resource>\n" +
                    "        <resource>\n" +
                    "            <file>http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/image/batch_0_ramlani_Chrysanthemum.jpg</file>\n" +
                    "            <pid>si:392371</pid>\n" +
                    "            <title>pdf_batch_111416(0)</title>\n" +
                    "            <resourceObjectCreated>true</resourceObjectCreated>\n" +
                    "            <dsDcCreated>true</dsDcCreated>\n" +
                    "            <dsRelsExtCreated>true</dsRelsExtCreated>\n" +
                    "            <dsMetadata>true</dsMetadata>\n" +
                    "            <dsObjCreated>true</dsObjCreated>\n" +
                    "            <dsTnCreated>true</dsTnCreated>\n" +
                    "            <dsSidoraCreated>false</dsSidoraCreated>\n" +
                    "            <parentChildRelationshipCreated>true</parentChildRelationshipCreated>\n" +
                    "            <codebookRelationshipCreated>true</codebookRelationshipCreated>\n" +
                    "            <complete>true</complete>\n" +
                    "        </resource>\n" +
                    "        <resource>\n" +
                    "            <file>http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/image/batch_0_ramlani_Desert.jpg</file>\n" +
                    "            <pid>si:392372</pid>\n" +
                    "            <title>pdf_batch_111416(0)</title>\n" +
                    "            <resourceObjectCreated>true</resourceObjectCreated>\n" +
                    "            <dsDcCreated>true</dsDcCreated>\n" +
                    "            <dsRelsExtCreated>true</dsRelsExtCreated>\n" +
                    "            <dsMetadata>true</dsMetadata>\n" +
                    "            <dsObjCreated>true</dsObjCreated>\n" +
                    "            <dsTnCreated>true</dsTnCreated>\n" +
                    "            <dsSidoraCreated>false</dsSidoraCreated>\n" +
                    "            <parentChildRelationshipCreated>true</parentChildRelationshipCreated>\n" +
                    "            <codebookRelationshipCreated>true</codebookRelationshipCreated>\n" +
                    "            <complete>true</complete>\n" +
                    "        </resource>\n" +
                    "    </resources>\n" +
                    "</Batch>\n";
        }
        return expectedResult;
    }

}
