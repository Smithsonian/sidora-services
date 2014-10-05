
package com.asoroka.sidora.csvmetadata;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import javax.inject.Qualifier;

/**
 * An injection {@link Qualifier} to mark configuration of a scanning limit.
 * 
 * @author ajs6f
 */
@Documented
@Retention(RUNTIME)
@Qualifier
public @interface ScanLimit {

}
