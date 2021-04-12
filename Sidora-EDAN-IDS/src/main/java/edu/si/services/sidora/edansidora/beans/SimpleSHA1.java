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

package edu.si.services.sidora.edansidora.beans;

import org.apache.cxf.common.util.Base64Utility;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * The Class SimpleSHA1.
 */

public class SimpleSHA1 {

    /**
     * Convert to hex.
     *
     * @param data
     *            the data
     *
     * @return the string
     */
    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (byte element : data) {
            int halfbyte = (element >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                }
                else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = element & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * SHA1.
     *
     * @param text
     *            the text
     *
     * @return the string
     *
     * @throws NoSuchAlgorithmException
     *             the no such algorithm exception
     * @throws UnsupportedEncodingException
     *             the unsupported encoding exception
     */
    public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("UTF-8"), 0, text.length());
        sha1hash = md.digest();

        // now convert to bash64
        String returnEncode = convertToHex(sha1hash);

        return returnEncode;
    }

    /**
     * SH a1plus base64.
     *
     * @param text
     *            the text
     *
     * @return the string
     *
     * @throws NoSuchAlgorithmException
     *             the no such algorithm exception
     * @throws UnsupportedEncodingException
     *             the unsupported encoding exception
     */
    public static String SHA1plusBase64(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String sha1text = SHA1(text);
        byte[] b = sha1text.getBytes(Charset.forName("UTF-8"));
        String returnEncode = Base64Utility.encode(b);
        return returnEncode;
    }

}
