
package com.asoroka.sidora.tabularmetadata;

import javax.inject.Provider;

/**
 * A class implementing this type must be able to produce a new instance of its concrete type <i>without
 * reflection</i>. The new instance should be essentially what would be produced by a default constructor, with any
 * configuration duplicated from the generating instance.
 * 
 * @author ajs6f
 */
public interface SelfTypeInstanceGenerator<SelfType extends SelfTypeInstanceGenerator<SelfType>> extends
        Provider<SelfType> {

    /**
     * @return A new instance of this type of object.
     */
    SelfType newInstance();

}
