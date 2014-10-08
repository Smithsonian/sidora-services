
package com.asoroka.sidora.csvmetadata.spring;

import static java.lang.Integer.parseInt;
import static org.springframework.beans.factory.support.AbstractBeanDefinition.AUTOWIRE_BY_TYPE;
import static org.springframework.util.StringUtils.hasText;

import org.springframework.beans.factory.BeanDefinitionStoreException;
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
        final String format = element.getAttribute("format");
        if (hasText(format)) {
            final String implClass = getImplClassName(format, "com.asoroka.sidora.csvmetadata.formats.CsvFormat");
            builder.addPropertyValue("format", getImplClass(implClass));
        }
        final String strategy = element.getAttribute("strategy");
        if (hasText(strategy)) {
            final String implClass = getImplClassName(strategy, "com.asoroka.sidora.csvmetadata.heuristics");
            final BeanDefinitionBuilder strategyBean = BeanDefinitionBuilder.genericBeanDefinition(implClass);

            builder.addPropertyValue("strategy", strategyBean);
        }

        final String headerStrategy = element.getAttribute("headerStrategy");
        if (hasText(headerStrategy)) {
            final String implClass = getImplClassName(headerStrategy, "com.asoroka.sidora.csvmetadata.heuristics");
            builder.addPropertyValue("headerStrategy", getImplClass(implClass));
        }
    }

    private static String getImplClassName(final String name, final String defaultPackage) {
        if (name.contains(".")) {
            // assume a FQN
            return name;
        }
        return defaultPackage + "." + name;
    }

    private static Class<?> getImplClass(final String name) {
        try {
            return Class.forName(name);
        } catch (final ClassNotFoundException e) {
            throw new BeanDefinitionStoreException("Bad tabular data metadata generator XML configuration!", e);
        }
    }

}
