
package com.asoroka.sidora.tabularmetadata.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Responsible for registering bean definition code.
 * 
 * @author ajs6f
 */
public class CsvMetadataNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("CsvMetadataGenerator", new CsvMetadataGeneratorBeanDefinitionParser());
        registerBeanDefinitionParser("FractionHeuristic", new FractionHeuristicBeanDefinitionParser());
        registerBeanDefinitionParser("TsvFormat", new TabSeparatedBeanDefinitionParser());

    }

}
