
package com.asoroka.sidora.csvmetadata.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @author ajs6f
 */
public class CsvMetadataNamespaceHandler extends NamespaceHandlerSupport {

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.xml.NamespaceHandler#init()
     */
    @Override
    public void init() {
        registerBeanDefinitionParser("CsvMetadataGenerator", new CsvMetadataGeneratorBeanDefinitionParser());
        registerBeanDefinitionParser("FractionHeuristic", new FractionHeuristicBeanDefinitionParser());
        registerBeanDefinitionParser("TsvFormat", new TabSeparatedBeanDefinitionParser());

    }

}
