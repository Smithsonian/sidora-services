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

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.si.services.sidora.rest.batch.model.BatchResource;
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
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author jbirkhimer
 */
public class BatchXMLResponseOutputTests {
    private static final Logger LOG = LoggerFactory.getLogger(BatchXMLResponseOutputTests.class);

    @Test
    public void requestXMLtest() throws JAXBException, FileNotFoundException {

        JAXBContext contextObj = JAXBContext.newInstance(BatchRequestResponse.class);

        Marshaller marshallerObj = contextObj.createMarshaller();
        marshallerObj.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);


        BatchRequestResponse requestStatus = new BatchRequestResponse("si:390403", "61f8627d-445e-45ae-82dc-78a2518fcde6");

        marshallerObj.marshal(requestStatus, new FileOutputStream("target/BatchRequestResponse.xml"));
    }

    @Test
    public void statusXMLtest() throws JAXBException, FileNotFoundException {

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

        marshallerObj.marshal(batchStatus, new FileOutputStream("target/BatchStatus.xml"));
    }

    @Test
    public void objectMapper() {

        ArrayList<HashMap<String, Object>> requestMap = getRequestMap();

        ArrayList<HashMap<String, Object>> statusResponse = getStatusResponse();


        //Map<String, Object> statusRequestMap = new HashMap<>();
        //statusRequestMap.putAll(requestMap.get(0));

        final ObjectMapper mapper = new ObjectMapper(); // jackson's objectmapper
        final BatchStatus batchStatus = mapper.convertValue(requestMap.get(0), BatchStatus.class);

        LOG.info("BatchRequestResponse Object:{}", batchStatus.toString());

    }

    private ArrayList<HashMap<String, Object>> getStatusResponse() {

        ObjectMapper m = new ObjectMapper();

        BatchResource resource1 = new BatchResource("61f8627d-445e-45ae-82dc-78a2518fcde6", "http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/image/batch_0_ramlani_Hydrangeas.jpg", "si:390403", "si:392370", "si:generalImageCModel", "ramlani", "pdf_batch_111416(0)", true, true, true, true, true, true, false, true, true, true, true, "2016-11-22 11:00:32", "2016-11-22 11:00:38");

        BatchResource resource2 = new BatchResource("61f8627d-445e-45ae-82dc-78a2518fcde6", "http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/image/batch_1_ramlani_Chrysanthemum.jpg", "si:390403", "si:392371", "si:generalImageCModel", "ramlani", "pdf_batch_111416(1)", true, true, true, true, true, true, false, true, true, true, true, "2016-11-22 11:00:32", "2016-11-22 11:00:41");

        BatchResource resource3 = new BatchResource("61f8627d-445e-45ae-82dc-78a2518fcde6", "http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/image/batch_2_ramlani_Desert.jpg", "si:390403", "si:392372", "si:generalImageCModel", "ramlani", "pdf_batch_111416(2)", true, true, true, true, true, true, false, true, true, true, true, "2016-11-22 11:00:32", "2016-11-22 11:00:44");


        // SQL result put in batchRequest camel header
        ArrayList<HashMap<String, Object>> statusResponse = new ArrayList<>();
        statusResponse.add(m.convertValue(resource1, HashMap.class));
        statusResponse.add(m.convertValue(resource2, HashMap.class));
        statusResponse.add(m.convertValue(resource3, HashMap.class));

        return statusResponse;
    }

    private ArrayList<HashMap<String, Object>> getRequestMap() {
        // Setting up request
        HashMap<String, Object> request = new HashMap<>();
        request.put("correlationId", "f469e72e-078d-4422-838f-146d84efb0c9");
        request.put("parentId", "si:390403");
        request.put("resourceFileList", "http://localhost/~jbirkhimer/Sidora-Batch-Test-Files/image/imageFiles.xml");
        request.put("ds_metadata", "http://localhost/~jbirkhimer/Sidora-Batch-Test-Files/image/metadata.xml");
        request.put("ds_sidora", "http://localhost/~jbirkhimer/Sidora-Batch-Test-Files/image/sidora.xml");
        request.put("association", "http://localhost/~jbirkhimer/Sidora-Batch-Test-Files/image/association.xml");
        request.put("resourceOwner", "ramlani");
        request.put("codebookPID", "null");
        request.put("resourceCount", 3);
        request.put("processCount", 3);
        request.put("request_consumed", true);
        request.put("request_complete", true);
        request.put("created", "2016-11-21 13:43:24.0");
        request.put("updated", "2016-11-21 13:43:28.0");

        // SQL result put in batchRequest camel header
        ArrayList<HashMap<String, Object>> requestMap = new ArrayList<>();
        requestMap.add(request);

        return requestMap;
    }
}
