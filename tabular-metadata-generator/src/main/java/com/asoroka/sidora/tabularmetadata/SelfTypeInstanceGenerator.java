
package com.asoroka.sidora.tabularmetadata;

import javax.inject.Provider;

/**
 * @author ajs6f
 */
public interface SelfTypeInstanceGenerator<SelfType extends SelfTypeInstanceGenerator<SelfType>> extends
        java.lang.Cloneable,
        Provider<SelfType> {

    /**
     * @return A new instance of this type of object.
     */
    public SelfType newInstance();

}
