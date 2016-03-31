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
package edu.si.services.fedorarepo;

/**
 * Custom exception class for the Fedora Repository search queries.  This class will be used to handle exception
 * that are more business logic related during the Fedora Client interactions.
 *
 * @author parkjohn
 */
public class FedoraObjectNotFoundException extends Exception{

    /**
     * Same behavior is as the super class
     */
    public FedoraObjectNotFoundException() {
        super();
    }

    /**
     * Same behavior is as the super class
     *
     * @param message the detailed message; same as the super class
     */
    public FedoraObjectNotFoundException(String message) {
        super(message);
    }

    /**
     * Same behavior is as the super class
     *
     * @param cause the cause of exception; same as the super class
     */
    public FedoraObjectNotFoundException(Throwable cause) {
        super(cause);
    }

    /**
     * Same behavior is as the super class
     *
     * @param message the detailed message; same as the super class
     * @param cause the cause of exception; same as the super class
     */
    public FedoraObjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
