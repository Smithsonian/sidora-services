
package com.asoroka.sidora.csvmetadata.spring;

import static java.lang.Integer.parseInt;
import static org.springframework.beans.factory.support.AbstractBeanDefinition.AUTOWIRE_BY_TYPE;
import static org.springframework.util.StringUtils.hasText;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

import com.asoroka.sidora.csvmetadata.CsvMetadataGenerator;

/**
 * @author ajs6f
 */
public class CsvMetadataGeneratorBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected Class<?> getBeanClass(final Element element) {
        return CsvMetadataGenerator.class;
    }

    @Override
    protected void doParse(final Element element, final BeanDefinitionBuilder builder) {
        builder.setAutowireMode(AUTOWIRE_BY_TYPE);
        final String scanLimit = element.getAttribute("scanLimit");
        if (hasText(scanLimit)) {
            builder.addPropertyValue("scanLimit", parseInt(scanLimit));
        }
        final String formatBean = element.getAttribute("format");
        if (hasText(formatBean)) {
            builder.addPropertyReference("format", formatBean);
        }
        final String strategyBean = element.getAttribute("strategy");
        if (hasText(strategyBean)) {
            builder.addPropertyReference("strategy", strategyBean);
        }
        final String headerStrategyBean = element.getAttribute("headerStrategy");
        if (hasText(headerStrategyBean)) {
            builder.addPropertyReference("headerStrategy", headerStrategyBean);
        }
    }

}
