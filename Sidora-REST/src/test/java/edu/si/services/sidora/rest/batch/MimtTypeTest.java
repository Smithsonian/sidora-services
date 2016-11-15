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

import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author jbirkhimer
 */
public class MimtTypeTest {

    public static void main(String[] args) throws IOException, URISyntaxException {
        //File path = new File("/home/jbirkhimer/IdeaProjects/sidora-services/Sidora-REST/src/test/resources/mimeType");

        String string = "file:///opt/sidora/smx/Sidora-Batch-Test-Files/audio/581231f5e9bf2_Yamaha-TG100-Whistle-C5.wav";
        File path = new File(string);
        //581231f5e6aa8_Maid_with_the_Flaxen_Hair.mp3

        URI path2 = new URI(string);

        System.out.println(path2 + " || MIME=" + new Tika().detect(path2.getPath()));



        /*File [] files = path.listFiles();
        //System.out.println(files);
        for (int i = 0; i < files.length; i++){
            if (files[i].isFile()){ //this line weeds out other directories/folders

                *//*System.out.println(files[i].getName() + " || MIME=" + Files.probeContentType(files[i].toPath()));

                System.out.println(files[i].getName() + " || MIME=" + new MimetypesFileTypeMap().getContentType(files[i]));

                System.out.println(files[i].getName() + " || MIME=" + MimeUtil.getMimeTypes(files[i]));

                if (!files[i].getName().contains("mkv") && !files[i].getName().contains("csv")) {
                    ContentInfoUtil util = new ContentInfoUtil();
                    System.out.println(files[i].getName() + " || MIME=" + util.findMatch(files[i]).getMimeType());
                }*//*

                System.out.println(files[i].getName() + " || MIME=" + new Tika().detect(files[i]));









                System.out.println("===========================================================");

            }
        }*/
    }

}
