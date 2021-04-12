/*
 * Copyright 2018-2019 Smithsonian Institution.
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

package edu.si.services.camel.thumbnailator;

import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestParamType;

import java.util.Base64;

/**
 * @author jbirkhimer
 */
public class ThumbnailatorRoutBuilder extends RouteBuilder {

    @PropertyInject(value = "si.fedora.user")
    private String fedoraUser;

    @PropertyInject(value = "si.fedora.password")
    private String fedoraPasword;

    @Override
    public void configure() throws Exception {

        restConfiguration().component("jetty").host("localhost").port("{{thumbnailator.jetty.port}}");

        rest("/thumbnailator")
                .post()
                    //.param().name("width").type(RestParamType.query).defaultValue("200").description("Thumbnail width").endParam()
                    //.param().name("height").type(RestParamType.query).defaultValue("150").description("Thumbnail height").endParam()
                    .to("direct:createThumbnail")
                .put("/{tnPID}").to("direct:addThumbnail");

        from("direct:createThumbnail").id("ThumbnailatorCreate")
                .to("thumbnailator:image?keepRatio=true&amp;size=(200,150)");

        from("direct:addThumbnail").id("ThumbnailatorAdd")
                .to("thumbnailator:image?keepRatio=true&amp;size=(200,150)")

                .setHeader("CamelHttpMethod").constant("POST")
                .setHeader("Authorization").simple("Basic " + Base64.getEncoder().encodeToString((fedoraUser + ":" + fedoraPasword).getBytes("UTF-8")), String.class)
                .toD("http://localhost:8080/fedora/objects/${header.tnPID}/datastreams/TN?mimeType=image/jpeg&controlGroup=M&dsLabel=Thumbnail&versionable=false");
    }
}
