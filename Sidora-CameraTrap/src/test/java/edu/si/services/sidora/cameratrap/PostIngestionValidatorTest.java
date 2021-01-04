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

package edu.si.services.sidora.cameratrap;

import edu.si.services.sidora.cameratrap.PostIngestionValidator;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit testing for the PostIngestionValidator class.
 *
 * @author parkjohn
 */
public class PostIngestionValidatorTest extends CamelTestSupport {

    private final PostIngestionValidator validator = new PostIngestionValidator();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Testing illegal argument exception thrown when null passed in for the expected Fedora Object's DSIDs into the validator
     *
     */
    @Test
    public void testValidateDatastreamTypeCheckMissingArgument() {
        thrown.expect(IllegalArgumentException.class);

        validator.validateDatastreamExists(null,"OBJ, TN");
    }

    /**
     * Testing illegal argument exception thrown when empty string passed in for the found Fedora Object's DSIDs into the validator
     *
     */
    @Test
    public void testValidateFedoraDSIDMissingArgument() {
        thrown.expect(IllegalArgumentException.class);

        validator.validateDatastreamExists("OBJ, TN", " ");
    }

    /**
     * Testing with various size and order of collection elements to compare between expected and found DSIDs using the validator
     *
     */
    @Test
    public void testValidateDatastreamExists() {

        String dataStreamTypesToCheck;
        String foundDatastreams;
        boolean result;

        //single element test
        dataStreamTypesToCheck = "OBJ";
        foundDatastreams = "OBJ";
        result = validator.validateDatastreamExists(dataStreamTypesToCheck, foundDatastreams);
        assertEquals(result, true);

        //multiple elements test
        dataStreamTypesToCheck = "OBJ,TN";
        foundDatastreams = "OBJ,TN";
        result = validator.validateDatastreamExists(dataStreamTypesToCheck, foundDatastreams);
        assertEquals(result, true);

        //mixed order test
        dataStreamTypesToCheck = "OBJ,DC,TN";
        foundDatastreams = "DC,OBJ,TN";
        result = validator.validateDatastreamExists(dataStreamTypesToCheck, foundDatastreams);
        assertEquals(result, true);

        //mixed order and empty spaces test
        dataStreamTypesToCheck = "OBJ,DC,TN";
        foundDatastreams = "DC,  OBJ  , TN  ";
        result = validator.validateDatastreamExists(dataStreamTypesToCheck, foundDatastreams);
        assertEquals(result, true);
    }

    /**
     * Testing if SIDORA datastream found in the available datastreams
     *
     */
    @Test
    public void testValidateDatastreamWhenSidoraDSExists() {

        String dataStreamTypesToCheck;
        String foundDatastreams;
        boolean result;

        //ignore SIDORA datastream when it is found from Fedora per business logic
        dataStreamTypesToCheck = "OBJ,DC,TN";
        foundDatastreams = "DC, OBJ, TN, SIDORA";
        result = validator.validateDatastreamExists(dataStreamTypesToCheck, foundDatastreams);
        assertEquals(result, true);

        //but the otherway around should fail the test
        dataStreamTypesToCheck = "DC, OBJ, TN, SIDORA";
        foundDatastreams = "OBJ,DC,TN";
        result = validator.validateDatastreamExists(dataStreamTypesToCheck, foundDatastreams);
        assertEquals(result, false);
    }

    /**
     * Testing case insensitivity for datastream checks
     *
     */
    @Test
    public void testValidateDatastreamCaseSensitivity() {

        String dataStreamTypesToCheck;
        String foundDatastreams;
        boolean result;

        dataStreamTypesToCheck = "OBJ,DC,TN";
        foundDatastreams = "dc, obj, Tn, SIdora";
        result = validator.validateDatastreamExists(dataStreamTypesToCheck, foundDatastreams);
        assertEquals(result, true);

    }

}
