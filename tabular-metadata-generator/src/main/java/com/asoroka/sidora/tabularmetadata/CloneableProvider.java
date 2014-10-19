
package com.asoroka.sidora.tabularmetadata;

import javax.inject.Provider;

/**
 * @author ajs6f
 */
public interface CloneableProvider<SelfType extends CloneableProvider<SelfType>> extends java.lang.Cloneable,
        Provider<SelfType> {

    /**
     * We override {@link Object#clone()} in order to narrow its return type for type-safety in the use of this
     * method.
     * 
     * @see java.lang.Object#clone()
     * @return A clone of this object.
     */
    public SelfType clone();

}
