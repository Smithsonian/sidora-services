
package com.asoroka.sidora.tabularmetadata.spring.defaults;

import org.springframework.stereotype.Component;

import com.asoroka.sidora.tabularmetadata.formats.CsvFormat;

/**
 * Supplies {@link CsvFormat.Default} as the default format in Spring integrations.
 * 
 * @author ajs6f
 */
@Component
public class DefaultCsvFormat extends CsvFormat.Default {

}
