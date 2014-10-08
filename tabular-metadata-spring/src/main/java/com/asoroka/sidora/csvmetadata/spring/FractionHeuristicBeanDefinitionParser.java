
package com.asoroka.sidora.csvmetadata.spring;

import static java.lang.Float.parseFloat;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

import com.asoroka.sidora.csvmetadata.heuristics.FractionHeuristic;

/**
 * Responsible for constructing {@link FractionHeuristic} beans.
 * 
 * @author ajs6f
 */
public class FractionHeuristicBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected Class<?> getBeanClass(final Element element) {
        return FractionHeuristic.class;
    }

    @Override
    protected void doParse(final Element element, final BeanDefinitionBuilder builder) {
        final String fraction = element.getAttribute("fraction");
        builder.addConstructorArgValue(parseFloat(fraction));
    }
}
