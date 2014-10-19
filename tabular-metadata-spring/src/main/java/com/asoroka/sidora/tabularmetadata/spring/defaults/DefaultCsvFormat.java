
package com.asoroka.sidora.tabularmetadata.spring.defaults;

import org.springframework.stereotype.Component;

import com.asoroka.sidora.tabularmetadata.formats.TabularFormat;

/**
 * Supplies {@link TabularFormat.Default} as the default format in Spring integrations.
 * 
 * @author ajs6f
 */
@Component
public class DefaultCsvFormat extends TabularFormat.Default {
    // NO CONTENT
}
