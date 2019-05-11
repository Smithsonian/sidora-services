package edu.si.services.solr;

import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Component;

/** Class for autowiring camel beans and config from spring xml file
 * @author jbirkhimer
 */
@Component
@ImportResource({"classpath:camel/sidora-solr-spring-boot-context.xml"})
public class CamelXmlRouteConfig {
    //empty class
}