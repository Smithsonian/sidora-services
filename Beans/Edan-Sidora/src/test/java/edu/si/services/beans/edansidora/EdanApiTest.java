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

package edu.si.services.beans.edansidora;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URLEncoder;
import java.util.Map;

/**
 * @author jbirkhimer
 */
public class EdanApiTest {

    private static final Logger LOG = LoggerFactory.getLogger(EdanApiTest.class);
    private static Configuration config = null;
    private String defaultTestProperties = "src/test/resources/test.properties";

    @Before
    public void setUp() throws ConfigurationException {
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.fileBased().setFile(new File(defaultTestProperties)));
        config = builder.getConfiguration();
        builder.save();
    }

    @Test
    public void edanApiTest() {
        LOG.info("Hello World");

        EdanApiBean edanApiBean = new EdanApiBean();
        edanApiBean.setServer(config.getString("si.ct.uscbi.server"));
        edanApiBean.setAppId(config.getString("si.ct.uscbi.appId"));
        edanApiBean.setEdanKey(config.getString("si.ct.uscbi.edanKey"));

        LOG.info("Server={}, appId={}, edanKay={}", edanApiBean.getServer(), edanApiBean.getAppId(), edanApiBean.getEdanKey());

        edanApiBean.startConnection();

        if (edanApiBean.getAppId() == "APPID") {
            LOG.info("Fill in server information in EdanApi.java source file in order to run a test.");
            return;
        }
        String sampleJsonContent = ""
                +"\n{"
                +"\n \"project_id\": \"p125\","
                +"\n \"project_name\": \"Sample Triangle Camera Trap Survey Project\","
                +"\n \"sub_project_id\": \"sp818\","
                +"\n \"sub_project_name\": \"Triangle Wild\","
                +"\n \"deployment_id\": \"d18981\","
                +"\n \"deployment_name\": \"RaleighBYO 416\","
                +"\n \"image_sequence_id\": \"d18981s2\","
                +"\n \"image\": {"
                +"\n   \"id\": \"emammal_ct:1696705\","
                +"\n   \"online_media\": {"
                +"\n     \"mediaCount\": 1,"
                +"\n     \"media\": ["
                +"\n       {"
                +"\n         \"content\": \"emammal_ct:1696705\","
                +"\n         \"idsId\": \"emammal_ct:1696705\","
                +"\n         \"type\": \"Images\","
                +"\n         \"caption\": \"Camera Trap Image White-tailed Deer\""
                +"\n       }"
                +"\n     ]"
                +"\n   },"
                +"\n   \"date_time\": \"2016-02-24 18:45:30\","
                +"\n   \"photo_type\": \"animal\","
                +"\n   \"photo_type_identified_by\": \"\","
                +"\n   \"interest_ranking\": \"None\""
                +"\n"
                +"\n },"
                +"\n \"image_identifications\": ["
                +"\n  {"
                +"\n    \"iucn_id\": \"42394\","
                +"\n    \"species_scientific_name\": \"Odocoileus virginianus\","
                +"\n    \"individual_animal_notes\": \"\","
                +"\n    \"species_common_name\": \"White-tailed Deer\","
                +"\n    \"count\": 1,"
                +"\n    \"age\": \"\","
                +"\n    \"sex\": \"\","
                +"\n    \"individual_id\": \"\","
                +"\n    \"animal_recognizable\": \"\""
                +"\n  }"
                +"\n ]"
                +"\n}";

        String sampleJson = "{\"type\":\"gg\",\"content\":"+sampleJsonContent+",\"status\":1,\"title\":\"Emammal test\",\"publicSearch\":false}";
        String edanUri = "content=" + URLEncoder.encode(sampleJson).replaceAll("\\+", "%20");
        Map<String, String> ma = edanApiBean.sendRequest(edanUri, "content/v1.1/admincontent/createContent.htm");

        for(Map.Entry<String, String> entry : ma.entrySet()) {
            System.out.println("K:" + entry.getKey() + "\tval:" + entry.getValue().replaceAll("\\\\n", "\n"));
            System.out.println("=====================================================================================");
        }
    }
}
