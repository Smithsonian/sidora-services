package edu.si.services.beans.velocityToolsHandler;

import static org.junit.Assert.*;

import edu.si.services.beans.velocityToolsHandler.VelocityToolsHandler;
import org.apache.velocity.tools.generic.EscapeTool;
import org.junit.Test;

/**
 * Tests Velocity tools.
 *
 * @author davisda
 */
public class VelocityToolsHandlerTest
{
    @Test
    public void testVelocityEscapeTool() throws Exception
    {
        EscapeTool et = VelocityToolsHandler.getVelocityEscapeTool();
        assertNotNull(et);
    }
}
