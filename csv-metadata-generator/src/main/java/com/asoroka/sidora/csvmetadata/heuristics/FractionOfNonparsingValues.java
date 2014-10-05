/**
 * 
 */

package com.asoroka.sidora.csvmetadata.heuristics;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import javax.inject.Qualifier;

/**
 * Indicates that this value should be used for the fraction of nonparsing values admitted by
 * {@link FractionHeuristic}.
 * 
 * @author ajs6f
 */
@Documented
@Retention(RUNTIME)
@Qualifier
public @interface FractionOfNonparsingValues {

}
