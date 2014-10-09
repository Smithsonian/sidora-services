
package com.asoroka.sidora.tabularmetadata.camel;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class TabularMetadataNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("TabularMetadataGenerator",
                new CamelTabularMetadataGeneratorBeanDefinitionParser());
    }
}
