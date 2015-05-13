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
        // here is a sample which processes the input files
        // (leaving them in place - see the 'noop' flag)
        // then performs content based routing on the message using XPath
        //from("file:src/data?noop=true")
        //        .choice()
        //        .when(xpath("/person/city = &#39;London&#39;"))
        //        .to("file:target/messages/uk")
        //        .otherwise()
        //        .to("file:target/messages/others");
    }
}
