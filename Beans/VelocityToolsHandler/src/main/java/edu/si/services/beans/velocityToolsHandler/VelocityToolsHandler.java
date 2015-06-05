package edu.si.services.beans.velocityToolsHandler;

import org.apache.velocity.tools.generic.EscapeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Converts an Excel spreadsheet (workbook) into a CSV. This class makes the following assumptions;
 * @author Mark B
 * @author davsida
 *
 * Derived from: http://svn.apache.org/repos/asf/poi/trunk/src/examples/src/org/apache/poi/ss/examples/ToCSV.java
 */
public class VelocityToolsHandler
{
    private static final Logger logger = LoggerFactory.getLogger(VelocityToolsHandler.class);

    public static EscapeTool getVelocityEscapeTool()
    {
        return new EscapeTool();
    }
}