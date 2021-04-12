package edu.si.services.sidora.derivatives;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ImportResource;

import javax.xml.xpath.XPathFactory;

@SpringBootApplication
@EnableConfigurationProperties
//@ImportResource({"file:config/camel/derivatives-route.xml"})
public class SidoraDerivativesApplication {

    public static void main(String[] args) {
        System.setProperty(XPathFactory.DEFAULT_PROPERTY_NAME + ":" + "http://saxon.sf.net/jaxp/xpath/om", "net.sf.saxon.xpath.XPathFactoryImpl");
        SpringApplication.run(SidoraDerivativesApplication.class, args);
    }

}
