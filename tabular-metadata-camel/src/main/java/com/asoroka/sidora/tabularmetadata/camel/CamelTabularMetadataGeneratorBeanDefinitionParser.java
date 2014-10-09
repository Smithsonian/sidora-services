
package com.asoroka.sidora.tabularmetadata.camel;

import org.w3c.dom.Element;

import com.asoroka.sidora.tabularmetadata.spring.TabularMetadataGeneratorBeanDefinitionParser;

public class CamelTabularMetadataGeneratorBeanDefinitionParser extends TabularMetadataGeneratorBeanDefinitionParser {

    @Override
    protected Class<?> getBeanClass(final Element element) {
        return CamelTabularMetadataGenerator.class;
    }

}
