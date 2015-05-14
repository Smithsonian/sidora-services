package edu.si.services.beans.cameratrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.camel.builder.RouteBuilder;
//import net.sf.saxon.TransformerFactoryImpl;

/**
 * A Camel Java DSL Router
 */
public class CameraTrapRouteBuilder extends RouteBuilder
{
    static Logger LOG = LoggerFactory.getLogger(CameraTrapRouteBuilder.class);
    //TransformerFactoryImpl tFactory = new TransformerFactoryImpl();

    /**
     * Configure the Camel routing rules Camera Trap...
     */
    public void configure()
    {
        //from("direct:validateSchematron")
        //        .to("schematron:///opt/sidora/servicemix/Input/schemas/DeploymentManifest2014.sch");
    }
}
