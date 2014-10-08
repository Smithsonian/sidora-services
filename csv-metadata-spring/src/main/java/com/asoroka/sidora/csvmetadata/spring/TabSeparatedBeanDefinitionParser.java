
package com.asoroka.sidora.csvmetadata.spring;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

import com.asoroka.sidora.csvmetadata.formats.CsvFormat.TabSeparated;

public class TabSeparatedBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected Class<?> getBeanClass(final Element element) {
        return TabSeparated.class;
    }

    @Override
    protected void doParse(final Element element, final BeanDefinitionBuilder builder) {
        // NO OP no configuration for this beans
    }

}
