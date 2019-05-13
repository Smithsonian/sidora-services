package edu.si.services.solr;

import de.codecentric.boot.admin.config.EnableAdminServer;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, JmsAutoConfiguration.class})
@EnableAdminServer
@EnableDiscoveryClient
public class SiServicesSidoraSolrApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiServicesSidoraSolrApplication.class, args);
    }

    @Bean
    public ServletRegistrationBean servletRegistrationBean(ApplicationContext context) {
        return new ServletRegistrationBean(new CXFServlet(), "/sidora/*");
    }
}